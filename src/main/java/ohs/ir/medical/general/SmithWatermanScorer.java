package ohs.ir.medical.general;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import ohs.math.ArrayMath;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.Pair;

public class SmithWatermanScorer {

	public class ScoreMatrix {

		private double[][] values;

		private List<Integer> s;

		private List<Integer> t;

		private Pair<Integer, Integer> indexAtBest;

		ScoreMatrix(List<Integer> s, List<Integer> t, double[][] values, Pair<Integer, Integer> indexAtBest) {
			this.s = s;
			this.t = t;
			this.values = values;
			this.indexAtBest = indexAtBest;
		}

		public double get(int i, int j) {
			return values[i][j];
		}

		public double getBestScore() {
			return values[indexAtBest.getFirst()][indexAtBest.getSecond()];
		}

		public List<Integer> getSource() {
			return s;
		}

		public List<Integer> getTarget() {
			return t;
		}

		public double[][] getValues() {
			return values;
		}

		public void set(int i, int j, double value) {
			values[i][j] = value;
		}

		@Override
		public String toString() {
			return toString(null);
		}

		public String toString(Indexer<String> wordIndexer) {
			StringBuilder sb = new StringBuilder();

			sb.append("<S/T>");
			for (int i = 0; i < t.size(); i++) {
				if (wordIndexer == null) {
					sb.append("\t" + t.get(i));
				} else {
					int w1 = t.get(i);
					String word = wordIndexer.getObject(w1);
					sb.append("\t" + word);
				}
			}

			sb.append("\n");

			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(0);
			nf.setGroupingUsed(false);

			int best_i = indexAtBest.getFirst();
			int best_j = indexAtBest.getSecond();

			for (int i = 1; i <= s.size(); i++) {
				if (wordIndexer == null) {
					sb.append(s.get(i - 1));
					for (int j = 1; j <= t.size(); j++) {
						double v = values[i][j];
						sb.append("\t" + nf.format(v));
					}
					sb.append("\n");
				} else {
					int index = s.get(i - 1);
					String word = wordIndexer.getObject(index);
					sb.append(word);
					for (int j = 1; j <= t.size(); j++) {
						double v = values[i][j];

						if (best_i == i && best_j == j) {
							sb.append("\t## " + nf.format(v));
						} else {
							sb.append("\t" + nf.format(v));
						}
					}
					sb.append("\n");
				}
			}
			return sb.toString();
		}
	}

	public static void main(String[] args) {
		List<String> s = new ArrayList<String>();
		List<String> t = new ArrayList<String>();

		String[] strs = { "MCCOHN", "COHEN" };

		for (int i = 0; i < strs[0].length(); i++) {
			s.add(strs[0].charAt(i) + "");
		}

		for (int i = 0; i < strs[1].length(); i++) {
			t.add(strs[1].charAt(i) + "");
		}

		// text.add("a");
		// text.add("e");
		//
		// t.add("a");
		// t.add("e");
		// t.add("a");

		SmithWatermanScorer sw = new SmithWatermanScorer();
		// sw.setChWeight(weights);

		// System.out.println(sw.compute(text, t));
		//
		//
		// SmithWatermanAligner al = new SmithWatermanAligner();
		// AlignResult ar = al.align(m);
		//
		// System.out.println();
		// System.out.println(m.toString());
		// System.out.println();
		// System.out.println(ar);

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	private Counter<Integer> weights;

	private double match_cost;

	private double unmatch_cost;

	private double gap_cost;

	private double[][] sw;

	public SmithWatermanScorer() {
		this(2, -1, -1);
		// this(3, 2, 1, false);
	}

	public SmithWatermanScorer(double match_cost, double unmatch_cost, double gap_cost) {
		this.match_cost = match_cost;
		this.unmatch_cost = unmatch_cost;
		this.gap_cost = gap_cost;
	}

	public ScoreMatrix compute(List<Integer> s, List<Integer> t) {
		int num_rows = s.size() + 1;
		int num_cols = t.size() + 1;

		double[][] sw = new double[num_rows][num_cols];

		int max_i = 0;
		int max_j = 0;
		double max = -Double.MAX_VALUE;

		for (int i = 1; i < num_rows; i++) {
			int si = s.get(i - 1);

			for (int j = 1; j < num_cols; j++) {
				double cost = 0;

				int tj = t.get(j - 1);

				double wi = 1;
				double wj = 1;

				boolean isMatched = false;

				if (si == tj) {
					cost = wi * match_cost;
				} else {
					double avg_w = (wi + wj) / 2;
					cost = avg_w * unmatch_cost;
				}

				double substitute_score = sw[i - 1][j - 1] + cost;
				double delete_score = sw[i - 1][j] + wi * gap_cost;
				double insert_score = sw[i][j - 1] + wj * gap_cost;
				double[] scores = new double[] { 0, substitute_score, delete_score, insert_score };
				int index = ArrayMath.argMax(scores);
				double value = scores[index];
				sw[i][j] = value;

				if (value > max) {
					max = value;
					max_i = i;
					max_j = j;
				}
			}
		}

		return new ScoreMatrix(s, t, sw, new Pair<Integer, Integer>(max_i, max_j));
	}

	public double getNormalizedScore(ScoreMatrix sm) {
		double score = sm.getBestScore();

		List<Integer> s = sm.getSource();
		List<Integer> t = sm.getTarget();

		double ret = 0;
		if (weights == null) {
			double max_match_score = match_cost * Math.min(s.size(), t.size());
			ret = score / max_match_score;
		} else {
			List<Integer> temp = s;

			if (s.size() > t.size()) {
				temp = t;
			}

			double weight_sum = 0;
			for (int i = 0; i < temp.size(); i++) {
				weight_sum += weights.getCount(temp.get(i));
			}
			ret = score / weight_sum;
		}
		return ret;
	}

	public void setWeights(Counter<Integer> weights) {
		this.weights = weights;
	}

}
