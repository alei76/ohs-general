package ohs.ir.medical.clef.ehealth_2014;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import ohs.io.TextFileReader;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

public class DocumentScorer {

	private double dirichlet_prior = 1500;

	private double fb_mixture = 0.1;

	private int fb_word_size = 100;

	private int fb_doc_size = 10;

	private double abbr_portion = 0.15;

	private double trans_mixture = 0;

	private boolean useDocCentralities = false;

	private boolean useClustering = false;

	private boolean useDischargeSummary = false;

	private double[] rm_mixtures = { 100, 0 };

	private Indexer<String> wordIndexer;

	private MedicalEnglishAnalyzer analyzer;

	private SparseMatrix abbrTransMatrix;

	private List<IndexReader> indexReaders;

	private List<SparseVector> collWordCountData;

	private List<SparseVector> docFreqData;

	public DocumentScorer(List<IndexReader> indexReaders, MedicalEnglishAnalyzer analyzer) throws Exception {
		this.indexReaders = indexReaders;

		this.analyzer = analyzer;

		prepareCollectionWordCounts();

		abbrTransMatrix = computeAbbreviationTranslationMatrix();
	}

	private SparseMatrix computeAbbreviationTranslationMatrix() throws Exception {
		CounterMap<Integer, Integer> counterMap = new CounterMap<Integer, Integer>();

		TextFileReader reader = new TextFileReader(new File(EHPath.ABBREVIATION_FILTERED_FILE));
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String shortForm = lines.get(0).toLowerCase();
			int sf = wordIndexer.indexOf(shortForm);

			if (sf < 0) {
				continue;
			}

			for (int i = 1; i < lines.size(); i++) {
				String[] parts = lines.get(i).split("\t");
				String longForm = parts[0];

				if (longForm.toLowerCase().contains(shortForm.toLowerCase())) {
					continue;
				}

				double count = Double.parseDouble(parts[1]);

				TokenStream ts = analyzer.tokenStream(CommonFieldNames.CONTENT, longForm);
				CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
				ts.reset();

				Counter<Integer> counter = new Counter<Integer>();

				while (ts.incrementToken()) {
					String word = attr.toString();
					int w = wordIndexer.indexOf(word);
					if (w < 0) {
						continue;
					}
					counter.incrementCount(w, count);
				}
				ts.end();
				ts.close();

				if (counter.size() > 0) {
					counterMap.setCounter(sf, counter);
				}
			}
		}
		reader.close();

		SparseMatrix ret = VectorUtils.toSpasreMatrix(counterMap);
		// ret.normalizeColumns();
		ret.normalizeRows();
		return ret;
	}

	private SparseVector computeClusterScores(SparseVector queryModel, List<SparseVector> clusterWordCounts,
			SparseVector collWordCounts) {
		SparseVector ret = new SparseVector(clusterWordCounts.size());

		for (int i = 0; i < queryModel.size(); i++) {
			int w = queryModel.indexAtLoc(i);
			double prob_w_in_query = queryModel.probAtLoc(i);
			double prob_w_in_collection = collWordCounts.probAlways(w);

			for (int j = 0; j < clusterWordCounts.size(); j++) {
				SparseVector cwc = clusterWordCounts.get(j);

				double count_w_in_cluster = cwc.valueAlways(w);
				double count_sum_in_cluster = cwc.sum();
				double prob_w_in_cluster = (count_w_in_cluster + dirichlet_prior * prob_w_in_collection)
						/ (count_sum_in_cluster + dirichlet_prior);

				double prob_w_in_tr_doc = 0;
				prob_w_in_cluster = (1 - trans_mixture) * prob_w_in_cluster + trans_mixture * prob_w_in_tr_doc;

				if (prob_w_in_cluster > 0) {
					double div = prob_w_in_query * Math.log(prob_w_in_query / prob_w_in_cluster);
					ret.incrementAtLoc(j, j, div);
				}
			}
		}

		for (int i = 0; i < ret.size(); i++) {
			double divSum = ret.valueAtLoc(i);
			double approx_prob = Math.exp(-divSum);
			ret.setAtLoc(i, approx_prob);
		}
		ret.normalizeAfterSummation();
		return ret;
	}

	private SparseVector computeDocumentScores(SparseVector queryModel, SparseMatrix docWordCounts,
			SparseVector collWordCounts, SparseMatrix transModels) {
		SparseVector ret = new SparseVector(docWordCounts.rowSize());

		for (int i = 0; i < queryModel.size(); i++) {
			int w = queryModel.indexAtLoc(i);
			double prob_w_in_query = queryModel.probAtLoc(i);
			double prob_w_in_collection = collWordCounts.probAlways(w);

			SparseVector transModel = transModels.rowAlways(w);

			for (int j = 0; j < docWordCounts.rowSize(); j++) {
				int docId = docWordCounts.indexAtLoc(j);
				SparseVector dwc = docWordCounts.rowAtLoc(j);

				double count_w_in_doc = dwc.valueAlways(w);
				double count_sum_in_doc = dwc.sum();
				double prob_w_in_doc = (count_w_in_doc + dirichlet_prior * prob_w_in_collection)
						/ (count_sum_in_doc + dirichlet_prior);

				double prob_w_in_tr_doc = 0;
				for (int k = 0; k < transModel.size(); k++) {
					int t = transModel.indexAtLoc(k);
					if (w == t) {
						continue;
					}
					double prob_w_from_t = transModel.valueAlways(t);
					double prob_t_in_doc = dwc.probAlways(t);
					// double prob_t_in_collection =
					// collWordCounts.probAlways(t);
					// double count_t_in_doc = dwc.valueAlways(t);
					// double prob_t_in_doc = (count_t_in_doc + dirichlet_prior
					// * prob_t_in_collection)
					// / (count_sum_in_doc + dirichlet_prior);
					prob_w_in_tr_doc += prob_w_from_t * prob_t_in_doc;
				}
				prob_w_in_doc = (1 - trans_mixture) * prob_w_in_doc + trans_mixture * prob_w_in_tr_doc;

				if (prob_w_in_doc > 0) {
					double div = prob_w_in_query * Math.log(prob_w_in_query / prob_w_in_doc);
					ret.incrementAtLoc(j, docId, div);
				}
			}
		}

		for (int i = 0; i < ret.size(); i++) {
			double divSum = ret.valueAtLoc(i);
			double approx_prob = Math.exp(-divSum);
			ret.setAtLoc(i, approx_prob);
		}
		ret.normalizeAfterSummation();
		return ret;
	}

	private SparseVector computeRelevanceModel(SparseVector docScores, SparseMatrix docWordCounts,
			SparseVector collWordCounts, SparseVector docPriors) {

		Set<Integer> fbWords = new HashSet<Integer>();

		docScores.sortByValue();

		for (int i = 0; i < docScores.size() && i < fb_doc_size; i++) {
			int docId = docScores.indexAtLoc(i);
			SparseVector sv = docWordCounts.rowAlways(docId);
			for (int w : sv.indexes()) {
				fbWords.add(w);
			}
		}

		Counter<Integer> ret = new Counter<Integer>();

		for (int w : fbWords) {
			double prob_w_in_collection = collWordCounts.probAlways(w);

			for (int i = 0; i < docScores.size() && i < fb_doc_size; i++) {
				int docId = docScores.indexAtLoc(i);
				SparseVector dwc = docWordCounts.rowAlways(docId);
				double count_w_in_doc = dwc.valueAlways(w);
				double count_sum_in_doc = dwc.sum();
				double prob_w_in_doc = (count_w_in_doc + dirichlet_prior * prob_w_in_collection)
						/ (count_sum_in_doc + dirichlet_prior);
				double document_weight = docScores.valueAtLoc(i);
				double document_prior = docPriors.valueAlways(docId);
				double prob_w_in_fb_model = document_weight * prob_w_in_doc;

				if (document_prior > 0) {
					prob_w_in_fb_model *= document_prior;
				}

				// double prob_w_from_all_t = 0;
				// Counter<Integer> trwps = transMatrix.getCounter(w);
				//
				// for (int t : trwps.keySet()) {
				// if (w == t) {
				// continue;
				// }
				// double prob_t_to_w = trwps.getCount(t);
				// double count_t_in_doc = dwc.getCount(t);
				// double prob_t_in_collection =
				// clefCollWordCounts.getProbability(t);
				// double prob_t_in_doc = (count_t_in_doc + dirichlet_prior *
				// prob_t_in_collection) / (count_sum_in_doc + dirichlet_prior);
				// prob_w_from_all_t += prob_t_to_w * prob_t_in_doc;
				// }
				//
				// if (prob_w_from_all_t > 0) {
				// prob_w_in_fb_model *= prob_w_from_all_t;
				// }

				if (prob_w_in_fb_model > 0) {
					ret.incrementCount(w, prob_w_in_fb_model);
				}
			}
		}

		docScores.sortByIndex();

		ret.keepTopNKeys(fb_word_size);
		ret.normalize();

		System.out.printf("RM:\t%s\n", VectorUtils.toCounter(ret, wordIndexer));
		return VectorUtils.toSparseVector(ret);
	}

	private SparseVector computeRelevanceModelMixture(List<SparseVector> indexScoreData,
			List<SparseMatrix> docWordCountData, List<SparseVector> docPriorData) {
		List<SparseVector> relevanceModels = new ArrayList<SparseVector>();

		for (int i = 0; i < indexScoreData.size(); i++) {
			SparseVector relevanceModel = computeRelevanceModel(

					indexScoreData.get(i), docWordCountData.get(i), collWordCountData.get(i), docPriorData.get(i));

			relevanceModels.add(relevanceModel);
		}

		Counter<Integer> ret = new Counter<Integer>();

		ArrayMath.normalize(rm_mixtures);

		for (int i = 0; i < relevanceModels.size(); i++) {
			SparseVector relevanceModel = relevanceModels.get(i);

			for (int j = 0; j < relevanceModel.size(); j++) {
				int w = relevanceModel.indexAtLoc(j);
				double prob_in_rm = relevanceModel.valueAtLoc(j);
				double prob = rm_mixtures[i] * prob_in_rm;

				if (prob > 0) {
					ret.incrementCount(w, prob);
				}
			}
		}
		ret.keepTopNKeys(fb_word_size);
		ret.normalize();

		System.out.printf("RMM:\t%s\n", VectorUtils.toCounter(ret, wordIndexer));
		return VectorUtils.toSparseVector(ret);
	}

	private CounterMap<Integer, Integer> computeTranslatedDocumentModels(SparseMatrix docWordCounts,
			SparseVector collWordCounts, SparseMatrix transMatrix) {

		CounterMap<Integer, Integer> ret = new CounterMap<Integer, Integer>();

		for (int i = 0; i < docWordCounts.rowSize(); i++) {
			int docId = docWordCounts.indexAtLoc(i);
			SparseVector dwc = docWordCounts.rowAtLoc(i);
			Counter<Integer> trwp = new Counter<Integer>();

			for (int j = 0; j < dwc.size(); j++) {
				int w = dwc.indexAtLoc(j);
				double prob_w_from_all_t = 0;
				SparseVector transModel = transMatrix.rowAlways(w);

				if (transModel == null) {
					continue;
				}

				for (int k = 0; k < transModel.size(); k++) {
					int t = transModel.indexAtLoc(k);

					if (w == t) {
						continue;
					}

					double prob_t_to_w = transModel.probAtLoc(k);
					double count_t_in_doc = dwc.valueAlways(t);
					double count_sum_in_doc = dwc.sum();
					double prob_t_in_collection = collWordCounts.probAlways(t);
					double prob_t_in_doc = (count_t_in_doc + dirichlet_prior * prob_t_in_collection)
							/ (count_sum_in_doc + dirichlet_prior);
					prob_w_from_all_t += prob_t_to_w * prob_t_in_doc;
				}

				if (prob_w_from_all_t > 0) {
					trwp.setCount(w, prob_w_from_all_t);
				}

				trwp.normalize();
			}
			ret.setCounter(docId, trwp);
		}
		return ret;
	}

	private SparseVector computeWordCentralities(SparseMatrix co) {
		Indexer<Integer> newWordIndexer = new Indexer<Integer>();

		for (int i = 0; i < co.rowSize(); i++) {
			int w1 = co.indexAtLoc(i);
			newWordIndexer.add(w1);
			for (int w2 : co.rowAtLoc(i).indexes()) {
				newWordIndexer.add(w2);
			}
		}

		int max_words = newWordIndexer.size();

		double[][] transMat = ArrayUtils.matrix(max_words, 0);

		for (int i = 0; i < co.rowSize(); i++) {
			int w1 = co.indexAtLoc(i);
			int nw1 = newWordIndexer.indexOf(w1);

			SparseVector sv = co.rowAtLoc(i);
			for (int j = i + 1; j < sv.size(); j++) {
				int w2 = sv.indexAtLoc(j);
				int nw2 = newWordIndexer.indexOf(w2);
				double value = sv.valueAtLoc(j);

				transMat[nw1][nw2] = value;
				transMat[nw2][nw1] = value;
			}
		}

		ArrayMath.normalizeColumns(transMat);

		double[] centralities = new double[transMat.length];

		ArrayMath.randomWalk(transMat, centralities, 100, 0.000001, 0.85);

		SparseVector ret = new SparseVector(max_words);

		for (int i = 0; i < centralities.length; i++) {
			int w = newWordIndexer.getObject(i);
			double value = centralities[i];
			ret.setAtLoc(i, w, value);
		}
		ret.summation();

		return ret;
	}

	private SparseMatrix computeWordCooccurrences(List<List<String>> dischargeWords, int windowSize) {
		CounterMap<Integer, Integer> temp = new CounterMap<Integer, Integer>();

		for (int i = 0; i < dischargeWords.size(); i++) {
			List<String> words = dischargeWords.get(i);

			int[] ws = new int[words.size()];

			for (int j = 0; j < words.size(); j++) {
				String word = words.get(j);
				int w = wordIndexer.indexOf(word);
				ws[j] = w;
			}

			for (int j = 0; j < ws.length; j++) {
				int w1 = ws[j];
				if (w1 < 0) {
					continue;
				}

				for (int k = j + 1; k < ws.length; k++) {
					int w2 = ws[k];

					if (w2 < 0 || w1 == w2) {
						continue;
					}

					int dist = k - j;

					if (dist > windowSize) {
						break;
					}

					// if (abbrTransMatrix.getCounter(w1).getCount(w2) == 0 ||
					// abbrTransMatrix.getCounter(w2).getCount(w1) == 0) {
					// continue;
					// }

					double hal = CountPropagation.hal(j, k, windowSize);
					temp.incrementCount(w1, w2, hal);
				}
			}
		}

		CounterMap<Integer, Integer> ret = new CounterMap<Integer, Integer>();

		int doc_size_in_collection = indexReaders.get(0).maxDoc();

		for (int w1 : temp.keySet()) {
			Counter<Integer> counter = temp.getCounter(w1);

			double doc_freq_of_w1 = docFreqData.get(0).valueAlways(w1);
			double idf_w1 = doc_freq_of_w1 == 0 ? 0 : Math.log((doc_size_in_collection + 1) / doc_freq_of_w1);

			for (int w2 : counter.keySet()) {
				double doc_freq_of_w2 = docFreqData.get(0).valueAlways(w2);
				double idf_w2 = doc_freq_of_w1 == 0 ? 0 : Math.log((doc_size_in_collection + 1) / doc_freq_of_w2);
				double hal = counter.getCount(w2);
				double weighted_hal = hal * idf_w1 * idf_w2;
				ret.setCount(w1, w2, weighted_hal);
				ret.setCount(w2, w1, weighted_hal);
			}
		}

		return VectorUtils.toSpasreMatrix(ret);
	}

	private SparseMatrix computeWordTranslationModels(SparseVector indexScores) throws Exception {

		CounterMap<Integer, Integer> ret = new CounterMap<Integer, Integer>();

		indexScores.sortByValue();

		Set<Integer> wordSet = new TreeSet<Integer>();

		for (int i = 0; i < indexScores.size() && i < fb_doc_size; i++) {
			int indexId = indexScores.indexAtLoc(i);
			double score = indexScores.valueAtLoc(i);

			Terms termVector = indexReaders.get(0).getTermVector(indexId, CommonFieldNames.CONTENT);

			TermsEnum reuse = null;
			TermsEnum iterator = termVector.iterator();
			BytesRef ref = null;
			DocsAndPositionsEnum docsAndPositions = null;

			Map<Integer, Integer> pos_word = new TreeMap<Integer, Integer>();

			while ((ref = iterator.next()) != null) {
				docsAndPositions = iterator.docsAndPositions(null, docsAndPositions);
				if (docsAndPositions.nextDoc() != 0) {
					throw new AssertionError();
				}
				String word = ref.utf8ToString();
				int w = wordIndexer.indexOf(word);

				if (w < 0) {
					continue;
				}

				int count = docsAndPositions.freq();

				for (int j = 0; j < count; ++j) {
					final int position = docsAndPositions.nextPosition();
					pos_word.put(position, w);
				}
			}

			List<Integer> words = new ArrayList<Integer>(pos_word.values());
			List<Integer> poss = new ArrayList<Integer>(pos_word.keySet());

			int windowSize = 4;

			CounterMap<Integer, Integer> co = new CounterMap<Integer, Integer>();

			for (int j = 0; j < words.size(); j++) {
				int w1 = words.get(j);
				int pos1 = poss.get(j);

				for (int k = j + 1; k < words.size(); k++) {
					int w2 = words.get(k);

					if (w1 == w2) {
						continue;
					}

					int pos2 = poss.get(k);
					int dist = pos2 - pos1;

					if (dist > windowSize) {
						break;
					}

					// if (abbrTransMatrix.getCounter(w1).getCount(w2) == 0 ||
					// abbrTransMatrix.getCounter(w2).getCount(w1) == 0) {
					// continue;
					// }

					double hal = CountPropagation.hal(pos1, pos2, windowSize);
					double association = hal;
					// System.out.printf("[%d, %d = %s]\n", pos1, pos2,
					// gaussian);
					co.incrementCount(w1, w2, association);
					co.incrementCount(w2, w1, association);

					wordSet.add(w1);
					wordSet.add(w2);
				}
			}

			// for (int w1 : co.keySet()) {
			// Counter<Integer> counter = co.getCounter(w1);
			// double prob_w1_in_coll = collWordCountData.get(0).probAlways(w1);
			// for (int w2 : counter.keySet()) {
			// double prob_w2_in_coll = collWordCountData.get(0).probAlways(w2);
			// double hal = counter.getCount(w2);
			// double association = prob_w1_in_coll * prob_w2_in_coll * hal *
			// score;
			// ret.incrementCount(w1, w2, association);
			// }
			// }

			ret.incrementAll(co);
		}
		// ret.normalize();

		System.out.println(VectorUtils.toCounterMap(ret, wordIndexer, wordIndexer));
		System.out.println();

		// ret = ret.invert();

		indexScores.sortByIndex();

		Indexer<Integer> newWordIndexer = new Indexer<Integer>();

		for (int w : wordSet) {
			int nw = newWordIndexer.getIndex(w);
		}

		int numNewWords = newWordIndexer.size();
		double[][] m = ArrayUtils.matrix(numNewWords, 0);

		for (int w1 : ret.keySet()) {
			int nw1 = newWordIndexer.indexOf(w1);
			Counter<Integer> counter = ret.getCounter(w1);
			for (int w2 : counter.keySet()) {
				int nw2 = newWordIndexer.indexOf(w2);
				double count = counter.getCount(w2);
				m[nw1][nw2] = count;
			}
		}

		// int numRows = m.length;
		// int nomCols = m[0].length;
		// boolean sparse = true;
		// NumberFormat nf = NumberFormat.getInstance();
		// nf.setGroupingUsed(false);
		//
		// System.out.println(ArrayUtils.toString(m, numRows, nomCols, sparse,
		// nf));

		ArrayMath.normalizeColumns(m);

		double[] importances = new double[m.length];

		ArrayMath.randomWalk(m, importances, 10, 0.00001, 0.85);

		Counter<String> wordImportances = new Counter<String>();

		for (int i = 0; i < importances.length; i++) {
			double importance = importances[i];
			int w = newWordIndexer.getObject(i);
			String word = wordIndexer.getObject(w);
			wordImportances.setCount(word, importance);
		}

		System.out.println(wordImportances);
		System.out.println();

		return VectorUtils.toSpasreMatrix(ret);
	}

	private SparseVector expandModel(SparseVector targetModel, SparseMatrix abbrTransMatrix) {
		Counter<Integer> newModel = new Counter<Integer>();

		for (int i = 0; i < targetModel.size(); i++) {
			int w = targetModel.indexAtLoc(i);
			double prob_w_in_query = targetModel.valueAtLoc(i);
			SparseVector trwp = abbrTransMatrix.rowAlways(w);

			if (trwp.size() > 0) {
				double discounted_prob_w_in_query = (1 - abbr_portion) * prob_w_in_query;
				double remain_mass = abbr_portion * prob_w_in_query;
				Counter<Integer> counter = new Counter<Integer>();
				for (int j = 0; j < trwp.size(); j++) {
					int t = trwp.indexAtLoc(j);
					double prob_t = trwp.valueAtLoc(j);
					double prob_t_by_distribution = remain_mass * prob_t;
					counter.incrementCount(t, prob_t_by_distribution);
				}
				counter.incrementCount(w, discounted_prob_w_in_query);
				newModel.incrementAll(counter);
			} else {
				newModel.incrementCount(w, prob_w_in_query);
			}
		}
		newModel.normalize();

		SparseVector ret = VectorUtils.toSparseVector(newModel);

		System.out.printf("Expand Model:\t%s\n", VectorUtils.toCounter(newModel, wordIndexer));
		return ret;
	}

	private Counter<String> getCounter(List<String> words) {
		Counter<String> ret = new Counter<String>();
		for (String word : words) {
			ret.incrementCount(word, 1);
		}
		return ret;
	}

	private List<SparseMatrix> getDocumentWordCounts(List<SparseVector> indexScoreData) throws Exception {
		List<SparseMatrix> ret = new ArrayList<SparseMatrix>();

		for (int i = 0; i < indexScoreData.size(); i++) {
			SparseVector indexScores = indexScoreData.get(i);
			CounterMap<Integer, Integer> docWordCounts = new CounterMap<Integer, Integer>();

			Set<Integer> toRemove = new TreeSet<Integer>();

			for (int j = 0; j < indexScores.size(); j++) {
				int indexId = indexScores.indexAtLoc(j);
				Terms termVector = indexReaders.get(i).getTermVector(indexId, CommonFieldNames.CONTENT);

				if (termVector == null) {
					toRemove.add(indexId);
					continue;
				}

				TermsEnum reuse = null;
				TermsEnum iterator = termVector.iterator();
				BytesRef ref = null;
				DocsAndPositionsEnum docsAndPositions = null;
				Counter<Integer> counter = new Counter<Integer>();

				while ((ref = iterator.next()) != null) {
					docsAndPositions = iterator.docsAndPositions(null, docsAndPositions);
					if (docsAndPositions.nextDoc() != 0) {
						throw new AssertionError();
					}
					String word = ref.utf8ToString();
					int w = wordIndexer.indexOf(word);

					if (w < 0) {
						continue;
					}

					int freq = docsAndPositions.freq();

					counter.incrementCount(w, freq);

					// for (int j = 0; j < freq; ++j) {
					// final int position = docsAndPositions.nextPosition();
					// }
				}

				docWordCounts.setCounter(indexId, counter);
			}

			indexScores.prune(toRemove);

			SparseMatrix sm = VectorUtils.toSpasreMatrix(docWordCounts);

			ret.add(sm);
		}

		return ret;
	}

	private SparseVector getModel(Counter<String> wordCounts) {
		SparseVector ret = VectorUtils.toSparseVector(wordCounts, wordIndexer);
		ret.normalize();
		System.out.printf("Model:\t%s\n", VectorUtils.toCounter(ret, wordIndexer));
		return ret;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	private void prepareCollectionWordCounts() throws Exception {
		collWordCountData = new ArrayList<SparseVector>();
		docFreqData = new ArrayList<SparseVector>();
		wordIndexer = new Indexer<String>();

		for (int i = 0; i < indexReaders.size(); i++) {
			IndexReader indexReader = indexReaders.get(i);

			Counter<Integer> wordCounts = new Counter<Integer>();
			Counter<Integer> docFreqs = new Counter<Integer>();

			Fields fields = MultiFields.getFields(indexReader);
			Terms terms = fields.terms(CommonFieldNames.CONTENT);

			TermsEnum iterator = terms.iterator();
			BytesRef byteRef = null;

			while ((byteRef = iterator.next()) != null) {
				String word = byteRef.utf8ToString();
				int docFreq = iterator.docFreq();
				double count = iterator.totalTermFreq();
				int w = wordIndexer.getIndex(word);
				wordCounts.incrementCount(w, count);
				docFreqs.setCount(w, docFreq);
			}

			SparseVector sv1 = VectorUtils.toSparseVector(wordCounts);
			SparseVector sv2 = VectorUtils.toSparseVector(docFreqs);

			collWordCountData.add(sv1);
			docFreqData.add(sv2);

			System.out.printf("vocabulary size:\t%d\n", sv1.size());
		}
		System.out.printf("Entire vocabulary size:\t%d\n", wordIndexer.size());
	}

	public Counter<Integer> score(List<String> queryWords, List<SparseVector> docScoreData,
			List<List<String>> dischargeWords) throws Exception {

		SparseVector queryModel = getModel(getCounter(queryWords));

		if (abbr_portion > 0) {
			queryModel = expandModel(queryModel, abbrTransMatrix);
		}

		if (useDischargeSummary) {
			SparseVector wordCentralities = computeWordCentralities(computeWordCooccurrences(dischargeWords, 3));
			Counter<String> counter = new Counter<String>();

			for (List<String> words : dischargeWords) {
				for (String word : words) {
					counter.incrementCount(word, 1);
				}
			}

			// SparseVector dsModel = VectorUtils.toSparseVector(counter,
			// wordIndexer);
			// dsModel.normalize();
			//
			// System.out.println(VectorUtils.toCounter(dsModel, wordIndexer));
			// System.out.println(VectorUtils.toCounter(wordCentralities,
			// wordIndexer));
			//
			// ParsimoniousEstimator parEst = new
			// ParsimoniousEstimator(wordIndexer, collWordCountData.get(0), 50,
			// 0.5);
			// SparseVector dsModel2 =
			// parEst.estimate(VectorUtils.toSparseVector(counter,
			// wordIndexer));
			//
			// System.out.println(VectorUtils.toCounter(wordCentralities,
			// wordIndexer));
			// System.out.println(VectorUtils.toCounter(dsModel, wordIndexer));
			// System.out.println(VectorUtils.toCounter(dsModel2, wordIndexer));

			queryModel = updateModel(queryModel, wordCentralities, 0.1);
		}

		List<SparseMatrix> docWordCountData = getDocumentWordCounts(docScoreData);

		if (useClustering) {
			DocumentClusterer docClusterer = new DocumentClusterer(

					docScoreData.get(0), docWordCountData.get(0), collWordCountData.get(0), docFreqData.get(0),
					indexReaders.get(0).maxDoc(), docScoreData.get(0).size(), 0.9, wordIndexer);

			docClusterer.doClustering();

			SparseVector clusterScores = computeClusterScores(queryModel, docClusterer.getClusterWordCountData(),
					collWordCountData.get(0));

			clusterScores.sortByValue();

			Set<Integer> toKeep = new TreeSet<Integer>();

			SparseVector docScores = docScoreData.get(0);

			// System.out.println(docScores.toString());

			for (int i = 0; i < clusterScores.size(); i++) {
				int cId = clusterScores.indexAtLoc(i);
				double cluster_score = clusterScores.valueAtLoc(i);
				SparseVector clusterWordCounts = docClusterer.getClusterWordCountData().get(i);
				Set<Integer> docIds = docClusterer.getClusterDocumentMap().get(cId);

				// System.out.printf("%d, %d, %s, %s\n", cId, docIds.size(),
				// cluster_score,
				// VectorUtils.toCounter(clusterWordCounts, wordIndexer));

				for (int docId : docIds) {
					int loc = docScores.location(docId);
					double doc_score = docScores.valueAtLoc(loc);
					double new_doc_score = cluster_score * doc_score;
					docScores.setAtLoc(loc, new_doc_score);
				}
			}
			docScores.normalizeAfterSummation();

			System.out.println(docScores.toString());
			System.out.println();
		}

		List<SparseVector> docPriorData = new ArrayList<SparseVector>();

		SparseVector docPriors = new SparseVector(docScoreData.get(0).copyIndexes());
		docPriors.setAll(1f / docPriors.size());
		docPriorData.add(docPriors);

		if (useDocCentralities) {
			DocumentCentralityEstimator centEstimator = new DocumentCentralityEstimator(collWordCountData.get(0));
			SparseVector docCentralities = centEstimator.estimate(docWordCountData.get(0));
			// docPriors.setValues(docCentralities.copyValues());
			// docPriors.normalizeAfterSummation();

			SparseVector docScores = docScoreData.get(0);
			ArrayMath.multiply(docScores.values(), docCentralities.values(), docScores.values());
			docScores.normalizeAfterSummation();
		}

		if (fb_mixture > 0) {
			SparseVector relevanceModel = computeRelevanceModelMixture(docScoreData, docWordCountData, docPriorData);
			queryModel = updateModel(queryModel, relevanceModel, fb_mixture);
		}

		SparseMatrix transModels = new SparseMatrix(new HashMap<Integer, SparseVector>());

		if (trans_mixture > 0) {
			transModels = computeWordTranslationModels(docScoreData.get(0));
		}

		SparseVector newDocScores = computeDocumentScores(queryModel, docWordCountData.get(0), collWordCountData.get(0),
				transModels);

		return VectorUtils.toCounter(newDocScores);
	}

	// private SparseVector expandQueryModel(SparseVector queryModel,
	// SparseMatrix abbrTransMatrix) {
	// Counter<Integer> newQM = new Counter<Integer>();
	//
	// for (int i = 0; i < queryModel.size(); i++) {
	// int w = queryModel.indexAtLoc(i);
	// double prob_w_in_query = queryModel.valueAtLoc(i);
	// SparseVector trwp = abbrTransMatrix.rowAlways(w);
	//
	// if (trwp.size() > 0) {
	// double discounted_prob_w_in_query = (1 - abbr_portion) * prob_w_in_query;
	// double remain_mass = abbr_portion * prob_w_in_query;
	// Counter<Integer> counter = new Counter<Integer>();
	// for (int j = 0; j < trwp.size(); j++) {
	// int t = trwp.indexAtLoc(j);
	// double prob_t = trwp.valueAtLoc(j);
	// double prob_t_by_distribution = remain_mass * prob_t;
	// counter.incrementCount(t, prob_t_by_distribution);
	// }
	// counter.incrementCount(w, discounted_prob_w_in_query);
	// newQM.incrementAll(counter);
	// } else {
	// newQM.incrementCount(w, prob_w_in_query);
	// }
	// }
	// newQM.normalize();
	//
	// SparseVector ret = VectorUtils.toSparseVector(newQM);
	//
	// System.out.printf("QM-2:\t%s\n", VectorUtils.toCounter(newQM,
	// wordIndexer));
	// return ret;
	// }

	public void setAbbrPortion(double abbr_portion) {
		this.abbr_portion = abbr_portion;
	}

	public void setDirichletPrior(double dirichlet_prior) {
		this.dirichlet_prior = dirichlet_prior;
	}

	public void setFbDocSize(int fb_doc_size) {
		this.fb_doc_size = fb_doc_size;
	}

	public void setFbMixture(double fb_mixture) {
		this.fb_mixture = fb_mixture;
	}

	public void setFbWordSize(int fb_word_size) {
		this.fb_word_size = fb_word_size;
	}

	public void setRMmixtures(double[] rm_mixtures) {
		this.rm_mixtures = rm_mixtures;
	}

	public void setTransMixture(double trans_mixture) {
		this.trans_mixture = trans_mixture;
	}

	public void setUseClustering(boolean useClustering) {
		this.useClustering = useClustering;
	}

	public void setUseDischargeSummary(boolean useDischargeSummary) {
		this.useDischargeSummary = useDischargeSummary;
	}

	public void setUseDocCentralities(boolean useDocCentralities) {
		this.useDocCentralities = useDocCentralities;
	}

	private SparseVector updateModel(SparseVector targetModel, SparseVector otherModel, double other_mixture) {
		Counter<Integer> ret = new Counter<Integer>();

		for (int i = 0; i < targetModel.size(); i++) {
			int w = targetModel.indexAtLoc(i);
			double prob = targetModel.probAtLoc(i);
			ret.incrementCount(w, (1 - other_mixture) * prob);
		}
		for (int i = 0; i < otherModel.size(); i++) {
			int w = otherModel.indexAtLoc(i);
			double prob = otherModel.probAtLoc(i);
			ret.incrementCount(w, other_mixture * prob);
		}
		ret.normalize();
		System.out.printf("Update Model:\t%s\n\n", VectorUtils.toCounter(ret, wordIndexer));
		return VectorUtils.toSparseVector(ret);
	}
}
