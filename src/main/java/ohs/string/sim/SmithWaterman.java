package ohs.string.sim;

import ohs.math.ArrayMath;

public class SmithWaterman {

	private class ScoreMatrix extends MemoMatrix {
		public ScoreMatrix(Sequence s, Sequence t) {
			super(s, t);
		}

		public double compute(int i, int j) {
			if (i == 0)
				return 0;
			if (j == 0)
				return 0;

			String si = getSource().get(i - 1);
			String tj = getTarget().get(j - 1);

			double cost = 0;

			if (si.equals(tj)) {
				cost = match_cost;
			} else {
				cost = unmatch_cost;
			}

			double replace_score = get(i - 1, j - 1) + cost;
			double delete_score = get(i - 1, j) + gap_cost;
			double insert_score = get(i, j - 1) + gap_cost;
			double ret = ArrayMath.max(new double[] { 0, replace_score, delete_score, insert_score });

			if (ret > max) {
				max = ret;
				indexAtMax.set(i, j);
			}
			return ret;
		}
	}

	public static void main(String[] argv) {
		String[] strs = { "You and I love New York !!!", "I hate New Mexico !!!" };

		SmithWaterman sw = new SmithWaterman();

		// System.out.println(sw.compute(new StringSequence(strs[0]), new StringSequence(strs[1])));
		System.out.println(sw.getSimilarity(new StringSequence(strs[0]), new StringSequence(strs[1])));

	}

	private double match_cost;

	private double unmatch_cost;

	private double gap_cost;

	public SmithWaterman() {
		this(2, -1, -1);
		// this(3, 2, 1, false);
	}

	public SmithWaterman(double match_cost, double unmatch_cost, double gap_cost) {
		this.match_cost = match_cost;
		this.unmatch_cost = unmatch_cost;
		this.gap_cost = gap_cost;
	}

	public MemoMatrix compute(Sequence s, Sequence t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		ret.compute(s.length(), t.length());
		return ret;
	}

	public double getSimilarity(Sequence s, Sequence t) {
		MemoMatrix m = compute(s, t);
		double score = m.getMaxScore();
		float max_score = Math.min(s.length(), t.length());
		if (Math.max(match_cost, unmatch_cost) > -gap_cost) {
			max_score *= Math.max(match_cost, unmatch_cost);
		} else {
			max_score *= -gap_cost;
		}

		// check for 0 maxLen
		if (max_score == 0) {
			return 1.0f; // as both strings identically zero length
		} else {
			return (score / max_score);
		}
	}

}
