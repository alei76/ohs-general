package ohs.ir.eval;

import java.util.List;

import ohs.math.ArrayUtils;
import ohs.math.CommonFuncs;
import ohs.types.Counter;

public class Metrics {

	public static double averagePrecisionAtN(List<String> docIds, int n, Counter<String> docRels) {
		double num_relevant_docs = 0;
		double ret = 0;

		for (int i = 0; i < docIds.size() && i < n; i++) {
			String docId = docIds.get(i);
			double relevance = docRels.getCount(docId);

			if (relevance > 0) {
				int rank = i + 1;
				double precision_at_r = precisionAtN(docIds, rank, docRels);
				ret += precision_at_r;
				num_relevant_docs++;
			}
		}

		if (num_relevant_docs > 0) {
			ret = ret / num_relevant_docs;
		}
		return ret;
	}

	public static double binaryPreference(List<String> docIds, Counter<String> docRels) {
		return binaryPreference(docIds, docRels, 0);
	}

	/**
	 * 
	 * Buckley, C., & Voorhees, E. M. (2004). Retrieval evaluation with incomplete information. In Proceedings of the 27th annual
	 * international conference on Research and development in information retrieval - SIGIR ’04 (p. 25). New York, New York, USA: ACM
	 * Press. doi:10.1145/1008992.1009000
	 * 
	 * @param docIds
	 * @param docRels
	 * @param num_pseudo_irrelevant
	 * @return
	 */
	private static double binaryPreference(List<String> docIds, Counter<String> docRels, double num_pseudo_irrelevant) {
		double num_relevant = 0;
		Counter<Integer> irrelevantCounts = new Counter<Integer>();

		for (int r = 0; r < docIds.size(); r++) {
			String docId = docIds.get(r);
			double relevance = docRels.getCount(docId);
			if (relevance > 0) {
				irrelevantCounts.setCount(r, num_relevant);
			} else {
				num_relevant++;
			}
		}

		double ret = 0;
		for (int rank : irrelevantCounts.keySet()) {
			double num_irrelevant_at_rank = irrelevantCounts.getCount(rank);
			ret += (1 - (num_irrelevant_at_rank / (num_relevant + num_pseudo_irrelevant)));
		}
		ret /= num_relevant;
		return ret;
	}

	public static double binaryPreference10(List<String> docIds, Counter<String> docRels) {
		return binaryPreference(docIds, docRels, 10);
	}

	public static double[] computeDCGs(double[] gains) {
		double[] dcgs = new double[gains.length];
		double dcg = 0;

		for (int i = 0; i < gains.length; i++) {
			double rank = i + 1;
			// double discount_factor = log2 / Math.log(1 + rank);
			double discount_factor = 1 / CommonFuncs.log2(1 + rank);
			double dg = discount_factor * gains[i];
			dcg += dg;
			dcgs[i] = dcg;
		}
		return dcgs;
	}

	public static double f1(double precision, double recall) {
		double ret = 0;
		if (precision > 0 || recall > 0) {
			ret = 2 * precision * recall / (precision + recall);
		}
		return ret;
	}

	public static double f1(List<String> docIds, int n, Counter<String> docRels) {
		double precision = precisionAtN(docIds, n, docRels);
		double recall = recallAtN(docIds, n, docRels);
		return f1(precision, recall);
	}

	/**
	 * Yilmaz, E., & Aslam, J. A. (2006). Estimating average precision with incomplete and imperfect judgments. In Proceedings of the 15th
	 * ACM international conference on Information and knowledge management - CIKM ’06 (p. 102). New York, New York, USA: ACM Press.
	 * doi:10.1145/1183614.1183633
	 * 
	 * @param docIds
	 * @param docRels
	 * @return
	 */
	public static double inferredAveragePrecision(List<String> docIds, Counter<String> docRels) {
		double ret = 0;

		return ret;
	}

	public static void main(String[] args) {
		{
			Counter<String> docScores = new Counter<String>();
			Counter<String> docRelevances = new Counter<String>();

			for (int i = 0; i < 20; i++) {
				String docId = String.format("doc_%d", i);
				docScores.setCount(docId, 20f / (i + 1));
			}

			docRelevances.setCount("doc_0", 1);
			docRelevances.setCount("doc_1", 1);
			docRelevances.setCount("doc_5", 1);
			docRelevances.setCount("doc_10", 1);
			docRelevances.setCount("doc_16", 1);

			List<String> docIds = docScores.getSortedKeys();

			rankBiasedPrecisionAtN(docIds, 20, docRelevances, 0.8);
		}
		{
			Counter<String> docScores = new Counter<String>();
			docScores.setCount("http://abc.go.com/", 10);
			docScores.setCount("http://www.abcteach.com/", 9);
			docScores.setCount("http://abcnews.go.com/sections/scitech/", 8);
			docScores.setCount("http://www.abc.net.au/", 7);
			docScores.setCount("http://abcnews.go.com/", 6);
			docScores.setCount("http://abc.org/", 5);

			Counter<String> docRelevances = new Counter<String>();
			docRelevances.setCount("http://abc.go.com/", 5);
			docRelevances.setCount("http://www.abcteach.com/", 2);
			docRelevances.setCount("http://abcnews.go.com/sections/scitech/", 4);
			docRelevances.setCount("http://www.abc.net.au/", 4);
			docRelevances.setCount("http://abcnews.go.com/", 4);
			docRelevances.setCount("http://abc.org/", 4);

			normalizedDiscountedCumulativeGainAtN(docScores.getSortedKeys(), docScores.size(), docRelevances);
		}

		{
			Counter<String> docScores = new Counter<String>();
			docScores.setCount("http://abc.go.com/", 10);
			docScores.setCount("http://www.abcteach.com/", 9);
			docScores.setCount("http://abcnews.go.com/sections/scitech/", 8);
			docScores.setCount("http://www.abc.net.au/", 7);
			docScores.setCount("http://abcnews.go.com/", 6);
			docScores.setCount("http://abc.org/", 5);

			Counter<String> docRelevances = new Counter<String>();
			docRelevances.setCount("http://abc.go.com/", 5);
			docRelevances.setCount("http://www.abcteach.com/", 2);
			docRelevances.setCount("http://abcnews.go.com/sections/scitech/", 4);
			docRelevances.setCount("http://www.abc.net.au/", 4);
			docRelevances.setCount("http://abcnews.go.com/", 4);
			docRelevances.setCount("http://abc.org/", 4);

			normalizedDiscountedCumulativeGainAtN(docScores.getSortedKeys(), docScores.size(), docRelevances);
		}

		{

			Counter<String> docScores = new Counter<String>();
			Counter<String> docRelevances = new Counter<String>();
			double[] rels = new double[] { 3, 2, 3, 0, 1, 2 };

			for (int i = 0; i < rels.length; i++) {
				docRelevances.setCount(i + "", rels[i]);
				docScores.setCount(i + "", rels.length - i);
			}

			normalizedDiscountedCumulativeGainAtN(docScores.getSortedKeys(), docScores.size(), docRelevances);
		}

		{
			{
				Counter<String> docScores = new Counter<String>();
				Counter<String> docRelevances = new Counter<String>();

				for (int i = 0; i < 20; i++) {
					String docId = String.format("doc_%d", i);
					docScores.setCount(docId, 20f / (i + 1));
				}

				docRelevances.setCount("doc_0", 3);
				docRelevances.setCount("doc_1", 2);
				docRelevances.setCount("doc_4", 1);
				docRelevances.setCount("doc_10", 1);
				docRelevances.setCount("doc_16", 1);

				List<String> docIds = docScores.getSortedKeys();

				int[] ns = { 2, 5 };

				for (int i = 0; i < ns.length; i++) {
					int n = ns[i];
					double p = precisionAtN(docIds, n, docRelevances);
					double map = averagePrecisionAtN(docIds, n, docRelevances);
					double ndcg = normalizedDiscountedCumulativeGainAtN(docIds, n, docRelevances);
					System.out.println(String.format("%d\t%s\t%s\t%s", n, p, map, ndcg));
				}

			}
		}

		// System.out.println();
	}

	public static double normalizedDiscountedCumulativeGainAtN(List<String> docIds, int n, Counter<String> docRels) {
		n = docIds.size() < n ? docIds.size() : n;

		double[] gains = new double[n];
		double[] max_gains = new double[n];

		for (int i = 0; i < n; i++) {
			String docId = docIds.get(i);
			double relevance = docRels.getCount(docId);
			double gain = Math.pow(2, relevance) - 1;
			gains[i] = gain;
			max_gains[i] = gain;
		}

		ArrayUtils.sort(max_gains);

		double[] dcgs = computeDCGs(gains);
		double[] max_dcgs = computeDCGs(max_gains);

		double[] ndcgs = new double[n];

		for (int i = 0; i < n; i++) {
			double dcg = dcgs[i];
			double max_dcg = max_dcgs[i];
			double ndcg = 0;

			if (max_dcg > 0) {
				ndcg = dcg / max_dcg;
			}
			ndcgs[i] = ndcg;
		}

		// System.out.println("[DOC_NDCG]");
		// System.out.println(doc_ndcg.toStringSortedByValues(true, true,
		// doc_ndcg.size()));

		double ndcg_at_n = ndcgs[n - 1];
		return ndcg_at_n;
	}

	public static double precisionAtN(List<String> docIds, int n, Counter<String> docRels) {
		double num_correct = relevantAtN(docIds, n, docRels);
		double ret = num_correct / n;
		return ret;
	}

	/**
	 * "Rank-Biased Precision for Measurement of Retrieval Effectiveness" at ACM TOIS' 2008.
	 * 
	 * @param docIds
	 * @param n
	 * @param docRels
	 * @param p
	 * @return
	 */
	public static double rankBiasedPrecisionAtN(List<String> docIds, int n, Counter<String> docRels, double p) {
		double ret = 0;

		for (int i = 0; i < docIds.size() && i < n; i++) {
			String docId = docIds.get(i);
			double relevance = docRels.getCount(docId);
			if (relevance > 1) {
				relevance = 1;
			}
			ret += relevance * Math.pow(p, i);
		}
		ret *= (1 - p);
		return ret;
	}

	public static double recallAtN(List<String> docIds, int n, Counter<String> docRels) {
		double num_relevant_docs = relevant(docRels);
		double num_relevant_at_n = relevantAtN(docIds, n, docRels);
		double ret = 0;
		if (num_relevant_docs > 0) {
			ret = num_relevant_at_n / num_relevant_docs;
		}
		return ret;
	}

	public static double reciprocalRank(List<String> docIds, int n, Counter<String> docRels) {
		double ret = 0;
		for (int i = 0; i < docIds.size() && i < n; i++) {
			String docId = docIds.get(i);
			if (docRels.getCount(docId) > 0) {
				double rank = i + 1;
				ret = 1f / rank;
				break;
			}
		}
		return ret;
	}

	public static double relevant(Counter<String> docRels) {
		double ret = 0;
		for (String docId : docRels.keySet()) {
			double relevance = docRels.getCount(docId);
			if (relevance > 0) {
				ret++;
			}
		}
		return ret;
	}

	public static double relevantAtN(List<String> docIds, int n, Counter<String> docRels) {
		double ret = 0;
		for (int i = 0; i < docIds.size() && i < n; i++) {
			if (docRels.getCount(docIds.get(i)) > 0) {
				ret++;
			}
		}
		return ret;
	}

	public static double[] riskRewardFunction(Counter<String> baselines, Counter<String> targets) {
		double risk = 0;
		double reward = 0;

		for (String qId : baselines.keySet()) {
			double score1 = baselines.getCount(qId);
			double score2 = targets.getCount(qId);
			risk += Math.max(0, score1 - score2);
			reward += Math.max(0, score2 - score1);
		}
		risk /= baselines.size();
		reward /= baselines.size();
		return new double[] { risk, reward };
	}

	/**
	 * Risk-Reward Tradeoff
	 * 
	 * 1. Wang, L., Bennett, P.N., Collins-Thompson, K.: Robust ranking models via risk-sensitive optimization. Proceedings of the 35th
	 * international ACM SIGIR conference on Research and development in information retrieval - SIGIR ’12. p. 761. ACM Press, New York, New
	 * York, USA (2012).
	 * 
	 * 
	 * @param baselines
	 * @param targets
	 * @param alpha
	 * @return
	 */

	public static double riskRewardTradeoff(Counter<String> baselines, Counter<String> targets) {
		return riskRewardTradeoff(baselines, targets, 5);
	}

	public static double riskRewardTradeoff(Counter<String> baselines, Counter<String> targets, double alpha) {
		double[] rr = riskRewardFunction(baselines, targets);
		double risk = rr[0];
		double reward = rr[1];
		double ret = reward - (1 + alpha) * risk;
		return ret;
	}

	private double[] getDocRelevances(List<String> docIds, int n, Counter<String> docRels) {
		int num_docs = docIds.size() < n ? docIds.size() : n;
		double[] ret = new double[num_docs];
		for (int i = 0; i < docIds.size() && i < n; i++) {
			String docId = docIds.get(i);
			double relevance = docRels.getCount(docId);
			ret[i] = relevance;
		}
		return ret;
	}

}
