package ohs.ir.eval;

import java.text.NumberFormat;
import java.util.Set;
import java.util.TreeSet;

import ohs.types.Counter;
import ohs.types.CounterMap;

public class Performance {
	private int total_relevant_in_ret;

	private int total_relevant;

	private int total_retrieved;

	private int top_n;

	private double map;

	private double ndcg;

	private double precision;

	private int total_relevant_at_n;

	private CounterMap<MetricType, String> metricQueryScores;

	private double precision_rrt;

	private double map_rrt;

	private double ndcg_rrt;

	public Performance(int top_n, CounterMap<MetricType, String> metricQueryScores) {
		this.top_n = top_n;
		this.metricQueryScores = metricQueryScores;
		computeAveragePerformance();
	}

	private void computeAveragePerformance() {
		total_retrieved = (int) metricQueryScores.getCounter(MetricType.RETRIEVED).totalCount();
		total_relevant = (int) metricQueryScores.getCounter(MetricType.RELEVANT).totalCount();
		total_relevant_in_ret = (int) metricQueryScores.getCounter(MetricType.RELEVANT_IN_RET).totalCount();
		total_relevant_at_n = (int) metricQueryScores.getCounter(MetricType.RELEVANT_AT).totalCount();
		map = metricQueryScores.getCounter(MetricType.AP).average();
		ndcg = metricQueryScores.getCounter(MetricType.NDCG).average();
		precision = metricQueryScores.getCounter(MetricType.P).average();
	}

	public void computeRiskRewardTradeoffs(Performance baseline) {
		CounterMap<MetricType, String> baselineScores = baseline.getMetricQueryScores();
		CounterMap<MetricType, String> targetScores = metricQueryScores;

		MetricType[] mts = { MetricType.P, MetricType.AP, MetricType.NDCG };
		double[] rrts = new double[mts.length];

		for (int i = 0; i < mts.length; i++) {
			MetricType mt = mts[i];
			Counter<String> qs1 = baselineScores.getCounter(mt);
			Counter<String> gs2 = targetScores.getCounter(mt);
			rrts[i] = Metrics.riskRewardTradeoff(qs1, gs2, 1);
		}

		precision_rrt = rrts[0];
		map_rrt = rrts[1];
		ndcg_rrt = rrts[2];
	}

	public double getMAP() {
		return map;
	}

	public double getMapRiskRewardTradeoff() {
		return map_rrt;
	}

	public CounterMap<MetricType, String> getMetricQueryScores() {
		return metricQueryScores;
	}

	public double getNDCG() {
		return ndcg;
	}

	public double getNdcgRiskRewardTradeoff() {
		return ndcg_rrt;
	}

	public double getPrecisionAtN() {
		return precision;
	}

	public double getPrecisionRiskRewardTradeoff() {
		return precision_rrt;
	}

	public int getTopN() {
		return top_n;
	}

	public int getTotalCorrect() {
		return total_relevant_in_ret;
	}

	public int getTotalCorrectAtN() {
		return total_relevant_at_n;
	}

	public int getTotalRelevant() {
		return total_relevant;
	}

	public int getTotalRetrieved() {
		return total_retrieved;
	}

	public String toString() {
		return toString(false, null);
	}

	public String toString(boolean showEachQueryPerformance, Performance baseline) {
		StringBuffer ret = new StringBuffer();

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(5);
		nf.setGroupingUsed(false);

		Set<String> queryIds = new TreeSet<String>(metricQueryScores.getCounter(MetricType.RETRIEVED).keySet());

		ret.append(String.format("[Performance for Top-%d]\n", top_n));
		ret.append(String.format("Queries:\t%d\n", queryIds.size()));
		ret.append(String.format("Relevant:\t%d\n", total_relevant));
		ret.append(String.format("Retrieved:\t%d\n", total_retrieved));
		ret.append(String.format("Relevant in Retrieved:\t%d\n", total_relevant_in_ret));
		ret.append(String.format("Relevant@%d:\t%d\n", top_n, total_relevant_at_n));
		ret.append(String.format("P@%d:\t%s\n", top_n, nf.format(precision)));
		ret.append(String.format("MAP@%d:\t%s\n", top_n, nf.format(map)));
		ret.append(String.format("NDCG@%d:\t%s\n", top_n, nf.format(ndcg)));
		ret.append(String.format("P_RRT@%d:\t%s\n", top_n, nf.format(precision_rrt)));
		ret.append(String.format("MAP_RRT@%d:\t%s\n", top_n, nf.format(map_rrt)));
		ret.append(String.format("NDCG_RRT@%d:\t%s\n", top_n, nf.format(ndcg_rrt)));

		if (showEachQueryPerformance) {
			MetricType[] mts = { MetricType.RETRIEVED, MetricType.RELEVANT, MetricType.RELEVANT_IN_RET, MetricType.RELEVANT_AT,
					MetricType.P, MetricType.AP, MetricType.NDCG };

			ret.append("[Individual Performances]\n");
			ret.append("Id");
			for (MetricType mt : mts) {
				ret.append(String.format("\t%s", mt));
			}

			if (baseline != null) {
				for (int i = 4; i < mts.length; i++) {
					ret.append(String.format("\t%s_CHN", mts[i]));
				}
			}

			ret.append("\n");

			for (String qId : queryIds) {
				ret.append(qId);
				for (int i = 0; i < mts.length; i++) {
					MetricType mt = mts[i];
					double score = metricQueryScores.getCount(mt, qId);
					if (i < 4) {
						ret.append("\t" + (int) score);
					} else {
						ret.append("\t" + nf.format(score));
					}
				}

				if (baseline != null) {
					for (int i = 4; i < mts.length; i++) {
						MetricType mt = mts[i];
						double score = metricQueryScores.getCount(mt, qId);
						double baseline_score = baseline.getMetricQueryScores().getCount(mt, qId);
						double change = score - baseline_score;
						ret.append("\t" + nf.format(change));
					}
				}

				ret.append("\n");
			}
		}

		return ret.toString().trim();
	}
}
