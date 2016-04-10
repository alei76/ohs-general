package ohs.string.sim;

/**
 * Winkler'text reweighting scheme for distance metrics. In the literature, this was applied to the Jaro metric ('An Application of the
 * Fellegi-Sunter Model of Record Linkage to the 1990 U.S. Decennial Census' by William E. Winkler and Yves Thibaudeau.)
 */

public class JaroWinkler<E> implements StringScorer<E> {

	static public void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "COHEN", "MCCOHN" };

		// String[] strs = { "MARTHA", "MARHTA" };
		// String[] strs = { "DIXON", "DICKSONX" };
		String[] strs = { "ABC", "ABC" };

		// String[] strs = { "I love New York !!!", "I hate New Mexico !!!" };

		{

			JaroWinkler<Character> ed = new JaroWinkler<Character>();
			// MemoMatrix m = sw.compute(new CharSequence(strs[0]), new CharSequence(strs[1]));
			// MemoMatrix m = ed.compute(new StrSequence(strs[0]), new StrSequence(strs[1]));
			System.out.println(ed.getSimilarity(SequenceFactory.newCharSequences(strs[0], strs[1])));

		}

		// System.out.println(m.getBestScore());

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	private Jaro<E> jaro;

	private double p = 0.1;

	public JaroWinkler() {
		jaro = new Jaro<E>();
	}

	/**
	 * Rescore the jaro'text scores, to account for the subjectively greater importance of the first few characters.
	 * <p>
	 * Note: the jaro must produce scores between 0 and 1.
	 */
	public JaroWinkler(Jaro<E> innerDistance) {
		this.jaro = innerDistance;
	}

	private int commonPrefixLength(int maxLength, Sequence<E> common1, Sequence<E> common2) {
		int n = Math.min(maxLength, Math.min(common1.length(), common2.length()));
		for (int i = 0; i < n; i++) {
			if (!common1.get(i).equals(common2.get(i)))
				return i;
		}
		return n; // first n characters are the same
	}

	@Override
	public double getDistance(Sequence<E> s, Sequence<E> t) {
		return 0;
	}

	@Override
	public double getSimilarity(Sequence<E> s, Sequence<E> t) {
		double dist = jaro.getSimilarity(s, t);
		if (dist < 0 || dist > 1)
			throw new IllegalArgumentException("jaro should produce scores between 0 and 1");
		int prefix_len = commonPrefixLength(4, s, t);
		dist = dist + prefix_len * p * (1 - dist);
		return dist;
	}

}
