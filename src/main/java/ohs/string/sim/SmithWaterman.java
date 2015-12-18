package ohs.string.sim;

import java.util.HashSet;
import java.util.Set;

import ohs.entity.ENTPath;
import ohs.entity.data.struct.BilingualText;
import ohs.entity.org.DataReader;
import ohs.math.ArrayMath;
import ohs.types.Counter;

public class SmithWaterman {

	private class ScoreMatrix extends MemoMatrix {
		public ScoreMatrix(String s, String t) {
			super(s, t);
			best = -Double.MAX_VALUE;
		}

		public double compute(int i, int j) {
			if (i == 0)
				return 0;
			if (j == 0)
				return 0;

			char si = getSource().charAt(i - 1);
			char tj = getTarget().charAt(j - 1);

			double wi = 1;
			double wj = 1;

			if (chWeights != null) {
				wi = chWeights.getCount(si);
				wj = chWeights.getCount(tj);

				wi = Math.exp(wi);
				wj = Math.exp(wj);
			}

			double cost = 0;

			if (si == tj) {
				cost = wi * match_cost;
			} else {
				double avg_w = (wi + wj) / 2;
				cost = avg_w * unmatch_cost;
			}

			double substitute_score = get(i - 1, j - 1) + cost;
			double delete_score = get(i - 1, j) + wi * gap_cost;
			double insert_score = get(i, j - 1) + wj * gap_cost;
			double[] scores = new double[] { 0, substitute_score, delete_score, insert_score };
			int index = ArrayMath.argMax(scores);
			double ret = scores[index];

			if (ret > best) {
				best = ret;
				indexAtBest.set(i, j);
			}
			return ret;
		}
	}

	public static Counter<Character> getWeights() {
		Counter<BilingualText> c = DataReader.readBilingualTextCounter(ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE);

		Counter<Character> ret = new Counter<Character>();
		Counter<Character> docFreqs = new Counter<Character>();

		for (BilingualText orgName : c.keySet()) {
			String korName = orgName.getKorean();
			Set<Character> set = new HashSet<Character>();

			for (int i = 0; i < korName.length(); i++) {
				ret.incrementCount(korName.charAt(i), 1);
				set.add(korName.charAt(i));
			}

			for (char ch : set) {
				docFreqs.incrementCount(ch, 1);
			}
		}

		for (char ch : ret.keySet()) {
			double tf = Math.log(ret.getCount(ch)) + 1;
			double df = docFreqs.getCount(ch);
			double num_docs = c.size();
			double idf = Math.log((num_docs + 1) / df);
			double tfidf = tf * idf;
			ret.setCount(ch, idf);
		}

		return ret;
	}

	public static void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		String[] strs = { "MCCOHN", "COHEN" };
		// String[] strs = { "ABCDE", "FABCGG" };
		// strs = new String[] { "부산대학교 고분자공학과", "부산대학교 병원" };
		String s = strs[0];
		String t = strs[1];

		// Counter<Character> chWeights = getWeights();

		SmithWaterman sw = new SmithWaterman();
		// sw.setChWeight(chWeights);

		System.out.println(sw.getNormalizedScore(s, t));

		MemoMatrix m = sw.compute(s, t);

		Aligner al = new Aligner();
		AlignResult ar = al.align(m);

		System.out.println();
		System.out.println(m.toString());
		System.out.println();
		System.out.println(ar);

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	private Counter<Character> chWeights;

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

	public MemoMatrix compute(String s, String t) {
		ScoreMatrix ret = new ScoreMatrix(s, t);
		ret.compute(s.length(), t.length());
		return ret;
	}

	public double getNormalizedScore(String s, String t) {
		double score = getScore(s, t);
		double ret = 0;
		if (chWeights == null) {
			double max_match_score = match_cost * Math.min(s.length(), t.length());
			ret = score / max_match_score;
		} else {
			String temp = s;

			if (s.length() > t.length()) {
				temp = t;
			}

			double weight_sum = 0;
			for (int i = 0; i < temp.length(); i++) {
				double w = chWeights.getCount(temp.charAt(i));
				weight_sum += Math.exp(w);
			}
			ret = score / weight_sum;
		}

		return ret;
	}

	public double getScore(String s, String t) {
		return compute(s, t).getBestScore();
	}

	public void setChWeight(Counter<Character> chWeights) {
		this.chWeights = chWeights;
	}

	public String toString() {
		return "[Smith Waterman]";
	}

}
