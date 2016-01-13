package ohs.string.search.ppss;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.string.search.ppss.Gram.Type;
import ohs.types.Counter;
import ohs.types.ListMap;

/**
 * A class of determining pivots among prefixes through dynamic programming outlined at Algorithm 3.
 * 
 *
 * 
 * @author Heung-Seon Oh
 */
public class OptimalPivotSelector extends PivotSelector {

	/**
	 * Sort grams by starting positions in a descending order.
	 * 
	 * @author ohs
	 */
	public class GramComparator implements Comparator<Gram> {
		@Override
		public int compare(Gram o1, Gram o2) {
			return o1.getStart() - o2.getStart();
		}
	}

	private Counter<String> gramWeights;

	private double[][] W;

	private Map<Integer, Gram> gramOrders;

	private Gram[] prefixes;

	private double[] wgs;

	public OptimalPivotSelector(int q, int prefix_size, int pivot_size) {
		this.q = q;
		this.prefix_size = prefix_size;
		this.pivot_size = pivot_size;
	}

	/**
	 * @param m
	 *            First i prefix q-grams
	 * @param n
	 *            Number of pivotal q-grams to select
	 * @return
	 * @return
	 */
	private double computeWeightMatrix(int m, int n) {
		double ret = Double.POSITIVE_INFINITY;

		if (Double.isNaN(W[m][n])) {
			if (n == 0) {
				int min_k = ArrayMath.argMin(wgs, 0, m + 1);
				ret = wgs[min_k];
			} else if (m < n) {
				ret = Double.POSITIVE_INFINITY;
			} else {
				Counter<Integer> c = new Counter<Integer>();

				for (int k = n; k <= m; k++) {
					int kp = -1;

					/*
					 * Find kp which is not overlapped with k.
					 */

					for (int l = k - 1; l > -1; l--) {
						Gram gram1 = prefixes[k];
						Gram gram2 = prefixes[l];
						int end = gram2.getStart() + gram2.getString().length() - 1;
						if (gram1.getStart() > end) {
							kp = l;
							break;
						}
					}

					if (kp == -1) {
						continue;
					}

					/*
					 * Compute W(kp, n-1)
					 */

					double optimal_value = computeWeightMatrix(kp, n - 1);
					double w = wgs[k];
					c.setCount(k, optimal_value + w);
				}

				if (c.size() > 0) {
					ret = c.min();
				} else {
					// System.out.println();
				}
			}
		} else {
			ret = W[m][n];
		}

		W[m][n] = ret;

		// System.out.printf("[M,N]=[%d,%d]\n", m, n);
		// System.out.println(ArrayUtils.toString(W));
		// System.out.println();

		return ret;
	}

	@Override
	public void select(Gram[] grams) {
		this.grams = grams;

		selectPrefixes();

		prefixes = new Gram[prefix_size];

		ListMap<Type, Integer> typeLocs = GramUtils.groupGramsByTypes(grams, true);

		List<Integer> prefixLocs = typeLocs.get(Type.PREFIX);

		if (prefixLocs.size() < prefix_size) {
			List<Integer> disjointPrefixLocs = selectDisjointPrefixLocs();
			for (int i = 0; i < disjointPrefixLocs.size(); i++) {
				int loc = disjointPrefixLocs.get(i);
				grams[loc].setType(Type.PIVOT);
			}
		} else {
			for (int i = 0, j = 0; i < prefixLocs.size(); i++) {
				int loc = prefixLocs.get(i);
				prefixes[j++] = grams[loc];
			}

			Arrays.sort(prefixes, new GramComparator());

			W = new double[prefix_size][pivot_size];
			wgs = new double[prefix_size];

			for (int i = 0; i < prefix_size; i++) {
				wgs[i] = gramWeights.getCount(grams[i].getString());
				for (int j = 0; j < pivot_size; j++) {
					W[i][j] = Double.NaN;
				}
			}

			computeWeightMatrix(prefix_size - 1, pivot_size - 1);

			int[] locs = new int[pivot_size];

			for (int j = 0, s = 0; j < pivot_size; j++) {
				double[] col = new double[W.length];

				ArrayUtils.copyColumn(W, j, col);

				int i = ArrayMath.argMin(col, s, col.length);
				locs[j] = i;
				s = i + 1;
			}

			// if (locs.length != pivot_size) {
			// throw new Exception("Optimal pivot selector doesn't work well.");
			// }

			for (int i = 0; i < locs.length; i++) {
				int loc = locs[i];
				prefixes[loc].setType(Type.PIVOT);
			}
		}

		sortGramsByTypes();

		// System.out.println(GramUtils.toString(grams));

	}

	public void setGramWeights(Counter<String> gramWeights) {
		this.gramWeights = gramWeights;
	}

}
