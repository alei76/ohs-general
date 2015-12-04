package ohs.math;

import ohs.matrix.Matrix;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.matrix.Vector;

public class VectorChecker {

	public static boolean isProductable(Matrix a, Matrix b) {
		return a.colDim() == b.rowDim() ? true : false;
	}

	public static boolean isProductable(Matrix a, Matrix b, Matrix c) {
		int aRowDim = a.rowDim();
		int aColDim = a.colDim();

		int bRowDim = b.rowDim();
		int bColDim = b.colDim();

		int cRowDim = c.rowDim();
		int cColDim = c.colDim();

		if (aRowDim == cRowDim && bColDim == cColDim && aColDim == bRowDim) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isProductable(Matrix a, Vector b) {
		return a.colDim() == b.dim() ? true : false;
	}

	public static boolean isProductable(Matrix a, Vector b, Vector c) {
		return isProductable(a, b) && isProductable(b, c) ? true : false;
	}

	public static boolean isProductable(Vector a, Vector b) {
		return a.dim() == b.dim() ? true : false;
	}

	public static boolean isSameDimension(Vector a, Vector b) {
		return a.dim() == b.dim() ? true : false;
	}

	public static boolean isSameDimensions(Matrix a, Matrix b) {
		return a.rowDim() == b.rowDim() && a.colDim() == b.colDim() ? true : false;
	}

	public static boolean isSparse(Matrix a) {
		return a instanceof SparseMatrix;
	}

	public static boolean isSparse(Vector a) {
		return a instanceof SparseVector;
	}

}
