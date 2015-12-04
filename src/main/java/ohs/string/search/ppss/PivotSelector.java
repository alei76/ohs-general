package ohs.string.search.ppss;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ohs.string.search.ppss.Gram.Type;
import ohs.types.ListMap;

public abstract class PivotSelector implements Serializable {

	protected int q;

	protected int pivot_size;

	protected int prefix_size;

	protected Gram[] grams;

	abstract public void select(Gram[] grams);

	protected List<Integer> selectDisjointPrefixLocs() {
		List<Integer> ret = new ArrayList<Integer>();

		int len = GramUtils.getStringLength(grams);

		boolean[] visited = new boolean[len];

		for (int i = 0; i < grams.length && i < prefix_size; i++) {
			Gram gram = grams[i];
			int start = gram.getStart();
			int end = start + q;

			boolean isUsed = false;

			for (int j = start; j < end; j++) {
				if (visited[j]) {
					isUsed = true;
					break;
				} else {
					visited[j] = true;
				}
			}

			if (isUsed) {
				continue;
			}
			ret.add(i);
		}
		return ret;
	}

	protected void selectPrefixes() {
		for (int i = 0; i < grams.length; i++) {
			Gram gram = grams[i];
			if (i < prefix_size) {
				gram.setType(Type.PREFIX);
			} else {
				gram.setType(Type.SUFFIX);
			}
		}
	}

	protected void sortGramsByTypes() {
		ListMap<Type, Integer> typeLocs = GramUtils.groupGramsByTypes(grams, false);

		Gram[] tempGrams = new Gram[grams.length];

		Type[] types = { Type.PIVOT, Type.PREFIX, Type.SUFFIX };
		int new_loc = 0;

		for (Type type : types) {
			List<Integer> locs = typeLocs.get(type);

			for (int loc : locs) {
				if (tempGrams[new_loc] == null) {
					tempGrams[new_loc] = grams[loc];
					new_loc++;
				}
			}
		}

		for (int i = 0; i < grams.length; i++) {
			grams[i] = tempGrams[i];
		}

	}

}
