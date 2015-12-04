package ohs.string.sim;

import ohs.math.ArrayMath;

/**
 * @author Heung-Seon Oh
 * 
 *         Adaptation of AffineGap in SecondString
 * 
 */
public class AffineGap {

	// a set of three linked distance matrices
	protected class MatrixTrio extends MemoMatrix {
		protected class InsertSMatrix extends MemoMatrix {
			public InsertSMatrix(String s, String t) {
				super(s, t);
			}

			public double compute(int i, int j) {
				if (i == 0 || j == 0)
					return 0;
				double score1 = m.get(i - 1, j) + open_gap_score;
				double score2 = is.get(i - 1, j) + extend_gap_score;
				return ArrayMath.max(new double[] { lower_bound, score1, score2 });
			}
		}

		protected class InsertTMatrix extends MemoMatrix {
			public InsertTMatrix(String s, String t) {
				super(s, t);
			}

			public double compute(int i, int j) {
				if (i == 0 || j == 0)
					return 0;
				double score1 = m.get(i, j - 1) + open_gap_score;
				double score2 = it.get(i, j - 1) + extend_gap_score;
				return ArrayMath.max(new double[] { lower_bound, score1, score2 });
			}
		}

		protected MemoMatrix m;

		protected InsertSMatrix is;

		protected InsertTMatrix it;

		public MatrixTrio(String s, String t) {
			super(s, t);
			is = new InsertSMatrix(s, t);
			it = new InsertTMatrix(s, t);
			m = this;

			best = -Double.MAX_VALUE;
		}

		public double compute(int i, int j) {
			if (i == 0 || j == 0)
				return 0;

			char si = getSource().charAt(i - 1);
			char tj = getTarget().charAt(j - 1);
			double cost = si == tj ? match_cost : unmatch_cost;

			double score1 = m.get(i - 1, j - 1) + cost;
			double score2 = is.get(i - 1, j - 1) + cost;
			// double score3 = it.get(i = 1, j - 1) + cost;
			double score3 = it.get(i - 1, j = 1) + cost;
			double[] scores = new double[] { lower_bound, score1, score2, score3 };
			int max_index = ArrayMath.argMax(scores);
			double max = scores[max_index];

			if (max > best) {
				best = max;
				indexAtBest.set(i, j);
			}

			return max;
		}
	}

	static public void main(String[] argv) {
		AffineGap aligner = new AffineGap();

		String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		strs = new String[] { "ABC", "ABBBBBC" };
		strs = new String[] { "COHEN", "MCCOHN" };
		// String[] strs = { "국민은행", "국민대학교 금속재료공학부" };
		String s = strs[0];
		String t = strs[1];

		AffineGap af = new AffineGap();
		MemoMatrix m = af.compute(s, t);

		Aligner al = new Aligner();
		AlignResult ar = al.align(m);

		System.out.println(m.toString());
		System.out.println(ar.toString());

	}

	private double open_gap_score;

	private double extend_gap_score;

	private double lower_bound;

	private double match_cost;

	private double unmatch_cost;

	public AffineGap() {
		this(2, -1, 2, 1, -Double.MAX_VALUE);
	}

	public AffineGap(double match_score, double unmatch_score, double open_gap_score, double extend_gap_score, double lower_bound) {
		this.match_cost = match_score;
		this.unmatch_cost = unmatch_score;
		this.open_gap_score = open_gap_score;
		this.extend_gap_score = extend_gap_score;
		this.lower_bound = lower_bound;
	}

	public MemoMatrix compute(String s, String t) {
		MatrixTrio ret = new MatrixTrio(s, t);
		ret.compute(s.length(), t.length());
		return ret;
	}

	public double getScore(String s, String t) {
		return compute(s, t).getBestScore();
	}

}
