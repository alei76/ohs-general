package ohs.string.sim;

import java.lang.reflect.Array;
import java.util.List;

import ohs.utils.Generics;

/**
 * Jaro distance metric. From 'An Application of the Fellegi-Sunter Model of Record Linkage to the 1990 U.S. Decennial Census' by William E.
 * Winkler and Yves Thibaudeau.
 */

public class Jaro<E> implements StringScorer<E> {
	static public void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "COHEN", "MCCOHN" };

		// String[] strs = { "MARTHA", "MARHTA" };
		// String[] strs = { "DIXON", "DICKSONX" };
		String[] strs = { "ABC", "ABC" };

		// String[] strs = { "I love New York !!!", "I hate New Mexico !!!" };

		{

			Jaro<Character> ed = new Jaro<Character>();
			// MemoMatrix m = sw.compute(new CharSequence(strs[0]), new CharSequence(strs[1]));
			// MemoMatrix m = ed.compute(new StrSequence(strs[0]), new StrSequence(strs[1]));
			System.out.println(ed.getSimilarity(SequenceFactory.newCharSequences(strs[0], strs[1])));

		}

		// System.out.println(m.getBestScore());

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	public Jaro() {
	}

	private Sequence<E> common(Sequence<E> s, Sequence<E> t, int halflen) {
		// StringBuilder common = new StringBuilder();
		// StringBuilder copy = new StringBuilder(t);

		List<E> common = Generics.newArrayList();
		List<E> copy = Generics.newArrayList(t.length());

		for (int i = 0; i < t.length(); i++) {
			copy.add(t.get(i));
		}

		for (int i = 0; i < s.length(); i++) {
			E ch = s.get(i);
			boolean foundIt = false;
			for (int j = Math.max(0, i - halflen); !foundIt && j < Math.min(i + halflen, t.length()); j++) {
				if (copy.get(j).equals(ch)) {
					foundIt = true;
					common.add(ch);
					// copy.set(j, "*");
					copy.set(j, null);
				}
			}
		}

		Class c = common.get(0).getClass();
		E[] os = (E[]) Array.newInstance(c, common.size());

		for (int i = 0; i < common.size(); i++) {
			os[i] = common.get(i);
		}
		return new Sequence<E>(os);
	}

	@Override
	public double getSimilarity(Sequence<E> s, Sequence<E> t) {
		int halflen = halfLengthOfShorter(s, t);
		Sequence<E> common1 = common(s, t, halflen);
		Sequence<E> common2 = common(t, s, halflen);

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

	private int halfLengthOfShorter(Sequence<E> str1, Sequence<E> str2) {
		int ret = Math.min(str1.length(), str2.length());
		ret = ret / 2 + 1;
		return ret;
	}

	private int transpositions(Sequence<E> common1, Sequence<E> common2) {
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
	public double getDistance(Sequence<E> s, Sequence<E> t) {
		return 0;
	}

}
