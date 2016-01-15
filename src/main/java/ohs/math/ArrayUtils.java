package ohs.math;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ohs.utils.ByteSize;

public class ArrayUtils {

	public static double[] array(int size, double init) {
		double[] ret = new double[size];
		if (init != 0) {
			setAll(ret, init);
		}
		return ret;
	}

	public static double copy(Collection<Double> a, double[] b) {
		int loc = 0;
		Iterator<Double> iter = a.iterator();
		double sum = 0;
		while (iter.hasNext()) {
			b[loc] = iter.next();
			sum += b[loc];
			loc++;
		}
		return sum;
	}

	public static double copy(Collection<Float> a, float[] b) {
		int loc = 0;
		Iterator<Float> iter = a.iterator();
		double sum = 0;
		while (iter.hasNext()) {
			b[loc] = iter.next();
			sum += b[loc];
			loc++;
		}
		return sum;
	}

	public static int copy(Collection<Integer> a, int[] b) {
		int sum = 0;
		int loc = 0;
		Iterator<Integer> iter = a.iterator();
		while (iter.hasNext()) {
			b[loc] = iter.next();
			sum += b[loc];
			loc++;
		}
		return sum;
	}

	public static double[] copy(double[] a) {
		double[] ret = new double[a.length];
		copy(a, ret);
		return ret;
	}

	public static double copy(double[] a, double[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i];
			sum += b[i];
		}
		return sum;
	}

	public static int copy(double[] a, int[] b) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			b[i] = (int) a[i];
			sum += b[i];
		}
		return sum;
	}

	public static double copy(double[] a, List<Double> b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			b.add(a[i]);
			sum += a[i];
		}
		return sum;
	}

	public static double[][] copy(double[][] a) {
		double[][] b = new double[a.length][];
		for (int i = 0; i < a.length; i++) {
			b[i] = copy(a[i]);
		}
		return b;
	}

	public static double copy(double[][] a, double[][] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += copy(a[i], b[i]);
		}
		return sum;
	}

	public static int copy(double[][] a, int[][] b) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				b[i][j] = (int) a[i][j];
				sum += b[i][j];
			}
		}
		return sum;
	}

	public static double copy(float[] a, float[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i];
			sum += b[i];
		}
		return sum;
	}

	public static int[] copy(int[] a) {
		int[] b = new int[a.length];
		copy(a, b);
		return b;
	}

	public static double copy(int[] a, double[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i];
			sum += b[i];
		}
		return sum;
	}

	public static int copy(int[] a, int[] b) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i];
			sum += b[i];
		}
		return sum;
	}

	public static int copy(int[] a, List<Integer> b) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			b.add(a[i]);
			sum += a[i];
		}
		return sum;
	}

	public static int copy(int[] a, Set<Integer> b) {
		int sum = 0;
		for (int value : a) {
			b.add(value);
			sum += value;
		}
		return sum;
	}

	public static double copy(int[][] a, double[][] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				b[i][j] = a[i][j];
				sum += b[i][j];
			}
		}
		return sum;
	}

	public static double copyColumn(double[] a, double[][] b, int bj) {
		int[] dims = dimensions(b);
		double sum = 0;
		for (int i = 0; i < dims[0]; i++) {
			b[i][bj] = a[i];
			sum += b[i][bj];
		}
		return sum;
	}

	public static double copyColumn(double[][] a, int aj, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException();
		}
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i][aj];
			sum += b[i];
		}
		return sum;
	}

	public static double copyColumn(double[][] a, int aj, double[][] b, int bj) {
		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}
		int rowDim = a.length;
		int colDim = a[0].length;
		double sum = 0;
		for (int i = 0; i < rowDim; i++) {
			b[i][bj] = a[i][aj];
			sum += b[i][bj];
		}
		return sum;
	}

	public static double copyRow(double[] a, double[][] b, int bi) {
		return copy(a, b[bi]);
	}

	public static double copyRow(double[][] a, int ai, double[] b) {
		return copy(a[ai], b);
	}

	public static double copyRow(double[][] a, int ai, double[][] b, int bi) {
		return copy(a[ai], b[bi]);
	}

	public static double copySubarray(double[] a, double[] b, int start, int end) {
		int size = end - start;
		if (size != a.length) {
			throw new IllegalArgumentException();
		}
		double sum = 0;
		for (int i = start, j = 0; i < end; i++, j++) {
			b[i] = a[j];
			sum += b[i];
		}
		return sum;
	}

	public static double[] copySubarray(double[] a, int start, int end) {
		int size = end - start;
		double[] ret = new double[size];
		copySubarray(a, start, end, ret);
		return ret;
	}

	public static double copySubarray(double[] a, int start, int end, double[] b) {
		int size = end - start;
		if (size != b.length) {
			throw new IllegalArgumentException();
		}
		double sum = 0;
		for (int i = start, j = 0; i < end; i++, j++) {
			b[j] = a[i];
			sum += b[j];
		}
		return sum;
	}

	public static double[][] diagonal(double[] a) {
		double[][] ret = matrix(a.length, 0);
		for (int i = 0; i < a.length; i++) {
			ret[i][i] = a[i];
		}
		return ret;
	}

	public static double[][] diagonal(int size, double init) {
		double[][] ret = matrix(size, 0);
		for (int i = 0; i < ret.length; i++) {
			ret[i][i] = init;
		}
		return ret;
	}

	public static int[] dimensions(double[][] a) {
		int[] ret = new int[2];
		ret[0] = a.length;
		ret[1] = a[0].length;
		return ret;
	}

	public static NumberFormat getDoubleNumberFormat(int num_fractions) {
		NumberFormat ret = NumberFormat.getInstance();
		ret.setMinimumFractionDigits(num_fractions);
		ret.setGroupingUsed(false);
		return ret;
	}

	public static NumberFormat getIntegerNumberFormat() {
		NumberFormat ret = NumberFormat.getInstance();
		ret.setMinimumFractionDigits(0);
		ret.setGroupingUsed(false);
		return ret;
	}

	public static double[][] identity(int size, double init) {
		double[][] ret = new double[size][size];
		for (int i = 0; i < size; i++) {
			ret[i][i] = init;
		}
		return ret;
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		{
			int[] indexes = range(10);

			double[] props = new double[] { 3, 3, 3 };
			ArrayMath.normalize(props);

			splitInOrder(indexes, props);
		}

		{
			int[] dims = { 3, 2, 3 };
			int[] indexes = { 0, 1, 2 };

			int singleIndex = singleIndex(dims, indexes);

			int[] indexes2 = multipleIndexes(singleIndex, dims);

			System.out.println(toString(indexes));
			System.out.println(singleIndex);
			System.out.println(toString(indexes2));

			int iii = 0;

			if (iii == 0) {
				return;
			}
		}

		{
			double[][] x = { { Double.NaN, Double.POSITIVE_INFINITY }, { Double.NEGATIVE_INFINITY, 10.25 } };

			System.out.println(toString(x));
		}

		{
			int[] dims = { 2, 2, 3, 3 };
			int[] indexes = { 1, 1, 2, 1 };
			int singleIndex = singleIndex(dims, indexes);

			int[] indexes2 = multipleIndexes(singleIndex, dims);

			System.out.println(toString(indexes));
			System.out.println(toString(indexes2));
		}

		{
			int[] dims = { 2, 2, 3, 3 };
			int[] indexes = { 0, 1, 2, 0 };
			int singleIndex = singleIndex(dims, indexes);

			int[] indexes2 = multipleIndexes(singleIndex, dims);

			System.out.println(toString(indexes));
			System.out.println(toString(indexes2));
		}

		{

			double[] a = ArrayMath.random(0f, 1f, 10000000);

		}

		System.out.println("process ends.");

	}

	public static double[][] matrix(int size) {
		return matrix(size, 0);
	}

	public static double[][] matrix(int size, double init) {
		return matrix(size, size, init);
	}

	public static double[][] matrix(int rowSize, int colSize, double init) {
		double[][] ret = new double[rowSize][colSize];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = array(colSize, init);
		}
		return ret;
	}

	public static int maxColumnSize(int[][] a) {
		int ret = 0;
		for (int i = 0; i < a.length; i++) {
			if (a[i].length > ret) {
				ret = a[i].length;
			}
		}
		return ret;
	}

	public static int[] multipleIndexes(int singleIndex, int[] dims) {
		int[] ret = new int[dims.length];
		multipleIndexes(singleIndex, dims, ret);
		return ret;
	}

	public static void multipleIndexes(int singleIndex, int[] dims, int[] indexes) {
		if (!ArrayChecker.isSameDim(dims, indexes)) {
			throw new IllegalArgumentException();
		}

		int ret = 0;
		int base = 1;
		for (int i = dims.length - 1; i >= 0; i--) {
			if (i != dims.length - 1) {
				base *= dims[i + 1];
			}
		}

		int quotient = 0;
		int remainer = 0;

		for (int i = 0; i < dims.length - 1; i++) {
			if (singleIndex >= base) {
				quotient = singleIndex / base;
				remainer = (singleIndex - quotient * base);
				singleIndex = remainer;
			}

			if (i == dims.length - 2) {
				indexes[i] = quotient;
				indexes[i + 1] = remainer;
			} else {
				indexes[i] = quotient;
			}

			base /= dims[i + 1];
		}

	}

	public static int[] nonzeroIndexes(double[] x) {
		List<Integer> set = new ArrayList<Integer>();
		for (int i = 0; i < x.length; i++) {
			if (x[i] != 0) {
				set.add(i);
			}
		}

		int[] ret = new int[set.size()];
		copy(set, ret);
		return ret;
	}

	public static int[] nonzeroIndexes(int[] x) {
		List<Integer> set = new ArrayList<Integer>();
		for (int i = 0; i < x.length; i++) {
			if (x[i] != 0) {
				set.add(i);
			}
		}
		int[] ret = new int[set.size()];
		copy(set, ret);
		return ret;
	}

	public static void quickSort(int[] indexes, double[] values, boolean sortByIndex) {
		if (indexes.length != values.length) {
			throw new IllegalArgumentException();
		}

		quickSortHere(indexes, values, 0, indexes.length - 1, sortByIndex);
	}

	private static void quickSortHere(int[] indexes, double[] values, int low, int high, boolean sortByIndex) {
		if (low >= high)
			return;
		int p = quickSortPartition(indexes, values, low, high, sortByIndex);
		quickSortHere(indexes, values, low, p, sortByIndex);
		quickSortHere(indexes, values, p + 1, high, sortByIndex);
	}

	private static int quickSortPartition(int[] indexes, double[] values, int low, int high, boolean sortByIndex) {
		// First element
		// int pivot = a[low];

		// Middle element
		// int middle = (low + high) / 2;

		int i = low - 1;
		int j = high + 1;

		if (sortByIndex) {
			// ascending order
			int randomIndex = (int) (Math.random() * (high - low)) + low;
			int pivotValue = indexes[randomIndex];

			while (i < j) {
				i++;
				while (indexes[i] < pivotValue) {
					i++;
				}

				j--;
				while (indexes[j] > pivotValue) {
					j--;
				}

				if (i < j) {
					swap(indexes, i, j);
					swap(values, i, j);
				}
			}
		} else {
			// descending order
			int randomIndex = (int) (Math.random() * (high - low)) + low;
			double pivotValue = values[randomIndex];

			while (i < j) {
				i++;
				while (values[i] > pivotValue) {
					i++;
				}

				j--;
				while (values[j] < pivotValue) {
					j--;
				}

				if (i < j) {
					swap(indexes, i, j);
					swap(values, i, j);
				}
			}
		}
		return j;
	}

	public static double range(double[] a, double start, double increment) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			a[i] = start + i * increment;
			sum += a[i];
		}
		return sum;
	}

	public static int[] range(int size) {
		return range(size, 0, 1);
	}

	public static double[] range(int size, double start, double increment) {
		double[] a = new double[size];
		range(a, start, increment);
		return a;
	}

	public static int[] range(int size, int start, int increment) {
		int[] a = new int[size];
		range(a, 0, 1);
		return a;
	}

	public static int range(int[] a, int start, int increment) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			a[i] = start + i * increment;
			sum += a[i];
		}
		return sum;
	}

	public static int[] rankedIndexes(double[] a) {
		int[] b = range(a.length);
		quickSort(b, copy(a), false);
		return b;
	}

	public static double reshape(double[] a, double[][] b) {
		if (sizeOfEntries(b) != a.length) {
			throw new IllegalArgumentException();
		}
		double sum = 0;
		for (int i = 0, k = 0; i < a.length; i++) {
			for (int j = 0; j < b[i].length; j++) {
				b[i][j] = a[k];
				sum += a[k];
				k++;
			}
		}

		return sum;
	}

	// public static double random(double min, double max, double[] x) {
	// Random random = new Random();
	// double range = max - min;
	// double sum = 0;
	// for (int i = 0; i < x.length; i++) {
	// x[i] = range * random.nextDouble() + min;
	// sum += x[i];
	// }
	// return sum;
	// }
	//
	// public static double random(double min, double max, double[][] x) {
	// double sum = 0;
	// for (int i = 0; i < x.length; i++) {
	// sum += random(min, max, x[i]);
	// }
	// return sum;
	// }
	//
	// public static double[] random(double min, double max, int size) {
	// double[] x = new double[size];
	// random(min, max, x);
	// return x;
	// }
	//
	// public static double[][] random(double min, double max, int rows, int columns) {
	// double[][] x = new double[rows][columns];
	// random(min, max, x);
	// return x;
	// }
	//
	// public static int[] random(int min, int max, int size) {
	// int[] x = new int[size];
	// random(min, max, x);
	// return x;
	// }
	//
	// public static int random(int min, int max, int[] x) {
	// Random random = new Random();
	// double range = max - min + 1;
	// int sum = 0;
	// for (int i = 0; i < x.length; i++) {
	// x[i] = (int) (range * random.nextDouble()) + min;
	// sum += x[i];
	// }
	// return sum;
	// }

	public static double reshape(double[][] a, double[] b) {
		if (sizeOfEntries(a) != b.length) {
			throw new IllegalArgumentException();
		}

		double sum = 0;
		for (int i = 0, k = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				b[k] = a[i][j];
				sum += b[k];
				k++;
			}
		}
		return sum;
	}

	public static void reverse(double[] a) {
		int mid = a.length / 2;
		for (int i = 0; i < mid; i++) {
			swap(a, i, a.length - 1 - i);
		}
	}

	public static void reverse(int[] a) {
		int mid = a.length / 2;
		for (int i = 0; i < mid; i++) {
			swap(a, i, a.length - 1 - i);
		}
	}

	public static void set(double[] a, double value, int start, int end) {
		for (int i = start; i < end && i < a.length; i++) {
			a[i] = value;
		}
	}

	public static double setAll(double[][] a, double value) {
		double ret = 0;
		for (int i = 0; i < a.length; i++) {
			ret += setAll(a[i], value);
		}
		return ret;
	}

	public static double setAll(double[] a, double value) {
		Arrays.fill(a, value);
		return value * a.length;
	}

	/**
	 * Code from method java.util.Collections.shuffle();
	 */
	public static void shuffle(int[] a) {
		Random random = new Random();

		int count = a.length;
		for (int i = count; i > 1; i--) {
			swap(a, i - 1, random.nextInt(i));
		}
	}

	public static int singleIndex(int[] dims, int[] indexes) {
		if (!ArrayChecker.isSameDim(dims, indexes)) {
			throw new IllegalArgumentException();
		}

		int ret = 0;
		int base = 1;

		for (int i = dims.length - 1; i >= 0; i--) {
			if (indexes[i] >= dims[i]) {
				throw new IllegalArgumentException();
			}
			if (i == dims.length - 1) {
				ret += indexes[i];
			} else {
				base *= dims[i + 1];
				ret += base * indexes[i];
			}
		}
		return ret;
	}

	public static int sizeOfEntries(double[][] a) {
		int[] dims = dimensions(a);
		return dims[0] * dims[1];
	}

	public static int sizeOfNonzeros(double[] a) {
		int ret = 0;

		for (int i = 0; i < a.length; i++) {
			if (a[i] != 0) {
				ret++;
			}
		}
		return ret;
	}

	public static int sizeOfNonzeros(int[] a) {
		int ret = 0;
		for (int i = 0; i < a.length; i++) {
			if (a[i] != 0) {
				ret++;
			}
		}
		return ret;
	}

	public static void sort(double[] a) {
		int[] b = range(a.length);
		quickSort(b, a, false);
	}

	public static List<Integer>[] split(List<Integer> indexList, int num_folds) {
		double[] proportions = new double[num_folds];
		for (int i = 0; i < proportions.length; i++) {
			proportions[i] = 1f / proportions.length;
		}
		return splitInOrder(indexList, proportions);
	}

	public static List<Integer>[] splitInOrder(int[] indexes, double[] proportions) {
		// double[] fold_prop = copy(proportions);
		// ArrayMath.normalize(fold_prop);

		double[] fold_maxIndex = copy(proportions);
		ArrayMath.normalize(fold_maxIndex);

		ArrayMath.cumulate(fold_maxIndex, fold_maxIndex);

		List<Integer>[] ret = new List[fold_maxIndex.length];

		int[][] splits = new int[fold_maxIndex.length][];

		for (int i = 0; i < 3; i++) {
			shuffle(indexes);
		}

		for (int i = 0; i < fold_maxIndex.length; i++) {
			ret[i] = new ArrayList<Integer>();
			double value = fold_maxIndex[i];
			double maxIndex = Math.rint(value * indexes.length);
			fold_maxIndex[i] = maxIndex;
		}

		for (int i = 0, j = 0; i < indexes.length; i++) {
			// This gives a slight bias toward putting an extra instance in the
			// last InstanceList.

			int maxIndex = (int) fold_maxIndex[j];

			if (i >= maxIndex && j < ret.length) {
				j++;
			}

			ret[j].add(indexes[i]);
		}
		return ret;
	}

	public static List<Integer>[] splitInOrder(List<Integer> indexList, double[] proportions) {
		double[] fold_prop = copy(proportions);
		ArrayMath.normalize(fold_prop);

		double[] fold_maxIndex = copy(fold_prop);

		ArrayMath.cumulate(fold_maxIndex, fold_maxIndex);

		List<Integer>[] ret = new List[fold_prop.length];

		for (int i = 0; i < 3; i++) {
			Collections.shuffle(indexList);
		}

		for (int i = 0; i < fold_maxIndex.length; i++) {
			ret[i] = new ArrayList<Integer>();
			double value = fold_maxIndex[i];
			double maxIndex = Math.rint(value * indexList.size());
			fold_maxIndex[i] = maxIndex;
		}

		for (int i = 0, j = 0; i < indexList.size(); i++) {
			// This gives a slight bias toward putting an extra instance in the
			// last InstanceList.

			double maxIndex = fold_maxIndex[j];

			if (i >= maxIndex && j < ret.length) {
				j++;
			}

			ret[j].add(indexList.get(i));
		}
		return ret;
	}

	public static void swap(double[] x, int i, int j) {
		double v1 = x[i];
		double v2 = x[j];
		x[i] = v2;
		x[j] = v1;
	}

	public static void swap(int[] x, int i, int j) {
		int v2 = x[i];
		int v1 = x[j];
		x[i] = v1;
		x[j] = v2;
	}

	public static void swapColumns(double[][] x, int i, int j) {
		for (int k = 0; k < x.length; k++) {
			double temp = x[k][i];
			x[k][i] = x[k][j];
			x[k][j] = temp;
		}
	}

	public static void swapRows(double[][] x, int i, int j) {
		for (int k = 0; k < x[0].length; k++) {
			double temp = x[i][k];
			x[i][k] = x[j][k];
			x[j][k] = temp;
		}
	}

	public static String toString(double[] x) {
		return toString(x, x.length, false, false, getDoubleNumberFormat(4));
	}

	public static String toString(double[] x, int num_print, boolean sparse, boolean vertical, NumberFormat nf) {
		StringBuffer sb = new StringBuffer();

		String delim = "\t";

		if (vertical) {
			delim = "\n";
		}

		if (sparse) {
			for (int i = 0; i < x.length && i < num_print; i++) {
				sb.append(String.format("%s%d:%s", delim, i, nf.format(x[i])));
			}
		} else {
			for (int i = 0; i < x.length && i < num_print; i++) {
				sb.append(String.format("%s%s", delim, nf.format(x[i])));
			}
		}
		return sb.toString().trim();
	}

	public static String toString(double[][] x) {
		int num_rows = x.length;
		int num_cols = x[0].length;
		boolean sparse = false;

		return toString(x, num_rows, num_cols, sparse, getDoubleNumberFormat(4));
	}

	public static String toString(double[][] x, int num_print_rows, int num_print_cols, boolean sparse, NumberFormat nf) {

		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[Row size\t%d]\n", x.length));
		sb.append(String.format("[Col size\t%d]\n", x[0].length));

		if (sparse) {
			for (int i = 0; i < x.length && i < num_print_rows; i++) {
				StringBuffer sb2 = new StringBuffer();
				sb2.append(i);

				int num_nonzero_print_cols = 0;

				for (int j = 0; j < x[i].length && num_nonzero_print_cols < num_print_cols; j++) {
					Double v = new Double(x[i][j]);
					if (v != 0) {
						if (Double.isFinite(v) || Double.isInfinite(v) || Double.isNaN(v)) {
							sb2.append(String.format("\t%d:%s", j, v.toString()));
							num_nonzero_print_cols++;
						} else {
							sb2.append(String.format("\t%d:%s", j, nf.format(v.doubleValue())));
							num_nonzero_print_cols++;
						}
					}
				}

				if (num_nonzero_print_cols > 0) {
					sb.append(sb2.toString());
					sb.append("\n");
				}
			}
		} else {
			sb.append("#");
			for (int i = 0; i < x[0].length && i < num_print_cols; i++) {
				sb.append(String.format("\t%d", i));
			}
			sb.append("\n");

			for (int i = 0; i < x.length && i < num_print_rows; i++) {
				sb.append(i);
				for (int j = 0; j < x[i].length && j < num_print_cols; j++) {
					Double v = new Double(x[i][j]);
					if (!Double.isFinite(v)) {
						sb.append(String.format("\t%s", v.toString()));
					} else {
						sb.append(String.format("\t%s", nf.format(v.doubleValue())));
					}
				}
				sb.append("\n");
			}
		}

		return sb.toString().trim();
	}

	public static String toString(int[] x) {
		return toString(x, false, false);
	}

	public static String toString(int[] x, boolean sparse, boolean vertical) {
		StringBuffer sb = new StringBuffer();
		String delim = "\t";
		if (vertical) {
			delim = "\n";
		}

		if (sparse) {
			for (int i = 0; i < x.length; i++) {
				if (x[i] != 0) {
					sb.append(String.format("%s%d:%s", delim, i, x[i]));
				}
			}
		} else {
			for (int i = 0; i < x.length; i++) {
				sb.append(String.format("%s%s", delim, x[i]));
			}
		}
		return sb.toString().trim();
	}

	public static String toString(int[][] x) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < x.length; i++) {
			sb.append(toString(x[i], false, false));
			if (i != x.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public ByteSize byteSize(double[] a) {
		return new ByteSize(Double.BYTES * a.length);
	}

	public ByteSize byteSize(int[] a) {
		return new ByteSize(Integer.BYTES * a.length);
	}

	public List<Integer>[] splitInOrder(List<Integer> indexList, int[] counts) {
		List<Integer>[] ret = new List[counts.length];
		int idx = 0;
		for (int num = 0; num < counts.length; num++) {
			ret[num] = new ArrayList<Integer>();
			for (int i = 0; i < counts[num]; i++) {
				ret[num].add(indexList.get(idx)); // Transfer weights?
				idx++;
			}
		}
		return ret;
	}
}
