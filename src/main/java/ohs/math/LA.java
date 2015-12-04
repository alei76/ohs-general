package ohs.math;

import ohs.matrix.Matrix;
import ohs.matrix.SparseMatrix;
import ohs.matrix.Vector;

/**
 * @author Heung-Seon Oh
 * @version 1.2
 * @date 2009. 6. 4
 * 
 */
public class LA {

	public enum SolutionType {
		Unique, Infinite, No
	}

	public enum VariableType {
		Free, Basic
	}

	public static int MAX_ITER = 100;

	public static double TOLERANCE = 0.0000005f;

	public static void adjoint(double[][] a, double[][] b) {

		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bRowDim = b.length;
		int bColDim = b[0].length;

		if (aRowDim == bColDim && aColDim == bRowDim) {

		} else {
			throw new IllegalArgumentException();
		}

		double[][] temp = new double[aRowDim - 1][aColDim - 1];

		for (int i = 0; i < aRowDim; i++) {
			for (int j = 0; j < aColDim; j++) {
				b[j][i] = cofactor(a, i, j, temp);
			}
		}
	}

	public static Matrix asParametricVectorForm(Matrix U) {
		Matrix ret = new SparseMatrix(U.rowDim(), U.colDim(), 0, null);

		return ret;
	}

	/**
	 * [Reference]
	 * 
	 * 1. http://en.wikipedia.org/wiki/Triangular_matrix# Forward_and_Back_Substitution
	 * 
	 * @param u
	 *            input upper triangular matrix
	 * @param x
	 *            output
	 * @param y
	 *            input
	 * 
	 * @return
	 */
	public static void backSubstitution(double[][] u, double[] x, double[] y) {

		if (!ArrayChecker.isProductable(u, x, y)) {
			throw new IllegalArgumentException();
		}

		int rowDim = u.length;
		int colDim = u[0].length;

		for (int i = rowDim - 1; i > -1; i--) {
			double Uii = u[i][i];
			if (Uii != 0) {
				double sum = 0;
				for (int j = colDim - 1; j > i; j--) {
					double Xj = x[j];
					double Aij = u[i][j];
					sum += Aij * Xj;
				}
				double Yi = y[i];
				double Xi = (Yi - sum) / Uii;
				x[i] = Xi;
			}
		}
	}

	/**
	 * [Theorem 6.9 - The Best Approximation Theorem]
	 * 
	 * Let W be a subspace of R^n, y any vector in R^n, and y' the orthogonal projection of y onto W. Then y' is the closest point in W to
	 * y, in the sense that
	 * 
	 * |y-y'| < |y-v|
	 * 
	 * for all v in W distinct from y'.
	 * 
	 * 
	 * 
	 * @param A
	 * @param B
	 * @return
	 */
	public static double bestApproximation(double[][] a, double[] b) {
		if (!ArrayChecker.isProductable(a, b)) {
			throw new IllegalArgumentException("invalid best approximation");
		}

		double[] c = new double[b.length];
		orthogonalToProjection(a, b, c);
		return ArrayMath.normL2(c);
	}

	/**
	 * @param a
	 * @param i
	 * @param j
	 * @param b
	 *            Reusable matrix for computing minor matrix which is b[aRowDim-1][aColDim-1]
	 * @return
	 */
	public static double cofactor(double[][] a, int i, int j, double[][] b) {
		double ret = minor(a, i, j, b);
		if ((i + j) % 2 != 0) {
			ret *= -1;
		}
		return ret;
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void colOrthogonalBasis(double[][] a, double[][] b) {
		GramSchmidtProcess(a, b);
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void colOrthonormalBasis(double[][] a, double[][] b) {

		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}

		colOrthogonalBasis(a, b);

		int rowDim = a.length;
		int colDim = a[0].length;

		double[] ek = new double[colDim];

		for (int k = 0; k < colDim; k++) {
			ArrayUtils.copyColumn(b, k, ek);
			ArrayMath.unitVector(ek, ek);
			ArrayUtils.copyColumn(ek, b, k);
		}
	}

	/**
	 * [Definition]
	 * 
	 * For n >= 2, the determinant of an n x n matrix A =[a_ij] is the sum of n terms of the form +-a1j*det A_1j, with plus and minus signs
	 * alternating, where the entries a_11, a_12,...,a1n are from the first row of A. In symbols,
	 * 
	 * det A = a_11*detA_11 - a12*detA_12 + ... + (-1)^(1+n)*a_1n*detA_1n
	 * 
	 * [Theorem 3.2]
	 * 
	 * If A is a triangular matrix, then det A is the product of the entries on the main diagonal of A.
	 * 
	 * [Theorem 3.5]
	 * 
	 * If A is an n x n matrix, then det A^t = det A.
	 * 
	 * [Theorem 3.6]
	 * 
	 * If A and B are n x n matrices, then det A B = (det A) (det B).
	 * 
	 * 
	 * [Theorem 3.9]
	 * 
	 * If A is a 2 x 2 matrix, the area of the parallelogram determined by the columns of A is |det A|. If A is a 3 x 3 matrix, the volume
	 * of the parallelepiped determined by the columns of A is |det A|.
	 * 
	 * [Theorem 3.10]
	 * 
	 * Let T: R^2 -> R^2 be the linear transformation determined by a 2 x 2 matrix. If S is a parallelogram in R^2, then
	 * 
	 * {area of T(S) = |det A| * {area of S}
	 * 
	 * If T is determined by a 3 x 3 matrix A, and if S is a parallelepiped in R^3, then
	 * 
	 * {volume of T(S)} = |det A| * {volume of S}
	 * 
	 * @param A
	 * @return
	 */
	public static double determinant(double[][] a) {
		double ret = 0;

		if (ArrayChecker.isSquare(a)) {
			int rowDim = a.length;
			int colDim = a[0].length;

			if (ArrayChecker.isTriangular(a)) {
				ret = trace(a);
			} else {
				if (rowDim == 1) {
					ret = a[0][0];
				} else if (rowDim == 2) {
					ret = determinant2By2(a);
				} else if (rowDim == 3) {
					ret = determinant3By3(a);
				} else {
					double[][] b = new double[rowDim - 1][colDim - 1];
					ret = determinantByCofactor(a, true, b);
				}
			}
		}
		return ret;
	}

	private static double determinant2By2(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;
		if (rowDim == colDim && rowDim == 2) {

		} else {
			throw new IllegalArgumentException();
		}
		return a[0][0] * a[1][1] - a[0][1] * a[1][0];
	}

	private static double determinant3By3(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;
		if (rowDim == colDim && rowDim == 3) {

		} else {
			throw new IllegalArgumentException();
		}
		double ret =

		a[0][0] * a[1][1] * a[2][2]

				+ a[0][1] * a[1][2] * a[2][0]

				+ a[0][2] * a[1][0] * a[2][1]

				- a[0][2] * a[1][1] * a[2][0]

				- a[0][1] * a[1][0] * a[2][2]

				- a[0][0] * a[1][2] * a[2][1];

		return ret;

	}

	/**
	 * @param a
	 *            input
	 * 
	 * @param doSelection
	 *            Select a row or a column which has the most zero entries to reduce computations.
	 * 
	 * @param b
	 *            Reusable matrix for computing minor matrix. (row-1, column-1) of a
	 * @return
	 */
	private static double determinantByCofactor(double[][] a, boolean doSelection, double[][] b) {
		double ret = 0;
		int rowDim = a.length;
		int colDim = a[0].length;

		int maxZeroColumnLoc = -1;
		int maxZeroRowLoc = -1;

		/*
		 * Select a row or a column which has the most zero entries.
		 */

		if (doSelection) {
			int[] zerosInColumns = new int[rowDim];
			int[] zerosInRows = new int[colDim];

			int sumOfZerosInColumns = 0;
			int sumOfZerosInRows = 0;

			for (int i = 0; i < rowDim; i++) {
				for (int j = 0; j < colDim; j++) {
					if (a[i][j] == 0) {
						zerosInColumns[i]++;
						zerosInRows[j]++;
						sumOfZerosInColumns++;
						sumOfZerosInRows++;
					}
				}
			}

			int maxColumnZeros = -1;
			int maxRowZeros = -1;

			for (int i = 0; i < zerosInColumns.length; i++) {
				if (zerosInColumns[i] > maxColumnZeros) {
					maxColumnZeros = zerosInColumns[i];
					maxZeroRowLoc = i;
				}
			}

			for (int j = 0; j < zerosInRows.length; j++) {
				if (zerosInRows[j] > maxRowZeros) {
					maxRowZeros = zerosInRows[j];
					maxZeroColumnLoc = j;
				}
			}

			if (maxColumnZeros > 0 || maxRowZeros > 0) {
				if (maxColumnZeros > maxRowZeros) {
					maxZeroColumnLoc = -1;
				} else if (maxColumnZeros < maxRowZeros) {
					maxZeroRowLoc = -1;
				} else {

				}
			}
		}

		if (maxZeroRowLoc > -1) {
			int i = maxZeroRowLoc;
			for (int j = 0; j < colDim; j++) {
				if (a[i][j] != 0) {
					double a_ij = a[i][j];
					double cofactor = cofactor(a, i, j, b);
					ret += a_ij * cofactor;
				}
			}
		} else if (maxZeroColumnLoc > -1) {
			int j = maxZeroColumnLoc;
			for (int i = 0; i < rowDim; i++) {
				if (a[i][j] != 0) {
					double a_ij = a[i][j];
					double cofactor = cofactor(a, i, j, b);
					ret += a_ij * cofactor;
				}
			}
		} else {
			int i = 0;
			for (int j = 0; j < colDim; j++) {
				if (a[i][j] != 0) {
					double a_ij = a[i][j];
					double cofactor = cofactor(a, i, j, b);
					ret += a_ij * cofactor;
				}
			}
		}

		return ret;
	}

	public static void diagonalization(Matrix A) {
		// boolean isSymmetric = ArrayChecker.isSymmetric(A);
		// Matrix D = eigenValues(A);
		// Matrix P = eigenVectors(D, isSymmetric);
		//
		// Matrix AP = product(A, P);
		// Matrix PD = product(P, D);
		//
		// if (!AP.equals(PD)) {
		// throw new RuntimeException("Invalid matrix diagonalization");
		// }
	}

	public static double[] diagonalVector(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;
		int min = Math.min(rowDim, colDim);
		double[] ret = new double[min];
		for (int i = 0; i < min; i++) {
			ret[i] = a[i][i];
		}
		return ret;
	}

	/**
	 * 
	 * 
	 * [Definition]
	 * 
	 * For u and v in R^n, the distance between u and v, written as dist(u,v), is the length of the vector u - v. That is,
	 * 
	 * dist(u,v) = |u - v|
	 * 
	 * 
	 * 
	 * @param A
	 * @param B
	 * @return
	 */
	public static double distance(double[] a, double[] b) {
		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}
		double ret = 0;
		for (int i = 0; i < a.length; i++) {
			double diff = a[i] - b[i];
			ret += diff * diff;
		}
		ret = Math.sqrt(ret);
		return ret;
	}

	public static double dotProduct(double[] a, double[] b) {
		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}

		double ret = 0;
		for (int i = 0; i < a.length; i++) {
			ret += a[i] * b[i];
		}
		return ret;
	}

	/**
	 * [Reference]
	 * 
	 * 1. http://math.fullerton.edu/mathews/n2003/BackSubstitutionMod.html
	 * 
	 * @param l
	 *            input lower triangular matrix
	 * @param x
	 *            output
	 * @param y
	 *            output
	 * @return
	 */
	public static void forwardSubstitution(double[][] l, double[] x, double[] y) {
		if (!ArrayChecker.isProductable(l, x, y)) {
			throw new IllegalArgumentException();
		}

		int rowDim = l.length;
		int colDim = l[0].length;

		for (int i = 0; i < rowDim; i++) {
			double Lii = l[i][i];
			if (Lii == 0) {
				continue;
			}
			double sum = 0;
			for (int j = 0; j < i; j++) {
				double Lij = l[i][j];
				double Yj = x[j];
				sum += (Lij * Yj);
			}
			double Bi = y[i];
			double Yi = (Bi - sum) / Lii;
			x[i] = Yi;
		}
	}

	/**
	 * 
	 * http://mathworld.wolfram.com/FrobeniusNorm.html
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static double FrobeniusNorm(double[][] a, double[][] b) {
		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}

		int rowDim = a.length;
		int colDim = a[0].length;

		double ret = 0;

		for (int i = 0; i < rowDim; i++) {
			for (int j = 0; j < colDim; j++) {
				double diff = a[i][j] - b[i][j];
				ret += diff * diff;
			}
		}
		ret = Math.sqrt(ret);
		return ret;
	}

	/**
	 * 
	 * [Definition]
	 * 
	 * A leading entry of a row refers to the leftmost nonzero entry (in a nonzero row).
	 * 
	 * 
	 * [Definition]
	 * 
	 * Convert a matrix to reduced echelon form (REF). REF satisfies following properties.
	 * 
	 * 1. All nonzero rows are above any rows of all zeros.
	 * 
	 * 2. Each leading entry of a row is in a column to the right of the leading entry of the row above it.
	 * 
	 * 3. All entries in a column below a leading entry are zeros.
	 * 
	 * A method of finding solutions of a matrix using REF is called "Gaussian elimination".
	 * 
	 * 
	 * [Definition]
	 * 
	 * A pivot position in a matrix A is a location in A that corresponds to a leading 1 in the reduced echelon form of A. A pivot column is
	 * a column of A that contains a pivot position.
	 * 
	 * 
	 * [Reference]
	 * 
	 * 1. http://en.wikipedia.org/wiki/Gauss-Jordan_elimination
	 * 
	 * @param a
	 *            augmented matrix [d|e]
	 * 
	 * @return pivot columns
	 */
	public static int[] GaussElimination(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;
		int[] pivotColumns = new int[a.length];

		for (int i = 0; i < rowDim; i++) {
			pivotColumns[i] = -1;
		}

		/*
		 * Row Echelon Form
		 */

		for (int i = 0, j = 0; i < rowDim && j < colDim; j++) {
			if (a[i][j] == 0) {

				/*
				 * Partial pivoting - selects as a pivot the entry in a column having the largest absolute value to reduce roundoff errors
				 * in the calculations.
				 */
				int max_abs_nonzero_i = -1;
				double max_abs_value = 0;

				for (int ii = i + 1; ii < rowDim; ii++) {
					double value = a[ii][j];
					if (value != 0 && Math.abs(value) > Math.abs(max_abs_value)) {
						max_abs_nonzero_i = ii;
						max_abs_value = value;
					}
				}

				if (max_abs_nonzero_i > 0) {
					ArrayUtils.swapRows(a, i, max_abs_nonzero_i);
				}
			}

			if (a[i][j] != 0) {
				pivotColumns[i] = j;
				for (int ii = i + 1; ii < rowDim; ii++) {
					double scaleFactor = a[ii][j] / a[i][j];

					if (scaleFactor != 0) {
						for (int jj = 0; jj < colDim; jj++) {
							a[ii][jj] = a[ii][jj] - scaleFactor * a[i][jj];
						}
					}
				}
				i++;
			}
		}

		return pivotColumns;

	}

	/**
	 * Reduced Row Echelon Form
	 * 
	 * 
	 * @param a
	 * @param pivotColumns
	 */
	public static void GaussElimination(double[][] a, int[] pivotColumns) {
		int rowDim = a.length;
		int colDim = a[0].length;
		for (int i = rowDim - 1; i >= 0; i--) {
			int j = pivotColumns[i];

			if (j == -1) {
				continue;
			}

			{
				double scaleFactor = 1f / a[i][j];
				for (int jj = j; jj < colDim; jj++) {
					a[i][jj] *= scaleFactor;
				}
			}

			for (int ii = 0; ii < i; ii++) {
				double scaleFactor = -1 * a[ii][j];
				for (int jj = j; jj < colDim; jj++) {
					a[ii][jj] = a[ii][jj] + scaleFactor * a[i][jj];
				}
			}
		}
	}

	/**
	 * 
	 * [Theorem 6.11 - The Gram-Schmidt Process]
	 * 
	 * Given a basis {x1,...,xp} for a subspace W of R^n, define
	 * 
	 * v1 = x1
	 * 
	 * v2 = x2 - ((InnerProduct(x2,v1) / InnerProduct(v1,v1)) * v1)
	 * 
	 * v3 = x3 - ((InnerProduct(x3,v1) / InnerProduct(v1,v1)) * v1) - ((InnerProduct(x3,v2) / InnerProduct(v2,v2)) * v2)
	 * 
	 * vp = xp - ((InnerProduct(xp,v1) / InnerProduct(v1,v1)) * v1) - ... - ((InnerProduct(xp,v(p-1)) / InnerProduct(v(p-1),v(p-1))) *
	 * v(p-1) )
	 * 
	 * Then {v1
	 * 
	 * http://en.wikipedia.org/wiki/Gram%E2%80%93Schmidt_process
	 * 
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void GramSchmidtProcess(double[][] a, double[][] b) {

		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}

		int rowDim = a.length;
		int colDim = a[0].length;

		double[] vk = new double[colDim];
		double[] uj = new double[colDim];
		double[] uk = new double[colDim];
		double[] temp = new double[colDim];

		ArrayUtils.copyColumn(a, 0, b, 0);

		for (int k = 1; k < colDim; k++) {
			ArrayUtils.copyColumn(a, k, vk);
			ArrayUtils.setAll(uk, 0);

			for (int j = 0; j < k; j++) {
				ArrayUtils.copyColumn(b, j, uj);
				projectionAonB(vk, uj, temp);

				ArrayMath.add(temp, uk, uk);
			}
			ArrayMath.substract(vk, uk, uk);
			ArrayUtils.copyColumn(uk, b, k);
		}
	}

	private static void improve(double[][] a, double[] x, double[] b, double[] newx) {
		double[] ax = new double[x.length];
		double[] r = new double[x.length];
		double[] z = new double[x.length];

		for (int u = 0; u < MAX_ITER; u++) {
			product(a, x, ax);
			ArrayMath.substract(b, ax, r);
			solve(a, r, z, false);

			if (ArrayChecker.isZeroVector(z)) {
				break;
			}

			ArrayMath.add(newx, z, newx);
		}
	}

	/**
	 * 
	 * [Fact]
	 * 
	 * A^(-1) A = I and A A^(-1) = I
	 * 
	 * [Theorem 2.4]
	 * 
	 * Let A = {{a, b},{c,d}}. If ad-bc != 0, then A is invertible and
	 * 
	 * A^(-1) = 1 / (ad-bc) {{d,-b},{-c,a}}
	 * 
	 * If ad-bc=0, then A is not invertible.
	 * 
	 * [Fact]
	 * 
	 * det A = ad-bc
	 * 
	 * [Theorem 2.5]
	 * 
	 * If A is an invertible n x n matrix, then for each b in R^n, the equation Ax=b has the unique solution x=A^(-1) b.
	 * 
	 * [Theorem 2.6]
	 * 
	 * a. If A is an invertible matrix, then A^(-1) is invertible and
	 * 
	 * (A^(-1)) = A
	 * 
	 * b. If A and B are n x n invertible matrices, then so is AB, and the inverse of AB is the product of the inverses of A and B in the
	 * reverse order. That is,
	 * 
	 * (AB)^(-1) = B^(-1) A^(-1)
	 * 
	 * c. If A is an invertible matrix, then so is A^T, and the inverse of A^T is the transpose of A^(-1). That is,
	 * 
	 * (A^T)^(-1) = (A^(-1))^T
	 * 
	 * [Fact]
	 * 
	 * The product of n x n invertible matrices is invertible, and the inverse is the product of their inverses in the reverse order.
	 * 
	 * [Theorem 2.8 - The Invertible Matrix Theorem]
	 * 
	 * Let A be a square n x n matrix. Then the following statements are equivalent. That is, for a given A, the statements are either all
	 * true or all false.
	 * 
	 * a. A is an invertible matrix.
	 * 
	 * b. A is row equivalent to the n x n identity matrix.
	 * 
	 * c. A has n pivot positions.
	 * 
	 * d. The equation Ax=0 has only the trivial solution.
	 * 
	 * e. The columns of A form a linearly independent set.
	 * 
	 * f. The linear transformation x -> Ax is one-to-one.
	 * 
	 * g. The equation Ax=b has at least one solution for each b in R^n.
	 * 
	 * h. The columns of A span R^n.
	 * 
	 * i. The linear transformation x-> Ax maps R^n onto R^n.
	 * 
	 * j There is an n x n matrix C such that CA = I.
	 * 
	 * k. There is an n x n matrix D such that AD = I.
	 * 
	 * l. A^T is an invertible matrix.
	 * 
	 * [Fact]
	 * 
	 * Let A and B be square matrices. If AB = I, then A and B are both invertible, with B = A^(-1) and A = B^(-1).
	 * 
	 * 
	 * 
	 * 
	 * [Theorem 3.8 - An Inverse Formula]
	 * 
	 * Let A be an invertible n x n matrix. Then
	 * 
	 * A^-1 = (1 / det A) * adj A
	 * 
	 * 
	 * 
	 * @param a
	 *            input
	 * @param b
	 *            output
	 * @return
	 */
	public static void inverse(double[][] a, double[][] b) {
		int rowDim = a.length;
		int colDim = a[0].length;

		double[][] identity = ArrayUtils.identity(rowDim, 1);
		double[][] ai = joinColumns(a, identity);
		double[][] ia_inverse = ArrayUtils.copy(ai);

		GaussElimination(ia_inverse);

		for (int i = 0; i < rowDim; i++) {
			for (int j = 0; j < colDim; j++) {
				b[i][j] = ia_inverse[i][j + colDim];
			}
		}
	}

	public static double[][] joinColumns(double[] a, double[] b) {
		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}

		int rowDim = a.length;

		double[][] ret = new double[rowDim][2];

		for (int i = 0; i < a.length; i++) {
			ret[i][0] = a[i];
		}

		for (int i = 0; i < a.length; i++) {
			ret[i][1] = b[i];
		}

		return ret;

	}

	public static double[][] joinColumns(double[][] a, double[] b) {
		if (!ArrayChecker.isSameRowDim(a, b)) {
			throw new IllegalArgumentException();
		}

		int aRowDim = a.length;
		int aColDim = a[0].length;

		double[][] ret = new double[aRowDim][aColDim + 1];
		for (int i = 0; i < aRowDim; i++) {
			for (int j = 0; j < aColDim; j++) {
				ret[i][j] = a[i][j];
			}
			ret[i][aColDim] = b[i];
		}
		return ret;
	}

	/**
	 * 
	 * http://en.wikipedia.org/wiki/Augmented_matrix
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static double[][] joinColumns(double[][] a, double[][] b) {
		if (!ArrayChecker.isSameRowDim(a, b)) {
			throw new IllegalArgumentException();
		}

		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bRowDim = b.length;
		int bColDim = b[0].length;

		double[][] ret = new double[aRowDim][aColDim + bColDim];

		for (int i = 0; i < aRowDim; i++) {
			for (int j = 0; j < aColDim; j++) {
				ret[i][j] = a[i][j];
			}
		}

		for (int i = 0; i < bRowDim; i++) {
			for (int j = 0; j < bColDim; j++) {
				ret[i][aColDim + j] = b[i][j];
			}
		}

		return ret;
	}

	public static double leastSquareError(double[] a, double[] b) {
		// Vector X = leastSquaresSolution(B);
		// Vector AX = product(A, X);
		// return norm(B.subtract(AX));

		return 0;
	}

	public static Vector leastSquaresSolution(Matrix A, Vector B) {
		Vector X = null;

		// if (ArrayChecker.isLinearlyIndependent()) {
		// MatPair QR = QRdecomposition();
		// Matrix Q = QR.getFirst();
		// Matrix R = QR.getSecond();
		// Vector QtY = product(transpose(Q), B);
		// X = solve(R, QtY, false);
		// } else {
		// Matrix At = transpose(A);
		// Matrix AtA = product(At, A);
		// Vector AtY = product(At, B);
		// X = solve(AtA, AtY, false);
		// }
		return X;
	}

	public static Vector leastSquaresSolution(Vector B) {
		Vector X = null;
		// if (isLinearlyIndependent()) {
		// Pair<Matrix, Matrix> QR = QRdecomposition();
		// Matrix Q = QR.getFirst();
		// Matrix R = QR.getSecond();
		// Vector QtransB = Q.transpose().product(B);
		// X = R.solve(QtransB);
		// } else {
		// Matrix Atrans = transpose();
		// Matrix AtransA = Atrans.product(this);
		// Vector AtransB = Atrans.product(B);
		// X = AtransA.solve(AtransB);
		// }
		return X;
	}

	/**
	 * 
	 * * In linear algebra, the LU decomposition is a matrix decomposition which writes a matrix as the product of a lower and upper
	 * triangular matrix. The product sometimes includes a permutation matrix as well. This decomposition is used in numerical analysis to
	 * solve systems of linear equations or calculate the determinant.
	 * 
	 * 
	 * [LU factorization algorithm]
	 * 
	 * Decompose A into L(lower triangular) * U(upper triangular).
	 * 
	 * Algorithm: (p. 145)
	 * 
	 * 1. Reduce A to to an echelon form U by a sequence of row replacement operations.
	 * 
	 * 2. Place entries in L such that the same sequence of row operations reduces L to I.
	 * 
	 * [Reference]
	 * 
	 * 1. http://en.wikipedia.org/wiki/LU_decomposition
	 * 
	 * 
	 * @param a
	 *            input (M x N matrix)
	 * @param l
	 *            output (M x M matrix)
	 * @param u
	 *            output (M x N matrix)
	 */
	public static void LUFactorization(double[][] a, double[][] l, double[][] u) {
		int aRowDim = a.length;
		int aColDim = a[0].length;

		if (!ArrayChecker.isProductable(l, u, a)) {
			throw new IllegalArgumentException();
		}

		ArrayUtils.copy(a, u);

		int[] pivotColumns = new int[a.length];

		for (int i = 0; i < aRowDim; i++) {
			pivotColumns[i] = -1;
		}

		int numPivotColumns = 0;
		/*
		 * Row Echelon Form
		 */

		for (int i = 0, j = 0; i < aRowDim && j < aColDim; j++) {
			if (u[i][j] == 0) {

				/*
				 * Partial pivoting - selects as a pivot the entry in a column having the largest absolute value to reduce roundoff errors
				 * in the calculations.
				 */
				int max_abs_nonzero_i = -1;
				double max_abs_value = 0;

				for (int ii = i + 1; ii < aRowDim; ii++) {
					double value = u[ii][j];
					if (value != 0 && Math.abs(value) > Math.abs(max_abs_value)) {
						max_abs_nonzero_i = ii;
						max_abs_value = value;
					}
				}

				if (max_abs_nonzero_i > 0) {
					ArrayUtils.swapRows(a, i, max_abs_nonzero_i);
				}
			}

			if (u[i][j] != 0) {
				pivotColumns[i] = j;

				double lScaleFactor = 1f / u[i][j];
				l[i][numPivotColumns] = u[i][j] * lScaleFactor;

				for (int ii = i + 1; ii < aRowDim; ii++) {
					System.out.println(ArrayUtils.toString(l));
					System.out.println();

					l[ii][numPivotColumns] = u[ii][j] * lScaleFactor;

					System.out.println(ArrayUtils.toString(l));
					System.out.println();

					double scaleFactor = u[ii][j] / u[i][j];

					if (scaleFactor != 0) {
						for (int jj = 0; jj < aColDim; jj++) {
							u[ii][jj] = u[ii][jj] - scaleFactor * u[i][jj];
						}
					}
				}
				i++;
				numPivotColumns++;
			}
		}
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		{
			double[][] a = { { 1, 3 }, { -5, 0 } };
			double[][] b = { { 1, 0 }, { 0, 1 } };

			System.out.println(ArrayUtils.toString(joinColumns(a, b)));
		}

		{
			double[][] a = { { 1, 0, -5, 1 }, { 0, 1, 1, 4 }, { 0, 0, 0, 0 } };
			GaussElimination(a);
			System.out.println(ArrayUtils.toString(a));
		}

		{
			double[] a = { 2, -5 };
			double[] b = { 5, 1 };
			double[] c = { 0, 0 };

			projectionAonB(a, b, c);

			System.out.println(ArrayUtils.toString(b));
		}

		{
			double[][] a = { { 1, 2, -1 }, { 0, -5, 3 } };
			double[][] b = { { 4 }, { 3 }, { 7 } };
			double[][] c = new double[a.length][b[0].length];
			product(a, b, c);
			System.out.println(ArrayUtils.toString(c));
		}

		{
			double[][] a = { { 0, -1 }, { 1, 0 } };
			double[] b = { 4, 1 };
			double[] c = new double[b.length];

			product(a, b, c);

			System.out.println(ArrayUtils.toString(c));
		}

		{
			double[][] a = { { 2, 5 }, { -3, -7 } };
			double[][] b = { { -7, -5 }, { 3, 2 } };
			double[][] c = new double[2][2];

			product(a, b, c);

			System.out.println(ArrayUtils.toString(c));
		}

		{
			double[][] a = { { 0, 1, 2 }, { 1, 0, 3 }, { 4, -3, 8 } };
			double[][] b = new double[a.length][a[0].length];

			inverse(a, b);

			double[][] c = ArrayUtils.copy(a);

			product(a, b, c);

			System.out.println(ArrayUtils.toString(c));
		}

		{
			double[][] a = { { 2, 4, -1, 5, -2 }, { -4, -5, 3, -8, 1 }, { 2, -5, -4, 1, 8 }, { -6, 0, 7, -3, 1 } };
			double[][] l = new double[a.length][a.length];
			double[][] u = new double[a.length][a[0].length];
			double[][] b = new double[a.length][a[0].length];

			System.out.println(ArrayUtils.toString(a));

			System.out.println(ArrayUtils.toString(l));

			System.out.println(ArrayUtils.toString(u));

			LUFactorization(a, l, u);

			product(l, u, b);

			System.out.println(ArrayUtils.toString(a));

			System.out.println(ArrayUtils.toString(b));
		}

		{
			double[][] a = { { 2, 5, -3, -4, 8 }, { 4, 7, -4, -3, 9 }, { 6, 9, -5, 2, 4 }, { 0, -9, 6, 5, -6 } };

			System.out.println(rank(a, false));
		}

		{
			double[][] u = { { 4, -1, 2, 3 }, { 0, -2, 7, -4 }, { 0, 0, 6, 5 }, { 0, 0, 0, 3 } };
			double[] y = { 20, -7, 4, 6 };
			double[] x = new double[y.length];
			double[] ans_x = new double[] { 3, -4, -1, 2 };

			backSubstitution(u, x, y);

			System.out.println(ArrayUtils.toString(x));
		}

		{
			double[][] u = { { 4, -1, 2, 3 }, { 0, -2, 7, -4 }, { 0, 0, 6, 5 }, { 0, 0, 0, 7 } };
			double[] y = { 20, -7, 4, 6 };
			double[] x = new double[y.length];
			double[] ans_x = new double[] { 4.7857, 1.6190, -0.0476, 0.8571 };

			backSubstitution(u, x, y);

			double[] temp_y = new double[y.length];

			product(u, x, temp_y);

			System.out.println(ArrayUtils.toString(temp_y));
		}

		{
			double[][] l = { { 3, 0, 0, 0 }, { -1, 1, 0, 0 }, { 3, -2, -1, 0 }, { 1, -2, 6, 2 } };
			double[] y = { 5, 6, 4, 2 };
			double[] x = new double[y.length];

			forwardSubstitution(l, x, y);

			double[] temp_y = new double[y.length];

			product(l, x, temp_y);

			System.out.println(ArrayUtils.toString(temp_y));
		}

		// {
		// double[][] a = { { 1, 5, 0 }, { 2, 4, -1 }, { 0, -2, 0 } };
		//
		// System.out.println(determinant(a));
		// }

		{
			double[][] a =

			{ { 3, -7, 8, 9, -6 }

					, { 0, 2, -5, 7, 3 }

					, { 0, 0, 1, 5, 0 }

					, { 0, 0, 2, 4, -1 }

					, { 0, 0, 0, -2, 0 }

			};

			// System.out.println(determinant(a));
		}

		{
			double[][] a = { { -2, 2, -3 }, { -1, 1, 3 }, { 2, 0, -1 } };
			double[][] temp = new double[2][2];

			System.out.println(ArrayUtils.toString(a));

			System.out.println(determinantByCofactor(a, true, temp));
		}

		{
			double[][] a = { { 3, 2 }, { 1, 2 } };
			double[][] b = new double[a.length][a[0].length];
			GramSchmidtProcess(a, b);

			System.out.println(ArrayUtils.toString(b));
		}

		{
			double[][] a = { { 12, -51, 4 }, { 6, 167, -68 }, { -4, 24, -41 } };
			double[][] r = new double[3][3];
			double[][] q = new double[3][3];

			QRdecomposition(a, q, r);

			System.out.println(ArrayUtils.toString(q));
			System.out.println(ArrayUtils.toString(r));

			double[][] b = new double[3][3];

			LA.product(q, r, b);

			System.out.println(ArrayUtils.toString(b));
		}

		{
			double[][] a = { { 2, 0, 1 }, { 0, 3, 0 }, { 1, 0, 2 } };
			double[][] b = new double[3][3];

			QRalgorithm(a, b);

			System.out.println(ArrayUtils.toString(b));
		}

		System.out.println("process ends.");
	}

	/**
	 * 
	 * Return the determinant of a minor matrix which is a matrix excluding ith row and jth column. the value of ith row and jth column is
	 * called as cofactor of aij.
	 * 
	 * 
	 * @param A
	 * @param i
	 * @param j
	 * @param b
	 *            Reusable matrix for computing minor matrix which is b[aRowDim-1][aColDim-1]
	 * 
	 * @return
	 */
	public static double minor(double[][] a, int i, int j, double[][] b) {
		minorMatrix(a, i, j, b);
		return determinant(b);
	}

	/**
	 * 
	 * Return a minor matrix that is a sub-matrix excludes ith row and jth column from a matrix.
	 * 
	 * @param a
	 * @param i
	 * @param j
	 * @param b
	 *            Reusable matrix for computing minor matrix which is b[aRowDim-1][aColDim-1]
	 */
	public static void minorMatrix(double[][] a, int i, int j, double[][] b) {

		int aRowDim = a.length;
		int aColDim = a[0].length;

		int bRowDim = b.length;
		int BColDim = b[0].length;

		if (aRowDim - 1 == bRowDim && aColDim - 1 == BColDim) {

		} else {
			throw new IllegalArgumentException();
		}

		for (int ii = 0, m = 0; ii < aRowDim; ii++) {
			if (ii == i) {
				continue;
			}
			for (int jj = 0, n = 0; jj < aColDim; jj++) {
				if (jj == j) {
					continue;
				}
				b[m][n] = a[ii][jj];
				n++;
			}
			m++;
		}
	}

	public static Matrix nullBasis(double[][] a) {
		// Matrix pvf = SparseMatrix.newIdentity(A.colDim());
		//
		//
		//
		// Matrix AB = new SparseMatrix(A.rowDim(), A.colDim() + 1, 0);
		// for (int i : A.rowIndex()) {
		// for (int j : A.colIndex(i)) {
		// AB.set(i, j, A.get(i, j));
		// }
		// }
		//
		// Matrix U = GaussElimination(AB);

		return null;
	}

	/**
	 * 
	 * Component of y orthogonal to u.
	 * 
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void orthogonalToProjection(double[] a, double[] b, double[] c) {
		projectionAonB(a, b, c);
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] - c[i];
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void orthogonalToProjection(double[][] a, double[] b, double[] c) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Matrix invalid orthogonalToProjection");
		}

		projection(a, b, c);
		ArrayMath.substract(b, c, c);
	}

	public static void outerProduct(double[] a, double[] b, double[][] c) {
		int rowDim = a.length;
		int colDim = b.length;
		int[] dims = ArrayUtils.dimensions(c);

		if (rowDim == dims[0] && colDim == dims[1]) {

		} else {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < b.length; j++) {
				c[i][j] = a[i] * b[j];
			}
		}

	}

	/**
	 * @param a
	 *            input
	 * @param k
	 * @param b
	 *            output
	 */
	public static void power(double[][] a, int k, double[][] b) {
		if (!ArrayChecker.isSquare(a) || !ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException("Invalid matrix power");
		}

		ArrayUtils.copy(a, b);

		if (ArrayChecker.isDiagonal(a)) {
			int dim = a.length;
			for (int i = 0; i < dim; i++) {
				double value = a[i][i];
				b[i][i] = Math.pow(value, k);
			}
		} else {
			for (int i = 0; i < k; i++) {
				product(b, b, b);
			}
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void product(double[] a, double[][] b, double[] c) {
		double[][] aa = new double[1][];
		aa[0] = a;

		double[][] cc = new double[1][];
		cc[0] = c;

		product(aa, b, cc);
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 * @return
	 */
	public static double product(double[][] a, double[] b, double[] c) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			c[i] = dotProduct(a[i], b);
			sum += c[i];
		}
		return sum;
	}

	/**
	 * 
	 * /** [Theorem 2.1]
	 * 
	 * Let A, B, and C be matrices of the same size, and let r and s be scalars.
	 * 
	 * a. A + B = B + A
	 * 
	 * b. (A + B) + C = A + (B + C)
	 * 
	 * c. A + 0 = A
	 * 
	 * d. r(A + B) = rA + rB
	 * 
	 * e. (r + s)A = rA + sA
	 * 
	 * f. r(sA) = (rs)A
	 * 
	 * [Fact]
	 * 
	 * Each columns of AB is a linear combination of the columns of A using weights from the corresponding column of B.
	 * 
	 * [Row-Column Rule for Computing AB]
	 * 
	 * If the product AB is defined, then the entry in row i and column j of AB is the sum of the products of corresponding entries from row
	 * i of A and column j of B. If (AB)_ij denotes the (i,j)-entry in AB, and if A is an m x n matrix, then
	 * 
	 * (AB)_ij = a_i1b_1j + a_i2b_2j + ... + a_inb_nj
	 * 
	 * [Theorem 2.2]
	 * 
	 * Let A be an m x n matrix, let B and C have sizes for which indicated sums and products are defined.
	 * 
	 * a. A(BC) = (AB)C (associative law of multiplication)
	 * 
	 * b. A(B+C) = AB + AC (left distributive law)
	 * 
	 * c. (B+C)A = BA + CA (right distributive law)
	 * 
	 * d. r(AB) = (rA)B = A(rB) for any scalar r
	 * 
	 * e. I_mA = A = AI_n (identity for matrix multiplication)
	 * 
	 * 
	 * http://introcs.cs.princeton.edu/java/95linear/
	 * 
	 * @param a
	 * @param b
	 * @param c
	 *            output
	 */
	public static void product(double[][] a, double[][] b, double[][] c) {
		if (!ArrayChecker.isProductable(a, b, c)) {
			throw new IllegalArgumentException();
		}

		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bRowDim = b.length;
		int bColDim = b[0].length;

		double[] bc = new double[bRowDim]; // column j of B

		for (int j = 0; j < bColDim; j++) {
			ArrayUtils.copyColumn(b, j, bc);
			for (int i = 0; i < aRowDim; i++) {
				c[i][j] = dotProduct(a[i], bc);
			}
		}
	}

	public static double[][] product(double[][] a, double[][] b) {
		if (!ArrayChecker.isProductable(a, b)) {
			throw new IllegalArgumentException();
		}

		int[] dimA = ArrayUtils.dimensions(a);
		int[] dimB = ArrayUtils.dimensions(a);
		double[][] c = new double[dimA[0]][dimB[1]];
		product(a, b, c);
		return c;

	}

	/**
	 * 
	 * [Theorem 6.8 - The Orthogonal Decomposition Theorem]
	 * 
	 * Let W be a subspace of R^n. Then each y in R^n can be written uniquely in the form
	 * 
	 * y = y' + z
	 * 
	 * where y' is in W and z is in orthogonal to W. In fact, if {u1,...,up} is any orthogonal basis of W, then
	 * 
	 * y' = (InnerProduct(y,u1)/InnerProduct(u1,u1) * u1) + ... + (InnerProduct(y,up)/InnerProduct(up,up)*up)
	 * 
	 * and z = y - y'.
	 * 
	 * [Theorem 6.10]
	 * 
	 * If {u1,...,up} is an orthonormal basis for a subspace W of R^n, then
	 * 
	 * projection of y onto W = InnerProduct(y,u1) * u1 + ... + InnerProduct(y,up) *up
	 * 
	 * If U = {u1, u2, ..., up}, then
	 * 
	 * projection of y onto W = UU^T*y
	 * 
	 * 
	 * 
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void projection(double[][] a, double[] b, double[] c) {
		if (!ArrayChecker.isProductable(a, b, c)) {
			throw new IllegalArgumentException();
		}

		int rowDim = a.length;
		int colDim = a[0].length;

		double[] u_j = new double[rowDim];
		double[] projected = new double[rowDim];

		for (int j = 0; j < colDim; j++) {
			ArrayUtils.copyColumn(a, j, u_j);
			projectionAonB(b, u_j, projected);
			ArrayMath.add(projected, c, c);
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 * 
	 */
	public static void projectionAonB(double[] a, double[] b, double[] c) {
		if (ArrayChecker.isSameDim(a, b) && ArrayChecker.isSameDim(b, c)) {

		} else {
			throw new IllegalArgumentException();
		}

		double numerator = 0;
		double denominator = 0;

		for (int i = 0; i < a.length; i++) {
			numerator += a[i] * b[i];
			denominator += b[i] * b[i];
		}

		double scalar = 0;
		if (numerator != 0 && denominator != 0) {
			scalar = numerator / denominator;
		}

		ArrayMath.scale(b, scalar, c);
	}

	/**
	 * In numerical linear algebra, the QR algorithm is an eigenvalue algorithm; that is, a procedure to calculate the eigenvalues and
	 * eigenvectors of a matrix. The QR transformation was developed in 1961 by John G.F. Francis (England) and by Vera N. Kublanovskaya
	 * (USSR), working independently.[1] The basic idea is to perform a QR decomposition, writing the matrix as a product of an orthogonal
	 * matrix and an upper triangular matrix, multiply the factors in the other order, and iterate.
	 * 
	 * 
	 * [References]
	 * 
	 * 1. http://en.wikipedia.org/wiki/QR_algorithm
	 * 
	 * 2. http://math.fullerton.edu/mathews/n2003/QRMethodMod.html
	 * 
	 * 
	 * @param a
	 *            input
	 * @param q
	 *            output
	 * @param r
	 *            output
	 * 
	 * @return
	 */
	public static void QRalgorithm(double[][] a, double[][] b) {
		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}

		int aRowDim = a.length;
		int aColDim = a[0].length;

		ArrayUtils.copy(a, b);

		double[][] q = new double[aRowDim][aColDim];
		double[][] r = new double[aColDim][aColDim];

		for (int i = 0; i < 2000; i++) {
			QRdecomposition(b, q, r);
			product(r, q, b);
		}
	}

	/**
	 * 
	 * [Theorem 6.12 - The QR Factorization]
	 * 
	 * If A is an m x n matrix with linearly independent columns, then A can be factored as A = QR, where Q is an m x n matrix whose columns
	 * form an orthonormal basis for Col A and R is an n x n upper triangular invertible matrix with positive entries on its diagonal.
	 * 
	 * http://en.wikipedia.org/wiki/QR_decomposition
	 * 
	 * @param a
	 *            input - m x n matrix
	 * @param q
	 *            output - m x n matrix
	 * @param r
	 *            output - n x n matrix
	 * @return
	 */
	public static void QRdecomposition(double[][] a, double[][] q, double[][] r) {
		int rowDim = a.length;
		int colDim = a[0].length;

		if (!ArrayChecker.isProductable(q, r, a)) {
			throw new IllegalArgumentException();
		}

		colOrthonormalBasis(a, q);

		product(transpose(q), a, r);
	}

	public static int rank(double[][] a, boolean useQRalgorithm) {
		int ret = 0;
		if (useQRalgorithm) {
			// Pair<Matrix, Matrix> PD = diagonalization();
			// Matrix D = PD.getSecond();
			// Vector lambdaVec = D.diagonalVector();
			// ret = lambdaVec.nonZeroSize();
		} else {
			GaussElimination(a);

			int numColumnPivots = 0;

			for (int i = 0; i < a.length; i++) {
				for (int j = i; j < a[i].length; j++) {
					if (a[i][j] != 0) {
						ret++;
						break;
					}
				}
			}

		}
		return ret;
	}

	/**
	 * [Using Row Reduction to Solve Linear System]
	 * 
	 * 1. write the augmented matrix of the system.
	 * 
	 * 2. Use the row reduction algorithm to obtain an equivalent augmented matrix in echelon form. Decide whether the system is consistent.
	 * If there is no solution, stop; otherwise, go to the next step.
	 * 
	 * 3. Continue row reduction to obtain the reduced echelon form.
	 * 
	 * 4. Write the system of equations corresponding to the matrix obtained in step 3.
	 * 
	 * 5. Rewrite each nonzero equation from step 4 so that its one basic variable is expressed in terms of any free variables appearing in
	 * the equation.
	 * 
	 * [Definition]
	 * 
	 * If A is an m x n matrix, with columns a_1,...,a_n and if x is in R^n, then the product of A and x, denoted by Ax, is the linear
	 * combination of the columns of A using the corresponding entries in x as weights; that is,
	 * 
	 * Ax = [a_1 a_2 ... a_n][x_1, ...x_n]^T = x_1a_1 + x_2a_2 + ... + x_na_n
	 * 
	 * [Theorem 1.3]
	 * 
	 * If A is an m x n matrix, with columns a_1,...,a_n, and if b is R^m, the matrix equation,
	 * 
	 * Ax = b
	 * 
	 * has the same solution set as the vector equation
	 * 
	 * x_1a_1 + ... + x_na_n = b
	 * 
	 * which, in turn, has the same solution set as the system of linear equations whose augmented matrix is
	 * 
	 * [a_1 a_2 ... a_n b]
	 * 
	 * [Theorem 1.4]
	 * 
	 * Let A be an m x n matrix. Then the following statements are logically equivalent. That is, for a particular A, either they are all
	 * true statements or they are all false.
	 * 
	 * a. For each b in R^m, the equation Ax=b has a solution.
	 * 
	 * b. Each b in R^m is a linear combination of the columns of A.
	 * 
	 * c. The columns of A span(generate) R^m.
	 * 
	 * d. A has a pivot position in every row.
	 * 
	 * [Concept]
	 * 
	 * A system of linear equations is said to be homogeneous if it can be written in the form Ax=0, where A is an m x n matrix and 0 is the
	 * zero vector in R^m. Such a system Ax=0 always has a least one solution, namely x=0. This zero solution is usually called the trivial
	 * solution. For a given equation Ax=0, the important question is whethter there exists a nontrivial solution, that is a nonzero vector
	 * x that satisfies Ax=0. The Existence and Uniqueness Theorem leads immediately to the the following fact.
	 * 
	 * [Fact]
	 * 
	 * The homogeneous equation Ax=0 has a nontrivial solution iff the equation has at least one free variable.
	 * 
	 * [Theorem 1.6]
	 * 
	 * Suppose the equation Ax=b is consistent for some given b, and let p be a solution. Then the solution set of Ax=b is the set of all
	 * vectors of the form w = p + v_h, where v_h is any solution of the homogeneous equation Ax=0.
	 * 
	 * [Writing a Solution Set of Consistent System) in Parametric Vector Form]
	 * 
	 * 1. Row reduce the augmented matrix to reduced echelon form.
	 * 
	 * 2. Express each basic variable in terms of any free variables appearing in an equation.
	 * 
	 * 3. Write a typical solution x as a vector whose entries depend on the free variables, if any.
	 * 
	 * 4. Decompose x into a linear combination of vectors (with numeric entries) using the free variables as parameters.
	 * 
	 * 
	 * @param a
	 * @param b
	 * @param improve
	 * @return
	 */
	public static void solve(double[][] a, double[] x, double[] b, boolean improve) {
		if (!ArrayChecker.isProductable(a, x, b)) {
			throw new IllegalArgumentException("Invalid matrix solve");
		}

		int rowDim = a.length;
		int colDim = a[0].length;

		// LU decomposition
		if (ArrayChecker.isSquare(a)) {
			// if (isSingular(A)) {
			//
			// } else {

			double[][] l = new double[rowDim][rowDim];
			double[][] u = new double[rowDim][colDim];

			LUFactorization(a, l, u);

			forwardSubstitution(l, x, b);
			backSubstitution(u, x, b);

			if (improve) {
				// X = improve(a, X, permutedB);
			}
			// }

			// SVD
		} else if (rowDim < colDim) {

			// least-square solution
		} else if (rowDim > colDim) {

		}

	}

	/**
	 * 
	 * 
	 * [Theorem 3.7 - Cramer's Rule]
	 * 
	 * Let A be an invertible n x n matrix. For any b in R^n, the unique solution x of Ax=b has entries given by
	 * 
	 * 
	 * xi = (det Ai(b) / det A) , i =1,2,...,n
	 * 
	 * 
	 * @param a
	 *            input
	 * @param b
	 *            input
	 * @param c
	 *            output
	 */
	public static void solveByCramerRule(double[][] a, double[] b, double[] c) {
		if (!ArrayChecker.isProductable(a, b, c) || !ArrayChecker.isInvertible(a)) {
			throw new IllegalArgumentException("invalid solve");
		}

		int aRowDim = a.length;
		int aColDim = a[0].length;

		double[][] temp = new double[aRowDim - 1][aColDim - 1];

		double determinant = determinantByCofactor(a, false, temp);

		double[] column = new double[aRowDim];
		double[][] aa = new double[aRowDim][aColDim];

		for (int j = 0; j < aColDim; j++) {
			ArrayUtils.copy(a, aa);
			ArrayUtils.copyColumn(b, aa, j);
			double Xj = determinant(aa) / determinant;
			c[j] = Xj;
		}
	}

	/**
	 * 
	 * A quadratic form on R^n is a function Q defined on R^n whose value at a vector x in R^n can be computed by an expression of the form
	 * Q(x) = x^T A x, where A is an n x n symmetric matrix. The matrix A is called the matrix of the quadratic form.
	 * 
	 * [Definition]
	 * 
	 * A quadratic form Q is:
	 * 
	 * a. positive definite if Q(x) > 0 for all x != 0,
	 * 
	 * b. negative definite if Q(x) < 0 for all x != 0,
	 * 
	 * c. indefinite if Q(x) assumes both positive and negative values.
	 * 
	 * [Theorem 7.5 - Quadratic Forms and Eigenvalues]
	 * 
	 * Let A be an n x n symmetric matrix. Then a quadratic form x^T A x is:
	 * 
	 * a. positive definite iff the eigenvalues of A are all positive.
	 * 
	 * b. negative definite iff the eigenvalues of A are all negative, or
	 * 
	 * c. indefinite iff A has both positive and negative eigenvalues.
	 * 
	 * 
	 * @param A
	 * @param B
	 * @return
	 */
	public static double solveQuadraticForm(double[][] a, double[] b) {
		double[] c = new double[b.length];
		product(a, b, c);
		return dotProduct(c, c);
	}

	public static double solveQuadraticFormByChangeOfVariable(Matrix A, Vector X) {
		// Pair<Matrix, Matrix> PD = diagonalization();
		// Matrix P = PD.getFirst();
		// Matrix D = PD.getSecond();
		// Vector Y = product(transpose(P), X);
		// Vector DY = product(D, Y);
		// return innerProduct(Y, DY);

		return 0;
	}

	public static double trace(double[][] x) {
		int min = Math.min(x.length, x[0].length);
		double ret = 0;
		for (int i = 0; i < min; i++) {
			ret += x[i][i];
		}
		return ret;
	}

	public static double[][] transpose(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;
		double[][] b = new double[colDim][rowDim];
		transpose(a, b);
		return b;
	}

	/**
	 * 
	 * Let A and B denotes matrices whose sizes are appropriate for the following sums and products.
	 * 
	 * a. (A^T)^T = A
	 * 
	 * b. (A+B)^T = A^T + B^T
	 * 
	 * c. For any scalar, (rA)^T = rA^T
	 * 
	 * d. (AB)^T = B^T * A^T
	 * 
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void transpose(double[][] a, double[][] b) {
		if (!ArrayChecker.isTransposable(a, b)) {
			throw new IllegalArgumentException();
		}

		int aRowDim = a.length;
		int aColDim = a[0].length;

		for (int i = 0; i < aRowDim; i++) {
			for (int j = 0; j < aColDim; j++) {
				b[j][i] = a[i][j];
			}
		}
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 */
	public static void unitColumnVectors(double[][] a, double[][] b) {
		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}

		int aRowDim = a.length;
		int bColDim = b.length;

		double[] column = new double[aRowDim];

		for (int j = 0; j < bColDim; j++) {
			ArrayUtils.copyColumn(a, j, column);
			ArrayMath.unitVector(column, column);
			ArrayUtils.copyColumn(column, a, j);
		}
	}

	/**
	 * @param a
	 *            Reduced Row Echelon Form using GaussElimination
	 * @return
	 */
	public static VariableType[] variableTypes(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;
		VariableType[] ret = new VariableType[colDim];

		for (int j = 0; j < ret.length; j++) {
			ret[j] = VariableType.Free;
		}
		for (int i = 0; i < rowDim; i++) {
			for (int j = 0; j < colDim; j++) {
				if (a[i][j] == 1) {
					ret[j] = VariableType.Basic;
					break;
				}
			}
		}

		return ret;
	}

	public double[][] colBasis(double[][] a) {
		double[][] b = ArrayUtils.copy(a);
		int[] pivotColumns = GaussElimination(b);

		double[][] c = new double[a.length][pivotColumns.length];

		for (int i = 0; i < pivotColumns.length; i++) {
			int column = pivotColumns[i];
			ArrayUtils.copyColumn(b, column, c, i);
		}

		return c;
	}

	public int dimCol(double[][] a) {
		// double[] b = colBasis(a);
		// return colBasis().colDim();

		return 0;
	}

	public int dimNul() {
		// Vector B = new SparseVector(rowDim(), 0);
		// Matrix p = augmented(B).REF();
		// int numVariables = B.dim();
		// int numBasicVariables = p.pivotIndex().size();
		// int numFreeVariables = numVariables - numBasicVariables;
		// return numFreeVariables;

		return 0;
	}

	public int dimRow() {
		return rowBasis().rowDim();
	}

	/**
	 * @param a
	 *            input
	 * @param b
	 *            output
	 * @return
	 */
	public void eigenValues(double[][] a, double[][] b) {

		if (!ArrayChecker.isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}

		int rowDim = a.length;
		int colDim = a[0].length;

		Matrix ret = null;
		if (ArrayChecker.isTriangular(a)) {
			for (int i = 0; i < rowDim; i++) {
				b[i][i] = a[i][i];
			}
		} else {
			QRalgorithm(a, b);

			int aRowDim = a.length;
			int aColDim = a[0].length;

			for (int i = 0; i < aRowDim; i++) {
				for (int j = 0; j < aColDim; j++) {
					double value = b[i][j];

					if (i == j) {
						b[i][j] = Math.rint(b[i][j]);
					} else {
						b[i][j] = 0;
					}
				}
			}
		}
	}

	public Matrix eigenVectors(Matrix A, Matrix eigenValues, boolean isSymmetric) {
		// Vector lambdas = eigenValues.diagonalVector();
		// Map<Integer, Matrix> lambdaMap = new HashMap<Integer, Matrix>();
		// Indexer<Double> lambdaIndexer = new Indexer<Double>();
		//
		// for (int k = 0; k < lambdas.dim(); k++) {
		// double lambda = lambdas.get(k);
		// int lambdaIdx = lambdaIndexer.add(lambda);
		//
		// if (!lambdaMap.containsKey(lambdaIdx)) {
		// Matrix diagonal = newDiagonalMatrix(A.rowDim(), lambda);
		// Matrix C = A.subtract(diagonal);
		// Vector B = new SparseVector(A.rowDim(), 0);
		// Matrix nullBasis = nullBasis(C, B);
		// if (isSymmetric && nullBasis.colDim() > 1) {
		// nullBasis = GramSchmidtProcess(nullBasis);
		// }
		// lambdaMap.put(lambdaIdx, nullBasis);
		// }
		// }
		//
		// Matrix eigenVectors = new SparseMatrix(eigenValues.rowDim(),
		// eigenValues.colDim(), 0);
		//
		// int k = 0;
		// for (double lambda : lambdaIndexer.objects()) {
		// int lambdaIdx = lambdaIndexer.indexOf(lambda);
		// Matrix nullBasis = lambdaMap.get(lambdaIdx);
		// for (int j = 0; j < nullBasis.colDim(); j++) {
		// eigenVectors.setColVector(k++, nullBasis.colVector(j));
		// }
		// if (eigenVectors.colDim() == k) {
		// break;
		// }
		// }
		//
		// return eigenVectors;

		return null;
	}

	public void quadraticConstrainedOptimization() {
		// if (!isSymmetric()) {
		// throw new
		// IllegalArgumentException("Invalid matrix constrainedOptimization");
		// }
		// Pair<Matrix, Matrix> PD = diagonalization();
		// Matrix P = PD.getFirst();
		// Matrix D = PD.getSecond();
		// Counter<Integer> lambdaCounter = D.diagonalVector().asCounter();
		// int minLambdaIdx = lambdaCounter.argMin();
		// int maxLambdaIdx = lambdaCounter.argMax();
		// double minLambda = lambdaCounter.getCount(minLambdaIdx);
		// double maxLambda = lambdaCounter.getCount(maxLambdaIdx);
		// return new Pair<Double, Double>(minLambda, maxLambda);
	}

	public void quadraticFormType() {
		// Matrix eigenValues = eigenValues();
		// Vector lambdaVec = eigenValues.diagonalVector();
		// int numPositives = 0;
		// int numNegatives = 0;
		//
		// for (int i = 0; i < lambdaVec.get(i); i++) {
		// double lambda = lambdaVec.get(i);
		// if (lambda > 0) {
		// numPositives++;
		// } else if (lambda < 0) {
		// numNegatives++;
		// }
		// }
		//
		// QuadraticFormType ret;
		// if (numPositives > 0 && numNegatives == 0) {
		// ret = QuadraticFormType.POSITIVE_DEFINITE;
		// } else if (numNegatives > 0 && numPositives == 0) {
		// ret = QuadraticFormType.NEGATIVE_DEFINITE;
		// } else {
		// ret = QuadraticFormType.INDEFINITE;
		// }
	}

	public Matrix rowBasis() {
		// Matrix p = REF();
		// List<IntPair> pivotIndexes = p.pivotIndex();
		//
		// Matrix ret = new SparseMatrix(pivotIndexes.size(), colDim(), 0);
		// int k = 0;
		// for (IntPair pair : pivotIndexes) {
		// int i = pair.getFirst();
		// ret.setRowVector(k++, p.rowVector(i));
		// }
		return null;
	}

	public void SpectralDecomposition() {
	}

	public void SVD() {
		// if (ArrayChecker.isSquare()) {
		// throw new IllegalArgumentException("Invalid matrix SVD");
		// }
		//
		// Matrix AtransA = transpose().product(this);
		// Pair<Matrix, Matrix> diagonalization = AtransA.diagonalization();
		// Matrix V = diagonalization.getFirst().unitVectors();
		// Matrix D = diagonalization.getSecond();
		// Vector lambdaVec = D.diagonalVector();
		//
		// Matrix SIGMA = new SparseMatrix(rowDim(), colDim(), 0);
		// Matrix tempV = new SparseMatrix(V.rowDim(), V.colDim(), 0);
		// PriorityQueue<Integer> pq = lambdaVec.asPriorityQueue();
		// List<Double> orderedSingularValues = new ArrayList<Double>();
		// int min = Math.min(rowDim(), colDim());
		// int aj = 0;
		//
		// while (pq.hasNext()) {
		// double lambda = pq.getPriority();
		// int j = pq.next();
		// double singularValue = Math.sqrt(lambda);
		// if (singularValue == 0) {
		// break;
		// }
		//
		// SIGMA.set(aj, aj, singularValue);
		// orderedSingularValues.add(singularValue);
		//
		// tempV.setColVector(aj, V.colVector(j));
		// aj++;
		// }
		// V = tempV;
		//
		// Matrix U = new SparseMatrix(rowDim(), rowDim(), 0);
		// int numNonZeroVecs = 0;
		// int rank = orderedSingularValues.size();
		//
		// for (int j = 0; j < rank; j++) {
		// double singularValue = orderedSingularValues.get(j);
		// Vector v = V.colVector(j);
		// Vector Av = product(v);
		// Vector u = Av.multiplyScalar(1 / singularValue);
		//
		// if (!u.isZeroVector()) {
		// U.setColVector(j, u);
		// numNonZeroVecs++;
		// }
		// }
		//
		// if (numNonZeroVecs != U.colDim()) {
		// Matrix A = new SparseMatrix(lambdaVec.size(), U.rowDim(), 0);
		//
		// for (int k = 0; k < rank; k++) {
		// A.setRowVector(k, U.colVector(k));
		// }
		//
		// Vector B = new SparseVector(lambdaVec.size(), 0);
		// Matrix nullBasis = A.nullBasis(B).GramSchmidtProcess();
		//
		// for (int bj = 0, cj = rank; bj < nullBasis.colDim() && cj <
		// U.colDim(); bj++, cj++) {
		// Vector w = nullBasis.colVector(bj).unitVector();
		// U.setColVector(cj, w);
		// }
		//
		// }
		//
		// return new Triple<Matrix, Matrix, Matrix>(U, SIGMA, V.transpose());
	}

}
