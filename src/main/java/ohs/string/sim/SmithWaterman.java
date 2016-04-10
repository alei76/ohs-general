package ohs.string.sim;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.utils.Conditions;

public class SmithWaterman<E> implements StringScorer<E> {

	private class ScoreMatrix extends MemoMatrix<E> {
		public ScoreMatrix(Sequence<E> s, Sequence<E> t) {
			super(s, t);
		}

		@Override
		public double compute(int i, int j) {
			if (i == 0)
				return 0;
			if (j == 0)
				return 0;

			E si = getSource().get(i - 1);
			E tj = getTarget().get(j - 1);

			double cost = Conditions.value(si.equals(tj), match_cost, unmatch_cost);
			double replace_score = get(i - 1, j - 1) + cost;
			double delete_score = get(i - 1, j) + gap_cost;
			double insert_score = get(i, j - 1) + gap_cost;
			double ret = ArrayMath.max(ArrayUtils.array(0, replace_score, delete_score, insert_score));

			if (ret > max) {
				max = ret;
				indexAtMax.set(i, j);
			}
			return ret;
		}
	}

	public static void main(String[] argv) {
		String[] strs = { "You and I love New York !!!", "I hate New Mexico !!!" };

		SmithWaterman<Character> sw = new SmithWaterman<Character>();

		// System.out.println(sw.compute(new StrSequence(strs[0]), new StrSequence(strs[1])));
		System.out.println(sw.getSimilarity(SequenceFactory.newCharSequences(strs[0], strs[1])));

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

	public ScoreMatrix compute(Sequence<E> s, Sequence<E> t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		ret.compute(s.length(), t.length());
		return ret;
	}

	@Override
	public double getDistance(Sequence<E> s, Sequence<E> t) {
		return 0;
	}

	@Override
	public double getSimilarity(Sequence<E> s, Sequence<E> t) {
		ScoreMatrix m = compute(s, t);
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
