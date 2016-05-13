package ohs.ir.medical.trec.cds_2014;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

public class PRFDocumentScorer {

	private double dirichlet_prior = 1500;

	private double fb_mixture = 0.1;

	private int fb_word_size = 100;

	private int fb_doc_size = 10;

	private Indexer<String> wordIndexer;

	private MedicalEnglishAnalyzer analyzer;

	private IndexReader indexReader;

	private SparseVector collWordCounts;

	private SparseVector docFreqData;

	private SparseMatrix abbrTransModel;

	private double abbr_portion = 0.15;

	public PRFDocumentScorer(IndexReader indexReader, MedicalEnglishAnalyzer analyzer, VocabularyData vocData, SparseMatrix abbrTransModel)
			throws Exception {
		this.indexReader = indexReader;
		this.analyzer = analyzer;
		this.abbrTransModel = abbrTransModel;

		collWordCounts = vocData.getCollectionWordCountData();
		docFreqData = vocData.getDocumentFrequencies();
		wordIndexer = vocData.getWordIndexer();
	}

	private SparseVector computeDocumentScores(SparseVector queryModel, SparseMatrix docWordCounts) {
		SparseVector ret = new SparseVector(docWordCounts.rowSize());

		for (int i = 0; i < queryModel.size(); i++) {
			int w = queryModel.indexAtLoc(i);
			double prob_w_in_query = queryModel.probAtLoc(i);
			double prob_w_in_collection = collWordCounts.probAlways(w);

			for (int j = 0; j < docWordCounts.rowSize(); j++) {
				int docId = docWordCounts.indexAtLoc(j);
				SparseVector dwc = docWordCounts.rowAtLoc(j);

				double count_w_in_doc = dwc.valueAlways(w);
				double count_sum_in_doc = dwc.sum();
				double prob_w_in_doc = (count_w_in_doc + dirichlet_prior * prob_w_in_collection) / (count_sum_in_doc + dirichlet_prior);

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

	private SparseVector computeRelevanceModel(SparseVector docScores, SparseMatrix docWordCountsData, SparseVector docPriors) {

		Set<Integer> fbWords = new HashSet<Integer>();

		docScores.sortByValue();

		for (int i = 0; i < docScores.size() && i < fb_doc_size; i++) {
			int docId = docScores.indexAtLoc(i);
			SparseVector sv = docWordCountsData.rowAlways(docId);
			for (int w : sv.indexes()) {
				fbWords.add(w);
			}
		}

		Counter<Integer> ret = new Counter<Integer>();

		for (int w : fbWords) {
			double prob_w_in_collection = collWordCounts.probAlways(w);

			for (int i = 0; i < docScores.size() && i < fb_doc_size; i++) {
				int docId = docScores.indexAtLoc(i);
				SparseVector dwc = docWordCountsData.rowAlways(docId);
				double count_w_in_doc = dwc.valueAlways(w);
				double count_sum_in_doc = dwc.sum();
				double prob_w_in_doc = (count_w_in_doc + dirichlet_prior * prob_w_in_collection) / (count_sum_in_doc + dirichlet_prior);
				double document_weight = docScores.valueAtLoc(i);
				double document_prior = docPriors.valueAlways(docId);
				double prob_w_in_fb_model = document_weight * prob_w_in_doc;

				if (document_prior > 0) {
					prob_w_in_fb_model *= document_prior;
				}

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

	private SparseVector expandModel(SparseVector queryModel, SparseMatrix abbrTransMatrix) {
		Counter<Integer> newModel = new Counter<Integer>();

		for (int i = 0; i < queryModel.size(); i++) {
			int w = queryModel.indexAtLoc(i);
			double prob_w_in_query = queryModel.valueAtLoc(i);
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
		ret.setDim(wordIndexer.size());

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

	public SparseMatrix getDocumentWordCounts(SparseVector indexScores) throws Exception {

		CounterMap<Integer, Integer> docWordCounts = new CounterMap<Integer, Integer>();

		Set<Integer> toRemove = new TreeSet<Integer>();

		for (int j = 0; j < indexScores.size(); j++) {
			int indexId = indexScores.indexAtLoc(j);
			Terms termVector = indexReader.getTermVector(indexId, CommonFieldNames.CONTENT);

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

		SparseMatrix sm = VectorUtils.toSpasreMatrix(docWordCounts, indexReader.maxDoc(), wordIndexer.size(), -1);
		return sm;
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

	public Counter<Integer> score(List<String> queryWords, SparseVector docScores) throws Exception {
		SparseVector queryModel = getModel(getCounter(queryWords));

		queryModel = expandModel(queryModel, abbrTransModel);

		SparseMatrix docWordCountsData = getDocumentWordCounts(docScores);

		SparseVector docPriors = new SparseVector(docScores.copyIndexes());
		docPriors.setAll(1f / docPriors.size());

		if (fb_mixture > 0) {
			SparseVector relevanceModel = computeRelevanceModel(docScores, docWordCountsData, docPriors);
			queryModel = updateModel(queryModel, relevanceModel, fb_mixture);
		}

		SparseVector newDocScores = computeDocumentScores(queryModel, docWordCountsData);

		return VectorUtils.toCounter(newDocScores);
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
