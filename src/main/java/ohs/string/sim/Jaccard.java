package ohs.string.sim;

import ohs.types.Counter;
import ohs.utils.Generics;

public class Jaccard<E> implements StringScorer<E> {
	static public void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "COHEN", "MCCOHN" };

		String[] strs = { "ABC", "ABCDEA" };

		// String[] strs = { "I love New York !!!", "I hate New Mexico !!!" };

		{

			Jaccard<Character> ed = new Jaccard<Character>();
			// MemoMatrix m = sw.compute(new CharSequence(strs[0]), new CharSequence(strs[1]));
			// MemoMatrix m = ed.compute(new StrSequence(strs[0]), new StrSequence(strs[1]));
			System.out.println(ed.getSimilarity(SequenceFactory.newCharSequences(strs[0], strs[1])));

		}

		// System.out.println(m.getBestScore());

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	public Counter<E> getTokenCounts(Sequence<E> s) {
		Counter<E> ret = Generics.newCounter();
		for (E key : s.values()) {
			ret.incrementCount(key, 1);
		}
		return ret;
	}

	@Override
	public double getSimilarity(Sequence<E> s, Sequence<E> t) {

		Counter<E> small = getTokenCounts(s);
		Counter<E> large = getTokenCounts(t);
		double num_commons = 0;

		if (small.size() > large.size()) {
			Counter<E> tmp = small;
			small = large;
			large = tmp;
		}

		for (E key : small.keySet()) {
			if (large.containsKey(key)) {
				num_commons++;
			}
		}
		double ret = num_commons / (small.size() + large.size() - num_commons);
		return ret;
	}

	public double getDistance(Sequence<E> s, Sequence<E> t) {
		return 0;
	}
}
