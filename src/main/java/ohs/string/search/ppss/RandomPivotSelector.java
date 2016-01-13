package ohs.string.search.ppss;

import java.util.Collections;
import java.util.List;

import ohs.string.search.ppss.Gram.Type;

/**
 * A class of determining pivots among prefixes through random selection.
 * 
 * @author Heung-Seon Oh
 */
public class RandomPivotSelector extends PivotSelector {

	public RandomPivotSelector(int q, int prefix_size, int pivot_size) {
		this.q = q;
		this.prefix_size = prefix_size;
		this.pivot_size = pivot_size;
	}

	@Override
	public void select(Gram[] grams) {
		this.grams = grams;

		selectPrefixes();

		List<Integer> locs = selectDisjointPrefixLocs();

		Collections.shuffle(locs);

		for (int i = 0; i < locs.size() && i < pivot_size; i++) {
			grams[locs.get(i)].setType(Type.PIVOT);
		}

		sortGramsByTypes();

		// StringBuffer sb = new StringBuffer();
		//
		// for (int i = 0; i < grams.length; i++) {
		// sb.append(grams[i].toString());
		// if (i != grams.length - 1) {
		// sb.append("\n");
		// }
		// }
		// System.out.println(sb.toString() + "\n");
	}

	// private List<Integer> selectDisjointPrefixLocs() {
	// List<Integer> ret = new ArrayList<Integer>();
	//
	// int len = GramUtils.getStringLength(grams);
	//
	// boolean[] visited = new boolean[len];
	//
	// for (int i = 0; i < grams.length && i < prefix_size; i++) {
	// Gram gram = grams[i];
	// int start = gram.getStart();
	// int end = start + q;
	//
	// boolean isUsed = false;
	//
	// for (int j = start; j < end; j++) {
	// if (visited[j]) {
	// isUsed = true;
	// } else {
	// visited[j] = true;
	// }
	// }
	//
	// if (isUsed) {
	// continue;
	// }
	// ret.add(i);
	// }
	// return ret;
	// }

}
