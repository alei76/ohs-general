package ohs.string.sim;

import java.util.List;

import ohs.utils.Generics;

/**
 * Jaro distance metric. From 'An Application of the Fellegi-Sunter Model of Record Linkage to the 1990 U.S. Decennial Census' by William E.
 * Winkler and Yves Thibaudeau.
 */

public class Jaro implements SimScorer {
	static public void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "COHEN", "MCCOHN" };

		// String[] strs = { "MARTHA", "MARHTA" };
		// String[] strs = { "DIXON", "DICKSONX" };
		String[] strs = { "ABC", "ABC" };

		// String[] strs = { "I love New York !!!", "I hate New Mexico !!!" };

		{

			Jaro ed = new Jaro();
			// MemoMatrix m = sw.compute(new CharSequence(strs[0]), new CharSequence(strs[1]));
			// MemoMatrix m = ed.compute(new StrSequence(strs[0]), new StrSequence(strs[1]));
			System.out.println(ed.getSimilarity(new CharSequence(strs[0]), new CharSequence(strs[1])));

		}

		// System.out.println(m.getBestScore());

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	public Jaro() {
	}

	private Sequence common(Sequence s, Sequence t, int halflen) {
		// StringBuilder common = new StringBuilder();
		// StringBuilder copy = new StringBuilder(t);

		List<String> common = Generics.newArrayList();
		List<String> copy = Generics.newArrayList(t.length());

		for (int i = 0; i < t.length(); i++) {
			copy.add(t.get(i));
		}

		for (int i = 0; i < s.length(); i++) {
			String ch = s.get(i);
			boolean foundIt = false;
			for (int j = Math.max(0, i - halflen); !foundIt && j < Math.min(i + halflen, t.length()); j++) {
				if (copy.get(j).equals(ch)) {
					foundIt = true;
					common.add(ch);
					copy.set(j, "*");
				}
			}
		}

		return new StrSequence(common);
	}

	@Override
	public double getSimilarity(Sequence s, Sequence t) {
		int halflen = halfLengthOfShorter(s, t);
		Sequence common1 = common(s, t, halflen);
		Sequence common2 = common(t, s, halflen);
		if (common1.length() != common2.length())
			return 0;
		if (common1.length() == 0 || common2.length() == 0)
			return 0;
		int transpositions = transpositions(common1, common2);
		double s1 = 1f * common1.length() / s.length();
		double s2 = 1f * common2.length() / t.length();
		double s3 = 1f * (common1.length() - transpositions) / common1.length();
		double dist = (s1 + s2 + s3) / 3;
		// double dist = (common1.length() / ((double) text.length()) + common2.length() / ((double) t.length())
		// + (common1.length() - transpositions) / ((double) common1.length())) / 3.0;
		return dist;
	}

	private int halfLengthOfShorter(Sequence str1, Sequence str2) {
		int ret = Math.min(str1.length(), str2.length());
		ret = ret / 2 + 1;
		return ret;
	}

	private int transpositions(Sequence common1, Sequence common2) {
		int transpositions = 0;
		for (int i = 0; i < common1.length(); i++) {
			if (!common1.get(i).equals(common2.get(i))) {
				transpositions++;
			}
		}
		transpositions /= 2;
		return transpositions;
	}

	@Override
	public double getDistance(Sequence s, Sequence t) {
		return 0;
	}

}
