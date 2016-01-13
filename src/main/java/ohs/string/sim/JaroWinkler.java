package ohs.string.sim;

/**
 * Winkler's reweighting scheme for distance metrics. In the literature, this was applied to the Jaro metric ('An Application of the
 * Fellegi-Sunter Model of Record Linkage to the 1990 U.S. Decennial Census' by William E. Winkler and Yves Thibaudeau.)
 */

public class JaroWinkler {
	private static int commonPrefixLength(int maxLength, Sequence common1, Sequence common2) {
		int n = Math.min(maxLength, Math.min(common1.length(), common2.length()));
		for (int i = 0; i < n; i++) {
			if (!common1.get(i).equals(common2.get(i)))
				return i;
		}
		return n; // first n characters are the same
	}

	static public void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "COHEN", "MCCOHN" };

		// String[] strs = { "MARTHA", "MARHTA" };
		// String[] strs = { "DIXON", "DICKSONX" };
		String[] strs = { "ABC", "ABC" };

		// String[] strs = { "I love New York !!!", "I hate New Mexico !!!" };

		{

			JaroWinkler ed = new JaroWinkler();
			// MemoMatrix m = sw.compute(new CharacterSequence(strs[0]), new CharacterSequence(strs[1]));
			// MemoMatrix m = ed.compute(new StringSequence(strs[0]), new StringSequence(strs[1]));
			System.out.println(ed.getSimilarity(new CharacterSequence(strs[0]), new CharacterSequence(strs[1])));

		}

		// System.out.println(m.getBestScore());

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	private Jaro jaro;

	private double p = 0.1;

	public JaroWinkler() {
		jaro = new Jaro();
	}

	/**
	 * Rescore the jaro's scores, to account for the subjectively greater importance of the first few characters.
	 * <p>
	 * Note: the jaro must produce scores between 0 and 1.
	 */
	public JaroWinkler(Jaro innerDistance) {
		this.jaro = innerDistance;
	}

	public double getSimilarity(Sequence s, Sequence t) {
		double dist = jaro.getSimilarity(s, t);
		if (dist < 0 || dist > 1)
			throw new IllegalArgumentException("jaro should produce scores between 0 and 1");
		int prefix_len = commonPrefixLength(4, s, t);
		dist = dist + prefix_len * p * (1 - dist);
		return dist;
	}

}
