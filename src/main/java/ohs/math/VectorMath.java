package ohs.math;

import java.util.Arrays;
import java.util.List;

import ohs.matrix.DenseMatrix;
import ohs.matrix.DenseVector;
import ohs.matrix.Matrix;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.matrix.Vector;
import ohs.types.Counter;

/**
 * @author Heung-Seon Oh
 * 
 */
public class VectorMath {

	public static void cumulate(Vector x) {
		double sum = 0;
		for (int i = 0; i < x.size(); i++) {
			sum += x.valueAtLoc(i);
			x.setAtLoc(i, sum);
		}
		x.setSum(sum);
	}

	public static SparseVector add(Vector a, Vector b) {
		return add(new Vector[] { a, b });
	}

	public static void add(Vector a, Vector b, Vector c) {
		addAfterScale(a, b, 1, 1, c);
	}

	public static SparseVector add(Vector[] as) {
		double[] coefs = new double[as.length];
		ArrayUtils.setAll(coefs, 1);
		return addAfterScale(as, coefs);
	}

	public static SparseVector addAfterScale(Vector a, Vector b, double ac, double bc) {
		return addAfterScale(new Vector[] { a, b }, new double[] { ac, bc });
	}

	public static void addAfterScale(Vector a, Vector b, double ac, double bc, Vector c) {
		if (VectorChecker.isSameDimension(a, b) && VectorChecker.isSameDimension(b, c)) {

		} else {
			new IllegalArgumentException("different dimension");
		}

		if (isSparse(c)) {
			SparseVector sv = addAfterScale(a, b, ac, bc);
			c.setIndexes(sv.indexes());
			c.setValues(sv.values());
			c.setSum(sv.sum());
		} else {
			for (int i = 0; i < a.size(); i++) {
				int index = a.indexAtLoc(i);
				double value = a.valueAtLoc(i);
				c.increment(index, ac * value);
			}

			for (int i = 0; i < b.size(); i++) {
				int index = b.indexAtLoc(i);
				double value = b.valueAtLoc(i);
				c.increment(index, ac * value);
			}
		}
	}

	public static SparseVector addAfterScale(Vector[] as, double[] coefs) {
		Counter<Integer> c = new Counter<Integer>();
		for (int i = 0; i < as.length; i++) {
			Vector a = as[i];
			double coef = coefs[i];
			for (int j = 0; j < a.size(); j++) {
				int index = a.indexAtLoc(j);
				double value = a.valueAtLoc(j);
				c.incrementCount(index, coef * value);
			}
		}
		return VectorUtils.toSparseVector(c);
	}

	public static double cosine(Vector a, Vector b, boolean normalizeBefore) {
		if (a.dim() != b.dim()) {
			new IllegalArgumentException("different dimension");
		}
		double norm1 = 0;
		double norm2 = 0;
		double dotProduct = 0;

		if (!isSparse(a) && !isSparse(b)) {
			for (int i = 0; i < a.size(); i++) {
				double value1 = normalizeBefore ? a.prob(i) : a.value(i);
				double value2 = normalizeBefore ? b.prob(i) : b.value(i);
				dotProduct += (value1 * value2);
				norm1 += value1 * value1;
				norm2 += value2 * value2;
			}
		} else {
			int i = 0, j = 0;
			while (i < a.size() && j < b.size()) {
				int index1 = a.indexAtLoc(i);
				int index2 = b.indexAtLoc(j);

				double value1 = normalizeBefore ? a.probAtLoc(i) : a.valueAtLoc(i);
				double value2 = normalizeBefore ? b.probAtLoc(j) : b.valueAtLoc(j);

				if (index1 == index2) {
					dotProduct += (value1 * value2);
					norm1 += (value1 * value1);
					norm2 += (value2 * value2);
					i++;
					j++;
				} else if (index1 > index2) {
					norm2 += (value2 * value2);
					j++;
				} else if (index1 < index2) {
					norm1 += (value1 * value1);
					i++;
				}
			}
		}

		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);
		double cosine = ArrayMath.cosine(dotProduct, norm1, norm2);
		return cosine;
	}

	public static void DFRee(List<SparseVector> xs) {
		int maxId = 0;

		for (SparseVector x : xs) {
			int id = x.indexAtLoc(x.size() - 1);
			if (id > maxId) {
				maxId = id;
			}
		}

		DenseVector term_countInBG = new DenseVector(maxId + 1);
		for (SparseVector x : xs) {
			add(term_countInBG, x, term_countInBG);
		}

		double numberOfTokens = term_countInBG.sum();

		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);
			double docLength = x.sum();

			for (int j = 0; j < x.size(); j++) {
				int termId = x.indexAtLoc(j);
				double tf = x.valueAtLoc(j);

				double prior = tf / docLength;
				double posterior = (tf + 1d) / (docLength + 1);
				double termFrequency = term_countInBG.value(termId);
				double InvPriorCollection = numberOfTokens / termFrequency;
				double norm = tf * CommonFuncs.log2(posterior / prior);
				double weight = norm * (tf * (-CommonFuncs.log2(prior * InvPriorCollection)) +

				(tf + 1d) * (+CommonFuncs.log2(posterior * InvPriorCollection)) + 0.5 * CommonFuncs.log2(posterior / prior));
				x.setAtLoc(j, weight);
			}
			unitVector(x);
		}
	}

	public static void distribute(Vector x, double sum) {
		double portionSum = 0;
		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			double prob = x.sum() == 1 ? x.valueAtLoc(i) : x.probAtLoc(i);
			double portion = sum * prob;
			x.setAtLoc(i, portion);
			portionSum += portion;
		}
		x.setSum(portionSum);
	}

	public static Vector distributeTo(Vector x, double sum) {
		Vector ret = x.copy();
		distribute(x, sum);
		return ret;
	}

	public static double dotProduct(Vector a, Vector b, boolean normalizeBefore) {
		if (a.dim() != b.dim()) {
			new IllegalArgumentException("different dimension");
		}
		double ret = 0;
		if (isSparse(a) && isSparse(b)) {
			int i = 0, j = 0;
			while (i < a.size() && j < b.size()) {
				int index1 = a.indexAtLoc(i);
				int index2 = a.indexAtLoc(j);
				double value1 = normalizeBefore ? a.probAtLoc(i) : a.valueAtLoc(i);
				double value2 = normalizeBefore ? b.probAtLoc(j) : b.valueAtLoc(j);
				if (index1 == index2) {
					ret += (value1 * value2);
					i++;
					j++;
				} else if (index1 > index2) {
					j++;
				} else if (index1 < index2) {
					i++;
				}
			}
		} else if (!isSparse(a) && !isSparse(b)) {
			for (int i = 0; i < a.dim(); i++) {
				double value1 = normalizeBefore ? a.probAtLoc(i) : a.valueAtLoc(i);
				double value2 = normalizeBefore ? b.probAtLoc(i) : b.valueAtLoc(i);
				ret += value1 * value2;
			}
		} else {
			SparseVector sv = null;
			DenseVector dv = null;

			if (isSparse(a)) {
				dv = (DenseVector) b;
				sv = (SparseVector) a;
			} else {
				dv = (DenseVector) a;
				sv = (SparseVector) b;
			}

			for (int i = 0; i < sv.size(); i++) {
				int index = sv.indexAtLoc(i);
				double value2 = normalizeBefore ? sv.probAtLoc(i) : sv.valueAtLoc(i);
				double value1 = normalizeBefore ? dv.prob(index) : dv.value(index);
				ret += value1 * value2;
			}
		}
		return ret;
	}

	public static double entropy(Vector x) {
		return ArrayMath.entropy(x.values());
	}

	public static double euclideanDistance(Vector a, Vector b, boolean normalizeBefore) {
		double ret = 0;
		int i = 0, j = 0;
		while (i < a.size() && j < b.size()) {
			int index1 = a.indexAtLoc(i);
			int index2 = a.indexAtLoc(j);
			double value1 = normalizeBefore ? a.probAtLoc(i) : a.valueAtLoc(i);
			double value2 = normalizeBefore ? b.probAtLoc(i) : b.valueAtLoc(j);
			double diff = 0;
			if (index1 == index2) {
				diff = value1 - value2;
				i++;
				j++;
			} else if (index1 > index2) {
				diff = -value2;
				j++;
			} else if (index1 < index2) {
				diff = value1;
				i++;
			}
			ret += diff * diff;
		}
		ret = Math.sqrt(ret);
		return ret;
	}

	public static void exponentiate(Vector a, boolean normalizeByMax) {
		if (normalizeByMax) {
			double max = a.max();
			double sum = 0;
			for (int i = 0; i < a.size(); i++) {
				double value = a.valueAtLoc(i);
				value = Math.exp(value - max);
				a.setAtLoc(i, value);
				sum += value;
			}
			a.setSum(sum);
			a.normalize();
		} else {
			double sum = 0;
			for (int i = 0; i < a.size(); i++) {
				double value = a.valueAtLoc(i);
				value = Math.exp(value);
				a.setAtLoc(i, value);
				sum += value;
			}
			a.setSum(sum);
		}
	}

	public static double geometricMean(Vector x) {
		double logSum = 0;
		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			double value = x.valueAtLoc(index);
			logSum += Math.log(value);
		}
		return Math.exp(logSum / x.size());
	}

	public static boolean isSparse(Matrix x) {
		return x instanceof SparseMatrix;
	}

	public static boolean isSparse(Vector x) {
		return x instanceof SparseVector;
	}

	public static boolean isValid(Vector x) {
		boolean ret = true;

		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			double value = x.valueAtLoc(i);
			if (Double.isInfinite(value) || Double.isNaN(value) || index < 0) {
				System.out.println(String.format("(%d, %d, %s)", i, index, value));
				System.exit(0);
				ret = false;
				break;
			}
		}

		return ret;
	}

	public static double jsDivergence(Vector a, Vector b) {
		return lambdaDivergence(a, b, 0.5);
	}

	public static double klDivergence(Vector a, Vector b, boolean symmetric) {
		double ret = 0;
		int i = 0, j = 0;

		while (i < a.size() && j < b.size()) {
			int index1 = a.indexAtLoc(i);
			int index2 = b.indexAtLoc(j);

			if (index1 == index2) {
				double value1 = a.sum() == 1 ? a.valueAtLoc(i) : a.probAtLoc(i);
				double value2 = b.sum() == 1 ? b.valueAtLoc(j) : b.probAtLoc(j);
				if (value1 != value2 && value1 > 0 && value2 > 0) {
					double div = 0;
					if (symmetric) {
						div = value1 * Math.log(value1 / value2) + value2 * Math.log(value2 / value1);
					} else {
						div = value1 * Math.log(value1 / value2);
					}
					ret += div;
				}
				i++;
				j++;
			} else if (index1 > index2) {
				j++;
			} else if (index1 < index2) {
				i++;
			}
		}

		return ret;
	}

	public static double lambdaDivergence(Vector a, Vector b, double lambda) {
		double ret = 0;
		int i = 0, j = 0;

		while (i < a.size() && j < b.size()) {
			int index1 = a.indexAtLoc(i);
			int index2 = b.indexAtLoc(j);

			if (index1 == index2) {
				double value1 = a.sum() == 1 ? a.valueAtLoc(i) : a.probAtLoc(i);
				double value2 = b.sum() == 1 ? b.valueAtLoc(j) : b.probAtLoc(j);
				if (value1 != value2 && value1 > 0 && value2 > 0) {
					double value3 = lambda * value1 + (1 - lambda) * value2;
					double term1 = value1 * Math.log(value1 / value3);
					double term2 = value2 * Math.log(value2 / value3);
					double div = lambda * term1 + (1 - lambda) * term2;
					ret += div;
				}
				i++;
				j++;
			} else if (index1 > index2) {
				j++;
			} else if (index1 < index2) {
				i++;
			}
		}

		return ret;
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		int[] indexes = ArrayUtils.range(10);
		double[] values = ArrayMath.random(0f, 1f, 10);

		SparseVector sv = new SparseVector(indexes, values, 0);
		sv.sortByValue();
		System.out.println(sv.toString());
		sv.sortByIndex();
		;
		System.out.println(sv.toString());

		Vector x1 = new SparseVector(indexes, values, -1);
		System.out.println("process ends.");

	}

	public static double mean(Vector x) {
		return x.sum() / x.size();
	}

	public static void normalizeAfterExp(Vector x) {
		double sum = 0;
		for (int i = 0; i < x.size(); i++) {
			double value = x.valueAtLoc(i);
			value = Math.exp(value);
			sum += value;
			x.setAtLoc(i, value);
		}
		x.setSum(sum);
		x.normalize();
	}

	public static void normalizeByMinMax(List<SparseVector> xs) {
		int maxIndex = 0;
		for (SparseVector x : xs) {
			int index = x.indexAtLoc(x.size() - 1);
			if (index > maxIndex) {
				maxIndex = index;
			}
		}

		normalizeByMinMax(xs, maxIndex + 1);

	}

	public static void normalizeByMinMax(List<SparseVector> xs, int indexSize) {
		DenseVector index_max = new DenseVector(indexSize);
		DenseVector index_min = new DenseVector(indexSize);

		index_max.setAll(-Double.MAX_VALUE);
		index_min.setAll(Double.MAX_VALUE);

		index_max.setSum(0);
		index_min.setSum(0);

		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);

			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j);
				double value = x.valueAtLoc(j);

				if (value > index_max.value(index)) {
					index_max.set(index, value);
				}

				if (value < index_min.value(index)) {
					index_min.set(index, value);
				}
			}
		}

		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);

			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j);
				double value = x.valueAtLoc(j);

				double max = index_max.value(index);
				double min = index_min.value(index);

				if (max == -Double.MAX_VALUE || min == Double.MAX_VALUE) {
					continue;
				}

				if (min == max) {
					continue;
				}

				double newValue = (value - min) / (max - min);
				x.setAtLoc(j, newValue);
			}
			x.summation();
		}
	}

	public static void normalizeBySigmoid(SparseVector x) {
		double sum = 0;
		for (int i = 0; i < x.size(); i++) {
			double value = x.valueAtLoc(i);
			value = 1f / (1f + Math.exp(-value));
			x.setAtLoc(i, value);

			sum += value;
		}
		x.setSum(sum);
		x.normalize();
	}

	public static void normalizeLogProbs(Vector x, boolean scale) {
		double max = x.max();
		x.incrementAll(-max);
		if (scale) {
			double min = x.min();
			if (min != 0) {
				x.scale(1f / Math.abs(min));
			}
		}
		normalizeAfterExp(x);
	}

	public static double normL1(Vector x, boolean normalizeBefore) {
		double ret = 0;
		for (int i = 0; i < x.size(); i++) {
			double value = normalizeBefore ? x.probAtLoc(i) : x.valueAtLoc(i);
			ret += Math.abs(value);
		}
		return ret;
	}

	public static double normL2(Vector x, boolean normalizeBefore) {
		double ret = 0;
		for (int i = 0; i < x.size(); i++) {
			double value = normalizeBefore ? x.probAtLoc(i) : x.valueAtLoc(i);
			ret += (value * value);
		}
		ret = Math.sqrt(ret);
		return ret;
	}

	// public static void pointwiseMultiply(Matrix a, Matrix b) {
	// if (!VectorChecker.isProductable(a, b)) {
	// new IllegalArgumentException("different dimension");
	// }
	//
	// if (isSparse(a) && isSparse(b)) {
	// SparseMatrix m1 = (SparseMatrix) a;
	// SparseMatrix m2 = (SparseMatrix) b;
	// for (int i = 0; i < m1.rowSize(); i++) {
	// int rowId = m1.indexAtRowLoc(i);
	// Vector row1 = m1.vectorAtRowLoc(i);
	// Vector row2 = m2.rowAlways(rowId);
	//
	// if (row2 == null) {
	// row1.setAll(0);
	// } else {
	// pointwiseMultiply(row1, row2);
	// }
	// }
	// } else if (!isSparse(a) && !isSparse(b)) {
	// DenseMatrix m1 = (DenseMatrix) a;
	// DenseMatrix m2 = (DenseMatrix) b;
	// for (int i = 0; i < m1.rowDim(); i++) {
	// pointwiseMultiply(m1.row(i), m2.row(i));
	// }
	// } else if (!isSparse(a) && isSparse(b)) {
	// DenseMatrix m1 = (DenseMatrix) a;
	// SparseMatrix m2 = (SparseMatrix) b;
	//
	// for (int i = 0; i < m1.rowDim(); i++) {
	// Vector row1 = m1.row(i);
	// Vector row2 = m2.rowAlways(i);
	// if (row2 == null) {
	// row1.setAll(0);
	// } else {
	// pointwiseMultiply(row1, row2);
	// }
	// }
	// } else if (isSparse(a) && !isSparse(b)) {
	// SparseMatrix m1 = (SparseMatrix) a;
	// DenseMatrix m2 = (DenseMatrix) b;
	//
	// for (int i = 0; i < m1.rowSize(); i++) {
	// int rowId = m1.indexAtRowLoc(i);
	// Vector row1 = m1.vectorAtRowLoc(i);
	// Vector row2 = m2.row(rowId);
	// pointwiseMultiply(row1, row2);
	// }
	// }
	// }

	public static void pointwiseMultiply(Vector a, Vector b, Vector c) {
		if (!VectorChecker.isSameDimension(a, b)) {
			new IllegalArgumentException("different dimension");
		}

		if (c instanceof SparseVector) {
			Counter<Integer> counter = new Counter<Integer>();
			int i = 0, j = 0;
			while (i < a.size() && j < b.size()) {
				int index1 = a.indexAtLoc(i);
				int index2 = b.indexAtLoc(j);
				double value1 = a.valueAtLoc(i);
				double value2 = b.valueAtLoc(j);
				if (index1 == index2) {
					double product = value1 * value2;
					if (product != 0) {
						counter.incrementCount(index1, product);
					}
					i++;
					j++;
				} else if (index1 > index2) {
					j++;
				} else if (index1 < index2) {
					i++;
				}
			}
			SparseVector cc = (SparseVector) c;
			SparseVector temp = VectorUtils.toSparseVector(counter);
			cc.setIndexes(temp.indexes());
			cc.setValues(temp.values());
			cc.setSum(counter.totalCount());
		} else {
			DenseVector cc = (DenseVector) c;
			int i = 0, j = 0;
			while (i < a.size() && j < b.size()) {
				int index1 = a.indexAtLoc(i);
				int index2 = b.indexAtLoc(j);
				double value1 = a.valueAtLoc(i);
				double value2 = b.valueAtLoc(j);
				if (index1 == index2) {
					double product = value1 * value2;
					cc.increment(index1, product);
					i++;
					j++;
				} else if (index1 > index2) {
					j++;
				} else if (index1 < index2) {
					i++;
				}
			}
		}
	}

	public static void product(Matrix a, DenseVector b, DenseVector c) {
		if (!VectorChecker.isProductable(a, b, c)) {
			new IllegalArgumentException("different dimension");
		}

		if (isSparse(a)) {
			SparseMatrix m = (SparseMatrix) a;
			double sum = 0;
			int prevRowIndex = -1;

			for (int i = 0; i < m.rowDim(); i++) {
				int rowIndex = m.indexAtRowLoc(i);
				SparseVector row = m.vectorAtRowLoc(i);

				for (int j = prevRowIndex + 1; j < rowIndex; j++) {
					c.set(j, 0);
				}

				double dotProduct = dotProduct(row, b, false);
				c.set(rowIndex, dotProduct);
				sum += dotProduct;

				prevRowIndex = rowIndex;
			}
			c.setSum(sum);
		} else {
			DenseMatrix m = (DenseMatrix) a;
			double sum = 0;

			for (int i = 0; i < m.rowDim(); i++) {
				double dotProduct = dotProduct(m.row(i), b, false);
				c.set(i, dotProduct);
				sum += dotProduct;
			}
			c.setSum(sum);
		}
	}

	public static void product(Matrix a, Matrix b, Matrix c) {
		if (!VectorChecker.isProductable(a, b, c)) {
			new IllegalArgumentException("different dimension");
		}

		if (!isSparse(a) && !isSparse(b)) {
			DenseMatrix m1 = (DenseMatrix) a;
			DenseMatrix m2 = (DenseMatrix) b;
			DenseMatrix m3 = (DenseMatrix) c;

			for (int j = 0; j < b.colDim(); j++) {
				Vector column = b.column(j);
				for (int i = 0; i < a.rowDim(); i++) {
					Vector row = a.row(i);
					double dotProduct = dotProduct(row, column, false);
					c.set(i, j, dotProduct);
				}
			}
		}
	}

	public static SparseVector rank(Vector x) {
		SparseVector ret = null;
		if (isSparse(x)) {
			ret = (SparseVector) x.copy();
			ret.sortByValue();
			for (int i = 0; i < ret.size(); i++) {
				ret.setAtLoc(i, i + 1);
			}
			ret.setSum(0);
		} else {
			SparseVector m = ((DenseVector) x).toSparseVector();
			m.sortByValue();

			ret = m.copy();
			for (int i = 0; i < m.size(); i++) {
				ret.setAtLoc(i, i + 1);
			}
			ret.setSum(0);
		}
		ret.sortByIndex();
		return ret;
	}

	public static void scale(List<Vector> xs, double upper, double lower) {
		int max_index = Integer.MIN_VALUE;

		for (int i = 0; i < xs.size(); i++) {
			Vector x = xs.get(i);
			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j);
				if (index > max_index) {
					max_index = index;
				}
			}
		}

		double[] feature_max = new double[max_index + 1];
		double[] feature_min = new double[max_index + 1];

		Arrays.fill(feature_max, Double.MIN_VALUE);
		Arrays.fill(feature_min, Double.MAX_VALUE);

		for (int i = 0; i < xs.size(); i++) {
			Vector x = xs.get(i);
			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j);
				double value = x.valueAtLoc(j);
				feature_max[index] = Math.max(value, feature_max[index]);
				feature_min[index] = Math.min(value, feature_min[index]);
			}
		}

		for (int i = 0; i < xs.size(); i++) {
			Vector x = xs.get(i);
			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j);
				double value = x.valueAtLoc(j);

				double max = feature_max[index];
				double min = feature_min[index];

				if (max == min) {

				} else if (value == min) {

				} else if (value == max) {

				} else {
					value = lower + (upper - lower) * (value - min) / (max - min);
				}
				x.setAtLoc(j, index, value);
			}
		}
	}

	public static void subtract(Vector a, Vector b) {
		addAfterScale(a, b, 1, -1);
	}

	public static void subtract(Vector a, Vector b, Vector c) {
		addAfterScale(a, b, 1, -1, c);
	}

	public static void subtractAfterScale(Vector a, Vector b, double aCoef, double bCoef, SparseVector c) {
		addAfterScale(a, b, aCoef, -bCoef, c);
	}

	public static void unitVector(Vector x) {
		x.scale(1f / normL2(x, false));
	}

	public static double variance(Vector x) {
		return variance(x, mean(x));
	}

	public static double variance(Vector x, double mean) {
		double ret = 0;
		for (int i = 0; i < x.size(); i++) {
			double diff = x.valueAtLoc(i) - mean;
			ret += diff * diff;
		}
		ret /= x.size();
		return ret;
	}

}
