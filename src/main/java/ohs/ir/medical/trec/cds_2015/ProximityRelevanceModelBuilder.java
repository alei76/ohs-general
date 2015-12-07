package ohs.ir.medical.trec.cds_2015;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohs.ir.medical.general.WordCountBox;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.DeepListMap;
import ohs.types.Indexer;
import ohs.types.ListMap;

public class ProximityRelevanceModelBuilder {

	public static double hal(int i, int j, int window_size) {
		return window_size - (j - i) + 1;
	}

	private DeepListMap<Integer, Integer, Integer> docWordLocs;

	private int window_size;

	private int num_fb_docs;

	private int num_fb_words;

	private double dirichlet_prior;

	private Map<Integer, SparseMatrix> docWordToWordProxes;

	private Indexer<String> wordIndexer;

	private boolean makeLog;

	private StringBuffer logBuf;

	private int num_query_words;

	private SparseMatrix fbWordToWordProxes;

	public ProximityRelevanceModelBuilder(Indexer<String> wordIndexer) {
		this(wordIndexer, 5, 20, 2000, 2, false);
	}

	public ProximityRelevanceModelBuilder(Indexer<String> wordIndexer, int num_fb_docs, int num_fb_words, int dirichlet_prior,
			int window_size, boolean makeLog) {
		this.wordIndexer = wordIndexer;
		this.num_fb_docs = num_fb_docs;
		this.num_fb_words = num_fb_words;
		this.dirichlet_prior = dirichlet_prior;
		this.window_size = window_size;
		this.makeLog = makeLog;
	}

	public void computeWordProximities(SparseVector queryModel, SparseVector docScores, WordCountBox wcb) {
		setDocWordLocs(wcb.getDocWords());

		num_query_words = queryModel.size();

		docWordToWordProxes = new HashMap<Integer, SparseMatrix>();

		logBuf = new StringBuffer();

		SparseMatrix docWordCounts = wcb.getDocWordCounts();

		CounterMap<Integer, Integer> gcm = new CounterMap<Integer, Integer>();

		for (int i = 0; i < docWordCounts.rowSize(); i++) {
			int docId = docWordCounts.indexAtRowLoc(i);
			SparseVector wordCounts = docWordCounts.vectorAtRowLoc(i);
			List<Integer> words = wcb.getDocWords().get(docId);
			ListMap<Integer, Integer> wordLocs = docWordLocs.get(docId, false);

			// for (int j = 0; j < words.size(); j++) {
			// int w = words.get(j);
			// String word = wordIndexer.getObject(w);
			// System.out.printf("%d, %s\n", j, word);
			// }

			CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();

			for (int j = 0; j < queryModel.size(); j++) {

				int qw = queryModel.indexAtLoc(j);
				List<Integer> locs = wordLocs.get(qw);

				if (makeLog) {
					StringBuffer sb = new StringBuffer();
					sb.append(String.format("QWord:\t%s\n", wordIndexer.getObject(qw)));
				}

				for (int k = 0; k < locs.size(); k++) {
					int current_loc = locs.get(k);

					int start = current_loc - window_size;
					int end = current_loc + window_size + 1;

					if (start < 0) {
						start = 0;
					}

					if (end > words.size()) {
						end = words.size();
					}

					for (int context_loc = start; context_loc < end; context_loc++) {
						// if (loc == l) {
						// continue;
						// }

						int cw = words.get(context_loc);

						if (makeLog) {
							String cWord = wordIndexer.getObject(cw);
							if (context_loc == current_loc) {
								logBuf.append(String.format("CWord at [%d, %d]:\t%s (#)\n", context_loc, current_loc, cWord));
							} else {
								logBuf.append(String.format("CWord at [%d, %d]:\t%s\n", context_loc, current_loc, cWord));
							}
						}

						if (context_loc != current_loc) {
							double distance = window_size - Math.abs(context_loc - current_loc) + 1;
							cm.incrementCount(qw, cw, distance);
						}
					}

					if (makeLog) {
						logBuf.append("\n");
					}
				}

				if (makeLog) {
					logBuf.append("\n");
				}
			}

			// double mixture = 0.5;
			// double cnt_sum_in_coll = wcb.getCountSumInCollection();

			SparseVector docFreqs = wcb.getCollDocFreqs();
			double num_docs = wcb.getNumDocsInCollection();

			double cocnt_sum = cm.totalCount();

			for (int qw : cm.keySet()) {
				Counter<Integer> cws = cm.getCounter(qw);
				Counter<Integer> wordWeights = new Counter<Integer>();
				double df1 = docFreqs.valueAlways(qw);
				double idf1 = Math.log((num_docs + 1) / df1);

				for (int cw : cws.keySet()) {
					double cocnt = cws.getCount(cw) / cocnt_sum;
					double df2 = docFreqs.valueAlways(cw);
					double idf2 = Math.log((num_docs + 1) / df2);
					double weight = idf1 * idf2 * cocnt;
					wordWeights.setCount(cw, weight);
				}
				cm.setCounter(qw, wordWeights);
			}

			double score = docScores.probAlways(docId);

			gcm.incrementAll(cm, score);

			// System.out.println(VectorUtils.toCounterMap(cm, wordIndexer, wordIndexer));

			// cm.normalize();
			cm = cm.invert();

			System.out.println(VectorUtils.toCounterMap(cm, wordIndexer, wordIndexer));

			// cm1.normalize();

			SparseMatrix sm = VectorUtils.toSpasreMatrix(cm);
			docWordToWordProxes.put(docId, sm);
		}

		gcm.scale(1f / docWordCounts.rowSize());

		// double cnt_sum_in_coll = wcb.getCountSumInCollection();
		// double mixture_for_coll = 0.5;
		//
		// for (int qw : gcm.keySet()) {
		// Counter<Integer> cws = gcm.getCounter(qw);
		// for (int cw : cws.keySet()) {
		// double prob = cws.getProbability(cw);
		// double prob_w_in_coll = wcb.getCollDocFreqs().valueAlways(cw) / cnt_sum_in_coll;
		// prob = (1 - mixture_for_coll) * prob + mixture_for_coll * prob_w_in_coll;
		// cws.setCount(cw, prob);
		// }
		// }
		gcm = gcm.invert();
		fbWordToWordProxes = VectorUtils.toSpasreMatrix(gcm);

	}

	public StringBuffer getLogBuffer() {
		return logBuf;
	}

	public SparseVector getRelevanceModel(WordCountBox wcb, SparseVector docScores) throws IOException {

		docScores.sortByValue();

		SparseVector ret = new SparseVector(wcb.getCollWordCounts().size());

		for (int j = 0; j < wcb.getCollWordCounts().size(); j++) {
			int w = wcb.getCollWordCounts().indexAtLoc(j);
			double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
			double prob_w_in_coll = cnt_w_in_coll / wcb.getCollectionCountSum();

			for (int k = 0; k < docScores.size() && k < num_fb_docs; k++) {
				int docId = docScores.indexAtLoc(k);
				SparseMatrix wordToWordProxes = docWordToWordProxes.get(docId);
				double doc_weight = docScores.valueAtLoc(k);
				double local_weight = 0;

				StringBuffer sb = new StringBuffer();
				if (wordToWordProxes != null) {
					SparseVector wordProxes = wordToWordProxes.rowAlways(w);
					// SparseVector wordProxes = fbWordToWordProxes.rowAlways(w);
					// System.out.printf("%d\t%s\t%f\t%s\n", docId, wordIndexer.getObject(w), wordProxes.sum(),
					// VectorUtils.toCounter(wordProxes, wordIndexer));
					// System.out.printf("%d\t%s\t%f\t%s\n", docId, wordIndexer.getObject(w), wordProxes2.sum(),
					// VectorUtils.toCounter(wordProxes2, wordIndexer));
					// System.out.println();
					if (wordProxes.size() > 0) {
						local_weight = wordProxes.sum() / wordProxes.size();
					}
				}
				local_weight = Math.exp(local_weight);

				// SparseVector fbProxes = fbWordToWordProxes.rowAlways(qw);

				SparseVector wordCounts = wcb.getDocWordCounts().rowAlways(docId);
				double cnt_w_in_doc = wordCounts.valueAlways(w);
				double cnt_sum_in_doc = wordCounts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double prob_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;
				prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;
				double doc_prior = 1;
				// double fb_weight = fbProxes.sum();
				// local_weight = CommonFuncs.sigmoid(local_weight);
				double prob_w_in_fb_model = doc_weight * prob_w_in_doc * doc_prior * local_weight;

				if (prob_w_in_fb_model > 0) {
					ret.incrementAtLoc(j, w, prob_w_in_fb_model);
				}
			}
		}

		docScores.sortByIndex();
		ret.keepTopN(num_fb_words);
		ret.normalize();
		return ret;
	}

	private void setDocWordLocs(ListMap<Integer, Integer> docWords) {
		docWordLocs = new DeepListMap<Integer, Integer, Integer>();

		for (int docId : docWords.keySet()) {
			List<Integer> words = docWords.get(docId);
			ListMap<Integer, Integer> wordLocs = new ListMap<Integer, Integer>();

			for (int i = 0; i < words.size(); i++) {
				int w = words.get(i);
				wordLocs.put(w, i);
			}

			for (int w : wordLocs.keySet()) {
				List<Integer> locs = wordLocs.get(w);
				Collections.sort(locs);
			}

			docWordLocs.put(docId, wordLocs);
		}
	}
}
