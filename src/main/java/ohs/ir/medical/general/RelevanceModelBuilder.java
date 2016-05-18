package ohs.ir.medical.general;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Pair;

public class RelevanceModelBuilder {

	private int num_fb_docs;

	private int num_fb_words;

	private double dirichlet_prior;

	public RelevanceModelBuilder() {
		this(5, 20, 2000);
	}

	public RelevanceModelBuilder(int num_fb_docs, int num_fb_words, double dirichlet_prior) {
		this.num_fb_docs = num_fb_docs;
		this.num_fb_words = num_fb_words;
		this.dirichlet_prior = dirichlet_prior;
	}

	public SparseVector getPassageRelevanceModel(WordCountBox wcb, SparseVector docScores, SparseVector qLM, List<Integer> qWords)
			throws IOException {
		docScores.sortByValue();

		SparseVector ret = new SparseVector(wcb.getCollWordCounts().size());

		PassageGenerator psgGenerator = new PassageGenerator();

		Map<Integer, SparseVector[]> map1 = new HashMap<Integer, SparseVector[]>();
		Map<Integer, SparseVector> map2 = new HashMap<Integer, SparseVector>();

		for (int i = 0; i < docScores.size() && i < num_fb_docs; i++) {
			int docId = docScores.indexAtLoc(i);
			List<Integer> dWords = wcb.getDocToWords().get(docId);

			Counter<Pair<Integer, Integer>> psgLocScores = psgGenerator.generate(qWords, dWords);

			List<Pair<Integer, Integer>> psgLocs = psgLocScores.getSortedKeys();

			SparseVector[] psgWordCountData = new SparseVector[psgLocs.size()];

			for (int j = 0; j < psgLocs.size() && j < psgLocs.size(); j++) {
				Counter<Integer> c = new Counter<Integer>();

				Pair<Integer, Integer> psgLoc = psgLocs.get(j);

				int start = psgLoc.getFirst();
				int offset = psgLoc.getSecond();
				int end = start + offset;

				for (int k = start; k < end; k++) {
					int w_in_psg = dWords.get(k);
					c.incrementCount(w_in_psg, 1);
				}
				psgWordCountData[j] = VectorUtils.toSparseVector(c);
			}

			SparseVector psgScores = new SparseVector(psgLocs.size());

			for (int j = 0; j < psgLocs.size(); j++) {
				SparseVector psgWordCounts = psgWordCountData[j];

				if (psgWordCounts.size() == 0) {
					psgScores.incrementAtLoc(j, j, 0);
				} else {
					double div_sum = 0;

					for (int k = 0; k < qLM.size(); k++) {
						int w = qLM.indexAtLoc(k);
						double pr_w_in_query = qLM.valueAtLoc(k);
						double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
						double pr_w_in_coll = cnt_w_in_coll / wcb.getCountSum();

						double cnt_w_in_psg = psgWordCounts.valueAlways(w);
						double cnt_sum_in_psg = psgWordCounts.sum();
						double mixture_for_coll = dirichlet_prior / (cnt_sum_in_psg + dirichlet_prior);
						double pr_w_in_doc = cnt_w_in_psg / cnt_sum_in_psg;

						pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;

						if (pr_w_in_doc > 0) {
							div_sum += pr_w_in_query * Math.log(pr_w_in_query / pr_w_in_doc);
						}
					}

					double approx_prob = Math.exp(-div_sum);
					psgScores.incrementAtLoc(j, j, approx_prob);
				}
			}

			map1.put(docId, psgWordCountData);
			map2.put(docId, psgScores);
		}

		docScores.sortByIndex();

		for (int docId : map2.keySet()) {
			SparseVector psgScores = map2.get(docId);

			double doc_score = docScores.valueAlways(docId);
			double avg_psg_score = 0;

			if (psgScores.size() > 0) {
				psgScores.sortByValue();

				for (int j = 0; j < psgScores.size() && j < 20; j++) {
					avg_psg_score += psgScores.valueAtLoc(j);
				}

				avg_psg_score = avg_psg_score / psgScores.size();
			}

			avg_psg_score = Math.exp(avg_psg_score);

			docScores.set(docId, avg_psg_score);
		}

		docScores.sortByValue();

		for (int j = 0; j < wcb.getCollWordCounts().size(); j++) {
			int w = wcb.getCollWordCounts().indexAtLoc(j);
			double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
			double cnt_sum_in_coll = wcb.getCountSum();
			double pr_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;

			for (int k = 0; k < docScores.size() && k < num_fb_docs; k++) {
				int docId = docScores.indexAtLoc(k);
				double doc_weight = docScores.valueAtLoc(k);

				SparseVector wordCounts = wcb.getDocToWordCounts().rowAlways(docId);
				double cnt_w_in_doc = wordCounts.valueAlways(w);
				double cnt_sum_in_doc = wordCounts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double pr_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;
				pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;
				double doc_prior = 1;
				double pr_w_in_fb_model = doc_weight * pr_w_in_doc * doc_prior;

				if (pr_w_in_fb_model > 0) {
					ret.incrementAtLoc(j, w, pr_w_in_fb_model);
				}
			}
		}

		docScores.sortByIndex();
		ret.keepTopN(num_fb_words);
		ret.normalize();
		return ret;
	}

	public SparseVector getPassageRelevanceModel2(WordCountBox wcb, SparseVector docScores, SparseVector qLM, List<Integer> qWords)
			throws IOException {
		docScores.sortByValue();

		SparseVector ret = new SparseVector(wcb.getCollWordCounts().size());

		PassageGenerator psgGenerator = new PassageGenerator();

		CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();

		for (int i = 0; i < docScores.size() && i < num_fb_docs; i++) {
			int docId = docScores.indexAtLoc(i);
			List<Integer> dWords = wcb.getDocToWords().get(docId);

			Counter<Pair<Integer, Integer>> psgLocScores = psgGenerator.generate(qWords, dWords);

			List<Pair<Integer, Integer>> psgLocs = psgLocScores.getSortedKeys();

			SparseVector[] psgWordCountData = new SparseVector[psgLocs.size()];
			Counter<Integer> c = new Counter<Integer>();

			for (int j = 0; j < psgLocs.size() && j < psgLocs.size(); j++) {
				Pair<Integer, Integer> psgLoc = psgLocs.get(j);

				int start = psgLoc.getFirst();
				int offset = psgLoc.getSecond();
				int end = start + offset;

				for (int k = start; k < end; k++) {
					int w_in_psg = dWords.get(k);
					c.incrementCount(w_in_psg, 1);
				}
			}

			cm.setCounter(docId, c);
		}

		SparseMatrix docPsgWordCounts = VectorUtils.toSpasreMatrix(cm);
		SparseVector psgScores = new SparseVector(docPsgWordCounts.rowSize());

		for (int i = 0; i < docPsgWordCounts.rowSize(); i++) {
			int docId = docPsgWordCounts.indexAtLoc(i);
			SparseVector psgWordCounts = docPsgWordCounts.rowAtLoc(i);

			double div_sum = 0;

			for (int j = 0; j < qLM.size(); j++) {
				int w = qLM.indexAtLoc(j);
				double pr_w_in_query = qLM.valueAtLoc(j);
				double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
				double pr_w_in_coll = cnt_w_in_coll / wcb.getCountSum();

				double cnt_w_in_psg = psgWordCounts.valueAlways(w);
				double cnt_sum_in_psg = psgWordCounts.sum();
				double mixture_for_coll = 0.5;
				double pr_w_in_doc = cnt_w_in_psg / cnt_sum_in_psg;

				pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;

				if (pr_w_in_doc > 0) {
					div_sum += pr_w_in_query * Math.log(pr_w_in_query / pr_w_in_doc);
				}
			}

			double approx_prob = Math.exp(-div_sum);
			psgScores.incrementAtLoc(i, docId, approx_prob);
		}

		for (int j = 0; j < wcb.getCollWordCounts().size(); j++) {
			int w = wcb.getCollWordCounts().indexAtLoc(j);
			double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
			double cnt_sum_in_coll = wcb.getCountSum();
			double pr_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;

			for (int k = 0; k < docScores.size() && k < num_fb_docs; k++) {
				int docId = docScores.indexAtLoc(k);
				double doc_weight = docScores.valueAtLoc(k);
				double psg_weight = psgScores.valueAlways(docId);
				psg_weight = Math.exp(psg_weight);

				SparseVector docWordCounts = wcb.getDocToWordCounts().rowAlways(docId);
				SparseVector psgWordCounts = docPsgWordCounts.rowAlways(docId);

				double cnt_w_in_doc = docWordCounts.valueAlways(w);
				double cnt_sum_in_doc = docWordCounts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double pr_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;
				pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;
				double doc_prior = 1;
				double pr_w_in_fb_model = doc_weight * pr_w_in_doc * doc_prior * psg_weight;

				if (pr_w_in_fb_model > 0) {
					ret.incrementAtLoc(j, w, pr_w_in_fb_model);
				}
			}
		}

		docScores.sortByIndex();
		ret.keepTopN(num_fb_words);
		ret.normalize();
		return ret;
	}

	public SparseVector getPositionalRelevanceModel(SparseVector qlm, WordCountBox wcb, SparseVector docScores) {
		double pi = Math.PI;
		double sigma = 175;
		double coll_mixture = 0.5;

		int fb_type = 1;

		Counter<Integer> fbCounts = new Counter<Integer>();

		for (int i = 0; i < docScores.size(); i++) {
			int did = docScores.indexAtLoc(i);
			double doc_score = docScores.valueAtLoc(i);
			List<Integer> words = wcb.getDocToWords().get(did);
			List<Pair<Integer, Integer>> locWords = PlmUtils.getQueryLocsInDocument(qlm, words);

			double real_doc_len = locWords.size();
			double len_norm = Math.sqrt(2 * pi) * sigma;
			double pos_score_sum = 0;

			double[] posScores = new double[words.size()];

			for (int j = 0; j < words.size(); j++) {
				Counter<Integer> c = new Counter<Integer>();

				for (int qw : qlm.indexes()) {
					c.incrementCount(qw, 0);
				}

				for (int k = 0; k < locWords.size(); k++) {
					int pos = locWords.get(k).getFirst();
					int w = locWords.get(k).getSecond();

					double dis = (pos - j) / sigma;
					double pr = Math.exp(-dis * dis / 2.0) / len_norm;
					c.incrementCount(w, pr);
				}

				SparseVector plm = VectorUtils.toSparseVector(c);

				for (int k = 0; k < plm.size(); k++) {
					int w = plm.indexAtLoc(k);
					double pr_w_in_doc = plm.valueAtLoc(k);
					double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
					double coll_cnt_sum = wcb.getCountSum();
					double pr_w_in_coll = cnt_w_in_coll / coll_cnt_sum;
					pr_w_in_doc = (1 - coll_mixture) * pr_w_in_doc + coll_mixture * pr_w_in_coll;
					plm.setAtLoc(k, pr_w_in_doc);
				}
				plm.summation();

				double div_sum = 0;

				for (int k = 0; k < qlm.size(); k++) {
					int w = qlm.indexAtLoc(k);
					double pr_w_in_query = qlm.valueAtLoc(k);
					double pr_w_in_doc = plm.valueAlways(w);

					if (pr_w_in_doc > 0) {
						double div = pr_w_in_query * Math.log(pr_w_in_query / pr_w_in_doc);
						div_sum += div;
					}
				}

				double approx_prob = Math.exp(-div_sum);
				posScores[j] = approx_prob;
				pos_score_sum += approx_prob;
			}

			for (int j = 0; j < posScores.length; j++) {
				int w = words.get(j);
				double pos_score = posScores[j];

				if (fb_type == 1) {
					pos_score /= real_doc_len;
				} else if (fb_type == 2) {
					pos_score = pos_score * doc_score / pos_score_sum;
				}

				if (pos_score > 0) {
					fbCounts.incrementCount(w, pos_score);
				}
			}
		}

		SparseVector ret = VectorUtils.toSparseVector(fbCounts);
		ret.keepTopN(num_fb_words);
		ret.normalize();
		return ret;
	}

	public SparseVector getRelevanceModel(WordCountBox wcb, SparseVector docScores) throws IOException {
		return getRelevanceModel(wcb, docScores, null);
	}

	public SparseVector getRelevanceModel(WordCountBox wcb, SparseVector docScores, SparseVector docPriors) throws IOException {
		docScores.sortByValue();

		SparseVector ret = new SparseVector(wcb.getCollWordCounts().size());

		for (int j = 0; j < wcb.getCollWordCounts().size(); j++) {
			int w = wcb.getCollWordCounts().indexAtLoc(j);
			double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
			double cnt_sum_in_coll = wcb.getCountSum();
			double pr_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;

			for (int k = 0; k < docScores.size() && k < num_fb_docs; k++) {
				int d = docScores.indexAtLoc(k);
				double doc_weight = docScores.valueAtLoc(k);

				SparseVector wordCnts = wcb.getDocToWordCounts().rowAlways(d);
				double cnt_w_in_doc = wordCnts.valueAlways(w);
				double cnt_sum_in_doc = wordCnts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double pr_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;
				pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;
				double doc_prior = 1;

				if (docPriors != null) {
					doc_prior = docPriors.valueAtLoc(k);
				}
				double pr_w_in_fb_model = doc_weight * pr_w_in_doc * doc_prior;

				if (pr_w_in_fb_model > 0) {
					ret.incrementAtLoc(j, w, pr_w_in_fb_model);
				}
			}
		}
		docScores.sortByIndex();
		ret.keepTopN(num_fb_words);
		ret.normalize();
		return ret;
	}
}
