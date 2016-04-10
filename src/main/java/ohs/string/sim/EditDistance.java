package ohs.string.sim;

import ohs.math.ArrayMath;

public class EditDistance<E> implements StringScorer<E> {
	private class ScoreMatrix extends MemoMatrix<E> {

		public ScoreMatrix(Sequence<E> s, Sequence<E> t) {
			super(s, t);
		}

		@Override
		public double compute(int i, int j) {
			if (i == 0)
				return j;
			if (j == 0)
				return i;

			double cost = 0;

			E si = getSource().get(i - 1);
			E tj = getTarget().get(j - 1);

			// String si = (String) getSource().get(i - 1);
			// String tj = (String) getTarget().get(j - 1);
			cost = si.equals(tj) ? 0 : 1;

			double replace_score = get(i - 1, j - 1) + cost;
			double delete_score = get(i - 1, j) + 1;
			double insert_score = get(i, j - 1) + 1;
			double[] scores = new double[] { delete_score, insert_score, replace_score };
			int index = ArrayMath.argMin(scores);
			double ret = scores[index];
			return ret;
		}
	}

	static public void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "COHEN", "MCCOHN" };

		String[] strs = { "ABCD", "ABCDE" };

		// String[] strs = { "I love New York !!!", "I hate New Mexico !!!" };

		EditDistance<Character> ed = new EditDistance<Character>();
		// MemoMatrix m = sw.compute(new CharSequence(strs[0]), new CharSequence(strs[1]));
		// MemoMatrix m = ed.compute(new StrSequence(strs[0]), new StrSequence(strs[1]));
		System.out.println(ed.getSimilarity(SequenceFactory.newCharSequence(strs[0]), SequenceFactory.newCharSequence(strs[1])));

		// System.out.println(m.getBestScore());

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	public EditDistance() {

	}

	public ScoreMatrix compute(Sequence<E> s, Sequence<E> t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		ret.get(s.length(), t.length());
		return ret;
	}

	public double getDistance(Sequence<E> s, Sequence<E> t) {
		ScoreMatrix sm = compute(s, t);
		return sm.get(s.length(), t.length());
	}

	public double getSimilarity(Sequence<E> s, Sequence<E> t) {
		ScoreMatrix sm = compute(s, t);
		double edit_dist = sm.get(s.length(), t.length());
		double longer = Math.max(s.length(), t.length());
		double ret = 1 - (edit_dist / longer);
		return ret;
	}

}
