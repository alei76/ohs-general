package ohs.ir.task.medical;

import java.util.List;

import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Pair;

public class KLDivergenceScorer {

	private double dirichlet_prior;

	private boolean makeLog;

	private StringBuffer logBuf;

	public KLDivergenceScorer() {
		this(2000, false);
	}

	public KLDivergenceScorer(double dirichlet_prior, boolean makeLog) {
		this.dirichlet_prior = dirichlet_prior;
		this.makeLog = makeLog;
	}

	public String getLog() {
		return logBuf.toString();
	}

	public SparseVector score(WordCountBox wcb, SparseVector qlm) {
		SparseVector ret = new SparseVector(wcb.getDocWordCounts().rowSize());

		for (int i = 0; i < wcb.getDocWordCounts().rowSize(); i++) {
			int docId = wcb.getDocWordCounts().indexAtRowLoc(i);
			SparseVector wcs = wcb.getDocWordCounts().rowAtLoc(i);
			double div_sum = 0;

			for (int j = 0; j < qlm.size(); j++) {
				int w = qlm.indexAtLoc(j);
				double pr_w_in_query = qlm.valueAtLoc(j);
				double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
				double pr_w_in_coll = cnt_w_in_coll / wcb.getCollectionCountSum();

				double cnt_w_in_doc = wcs.valueAlways(w);
				double cnt_sum_in_doc = wcs.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double pr_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;

				pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;

				if (pr_w_in_doc > 0) {
					div_sum += pr_w_in_query * Math.log(pr_w_in_query / pr_w_in_doc);
				}
			}

			double approx_prob = Math.exp(-div_sum);
			ret.incrementAtLoc(i, docId, approx_prob);
		}
		return ret;
	}

	public SparseVector scoreByPassages(WordCountBox wcb, SparseVector qLM) {
		SparseVector ret = new SparseVector(wcb.getDocWordCounts().rowSize());

		int num_passages = 1;

		for (int i = 0; i < wcb.getDocWordCounts().rowSize(); i++) {
			int docId = wcb.getDocWordCounts().indexAtRowLoc(i);
			// SparseVector wordCounts = wcb.getDocWordCounts().rowAtLoc(i);

			List<Integer> words = wcb.getDocWords().get(docId);

			int num_words_in_passage = words.size() / num_passages;

			SparseVector[] psgWordCountData = new SparseVector[num_passages];

			for (int j = 0, loc = 0; j < num_passages; j++) {
				Counter<Integer> c = new Counter<Integer>();
				int temp_cnt = num_words_in_passage;

				for (int k = 0; k < num_words_in_passage && loc < words.size(); k++) {
					int w = words.get(loc++);
					c.incrementCount(w, 1);
				}

				psgWordCountData[j] = VectorUtils.toSparseVector(c);
			}

			SparseVector psgScores = new SparseVector(num_passages);

			for (int j = 0; j < num_passages; j++) {
				SparseVector psgWordCounts = psgWordCountData[j];

				double div_sum = 0;

				for (int k = 0; k < qLM.size(); k++) {
					int w = qLM.indexAtLoc(k);
					double pr_w_in_query = qLM.valueAtLoc(k);
					double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
					double pr_w_in_coll = cnt_w_in_coll / wcb.getCollectionCountSum();

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

			double max_score = psgScores.max();
			ret.incrementAtLoc(i, docId, max_score);
		}

		return ret;
	}

	public SparseVector scoreByPLMs(WordCountBox wcb, SparseVector qLM) {
		SparseVector ret = new SparseVector(wcb.getDocWordCounts().rowSize());

		int propFunction = 0;
		double sigma = 175;

		System.out.println(VectorUtils.toCounter(qLM, wcb.getWordIndexer()));

		for (int i = 0; i < wcb.getDocWordCounts().rowSize(); i++) {
			int docId = wcb.getDocWordCounts().indexAtRowLoc(i);
			List<Integer> words = wcb.getDocWords().get(docId);
			List<Pair<Integer, Integer>> locWords = PlmUtils.getQueryLocsInDocument(qLM, words);

			double doc_len = words.size();

			SparseVector plmScores = new SparseVector(locWords.size());

			// double psg_len_sum = 0;
			// for (int j = 0; j < ws.size(); j++) {
			// int center = j;
			// double psg_len = PropagationCountSum(center, doc_len, -1, sigma);
			// psg_len_sum += psg_len;
			// }

			for (int j = 0; j < locWords.size(); j++) {
				int center = (int) locWords.get(j).getFirst();
				double psg_len = PlmUtils.PropagationCountSum(center, doc_len, propFunction, sigma);

				Counter<Integer> c = new Counter<Integer>();

				for (int w : qLM.indexes()) {
					c.incrementCount(w, 0);
				}

				for (int k = 0; k < locWords.size(); k++) {
					int w = locWords.get(k).getSecond();
					int pos = locWords.get(k).getFirst();
					double prop_count = PlmUtils.PropagationCount((pos - center) / sigma, propFunction);
					double pr = prop_count / psg_len;
					c.incrementCount(w, pr);
				}

				SparseVector plm = VectorUtils.toSparseVector(c);

				for (int k = 0; k < plm.size(); k++) {
					int w = plm.indexAtLoc(k);
					double pr_w_in_doc = plm.valueAtLoc(k);
					double mixture_for_coll = dirichlet_prior / (dirichlet_prior + psg_len);
					double pr_w_in_coll = wcb.getCollWordCounts().valueAlways(w) / wcb.getCollectionCountSum();
					pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;
					plm.setAtLoc(k, pr_w_in_doc);
				}
				plm.summation();

				double div_sum = 0;

				for (int k = 0; k < qLM.size(); k++) {
					int w = qLM.indexAtLoc(k);
					double pr_w_in_q = qLM.valueAtLoc(k);
					double pr_w_in_doc = plm.valueAlways(w);
					if (pr_w_in_doc > 0) {
						div_sum += pr_w_in_q * Math.log(pr_w_in_q / pr_w_in_doc);
					}
				}

				double approx_prob = Math.exp(-div_sum);
				plmScores.incrementAtLoc(j, center, approx_prob);
			}
			double score = plmScores.size() == 0 ? 0 : plmScores.max();
			ret.incrementAtLoc(i, docId, score);

		}
		return ret;
	}

	public SparseVector scoreBySWPassages(WordCountBox wcb, SparseVector qLM, List<Integer> qWords) {
		SparseVector ret = new SparseVector(wcb.getDocWordCounts().rowSize());

		PassageGenerator psgGenerator = new PassageGenerator();

		for (int i = 0; i < wcb.getDocWordCounts().rowSize(); i++) {
			int docId = wcb.getDocWordCounts().indexAtRowLoc(i);
			// SparseVector wordCounts = wcb.getDocWordCounts().rowAtLoc(i);

			List<Integer> dWords = wcb.getDocWords().get(docId);

			Counter<Pair<Integer,Integer>> psgLocScores = psgGenerator.generate(qWords, dWords);

			List<Pair<Integer,Integer>> psgLocs = psgLocScores.getSortedKeys();

			int num_psgs = Math.min(psgLocs.size(), 5);

			if (num_psgs == 0) {
				SparseVector wordCounts = wcb.getDocWordCounts().rowAtLoc(i);
				double div_sum = 0;

				for (int j = 0; j < qLM.size(); j++) {
					int w = qLM.indexAtLoc(j);
					double pr_w_in_query = qLM.valueAtLoc(j);
					double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
					double pr_w_in_coll = cnt_w_in_coll / wcb.getCollectionCountSum();

					double cnt_w_in_doc = wordCounts.valueAlways(w);
					double cnt_sum_in_doc = wordCounts.sum();
					double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
					double pr_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;

					pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;

					if (pr_w_in_doc > 0) {
						div_sum += pr_w_in_query * Math.log(pr_w_in_query / pr_w_in_doc);
					}
				}

				double approx_prob = Math.exp(-div_sum);
				ret.incrementAtLoc(i, docId, approx_prob);
			} else {
				SparseVector[] psgWordCountData = new SparseVector[num_psgs];

				for (int j = 0; j < num_psgs; j++) {
					Counter<Integer> c = new Counter<Integer>();

					Pair<Integer,Integer> psgLoc = psgLocs.get(j);

					int start = psgLoc.getFirst();
					int offset = psgLoc.getSecond();
					int end = start + offset;

					for (int k = start; k < end; k++) {
						int w = dWords.get(k);
						c.incrementCount(w, 1);
					}

					psgWordCountData[j] = VectorUtils.toSparseVector(c);
				}

				SparseVector psgScores = new SparseVector(num_psgs);

				for (int j = 0; j < num_psgs; j++) {
					SparseVector psgWordCounts = psgWordCountData[j];

					double div_sum = 0;

					for (int k = 0; k < qLM.size(); k++) {
						int w = qLM.indexAtLoc(k);
						double pr_w_in_query = qLM.valueAtLoc(k);
						double cnt_w_in_coll = wcb.getCollWordCounts().valueAlways(w);
						double pr_w_in_coll = cnt_w_in_coll / wcb.getCollectionCountSum();

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

				double max_score = psgScores.max();
				ret.incrementAtLoc(i, docId, max_score);
			}
		}

		return ret;
	}

}
