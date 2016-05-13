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

	private DenseVector[] rows;

	public DenseMatrix(DenseVector[] rows) {
		this.rows = rows;
	}

	public DenseMatrix(double[][] values) {
		rows = new DenseVector[values.length];
		for (int i = 0; i < values.length; i++) {
			rows[i] = new DenseVector(values[i]);
		}
	}

	public DenseMatrix(int dim) {
		this(dim, dim);
	}

	public DenseMatrix(int rowDim, int colDim) {
		this(new double[rowDim][colDim]);
	}

	@Override
	public int colDim() {
		return rows[0].dim();
	}

	@Override
	public DenseVector column(int j) {
		DenseVector ret = new DenseVector(rowDim());
		for (int i = 0; i < rowDim(); i++) {
			ret.set(i, row(i).value(j));
		}
		return ret;
	}

	@Override
	public DenseVector columnSums() {
		DenseVector ret = new DenseVector(rowDim());
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
		return new DenseMatrix(rows);
	}

	public void increment(int i, int j, double value) {
		rows[i].increment(j, value);
	}

	@Override
	public int indexAtLoc(int row_loc) {
		return row_loc;
	}

	@Override
	public String info() {
		// TODO Auto-generated method stub
		return null;
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

	public void readObject(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		rows = new DenseVector[size];
		for (int i = 0; i < size; i++) {
			DenseVector v = new DenseVector();
			v.readObject(ois);
			rows[i] = v;
		}
	}

	public void readObject(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		readObject(ois);
		ois.close();
	}

	@Override
	public DenseVector row(int row) {
		return rows[row];
	}

	@Override
	public Vector rowAtLoc(int rowLoc) {
		return row(rowLoc);
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
		DenseVector ret = new DenseVector(rowDim());
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

	public void setRow(int row, DenseVector vector) {
		rows[row] = vector;
	}

	@Override
	public void setRow(int rowId, Vector x) {
		rows[rowId] = (DenseVector) x;

	}

	@Override
	public void setRowAtLoc(int loc, Vector x) {
		setRow(loc, x);
	}

	@Override
	public void setRowDim(int rowDim) {
		new UnsupportedOperationException("unsupported");

	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[row:\t%d]\n", rowDim()));
		sb.append(String.format("[col:\t%d]\n", colDim()));

		for (int i = 0; i < 15 && i < rowDim(); i++) {
			DenseVector vector = rows[i];
			sb.append(String.format("%dth: %s\n", i, vector.toString(20, true, null)));
		}

		return sb.toString().trim();
	}

	public double value(int row, int col) {
		return row(row).value(col);
	}

	@Override
	public double[][] values() {
		double[][] ret = new double[rows.length][];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = rows[i].values();
		}
		return ret;
	}

	@Override
	public void writeObject(ObjectOutputStream oos) throws Exception {
		oos.writeInt(rows.length);
		for (int i = 0; i < rows.length; i++) {
			rows[i].writeObject(oos);
		}
	}

	@Override
	public void writeObject(String fileName) throws Exception {
		System.out.printf("write to [%s].\n", fileName);
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		writeObject(oos);
		oos.close();
	}

}
