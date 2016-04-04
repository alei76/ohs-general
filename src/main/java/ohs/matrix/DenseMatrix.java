package ohs.matrix;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ohs.io.FileUtils;

public class DenseMatrix implements Matrix {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3542638642565119292L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double[][] a = { { 0, 1 }, { 2, 3 } };

		DenseMatrix m = new DenseMatrix(a);

		System.out.println(m.toString());
		System.out.println();
	}

	public static DenseMatrix read(ObjectInputStream ois) throws Exception {
		int rowDim = ois.readInt();
		int colDim = ois.readInt();
		int label = ois.readInt();

		DenseVector[] rowVectors = new DenseVector[rowDim];
		for (int i = 0; i < rowDim; i++) {
			rowVectors[i] = DenseVector.readStream(ois);
		}
		return new DenseMatrix(label, rowVectors);
	}

	public static DenseMatrix read(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		DenseMatrix ret = null;
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		ret = read(ois);
		ois.close();
		return ret;
	}

	private DenseVector[] rows;

	private int label;

	public DenseMatrix(double[][] values) {
		this(-1, values);
	}

	public DenseMatrix(int dim) {
		this(dim, dim, -1);
	}

	public DenseMatrix(int label, DenseVector[] rows) {
		this.rows = rows;
		this.label = label;
	}

	public DenseMatrix(int label, double[][] values) {
		rows = new DenseVector[values.length];
		for (int i = 0; i < values.length; i++) {
			rows[i] = new DenseVector(values[i], i);
		}
		this.label = label;
	}

	public DenseMatrix(int rowDim, int colDim) {
		this(rowDim, colDim, -1);
	}

	public DenseMatrix(int rowDim, int colDim, int label) {
		this(label, new double[rowDim][colDim]);
	}

	@Override
	public int colDim() {
		return rows[0].dim();
	}

	@Override
	public DenseVector column(int colId) {
		DenseVector ret = new DenseVector(rowDim(), label());
		for (int i = 0; i < rowDim(); i++) {
			ret.set(i, row(i).value(colId));
		}
		return ret;
	}

	@Override
	public DenseVector columnSums() {
		DenseVector ret = new DenseVector(rowDim(), label());
		for (int i = 0; i < rowDim(); i++) {
			DenseVector vector = row(i);
			for (int j = 0; j < vector.size(); j++) {
				ret.increment(j, vector.value(j));
			}
		}
		return ret;
	}

	public DenseMatrix copy() {
		DenseVector[] rows = new DenseVector[rowDim()];
		for (int i = 0; i < rowDim(); i++) {
			rows[i] = row(i).copy();
		}
		return new DenseMatrix(label(), rows);
	}

	public void increment(int row, int col, double value) {
		rows[row].increment(col, value);
	}

	@Override
	public int indexAtRowLoc(int rowLoc) {
		new UnsupportedOperationException("unsupported");
		return 0;
	}

	@Override
	public String info() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int label() {
		return label;
	}

	@Override
	public void normalizeColumns() {
		DenseVector col_sum = columnSums();
		for (int i = 0; i < rowDim(); i++) {
			DenseVector row = row(i);
			for (int j = 0; j < row.size(); j++) {
				row.scale(j, 1f / col_sum.value(j));
			}
		}
	}

	@Override
	public void normalizeRows() {
		for (int i = 0; i < rowDim(); i++) {
			row(i).normalizeAfterSummation();
		}
	}

	@Override
	public DenseVector row(int row) {
		return rows[row];
	}

	@Override
	public int rowDim() {
		return rows.length;
	}

	@Override
	public int[] rowIndexes() {
		new UnsupportedOperationException("unsupported");
		return null;
	}

	@Override
	public Vector[] rows() {
		return rows;
	}

	@Override
	public int rowSize() {
		return rowDim();
	}

	@Override
	public void rowSummation() {
		for (int i = 0; i < rowDim(); i++) {
			row(i).summation();
		}
	}

	@Override
	public DenseVector rowSums() {
		DenseVector ret = new DenseVector(rowDim(), label());
		for (int i = 0; i < rowDim(); i++) {
			DenseVector vector = row(i);
			vector.summation();
			ret.set(i, vector.sum());
		}
		return ret;
	}

	public void set(double value) {
		for (int i = 0; i < rows.length; i++) {
			rows[i].setAll(value);
		}
	}

	@Override
	public void set(int row, int col, double value) {
		row(row).set(col, value);
	}

	@Override
	public void setColDim(int colDim) {
		new UnsupportedOperationException("unsupported");

	}

	@Override
	public void setLabel(int label) {
		this.label = label;
	}

	public void setRow(int row, DenseVector vector) {
		rows[row] = vector;
	}

	@Override
	public void setRow(int rowId, Vector x) {
		rows[rowId] = (DenseVector) x;

	}

	@Override
	public void setRowDim(int rowDim) {
		new UnsupportedOperationException("unsupported");

	}

	@Override
	public void setVectorAtRowLoc(int loc, Vector x) {
		setRow(loc, x);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[label:\t%d]\n", label));
		sb.append(String.format("[row:\t%d]\n", rowDim()));
		sb.append(String.format("[col:\t%d]\n", colDim()));

		for (int i = 0; i < 15 && i < rowDim(); i++) {
			DenseVector vector = rows[i];
			sb.append(String.format("%dth: %s\n", i, vector.toString(20, true, false, null)));
		}

		return sb.toString().trim();
	}

	/**
	 * @added Eunyoung Kim
	 * 
	 *        transpose this matrix
	 */
	public void transpose() {
		DenseVector[] tRowVectors = new DenseVector[colDim()];
		for (int i = 0; i < colDim(); i++) {
			DenseVector vector = new DenseVector(rowDim(), i);
			double sum = 0;
			for (int j = 0; j < rowDim(); j++) {
				double value = row(j).value(i);
				vector.set(j, value);
				sum += value;
			}
			vector.setSum(sum);
			tRowVectors[i] = vector;
		}
		rows = tRowVectors;
	}

	public double value(int row, int col) {
		return row(row).value(col);
	}

	@Override
	public Vector vectorAtRowLoc(int rowLoc) {
		return row(rowLoc);
	}

	@Override
	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(rowDim());
		oos.writeInt(colDim());
		oos.writeInt(label());
		for (int i = 0; i < rowDim(); i++) {
			row(i).write(oos);
		}
	}

	@Override
	public void write(String fileName) throws Exception {
		System.out.printf("write to [%s].\n", fileName);
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		write(oos);
		oos.close();
	}

	@Override
	public double[][] values() {
		double[][] ret = new double[rows.length][];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = rows[i].values();
		}
		return ret;
	}

}
