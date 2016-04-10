package ohs.string.sim;

import ohs.math.ArrayMath;

public class NeedlemanWunsch<E> implements StringScorer<E> {

	private class ScoreMatrix extends MemoMatrix<E> {
		public ScoreMatrix(Sequence<E> s, Sequence<E> t) {
			super(s, t);
		}

		@Override
		public double compute(int i, int j) {
			if (i == 0)
				return j * gap_cost;
			if (j == 0)
				return i * gap_cost;

			E si = getSource().get(i - 1);
			E tj = getTarget().get(j - 1);

			double cost = si.equals(tj) ? match_cost : unmatch_cost;
			double replace_score = get(i - 1, j - 1) + cost;
			double delete_score = get(i - 1, j) + gap_cost;
			double insert_score = get(i, j - 1) + gap_cost;
			double ret = ArrayMath.max(new double[] { replace_score, delete_score, insert_score });
			if (ret < min) {
				min = ret;
				indexAtMin.set(i, j);
			}
			return ret;
		}
	}

	public static void main(String[] argv) {
		// String[] strs = { "You and I love New York !!!", "I hate New Mexico !!!" };
		// String[] strs = { "I love New York !!!", "I love New York !!!" };
		String[] strs = { "ABCD", "ABBBBBCD" };
		//
		NeedlemanWunsch<Character> nw = new NeedlemanWunsch<Character>();

		// System.out.println(nw.compute(new CharSequence(strs[0]), new CharSequence(strs[1])));
		// System.out.println(nw.getSimilarity(new CharSequence(strs[0]), new CharSequence(strs[1])));

		// Aligner aligner = new Aligner();
		// AlignResult ar = aligner.align(nw.compute(new CharSequence(strs[0]), new CharSequence(strs[1])));
		// System.out.println(ar);
	}

	private double match_cost;

	private double unmatch_cost;

	private double gap_cost;

	public NeedlemanWunsch() {
		// this(1, 0, 0, false);
		this(1, -1, -1);
	}

	public NeedlemanWunsch(double match_cost, double unmatch_cost, double gap_cost) {
		this.match_cost = match_cost;
		this.unmatch_cost = unmatch_cost;
		this.gap_cost = gap_cost;
	}

	public ScoreMatrix compute(Sequence<E> s, Sequence<E> t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		ret.compute(s.length(), t.length());
		return ret;
	}

	@Override
	public double getSimilarity(Sequence<E> s, Sequence<E> t) {
		ScoreMatrix m = compute(s, t);

		double score = m.get(s.length(), t.length());
		double max_score = Math.max(s.length(), t.length());
		double min = max_score;

		if (Math.max(match_cost, unmatch_cost) > gap_cost) {
			max_score *= Math.max(match_cost, unmatch_cost);
		} else {
			max_score *= gap_cost;
		}

		if (Math.min(match_cost, unmatch_cost) < gap_cost) {
			min *= Math.min(match_cost, unmatch_cost);
		} else {
			min *= gap_cost;
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
