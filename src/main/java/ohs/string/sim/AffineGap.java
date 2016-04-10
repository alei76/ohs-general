package ohs.string.sim;

import ohs.math.ArrayMath;

/**
 * @author Heung-Seon Oh
 * 
 *         Adaptation of AffineGap in SecondString
 * 
 */
public class AffineGap<E> implements StringScorer<E> {

	// a set of three linked distance matrices
	protected class MatrixTrio extends MemoMatrix<E> {
		protected class InsertSMatrix extends MemoMatrix<E> {
			public InsertSMatrix(Sequence<E> s, Sequence<E> t) {
				super(s, t);
			}

			@Override
			public double compute(int i, int j) {
				if (i == 0 || j == 0)
					return 0;
				double score1 = m.get(i - 1, j) + open_gap_cost;
				double score2 = is.get(i - 1, j) + extend_gap_cost;
				return ArrayMath.max(new double[] { lower_bound, score1, score2 });
			}
		}

		protected class InsertTMatrix extends MemoMatrix<E> {
			public InsertTMatrix(Sequence<E> s, Sequence<E> t) {
				super(s, t);
			}

			@Override
			public double compute(int i, int j) {
				if (i == 0 || j == 0)
					return 0;
				double score1 = m.get(i, j - 1) + open_gap_cost;
				double score2 = it.get(i, j - 1) + extend_gap_cost;
				return ArrayMath.max(new double[] { lower_bound, score1, score2 });
			}
		}

		protected MatrixTrio m;

		protected InsertSMatrix is;

		protected InsertTMatrix it;

		public MatrixTrio(Sequence<E> s, Sequence<E> t) {
			super(s, t);
			is = new InsertSMatrix(s, t);
			it = new InsertTMatrix(s, t);
			m = this;
		}

		@Override
		public double compute(int i, int j) {
			if (i == 0 || j == 0)
				return 0;

			E si = getSource().get(i - 1);
			E tj = getTarget().get(j - 1);
			double cost = si.equals(tj) ? match_cost : unmatch_cost;

			double score1 = m.get(i - 1, j - 1) + cost;
			double score2 = is.get(i - 1, j - 1) + cost;
			double score3 = it.get(i = 1, j - 1) + cost;
			// double score3 = it.get(i - 1, j = 1) + cost;
			double ret = ArrayMath.max(new double[] { lower_bound, score1, score2, score3 });
			return ret;
		}
	}

	static public void main(String[] argv) {

		// String[] strs = { "You and I love New York !!!", "I hate New Mexico !!!" };
		// String[] strs = { "I love New York !!!", "I love New York !!!" };
		String[] strs = { "ABCD", "ABCD" };

		AffineGap<Character> af = new AffineGap<Character>();

		// System.out.println(af.compute(new CharSequence(strs[0]), new CharSequence(strs[1])));
		System.out.println(af.getSimilarity(SequenceFactory.newCharSequences(strs[0], strs[1])));

	}

	private double open_gap_cost;

	private double extend_gap_cost;

	private double lower_bound;

	private double match_cost;

	private double unmatch_cost;

	public AffineGap() {
		this(2, -1, 2, 1, -Double.MAX_VALUE);
	}

	public AffineGap(double match_cost, double unmatch_cost, double open_gap_cost, double extend_gap_cost, double lower_bound) {
		this.match_cost = match_cost;
		this.unmatch_cost = unmatch_cost;
		this.open_gap_cost = open_gap_cost;
		this.extend_gap_cost = extend_gap_cost;
		this.lower_bound = lower_bound;
	}

	public MemoMatrix compute(Sequence<E> s, Sequence<E> t) {
		MatrixTrio ret = new MatrixTrio(s, t);
		ret.compute(s.length(), t.length());
		return ret;
	}

	@Override
	public double getSimilarity(Sequence<E> s, Sequence<E> t) {
		MemoMatrix m = compute(s, t);

		System.out.println(m.toString());
		double score = ArrayMath.max(m.getValues());
		double max_score = Math.max(s.length(), t.length());
		double min = max_score;
		if (Math.max(match_cost, unmatch_cost) > open_gap_cost) {
			max_score *= Math.max(match_cost, unmatch_cost);
		} else {
			max_score *= open_gap_cost;
		}
		if (Math.min(match_cost, unmatch_cost) < open_gap_cost) {
			min *= Math.min(match_cost, unmatch_cost);
		} else {
			min *= open_gap_cost;
		}
		if (min < 0.0f) {
			max_score -= min;
			score -= min;
		}

		// check for 0 maxLen
		if (max_score == 0) {
			return 1.0f; // as both strings identically zero length
		} else {
			return (score / max_score);
		}

	}

	@Override
	public double getDistance(Sequence<E> s, Sequence<E> t) {
		return 0;
	}

}
