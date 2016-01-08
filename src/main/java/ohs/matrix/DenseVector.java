package ohs.matrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;

public class DenseVector implements Vector {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1185683442330052104L;

	public static DenseVector read(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		DenseVector ret = readStream(ois);
		ois.close();
		return ret;
	}

	public static List<DenseVector> readList(ObjectInputStream ois) throws Exception {
		List<DenseVector> ret = new ArrayList<DenseVector>();
		int size = ois.readInt();
		for (int i = 0; i < size; i++) {
			DenseVector vector = readStream(ois);
			ret.add(vector);
		}
		return ret;
	}

	public static List<DenseVector> readList(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		List<DenseVector> ret = readList(ois);
		ois.close();
		System.out.printf("read [%d] vectors.\n", ret.size());
		return ret;
	}

	public static Map<Integer, List<DenseVector>> readMap(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		Map<Integer, List<DenseVector>> ret = new HashMap<Integer, List<DenseVector>>();
		List<DenseVector> allVectors = readList(fileName);

		for (int i = 0; i < allVectors.size(); i++) {
			DenseVector vector = allVectors.get(i);
			int topicId = vector.label();
			List<DenseVector> vectors = ret.get(topicId);

			if (vectors == null) {
				vectors = new ArrayList<DenseVector>();
				ret.put(topicId, vectors);
			}
			vectors.add(vector);
		}
		System.out.printf("read [%d] topics.\n", ret.keySet().size());
		return ret;
	}

	public static Map<Integer, List<DenseVector>> readMappedList(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		Map<Integer, List<DenseVector>> ret = new HashMap<Integer, List<DenseVector>>();
		List<DenseVector> allVectors = readList(fileName);

		for (int i = 0; i < allVectors.size(); i++) {
			DenseVector vector = allVectors.get(i);
			int topicId = vector.label();
			List<DenseVector> vectors = ret.get(topicId);

			if (vectors == null) {
				vectors = new ArrayList<DenseVector>();
				ret.put(topicId, vectors);
			}
			vectors.add(vector);
		}

		System.out.printf("read [%d] topics.\n", ret.keySet().size());
		return ret;
	}

	public static DenseVector readStream(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		int label = ois.readInt();
		int sizeOfNonzero = ois.readInt();
		double sum = 0;

		DenseVector ret = new DenseVector(size, label);
		for (int i = 0; i < sizeOfNonzero; i++) {
			int index = ois.readInt();
			double value = ois.readDouble();
			ret.set(index, value);
			sum += value;
		}
		ret.setSum(sum);

		return ret;
	}

	public static void write(ObjectOutputStream oos, List<DenseVector> xs) throws Exception {
		oos.writeInt(xs.size());
		for (int i = 0; i < xs.size(); i++) {
			DenseVector x = xs.get(i);
			x.write(oos);
		}
	}

	public static void write(String fileName, List<DenseVector> vectors) throws Exception {
		System.out.printf("write to [%s].\n", fileName);
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		write(oos, vectors);
		oos.close();
		System.out.printf("write [%d] vectors.\n", vectors.size());
	}

	private double[] values;

	private double sum;

	private int label;

	public DenseVector(double[] values) {
		this(values, -1);
	}

	public DenseVector(double[] values, int label) {
		this.values = values;
		this.label = label;
		this.sum = 0;
	}

	public DenseVector(int size) {
		this(size, -1);
	}

	public DenseVector(int size, int label) {
		this(new double[size], label);
	}

	public int argMax() {
		return ArrayMath.argMax(values);
	}

	public int argMin() {
		return ArrayMath.argMin(values);
	}

	public void clear() {
		setAll(0);
		sum = 0;
	}

	public DenseVector copy() {
		DenseVector ret = new DenseVector(ArrayUtils.copy(values), label);
		ret.setSum(sum);
		return ret;
	}

	public int dim() {
		return size();
	}

	public void increment(int i, double value) {
		values[i] += value;
		sum += value;
	}

	public void incrementAll(double increment) {
		sum = ArrayMath.add(values, increment, values);
	}

	public void incrementAtLoc(int loc, double value) {
		increment(loc, value);
	}

	public void incrementAtLoc(int loc, int i, double value) {
		new UnsupportedOperationException("unsupported");
	}

	public int indexAtLoc(int loc) {
		return loc;
	}

	public int[] indexes() {
		new UnsupportedOperationException("unsupported");
		return null;
	}

	@Override
	public String info() {
		// TODO Auto-generated method stub
		return null;
	}

	public void keepAbove(double cutoff) {
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			if (values[i] < cutoff) {
				values[i] -= value;
				sum -= value;
			}
		}
	}

	public void keepTopN(int topN) {
		SparseVector x1 = toSparseVector();
		x1.keepTopN(topN);
		DenseVector x2 = x1.toDenseVector();
		this.values = x2.values();
		this.sum = x2.sum();
	}

	public int label() {
		return label;
	}

	public int location(int index) {
		return index;
	}

	public double max() {
		int index = argMax();
		double max = Double.NEGATIVE_INFINITY;
		if (index > -1) {
			max = values[index];
		}
		return max;
	}

	public double min() {
		int index = argMin();
		double max = Double.POSITIVE_INFINITY;
		if (index > -1) {
			max = values[index];
		}
		return max;
	}

	public void normalize() {
		sum = ArrayMath.scale(values, 1f / sum, values);
	}

	public void normalizeAfterSummation() {
		sum = ArrayMath.sum(values);
		sum = ArrayMath.scale(values, 1f / sum, values);
	}

	@Override
	public void normalizeByL2Norm() {
		sum = ArrayMath.normalizeByL2Norm(values, values);
	}

	public double prob(int index) {
		return values[index] / sum;
	}

	public double probAlways(int index) {
		return prob(index);
	}

	public double probAtLoc(int loc) {
		return prob(loc);
	}

	public void prune(Set<Integer> toRemove) {
		for (int i : toRemove) {
			double value = values[i];
			values[i] -= value;
			sum -= value;
		}
	}

	public void pruneExcept(Set<Integer> toKeep) {
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			if (!toKeep.contains(i)) {
				values[i] -= value;
				sum -= value;
			}
		}
	}

	public SparseVector ranking() {
		return ranking(false);
	}

	public SparseVector ranking(boolean ascending) {
		SparseVector ret = toSparseVector().copy();
		ret.sortByValue();

		for (int i = 0; i < ret.size(); i++) {
			double rank = i + 1;
			if (ascending) {
				rank = ret.size() - i;
			}
			ret.setAtLoc(i, rank);
		}
		ret.sortByIndex();
		return ret;
	}

	public void scale(double factor) {
		ArrayMath.scale(values, factor, values);
	}

	@Override
	public void scale(int i, double factor) {
		values[i] *= factor;
	}

	@Override
	public void scaleAtLoc(int loc, double factor) {
		scale(loc, factor);
	}

	public void scaleAtLoc(int loc, int i, double factor) {
		new UnsupportedOperationException("unsupported");
	}

	public void set(int i, double value) {
		values[i] = value;
	}

	public void setAll(double value) {
		sum = ArrayUtils.setAll(values, value);
	}

	public void setAtLoc(int loc, double value) {
		set(loc, value);
	}

	public void setAtLoc(int loc, int i, double value) {
		if (loc != i) {
			new IllegalArgumentException("different index");
		}
		setAtLoc(loc, value);
	}

	public void setDim(int dim) {
		new UnsupportedOperationException("unsupported");

	}

	public void setIndexes(int[] indexes) {
		new UnsupportedOperationException("unsupported");
	}

	public void setLabel(int label) {
		this.label = label;
	}

	public void setSum(double sum) {
		this.sum = sum;
	}

	public void setValues(double[] values) {
		this.values = values;
	}

	public int size() {
		return values.length;
	}

	public int sizeOfNonzero() {
		int ret = 0;
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			if (value == 0) {
				continue;
			}
			ret++;
		}
		return ret;
	}

	public double sum() {
		return sum;
	}

	public void summation() {
		sum = ArrayMath.sum(values);
	}

	public SparseVector toSparseVector() {
		int[] newIndexes = new int[sizeOfNonzero()];
		double[] newValues = new double[newIndexes.length];
		double sum = 0;
		for (int i = 0, loc = 0; i < values.length; i++) {
			double value = values[i];
			if (value != 0) {
				newIndexes[loc] = i;
				newValues[loc] = value;
				sum += value;
				loc++;
			}
		}
		SparseVector ret = new SparseVector(newIndexes, newValues, label(), dim());
		ret.setSum(sum);
		return ret;
	}

	public String toString() {
		return toString(20, true, true, null);
	}

	public String toString(int numKeys, boolean printSparsely, boolean printLabel, NumberFormat nf) {
		if (nf == null) {
			nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(4);
			nf.setGroupingUsed(false);
		}

		StringBuffer sb = new StringBuffer();

		if (printLabel) {
			sb.append(String.format("%d (%d/%d, %s) ->", label, sizeOfNonzero(), values.length, nf.format(sum)));
		} else {
			sb.append(String.format("(%d/%d, %s) ->", sizeOfNonzero(), values.length, nf.format(sum)));
		}

		// int numPrint = 0;

		for (int i = 0, numPrint = 0; i < values.length; i++) {
			if (numPrint == numKeys) {
				break;
			}

			double value = values[i];

			if (printSparsely) {
				if (value != 0) {
					sb.append(String.format(" %d:%s", i, nf.format(value)));
					numPrint++;
				}
			} else {
				sb.append(String.format(" %d:%s", i, nf.format(value)));
				numPrint++;
			}

		}

		return sb.toString();
	}

	public double value(int index) {
		return values[index];
	}

	public double valueAlways(int index) {
		return value(index);
	}

	public double valueAtLoc(int loc) {
		return value(loc);
	}

	public double[] values() {
		return values;
	}

	public void write(ObjectOutputStream oos) throws IOException {
		int size = size();
		int label = label();
		int sizeOfNonzero = sizeOfNonzero();

		oos.writeInt(size);
		oos.writeInt(label);
		oos.writeInt(sizeOfNonzero);

		for (int i = 0; i < size; i++) {
			double value = value(i);
			if (value == 0) {
				continue;
			}
			oos.writeInt(i);
			oos.writeDouble(value);
		}
	}

	public void write(String fileName) throws Exception {
		System.out.printf("write to [%s].\n", fileName);
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		write(oos);
		oos.close();
	}
}
