package ohs.string.sim;

import ohs.types.Counter;

public class Jaccard implements SimScorer {
	static public void main(String[] argv) {
		// doMain(new SmithWatermanAligner(), argv);

		// String[] strs = { "William W. ‘Don’t call me Dubya’ Cohen", "William W. Cohen" };
		// String[] strs = { "COHEN", "MCCOHN" };

		String[] strs = { "ABC", "ABCDEA" };

		// String[] strs = { "I love New York !!!", "I hate New Mexico !!!" };

		{

			Jaccard ed = new Jaccard();
			// MemoMatrix m = sw.compute(new CharacterSequence(strs[0]), new CharacterSequence(strs[1]));
			// MemoMatrix m = ed.compute(new StringSequence(strs[0]), new StringSequence(strs[1]));
			System.out.println(ed.getSimilarity(new CharacterSequence(strs[0]), new CharacterSequence(strs[1])));

		}

		// System.out.println(m.getBestScore());

		// AlignResult ar = new SmithWatermanAligner().align(m);

		// System.out.println(ar.toString());

	}

	@Override
	public double getSimilarity(Sequence s, Sequence t) {
		Sequence ss = s;
		Sequence ll = t;

		if (s.length() > t.length()) {
			ss = t;
			ll = s;
		}
		Counter<String> c1 = ss.getTokenCounts();
		Counter<String> c2 = ll.getTokenCounts();
		double num_commons = 0;

		for (String key : c1.keySet()) {
			if (c2.containsKey(key)) {
				num_commons++;
			}
		}
		double ret = num_commons / (c1.size() + c2.size() - num_commons);
		return ret;
	}
}
