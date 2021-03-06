package ohs.matrix;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;

public class DenseVector implements Vector {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1185683442330052104L;

	public static void write(ObjectOutputStream oos, List<DenseVector> xs) throws Exception {
		oos.writeInt(xs.size());
		for (int i = 0; i < xs.size(); i++) {
			DenseVector x = xs.get(i);
			x.writeObject(oos);
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

	public DenseVector(double[] values) {
		this.values = values;
		this.sum = 0;
	}

	public DenseVector(int size) {
		this(new double[size]);
	}

	public DenseVector() {
		
	}

	@Override
	public int argMax() {
		return ArrayMath.argMax(values);
	}

	@Override
	public int argMin() {
		return ArrayMath.argMin(values);
	}

	public void clear() {
		setAll(0);
		sum = 0;
	}

	@Override
	public DenseVector copy() {
		DenseVector ret = new DenseVector(ArrayUtils.copy(values));
		ret.setSum(sum);
		return ret;
	}

	@Override
	public int dim() {
		return size();
	}

	@Override
	public void increment(int i, double value) {
		values[i] += value;
		sum += value;
	}

	@Override
	public void incrementAll(double increment) {
		sum = ArrayMath.add(values, increment, values);
	}

	@Override
	public void incrementAtLoc(int loc, double value) {
		increment(loc, value);
	}

	@Override
	public void incrementAtLoc(int loc, int i, double value) {
		new UnsupportedOperationException("unsupported");
	}

	@Override
	public int indexAtLoc(int loc) {
		return loc;
	}

	@Override
	public int[] indexes() {
		new UnsupportedOperationException("unsupported");
		return null;
	}

	@Override
	public String info() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void keepAbove(double cutoff) {
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			if (values[i] < cutoff) {
				values[i] -= value;
				sum -= value;
			}
		}
	}

	@Override
	public void keepTopN(int topN) {
		SparseVector x1 = toSparseVector();
		x1.keepTopN(topN);
		DenseVector x2 = x1.toDenseVector();
		this.values = x2.values();
		this.sum = x2.sum();
	}

	@Override
	public int location(int index) {
		return index;
	}

	@Override
	public double max() {
		int index = argMax();
		double max = Double.NEGATIVE_INFINITY;
		if (index > -1) {
			max = values[index];
		}
		return max;
	}

	@Override
	public double min() {
		int index = argMin();
		double max = Double.POSITIVE_INFINITY;
		if (index > -1) {
			max = values[index];
		}
		return max;
	}

	@Override
	public void normalize() {
		sum = ArrayMath.scale(values, 1f / sum, values);
	}

	@Override
	public void normalizeAfterSummation() {
		sum = ArrayMath.sum(values);
		sum = ArrayMath.scale(values, 1f / sum, values);
	}

	@Override
	public void normalizeByL2Norm() {
		sum = ArrayMath.normalizeByL2Norm(values, values);
	}

	@Override
	public double prob(int index) {
		return values[index] / sum;
	}

	@Override
	public double probAlways(int index) {
		return prob(index);
	}

	@Override
	public double probAtLoc(int loc) {
		return prob(loc);
	}

	@Override
	public void prune(Set<Integer> toRemove) {
		for (int i : toRemove) {
			double value = values[i];
			values[i] -= value;
			sum -= value;
		}
	}

	@Override
	public void pruneExcept(Set<Integer> toKeep) {
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			if (!toKeep.contains(i)) {
				values[i] -= value;
				sum -= value;
			}
		}
	}

	@Override
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

	public void readObject(ObjectInputStream ois) throws Exception {
		values = FileUtils.readDoubleArray(ois);
		summation();
	}

	public void readObject(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		readObject(ois);
		ois.close();
	}

	@Override
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

	@Override
	public void scaleAtLoc(int loc, int i, double factor) {
		new UnsupportedOperationException("unsupported");
	}

	@Override
	public void set(int i, double value) {
		values[i] = value;
	}

	@Override
	public void setAll(double value) {
		ArrayUtils.setAll(values, value);
		sum = value * values.length;
	}

	@Override
	public void setAtLoc(int loc, double value) {
		set(loc, value);
	}

	@Override
	public void setAtLoc(int loc, int i, double value) {
		if (loc != i) {
			new IllegalArgumentException("different index");
		}
		setAtLoc(loc, value);
	}

	@Override
	public void setDim(int dim) {
		new UnsupportedOperationException("unsupported");

	}

	@Override
	public void setIndexes(int[] indexes) {
		new UnsupportedOperationException("unsupported");
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
		return values.length;
	}

	@Override
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

	@Override
	public double sum() {
		return sum;
	}

	@Override
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
		SparseVector ret = new SparseVector(newIndexes, newValues, dim());
		ret.setSum(sum);
		return ret;
	}

	@Override
	public String toString() {
		return toString(20, true, null);
	}

	public String toString(int numKeys, boolean printSparsely, NumberFormat nf) {
		if (nf == null) {
			nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(4);
			nf.setGroupingUsed(false);
		}

		StringBuffer sb = new StringBuffer();
		sb.append(String.format("(%d/%d, %s) ->", sizeOfNonzero(), values.length, nf.format(sum)));

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

	@Override
	public double value(int index) {
		return values[index];
	}

	@Override
	public double valueAlways(int index) {
		return value(index);
	}

	@Override
	public double valueAtLoc(int loc) {
		return value(loc);
	}

	@Override
	public double[] values() {
		return values;
	}

	@Override
	public void writeObject(ObjectOutputStream oos) throws Exception {
		FileUtils.writeDoubleArray(oos, values);
	}

	@Override
	public void writeObject(String fileName) throws Exception {
		System.out.printf("write to [%s].\n", fileName);
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		writeObject(oos);
		oos.close();
	}
}
