package ohs.string.sim;

import ohs.math.ArrayMath;

public class EditDistance implements SimScorer {
	private class ScoreMatrix extends MemoMatrix {

		public ScoreMatrix(Sequence s, Sequence t) {
			super(s, t);
		}

		@Override
		public double compute(int i, int j) {
			if (i == 0)
				return j;
			if (j == 0)
				return i;

			String si = getSource().get(i - 1);
			String tj = getTarget().get(j - 1);

			double cost = si.equals(tj) ? 0 : 1;
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

		String[] strs = { "ABCD", "ABCD" };

		// String[] strs = { "I love New York !!!", "I hate New Mexico !!!" };

		EditDistance ed = new EditDistance();
		// MemoMatrix m = sw.compute(new CharacterSequence(strs[0]), new CharacterSequence(strs[1]));
		// MemoMatrix m = ed.compute(new StringSequence(strs[0]), new StringSequence(strs[1]));
		System.out.println(ed.getSimilarity(new StringSequence(strs[0]), new StringSequence(strs[1])));

		// System.out.println(m.getBestScore());

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	public EditDistance() {

	}

	public ScoreMatrix compute(Sequence s, Sequence t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		ret.get(s.length(), t.length());
		return ret;
	}

	public double getDistance(Sequence s, Sequence t) {
		ScoreMatrix sm = compute(s, t);
		return sm.get(s.length(), t.length());
	}

	public double getSimilarity(Sequence s, Sequence t) {
		ScoreMatrix sm = compute(s, t);
		double edit_dist = sm.get(s.length(), t.length());
		double longer = Math.max(s.length(), t.length());
		double ret = 1 - (edit_dist / longer);
		return ret;
	}

}
