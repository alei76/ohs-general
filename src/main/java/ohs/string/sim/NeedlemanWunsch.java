package ohs.string.sim;

import ohs.math.ArrayMath;
import ohs.types.Counter;

public class NeedlemanWunsch {

	private class ScoreMatrix extends MemoMatrix {
		public ScoreMatrix(String s, String t) {
			super(s, t);
			best = -Double.MAX_VALUE;
		}

		public double compute(int i, int j) {
			if (i == 0)
				return j * gap_cost;
			if (j == 0)
				return i * gap_cost;

			char si = getSource().charAt(i - 1);
			char tj = getTarget().charAt(j - 1);

			double cost = 0;

			if (si == tj) {
				cost = match_cost;
			} else {
				cost = unmatch_cost;
			}

			double substitute_score = get(i - 1, j - 1) + cost;
			double delete_score = get(i - 1, j) + gap_cost;
			double insert_score = get(i, j - 1) + gap_cost;
			double[] scores = new double[] { substitute_score, delete_score, insert_score };
			int index = ArrayMath.argMax(scores);
			double ret = scores[index];
			return ret;
		}
	}

	public static void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		String[] strs = { "MCCOHN", "COHEN" };
		// String[] strs = { "ABG", "EFG" };
		// String[] strs = { "부산대학교 고분자공학과", "부산대학교 병원" };
		strs = new String[] { "국민은행", "국민대학교 금속재료공학부" };

		String s = strs[0];
		String t = strs[1];

		NeedlemanWunsch nw = new NeedlemanWunsch();
		System.out.println(nw.compute(s, t));
		System.out.println(nw.getNormalizedScore(s, t));

		SmithWaterman sw = new SmithWaterman();
		System.out.println(sw.compute(s, t));
		System.out.println(sw.getNormalizedScore(s, t));

		// System.out.println(ar.toString());

	}

	private Counter<Character> chWeights;

	private double match_cost;

	private double unmatch_cost;

	private double gap_cost;

	private boolean ignoreGap;

	public NeedlemanWunsch() {
		// this(1, 0, 0, false);
		this(0, -1, -1, false);
	}

	public NeedlemanWunsch(double match_cost, double unmatch_cost, double gap_cost, boolean ignoreGap) {
		this.match_cost = match_cost;
		this.unmatch_cost = unmatch_cost;
		this.gap_cost = gap_cost;
		this.ignoreGap = ignoreGap;
	}

	public MemoMatrix compute(String s, String t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		ret.compute(s.length(), t.length());
		return ret;
	}

	public double getNormalizedScore(String s, String t) {
		MemoMatrix m = compute(s, t);
		double dissam_score = m.getValues()[s.length() - 1][t.length() - 1];
		double max_dissam_score = 0;
		
		if (s.length() > t.length()) {
			max_dissam_score = m.getValues()[s.length() - 1][0];
		} else {
			max_dissam_score = m.getValues()[0][t.length() - 1];
		}
		double dissam = dissam_score / max_dissam_score;
		double sim = 1 - dissam;
		return sim;
	}

	public double getScore(String s, String t) {
		MemoMatrix m = compute(s, t);
		double ret = m.getValues()[s.length() - 1][t.length() - 1];
		return ret;
	}

	public void setChWeight(Counter<Character> chWeights) {
		this.chWeights = chWeights;
	}

	public String toString() {
		return "[Needleman Wunsch]";
	}

}
