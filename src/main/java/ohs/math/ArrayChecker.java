package ohs.math;

import ohs.math.LA.SolutionType;
import ohs.math.LA.VariableType;

public class ArrayChecker {

	/**
	 * 
	 * A system of linear equations has either
	 * 
	 * 1. no solution, or
	 * 
	 * 2. exactly one solution, or
	 * 
	 * 3. infinitely many solutions.
	 * 
	 * A system of linear equations is said to be consistent if it has either one solution or infinitely many solutions.
	 * 
	 * A system is inconsistent if it has no solution.
	 * 
	 * 
	 * [ Theorem 1.2 - Existence and Uniqueness Theorem ]
	 * 
	 * A linear system is consistent iff the rightmost column of the augmented matrix is not a pivot column- that is, iff an echelon form of
	 * the augmented matrix has no row of the form
	 * 
	 * [0 ... 0 b] with nonzero
	 * 
	 * If a linear system is consistent, then the solution set contains either (i) a unique solution, when there are no free variables, or
	 * (ii) infinitely many solutions, when there is at least one free variables.
	 * 
	 * @param a
	 * @return
	 */
	public static SolutionType hasSolution(double[][] a) {
		SolutionType ret = SolutionType.No;

		LA.GaussElimination(a);

		if (isConsistent(a)) {
			VariableType[] types = LA.variableTypes(a);

			for (int i = 0; i < types.length; i++) {
				if (types[i] == VariableType.Free) {
					return SolutionType.Infinite;
				}
			}
			ret = SolutionType.Unique;
		}
		return ret;
	}

	/**
	 * 
	 * A system of linear equations has either
	 * 
	 * 1. no solution, or
	 * 
	 * 2. exactly one solution, or
	 * 
	 * 3. infinitely many solutions.
	 * 
	 * A system of linear equations is said to be consistent if it has either one solution or infinitely many solutions.
	 * 
	 * A system is inconsistent if it has no solution.
	 * 
	 * 
	 * [ Theorem 1.2 - Existence and Uniqueness Theorem ]
	 * 
	 * A linear system is consistent iff the rightmost column of the augmented matrix is not a pivot column- that is, iff an echelon form of
	 * the augmented matrix has no row of the form
	 * 
	 * [0 ... 0 b] with nonzero
	 * 
	 * If a linear system is consistent, then the solution set contains either (i) a unique solution, when there are no free variables, or
	 * (ii) infinitley many solutions, when there is at least one free variables.
	 * 
	 * 
	 * @param a
	 *            upper triangular matrix (augmented matrix)
	 * @return
	 */
	public static boolean isConsistent(double[][] a) {

		if (!isUpperTriangular(a)) {
			a = ArrayUtils.copy(a);
			LA.GaussElimination(a);
		}

		int rowDim = a.length;
		int colDim = a[0].length;
		for (int i = 0; i < rowDim; i++) {
			int numZeros = 0;
			for (int j = 0; j < colDim - 1; j++) {
				if (a[i][j] == 0) {
					numZeros++;
				}
			}
			if (numZeros == colDim - 1 && a[i][colDim - 1] != 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * 
	 * http://en.wikipedia.org/wiki/Diagonal_matrix
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isDiagonal(double[][] a) {
		if (isSquare(a)) {
			int rowDim = a.length;
			int colDim = a[0].length;

			for (int i = 0; i < rowDim; i++) {
				for (int j = 0; j < colDim; j++) {
					if (i != j && a[i][j] != 0) {
						return false;
					}
				}
			}
		} else {
			return false;
		}

		return true;
	}

	public static boolean isEqual(double[] a, double[] b) {
		if (!isSameDim(a, b)) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * http://en.wikipedia.org/wiki/Identity_matrix
	 * 
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isIdentity(double[][] a) {
		if (isSquare(a)) {
			int rowDim = a.length;
			int colDim = a[0].length;

			for (int i = 0; i < rowDim; i++) {
				for (int j = i + 1; j < colDim; j++) {
					if (a[i][i] == 1 && a[i][j] == 0 && a[j][i] == 0) {

					} else {
						return false;
					}
				}
			}
		} else {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * * [Theorem 2.16]
	 * 
	 * Let A be an n x n matrix. Then, the following statements are each equivalent to the statement that A is an invertible matrix.
	 * 
	 * m. The columns of A form a basis of R^n
	 * 
	 * n. Col A = R^n
	 * 
	 * o. dim Col A = n
	 * 
	 * p. rank A = n
	 * 
	 * Nul A = {0}
	 * 
	 * dim Nul A = 0
	 * 
	 * 
	 * [Theorem 3.4]
	 * 
	 * A square matrix A is invertible iff det A != 0.
	 * 
	 * 
	 * [Theorem - The Invertible Matrix Theorem]
	 * 
	 * Let A be an n x n matrix. Then A is invertible iff:
	 * 
	 * s. The number 0 is not an eigenvalue of A.
	 * 
	 * t. The determinant of A is not zero.
	 * 
	 * 
	 * [Theorem - The Invertible Matrix Theorem]
	 * 
	 * Let A be an n x n matrix. Then the following statements are each equivalent to the statement that A is an invertible matrix.
	 * 
	 * u. (Col A) orthogonal = {0}.
	 * 
	 * v. (Nul A) orthogonal = R^n.
	 * 
	 * w. Row A = R^n.
	 * 
	 * x. A has n nonzero singular values.
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isInvertible(double[][] a) {
		return (LA.determinant(a) == 0 ? false : true);
	}

	/**
	 * 
	 * [Definition]
	 * 
	 * An indexed set of vectors {v_1,..,v_p}, in R^n is said to be linearly independent if the vector equation
	 * 
	 * x_1v_1 + x_2v_2 + ... + x_pv_p = 0
	 * 
	 * has only trivial solution. The set {v_1,...,v_p} is said to be linearly dependent if there exist weights c_1,...,c_p, not all zero,
	 * such that
	 * 
	 * c_1v_1 + c_2v_2 + ... + c_pv_p = 0
	 * 
	 * 
	 * [Concept]
	 * 
	 * The columns of a matrix A are linearly independent iff the equation Ax = 0 has only the trivial solution.
	 * 
	 * [Concept]
	 * 
	 * A set of two vectors {v_1,v_2} is linearly dependent if at least one of the vectors is a multiple of the other. The set is linearly
	 * independent iff neither of the vectors is a multiple of the other.
	 * 
	 * 
	 * [Theorem 1.7 - Characterization of Linearly Dependent Sets]
	 * 
	 * An indexed set S = {v_1,...,v_p} of two or more vectors is linearly dependent iff at least one of the vectors in S is a linear
	 * combination of the others. In fact, if S is linearly dependent and v_1 != 0, then some v_j (with j > 1) is a linear combination of
	 * the preceding vectors, v_1,...,v_(j-1)
	 * 
	 * [Theorem 1.8]
	 * 
	 * If a set contains more vectors than there are entries in each vector, then the set is linearly dependent. That is, any set
	 * {v_1,...,v_p} in R3 is linearly dependent if p > n.
	 * 
	 * [Theorem 1.9]
	 * 
	 * If a set S = {v_1,...,v_p} in R^n contains the zero vector, then the set is linearly dependent.\
	 * 
	 * [Definition]
	 * 
	 * A transformation (or mapping) T is linear if:
	 * 
	 * (1) T(u+v) = T(u) + T(v) for all u, v in the domain of T;
	 * 
	 * (2) T(cu) = cT(u) for all u and all scalars c.
	 * 
	 * [Reference]
	 * 
	 * 1. http://en.wikipedia.org/wiki/Linear_independence
	 * 
	 * @param A
	 * @return
	 */
	public static boolean isLinearlyDependent(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;

		/*
		 * Determined by Theorem 1.8
		 */

		if (colDim > rowDim) {
			return true;
		}

		/*
		 * Determined by Theorem 1.9
		 */
		double[] column1 = new double[rowDim];

		for (int j = 0; j < colDim; j++) {
			ArrayUtils.copyColumn(a, j, column1);

			if (isZeroVector(column1)) {
				return true;
			}
		}

		ArrayUtils.setAll(column1, 0);

		double oldRatio = Double.NaN;

		double[] column2 = new double[rowDim];

		for (int j = 0; j < colDim; j++) {
			ArrayUtils.copyColumn(a, j, column1);

			for (int k = j + 1; k < colDim; k++) {
				ArrayUtils.copyColumn(a, k, column2);

				for (int i = 0; i < rowDim; i++) {
					double ratio = 0;

					if (column1[i] == column2[i]) {

					} else {
						if (column1[i] != 0) {
							ratio = column2[i] / column1[i];

							if (oldRatio == Double.NaN) {
								oldRatio = ratio;
							}

							if (oldRatio != ratio) {
								return false;
							}
						}
					}
				}
			}
		}

		return LA.determinant(a) == 0 ? false : true;
	}

	public static boolean isLinearlyIndependent(double[] a, double[] b) {
		return isLinearlyDependent(LA.joinColumns(a, b));
	}

	public static boolean isLowerTriangular(double[][] a) {
		if (isSquare(a)) {
			int rowDim = a.length;
			int colDim = a[0].length;

			for (int i = 0; i < rowDim; i++) {
				for (int j = i + 1; j < colDim; j++) {
					if (a[i][j] != 0) {
						return false;
					}
				}
			}
		} else {
			return false;
		}
		return true;
	}

	public static boolean isOrthogonal(double[] a, double[] b) {
		return (LA.dotProduct(a, b) == 0 ? true : false);
	}

	/**
	 * 
	 * 
	 * [Definition]
	 * 
	 * Two vectors u and v in R^n are orthogonal (to each other) if InnerProduct(u,v) = 0.
	 * 
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isOrthogonal(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;

		double[] column1 = new double[rowDim];
		double[] column2 = new double[rowDim];

		for (int j = 0; j < colDim; j++) {
			ArrayUtils.copyColumn(a, j, column1);

			for (int jj = j + 1; jj < colDim; jj++) {
				ArrayUtils.copyColumn(a, jj, column2);

				if (!isOrthogonal(column1, column2)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isOrthonormal(double[] a, double[] b) {
		if (LA.dotProduct(a, b) != 0 || LA.dotProduct(a, a) != 1 || LA.dotProduct(b, b) != 1) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * /** [Theorem 6.6]
	 * 
	 * An m x n matrix U has orthonomal columns iff U^T * U = I.
	 * 
	 * @param A
	 * @return
	 */
	public static boolean isOrthonormal(double[][] A) {
		int rowDim = A.length;
		int colDim = A[0].length;

		double[] column1 = new double[rowDim];
		double[] column2 = new double[rowDim];

		for (int j = 0; j < colDim; j++) {
			ArrayUtils.copyColumn(A, j, column1);

			if (LA.dotProduct(column1, column1) != 1) {
				return false;
			}

			for (int aj = j + 1; aj < colDim; aj++) {
				ArrayUtils.copyColumn(A, aj, column2);

				if (!isOrthogonal(column1, column2)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isProductable(double[][] a, double[] b) {
		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bDim = b.length;

		if (aColDim == bDim) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param a
	 *            M x K matrix
	 * @param b
	 *            K x N matrix
	 * @param c
	 *            M x N matrix
	 * @return
	 */
	public static boolean isProductable(double[][] a, double[] b, double[] c) {
		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bDim = b.length;
		int cDim = c.length;

		if (aColDim == bDim && bDim == cDim) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isProductable(double[][] a, double[][] b) {
		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bRowDim = b.length;
		int bColDim = b[0].length;

		if (aColDim == bRowDim) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isProductable(double[][] a, double[][] b, double[][] c) {
		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bRowDim = b.length;
		int bColDim = b[0].length;

		int cRowDim = c.length;
		int cColDim = c[0].length;

		if (cRowDim == aRowDim && cColDim == bColDim && aColDim == bRowDim) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * [Definition]
	 * 
	 * 1 . It is in row echelon form.
	 * 
	 * 2. Every leading coefficient is 1 and is the only nonzero entry in its column
	 * 
	 * http://en.wikipedia.org/wiki/Row_echelon_form
	 * 
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isReducedRowEchelonForm(double[][] a) {
		if (!isRowEchelonForm(a)) {
			return false;
		}

		int rowDim = a.length;
		int colDim = a[0].length;

		int[] leadingEntries = new int[a.length];

		for (int i = 0; i < leadingEntries.length; i++) {
			leadingEntries[i] = -1;
		}

		for (int i = 0; i < rowDim; i++) {
			for (int j = 0; j < colDim; j++) {
				if (a[i][j] != 0) {
					leadingEntries[i] = j;
					break;
				}
			}
		}

		for (int i = 0; i < rowDim; i++) {
			int j = leadingEntries[i];

			if (j == -1) {
				continue;
			}

			if (a[i][j] != 1) {
				return false;
			}

			for (int ii = 0; ii < rowDim; ii++) {
				if (ii == i) {
					continue;
				}

				if (a[ii][j] != 0) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 
	 * [Definition]
	 * 
	 * 1. All nonzero rows (rows with at least one nonzero element) are above any rows of all zeroes (all zero rows, if any, belong at the
	 * bottom of the matrix).
	 * 
	 * 2. The leading coefficient (the first nonzero number from the left, also called the pivot) of a nonzero row is always strictly to the
	 * right of the leading coefficient of the row above it (some texts add the condition that the leading coefficient must be 1.[1]).
	 * 
	 * 3. All entries in a column below a leading entry are zeroes (implied by the first two criteria)
	 * 
	 * 
	 * http://en.wikipedia.org/wiki/Row_echelon_form
	 * 
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isRowEchelonForm(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;

		int[] leadingEntries = new int[a.length];

		for (int i = 0; i < leadingEntries.length; i++) {
			leadingEntries[i] = -1;
		}

		for (int i = 0; i < rowDim; i++) {
			for (int j = 0; j < colDim; j++) {
				if (a[i][j] != 0) {
					leadingEntries[i] = j;
					break;
				}
			}
		}

		for (int i = 1; i < rowDim; i++) {
			int prev_j = leadingEntries[i - 1];
			int j = leadingEntries[i];

			if (prev_j == -1 && j != -1) {
				return false;
			}

			if (prev_j != -1 && j != -1 && prev_j >= j) {
				return false;
			}
		}

		for (int i = 0; i < rowDim; i++) {
			int j = leadingEntries[i];

			for (int ii = i + 1; ii < rowDim; ii++) {
				if (a[ii][j] != 0) {
					return false;
				}
			}
		}

		return true;
	}

	public static boolean isSameColumnDim(double[][] a, double[] b) {
		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bDim = b.length;

		if (aColDim == bDim) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isSameDim(double[] a, double[] b) {
		return a.length == b.length ? true : false;
	}

	public static boolean isSameDim(double[] a, double[] b, double[] c) {
		return isSameDim(a, b) && isSameDim(b, c) ? true : false;
	}

	public static boolean isSameDim(int[] a, double[] b) {
		return a.length == b.length ? true : false;
	}

	public static boolean isSameDim(int[] a, int[] b) {
		return a.length == b.length ? true : false;
	}

	public static boolean isSameDim(double[][] a, double[][] b) {
		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bRowDim = b.length;
		int bColDim = b[0].length;

		if (aRowDim == bRowDim && aColDim == bColDim) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isSameDim(double[][] a, double[][] b, double[][] c) {
		return isSameDim(a, b) && isSameDim(b, c) ? true : false;
	}

	public static boolean isSameRowDim(double[][] a, double[] b) {
		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bDim = b.length;

		if (aRowDim == bDim) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isSameRowDim(double[][] a, double[][] b) {
		int aRowDim = a.length;
		int bRowDim = b.length;

		if (aRowDim == bRowDim) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isSame(int a, int b) {
		return a == b ? true : false;
	}

	/**
	 * A matrix that is not invertible is sometimes called a singular matrix, and an invertible matrix is called a nonsingular matrix.
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isSingular(double[][] a) {
		return (!isInvertible(a) ? true : false);
	}

	// public boolean isDiagonalizable() {
	// if (isTriangular()) {
	// Vector eigenValues = eigenValues().diagonalVector();
	// Set<Double> eigenValueSet = new HashSet<Double>();
	// for (int i = 0; i < eigenValues.size(); i++) {
	// eigenValueSet.add(eigenValues.get(i));
	// }
	// return (eigenValues.dim() == eigenValueSet.size() ? true : false);
	// } else {
	// return isLinearlyIndependent();
	// }
	// }

	/**
	 * In mathematics, and in particular linear algebra, a skew-symmetric (or antisymmetric or antimetric[1]) matrix is a square matrix A
	 * whose transpose is also its negative; that is, it satisfies the condition -A = AT. If the entry in the i th row and j th column is
	 * aij, i.e. A = (aij) then the skew symmetric condition is aij = −aji. For example, the following matrix is skew-symmetric:
	 * 
	 * 
	 * http://en.wikipedia.org/wiki/Skew-symmetric_matrix
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isSkewSymmetric(double[][] a) {
		if (!isSquare(a)) {
			return false;
		}

		int dim = a.length;

		for (int i = 0; i < dim; i++) {
			for (int j = i + 1; j < dim; j++) {
				if (a[i][j] + a[j][i] != 0) {
					return false;
				}
			}
		}

		return true;

	}

	// public boolean isInNullSpace(Vector B) {
	// if (colDim() != B.dim()) {
	// throw new IllegalArgumentException("Invalid Matrix isNullSpace");
	// }
	// return product(B).isZeroVector();
	// }

	// public boolean isLinearlyIndependent() {
	// if (colDim() > rowDim()) {
	// return false;
	// }
	//
	// for (int j = 0; j < colDim(); j++) {
	// if (colVector(j).isZeroVector()) {
	// return false;
	// }
	// }
	//
	// Vector B = new SparseVector(rowDim(), 0);
	// Vector X = solve(B);
	// return X.isZeroVector();
	// }

	// public boolean isOneToOneMapping() {
	// return isLinearlyIndependent();
	// }

	public static boolean isSquare(double[][] a) {
		return (a.length == a[0].length ? true : false);
	}

	/**
	 * 
	 * 
	 * In linear algebra, a symmetric matrix is a square matrix, A, that is equal to its transpose
	 * 
	 * A = A^T.
	 * 
	 * [Theorem 7.1]
	 * 
	 * If A is symmetric, then any two eigenvectors from different eigenspaces are orthogonal.
	 * 
	 * [Reference]
	 * 
	 * 1. http://en.wikipedia.org/wiki/Symmetric_matrix
	 * 
	 * @param A
	 * @return
	 */
	public static boolean isSymmetric(double[][] a) {
		if (!isSquare(a)) {
			return false;
		}

		int rowDim = a.length;
		int colDim = a[0].length;

		for (int i = 0; i < rowDim; i++) {
			for (int j = i + 1; j < colDim; j++) {
				if (a[i][j] != a[j][i]) {
					return false;
				}
			}
		}

		return true;
	}

	public static boolean isTransposable(double[][] a, double[][] b) {
		int aRowDim = a.length;
		int aColDim = a[0].length;
		int bRowDim = b.length;
		int bColDim = b[0].length;

		if (aRowDim == bColDim && aColDim == bRowDim) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isTriangular(double[][] a) {
		return (isUpperTriangular(a) || isLowerTriangular(a) ? true : false);
	}

	public static boolean isUpperTriangular(double[][] a) {
		if (isSquare(a)) {
			int rowDim = a.length;
			int colDim = a[0].length;

			for (int i = 1; i < rowDim; i++) {
				for (int j = 0; j < i; j++) {
					if (a[i][j] != 0) {
						return false;
					}
				}
			}
		} else {
			return false;
		}

		return true;
	}

	public static boolean isValidProbs(double[] a) {
		boolean ret = true;
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			if (a[i] >= 0 && a[i] <= 1) {

			} else {
				return false;
			}
			sum = a[i];
		}

		if (sum != 1) {
			return false;
		}

		return true;
	}

	public static boolean isValidRealNumbers(double[] a) {
		for (int i = 0; i < a.length; i++) {
			if (Double.isNaN(a[i]) || Double.isInfinite(a[i]) || Double.POSITIVE_INFINITY == a[i] || Double.NEGATIVE_INFINITY == a[i]) {
				throw new IllegalArgumentException(String.format("invalid value [%d] at a[%d]\n", a[i], i));
			}
		}
		return true;
	}

	public static boolean isZeroVector(double[] a) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] != 0) {
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		{
			double[][] a = { { 1, 3, 4, 4, 6 }, { 0, 0, 2, 4, 5 }, { 0, 0, 0, 1, 6 } };
			System.out.println(isRowEchelonForm(a));
			System.out.println(isReducedRowEchelonForm(a));
		}

		{
			double[][] a = { { 1, 0, 3, 0, 5 }, { 0, 1, 0, 0, 4 }, { 0, 0, 0, 1, 6 } };
			System.out.println(isRowEchelonForm(a));
			System.out.println(isReducedRowEchelonForm(a));
		}

		{
			double[][] a = { { 1, 3 }, { -5, 0 } };
			double[][] b = { { 1, 0 }, { 0, 1 } };

			System.out.println(ArrayUtils.toString(LA.joinColumns(a, b)));
		}

		{
			double[][] a = { { 1, 6, 2, -5, -2, -4 },

					{ 0, 0, 2, -8, -1, 3 }, { 0, 0, 0, 0, 1, 7 } };

			System.out.println(hasSolution(a));
		}

		{
			double[][] a = { { 1, 5, 0, 21 }, { 0, 1, 1, 4 } };
			System.out.println(hasSolution(a));
		}

		{
			double[][] a = { { 0, 3, -6, 6, 4, -5 }, { 3, -7, 8, -5, 8, 9 }, { 3, -9, 12, -9, 6, 15 } };

			System.out.println(hasSolution(a));
		}

		{
			double[][] a = { { 1, 2, 7 }, { -2, 5, 4 }, { -5, 6, -3 } };
			System.out.println(hasSolution(a));
		}

		{
			double[][] a = { { 1, 5, -3 }, { -2, -13, 8 }, { 3, -3, 1 } };
			System.out.println(hasSolution(a));
		}

		System.out.println("process ends.");
	}

	public boolean isOntoMapping(double[][] a) {
		int rowDim = a.length;
		int colDim = a[0].length;
		double[][] b = new double[rowDim][colDim];

		ArrayUtils.copy(a, b);

		int[] pivotColumns = LA.GaussElimination(b);

		int numPivots = 0;

		for (int i = 0; i < pivotColumns.length; i++) {
			if (pivotColumns[i] != -1) {
				numPivots++;
			}
		}

		return (numPivots == rowDim ? true : false);
	}

}
