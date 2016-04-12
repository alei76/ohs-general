package ohs.utils;

import java.util.Collections;
import java.util.List;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;

public class DataSplitter {

	public static <E> List<E>[] split(List<E> ids, int num_folds) {
		return splitInOrder(ids, ArrayUtils.array(num_folds, 1f / num_folds));
	}

	public static <E> List<E>[] splitInOrder(List<E> ids, double[] proportions) {

		double[] fold_maxIndex = ArrayUtils.copy(proportions);

		ArrayMath.cumulate(fold_maxIndex, fold_maxIndex);

		List<E>[] ret = new List[proportions.length];

		for (int i = 0; i < 3; i++) {
			Collections.shuffle(ids);
		}

		for (int i = 0; i < fold_maxIndex.length; i++) {
			ret[i] = Generics.newArrayList();
			double value = fold_maxIndex[i];
			double maxIndex = Math.rint(value * ids.size());
			fold_maxIndex[i] = maxIndex;
		}

		for (int i = 0, j = 0; i < ids.size(); i++) {
			// This gives a slight bias toward putting an extra instance in the
			// last InstanceList.

			double maxIndex = fold_maxIndex[j];

			if (i >= maxIndex && j < ret.length) {
				j++;
			}

			ret[j].add(ids.get(i));
		}
		return ret;
	}

	public static <E> List<E>[] splitInOrder(List<E> ids, int[] counts) {
		List<E>[] ret = new List[counts.length];
		int idx = 0;
		for (int num = 0; num < counts.length; num++) {
			ret[num] = Generics.newArrayList();
			for (int i = 0; i < counts[num]; i++) {
				ret[num].add(ids.get(idx)); // Transfer weights?
				idx++;
			}
		}
		return ret;
	}
}
