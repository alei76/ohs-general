package ohs.string.sim;

import ohs.math.ArrayMath;

public class EditDistance {
	private class ScoreMatrix extends MemoMatrix {

		public ScoreMatrix(String s, String t) {
			super(s, t);
		}

		public double compute(int i, int j) {
			if (i == 0)
				return j;
			if (j == 0)
				return i;

			char si = getSource().charAt(i - 1);
			char tj = getTarget().charAt(j - 1);

			double cost = si == tj ? 0 : 1;
			double substitute_score = get(i - 1, j - 1) + cost;
			double delete_score = get(i - 1, j) + 1;
			double insert_score = get(i, j - 1) + 1;
			double[] scores = new double[] { delete_score, insert_score, substitute_score };
			int index = ArrayMath.argMin(scores);
			double ret = scores[index];

			return ret;
		}
	}

	static public void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "COHEN", "MCCOHN" };
		String[] sr1 = { "om", "beca" };
		String[] sr2 = { "ot", "yoytu" };
		// String[] sr2 = { "kitten", "sitting" };

		EditDistance sw = new EditDistance();
		MemoMatrix m = sw.compute(sr1[0], sr1[1]);
		System.out.println(m.toString());

		m = sw.compute(sr2[0], sr2[1]);
		System.out.println(m.toString());

		// System.out.println(m.getBestScore());

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	public EditDistance() {

	}

	public ScoreMatrix compute(String s, String t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		compute(s, t, ret);
		return ret;
	}

	private void compute(String s, String t, ScoreMatrix m) {
		m.get(s.length(), t.length());
	}

	public double getDistance(String s, String t) {
		ScoreMatrix sm = compute(s, t);
		return sm.get(s.length(), t.length());
	}

	public double getNormalizedScore(String s, String t) {
		double dist = getDistance(s, t);
		double longer = (s.length() > t.length()) ? s.length() : t.length();
		double ret = 1 - (dist / longer);
		return ret;
	}

	public String toString() {
		return "[Edit Distance]";
	}

}
