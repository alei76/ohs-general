package ohs.matrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.ListMap;

/**
 * @author Heung-Seon Oh
 * 
 */
public class SparseVector implements Vector {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6671749703272005320L;

	public static void write(ObjectOutputStream oos, List<SparseVector> vs) throws Exception {
		oos.writeInt(vs.size());
		for (int i = 0; i < vs.size(); i++) {
			vs.get(i).writeObject(oos);
		}
	}

	public static void write(String fileName, List<SparseVector> vs) throws Exception {
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		write(oos, vs);
		oos.close();
		System.out.printf("write [%d] vectors to [%s].\n", vs.size(), fileName);
	}

	private int[] indexes;

	private double[] values;

	private double sum;

	private int dim;

	public SparseVector() {

	}

	public SparseVector(Counter<Integer> c) {
		this(c, 0);
	}

	public SparseVector(Counter<Integer> c, int dim) {
		indexes = new int[c.size()];
		values = new double[c.size()];
		sum = 0;
		int loc = 0;
		for (Entry<Integer, Double> e : c.entrySet()) {
			incrementAtLoc(loc++, e.getKey(), e.getValue());
		}
		this.dim = dim;
		sortByIndex();
	}

	public SparseVector(int size) {
		this(new int[size], new double[size], size);
	}

	public SparseVector(int size, int dim) {
		this(new int[size], new double[size], dim);
	}

	public SparseVector(int[] indexes) {
		this(indexes, new double[indexes.length], indexes.length);
	}

	public SparseVector(int[] indexes, double[] values) {
		this(indexes, values, indexes.length);
	}

	public SparseVector(int[] indexes, double[] values, int dim) {
		this.indexes = indexes;
		this.values = values;
		this.sum = 0;
		this.dim = dim;
	}

	@Override
	public int argMax() {
		return indexAtLoc(argMaxLoc());
	}

	public int argMaxLoc() {
		return ArrayMath.argMax(values);
	}

	@Override
	public int argMin() {
		return indexAtLoc(argMinLoc());
	}

	public int argMinLoc() {
		return ArrayMath.argMin(values);
	}

	@Override
	public SparseVector copy() {
		SparseVector ret = new SparseVector(ArrayUtils.copy(indexes), ArrayUtils.copy(values), dim);
		ret.setSum(sum);
		return ret;
	}

	public int[] copyIndexes() {
		return ArrayUtils.copy(indexes);
	}

	public double[] copyValues() {
		return ArrayUtils.copy(values);
	}

	@Override
	public int dim() {
		return dim;
	}

	@Override
	public void increment(int i, double value) {
		int loc = location(i);
		if (loc > -1) {
			values[loc] += value;
			sum += value;
		}
	}

	@Override
	public void incrementAll(double value) {
		sum = ArrayMath.add(values, value, values);
	}

	@Override
	public void incrementAtLoc(int loc, double value) {
		values[loc] += value;
		sum += value;
	}

	@Override
	public void incrementAtLoc(int loc, int i, double value) {
		indexes[loc] = i;
		values[loc] += value;
		sum += value;
	}

	@Override
	public int indexAtLoc(int loc) {
		return indexes[loc];
	}

	@Override
	public int[] indexes() {
		return indexes;
	}

	@Override
	public String info() {
		return null;
	}

	@Override
	public void keepAbove(double cutoff) {
		List<Integer> is = new ArrayList<Integer>();
		List<Double> vs = new ArrayList<Double>();

		for (int i = 0; i < values.length; i++) {
			if (values[i] < cutoff) {
				continue;
			}
			is.add(indexes[i]);
			vs.add(values[i]);
		}

		indexes = new int[is.size()];
		values = new double[is.size()];
		sum = 0;
		for (int i = 0; i < is.size(); i++) {
			indexes[i] = is.get(i);
			values[i] = vs.get(i);
			sum += values[i];
		}
	}

	@Override
	public void keepTopN(int topN) {
		if (values.length > topN) {
			sortByValue();
			int[] newIndexes = new int[topN];
			double[] newValues = new double[topN];

			sum = 0;
			for (int i = 0; i < topN; i++) {
				newIndexes[i] = indexes[i];
				newValues[i] = values[i];
				sum += newValues[i];
			}
			indexes = newIndexes;
			values = newValues;
			sortByIndex();
		}
	}

	/**
	 * 
	 * it should be called after calling sortByIndex
	 * 
	 * @param index
	 * @return
	 */
	@Override
	public int location(int index) {
		return Arrays.binarySearch(indexes, index);
	}

	@Override
	public double max() {
		return valueAtLoc(argMaxLoc());
	}

	@Override
	public double min() {
		return valueAtLoc(argMinLoc());
	}

	@Override
	public void normalize() {
		sum = ArrayMath.normalize(values, values);
	}

	@Override
	public void normalizeAfterSummation() {
		sum = ArrayMath.scale(values, ArrayMath.sum(values), values);
	}

	@Override
	public void normalizeByL2Norm() {
		sum = ArrayMath.normalizeByL2Norm(values, values);
	}

	@Override
	public double prob(int index) {
		double ret = 0;
		int loc = location(index);
		if (loc > -1) {
			throw new IllegalArgumentException("not found");
		}
		return ret;
	}

	@Override
	public double probAlways(int index) {
		double ret = 0;
		int loc = location(index);
		if (loc > -1) {
			ret = probAtLoc(loc);
		}
		return ret;
	}

	@Override
	public double probAtLoc(int loc) {
		return values[loc] / sum;
	}

	@Override
	public void prune(final Set<Integer> toRemove) {
		List<Integer> is = new ArrayList<Integer>();
		List<Double> vs = new ArrayList<Double>();
		sum = 0;

		for (int i = 0; i < size(); i++) {
			int index = indexAtLoc(i);
			double value = valueAtLoc(i);
			if (toRemove.contains(index)) {
				continue;
			}
			is.add(index);
			vs.add(value);
			sum += value;
		}
		indexes = new int[is.size()];
		values = new double[vs.size()];
		ArrayUtils.copy(is, indexes);
		ArrayUtils.copy(vs, values);
	}

	@Override
	public void pruneExcept(final Set<Integer> toKeep) {
		List<Integer> is = new ArrayList<Integer>();
		List<Double> vs = new ArrayList<Double>();
		sum = 0;

		for (int i = 0; i < size(); i++) {
			int index = indexAtLoc(i);
			double value = valueAtLoc(i);
			if (!toKeep.contains(index)) {
				continue;
			}
			is.add(index);
			vs.add(value);
			sum += value;
		}

		indexes = new int[is.size()];
		values = new double[vs.size()];

		ArrayUtils.copy(is, indexes);
		ArrayUtils.copy(vs, values);
	}

	public int[] rankedIndexes() {
		sortByValue();
		int[] ret = new int[size()];
		for (int i = 0; i < size(); i++) {
			ret[i] = indexAtLoc(i);
		}
		sortByIndex();
		return ret;
	}

	@Override
	public SparseVector ranking() {
		return ranking(false);
	}

	public SparseVector ranking(boolean ascending) {
		SparseVector ret = copy();
		ret.sortByValue();

		for (int i = 0; i < ret.size(); i++) {
			int index = ret.indexAtLoc(i);
			double rank = i + 1;
			if (ascending) {
				rank = ret.size() - i;
			}
			ret.setAtLoc(i, index, rank);
		}
		ret.sortByIndex();
		return ret;
	}

	public void read(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		dim = ois.readInt();
		indexes = new int[size];
		values = new double[size];
		sum = 0;
		for (int i = 0; i < size; i++) {
			indexes[i] = ois.readInt();
			values[i] = ois.readDouble();
			sum += values[i];
		}
	}

	public void readObject(ObjectInputStream ois) throws IOException {
		int size = ois.readInt();
		indexes = new int[size];
		values = new double[size];
		dim = ois.readInt();
		sum = 0;
		for (int i = 0; i < size; i++) {
			indexes[i] = ois.readInt();
			values[i] = ois.readDouble();
			sum += values[i];
		}
	}

	public void readObject(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		readObject(ois);
		ois.close();
	}

	public void removeZeros() {
		sum = 0;
		List<Integer> is = new ArrayList<Integer>();
		List<Double> vs = new ArrayList<Double>();

		for (int i = 0; i < indexes.length; i++) {
			int index = indexes[i];
			double value = values[i];
			if (value == 0) {
				continue;
			}
			sum += values[i];
			is.add(index);
			vs.add(value);
		}

		indexes = new int[is.size()];
		values = new double[vs.size()];

		ArrayUtils.copy(is, indexes);
		ArrayUtils.copy(vs, values);
	}

	public void reset() {
		Arrays.fill(indexes, 0);
		Arrays.fill(values, 0);
		sum = 0;
	}

	public void reverse() {
		int middle = indexes.length / 2;
		for (int i = 0; i < middle; i++) {
			int left = i;
			int right = indexes.length - i - 1;
			swap(left, right);
		}
	}

	@Override
	public void scale(double factor) {
		sum = ArrayMath.scale(values, factor, values);
	}

	@Override
	public void scale(int i, double factor) {
		int loc = location(i);
		if (loc > -1) {
			scaleAtLoc(loc, factor);
		}
	}

	@Override
	public void scaleAtLoc(int loc, double factor) {
		values[loc] *= factor;

	}

	@Override
	public void scaleAtLoc(int loc, int i, double factor) {
		indexes[loc] = i;
		values[loc] *= factor;
	}

	@Override
	public void set(int i, double value) {
		int loc = location(i);
		if (loc > -1) {
			setAtLoc(loc, value);
		}
	}

	@Override
	public void setAll(double value) {
		ArrayUtils.setAll(values, value);
		sum = value * values.length;
	}

	@Override
	public void setAtLoc(int loc, double value) {
		values[loc] = value;
	}

	@Override
	public void setAtLoc(int loc, int i, double value) {
		indexes[loc] = i;
		values[loc] = value;
	}

	@Override
	public void setDim(int dim) {
		this.dim = dim;
	}

	@Override
	public void setIndexes(int[] indexes) {
		this.indexes = indexes;
	}

	@Override
	public void setSum(double sum) {
		this.sum = sum;
	}

	@Override
	public void setValues(double[] values) {
		this.values = values;
	}

	@Override
	public int size() {
		return indexes.length;
	}

	@Override
	public int sizeOfNonzero() {
		return ArrayUtils.sizeOfNonzeros(values);
	}

	public void sortByIndex() {
		ArrayUtils.quickSort(indexes, values, true);
	}

	public void sortByValue() {
		ArrayUtils.quickSort(indexes, values, false);
	}

	public void sortByValue(boolean descending) {
		ArrayUtils.quickSort(indexes, values, false);

		if (!descending) {
			reverse();
		}
	}

	@Override
	public double sum() {
		return sum;
	}

	@Override
	public void summation() {
		sum = ArrayMath.sum(values);
	}

	private void swap(int i, int j) {
		ArrayUtils.swap(indexes, i, j);
		ArrayUtils.swap(values, i, j);
	}

	public DenseVector toDenseVector() {
		DenseVector ret = new DenseVector(dim);
		for (int i = 0; i < indexes.length; i++) {
			int index = indexes[i];
			double value = values[i];
			ret.increment(index, value);
		}
		return ret;
	}

	@Override
	public String toString() {
		return toString(false, 20, null);
	}

	public String toString(boolean vertical, int numKeys, Indexer<String> featIndexer) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(4);
		nf.setGroupingUsed(false);

		StringBuffer sb = new StringBuffer();
		sb.append(String.format("(%d/%d, %s) ->", size(), dim(), nf.format(sum)));

		// sortByValue();

		if (vertical) {
			sb.append("\n");
		}

		for (int i = 0; i < indexes.length && i < numKeys; i++) {
			int index = indexes[i];
			double value = values[i];

			if (featIndexer == null) {
				sb.append(String.format(" %d:%s", index, nf.format(value)));
			} else {
				sb.append(String.format(" %s:%s", featIndexer.getObject(index), nf.format(value)));
			}

			if (vertical) {
				sb.append("\n");
			}
		}

		// sortByIndex();

		return sb.toString().trim();
	}

	public String toSvmString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size(); i++) {
			sb.append(String.format(" %d:%s", indexes[i], values[i] + ""));
			if (i != size() - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	@Override
	public double value(int index) {
		int loc = location(index);
		if (loc < 0) {
			// System.out.println(toString(true, size(), null, null));
			throw new IllegalArgumentException("not found");
		}
		return valueAtLoc(loc);
	}

	@Override
	public double valueAlways(int index) {
		double ret = 0;
		int loc = location(index);
		if (loc > -1) {
			ret = valueAtLoc(loc);
		}
		return ret;
	}

	@Override
	public double valueAtLoc(int loc) {
		return values[loc];
	}

	@Override
	public double[] values() {
		return values;
	}

	@Override
	public void writeObject(ObjectOutputStream oos) throws Exception {
		oos.writeInt(values.length);
		oos.writeInt(dim);

		for (int i = 0; i < values.length; i++) {
			oos.writeInt(indexes[i]);
			oos.writeDouble(values[i]);
		}
	}

	@Override
	public void writeObject(String fileName) throws Exception {
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		writeObject(oos);
		oos.close();
	}
}
