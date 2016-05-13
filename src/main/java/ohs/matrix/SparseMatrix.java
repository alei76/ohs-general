package ohs.matrix;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohs.io.FileUtils;
import ohs.math.VectorUtils;
import ohs.types.Counter;
import ohs.utils.Generics;

/**
 * @author Heung-Seon Oh
 * 
 */
public class SparseMatrix implements Matrix {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3542638642565119292L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	public static void write(String fileName, List<SparseMatrix> xs) throws Exception {
		System.out.printf("write to [%s].\n", fileName);

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		oos.writeInt(xs.size());
		for (int i = 0; i < xs.size(); i++) {
			SparseMatrix vector = xs.get(i);
			vector.writeObject(oos);
		}
		oos.close();

		System.out.printf("write [%d] matrices.\n", xs.size());
	}

	private int row_dim;

	private int col_dim;

	private int[] rowIndexes;

	private SparseVector[] rows;

	public SparseMatrix(int row_dim, int col_dim, int[] rowIndexes, SparseVector[] rows) {
		this.row_dim = row_dim;
		this.col_dim = col_dim;
		this.rowIndexes = rowIndexes;
		this.rows = rows;
	}

	public SparseMatrix(int row_dim, int col_dim, Map<Integer, SparseVector> entries) {
		this.row_dim = row_dim;
		this.col_dim = col_dim;
		setEntries(entries);
	}

	public SparseMatrix(Map<Integer, SparseVector> entries) {
		this(-1, -1, entries);
	}

	@Override
	public int colDim() {
		return col_dim;
	}

	@Override
	public SparseVector column(int j) {
		List<Integer> indexes = Generics.newArrayList();
		List<Double> values = Generics.newArrayList();

		for (int m = 0; m < rows.length; m++) {
			int i = rowIndexes[m];
			SparseVector row = rows[m];
			int loc = row.location(j);
			if (loc < 0) {
				continue;
			}
			indexes.add(i);
			values.add(row.valueAtLoc(loc));
		}
		return VectorUtils.toSparseVector(indexes, values, row_dim);
	}

	@Override
	public SparseVector columnSums() {
		Counter<Integer> c = Generics.newCounter();
		for (int m = 0; m < rows.length; m++) {
			SparseVector row = rows[m];
			for (int n = 0; n < row.size(); n++) {
				c.incrementCount(row.indexAtLoc(n), row.valueAtLoc(n));
			}
		}
		return new SparseVector(c, row_dim);
	}

	public SparseMatrix copy() {
		int[] newRowIndexes = new int[rowIndexes.length];
		SparseVector[] newRows = new SparseVector[rowIndexes.length];
		for (int i = 0; i < rowIndexes.length; i++) {
			newRowIndexes[i] = rowIndexes[i];
			newRows[i] = rows[i].copy();
		}
		return new SparseMatrix(rowDim(), colDim(), newRowIndexes, newRows);
	}

	public Map<Integer, SparseVector> entries() {
		Map<Integer, SparseVector> ret = new HashMap<Integer, SparseVector>();
		for (int i = 0; i < rowIndexes.length; i++) {
			ret.put(rowIndexes[i], rows[i]);
		}
		return ret;
	}

	@Override
	public int indexAtLoc(int loc) {
		return rowIndexes[loc];
	}

	@Override
	public String info() {
		return null;
	}

	public int locationAtRow(int i) {
		return Arrays.binarySearch(rowIndexes, i);
	}

	@Override
	public void normalizeColumns() {
		SparseVector columnSums = columnSums();
		for (int i = 0; i < rows.length; i++) {
			SparseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				int colId = row.indexAtLoc(j);
				double value = row.valueAtLoc(j);
				double sum = columnSums.valueAlways(colId);
				if (sum != 0) {
					row.setAtLoc(j, colId, value / sum);
				}
			}
			row.summation();
		}
	}

	@Override
	public void normalizeRows() {
		for (Vector row : rows) {
			row.normalizeAfterSummation();
		}
	}

	private int qPartition(int low, int high) {
		// First element
		// int pivot = a[low];

		// Middle element
		// int middle = (low + high) / 2;

		int i = low - 1;
		int j = high + 1;

		// ascending order
		int randomIndex = (int) (Math.random() * (high - low)) + low;
		int pivotValue = rowIndexes[randomIndex];

		while (i < j) {
			i++;
			while (rowIndexes[i] < pivotValue) {
				i++;
			}

			j--;
			while (rowIndexes[j] > pivotValue) {
				j--;
			}

			if (i < j) {
				swapRows(i, j);
			}
		}
		return j;
	}

	private void qSort(int low, int high) {
		if (low >= high)
			return;
		int p = qPartition(low, high);
		qSort(low, p);
		qSort(p + 1, high);
	}

	private void quicksort() {
		qSort(0, rowIndexes.length - 1);
	}

	public void readObject(ObjectInputStream ois) throws Exception {
		row_dim = ois.readInt();
		col_dim = ois.readInt();
		int size = ois.readInt();

		rowIndexes = new int[size];
		rows = new SparseVector[size];

		for (int i = 0; i < size; i++) {
			rowIndexes[i] = ois.readInt();
			rows[i].read(ois);
		}
	}

	public void readObject(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		readObject(ois);
		ois.close();
	}

	@Override
	public Vector row(int i) {
		int loc = locationAtRow(i);
		if (loc < 0) {
			throw new IllegalArgumentException("not found");
		}
		return rows[loc];
	}

	public SparseVector rowAlways(int i) {
		SparseVector ret = new SparseVector(0);
		int loc = locationAtRow(i);
		if (loc > -1) {
			ret = rows[loc];
		}
		return ret;
	}

	@Override
	public SparseVector rowAtLoc(int loc) {
		return rows[loc];
	}

	@Override
	public int rowDim() {
		return row_dim;
	}

	@Override
	public int[] rowIndexes() {
		return rowIndexes;
	}

	@Override
	public Vector[] rows() {
		return rows;
	}

	@Override
	public int rowSize() {
		return rowIndexes.length;
	}

	@Override
	public void rowSummation() {
		for (Vector row : rows) {
			row.summation();
		}
	}

	@Override
	public SparseVector rowSums() {
		SparseVector ret = new SparseVector(rows.length, row_dim);
		for (int m = 0; m < rowIndexes.length; m++) {
			ret.incrementAtLoc(m, rowIndexes[m], rows[m].sum());
		}
		return ret;
	}

	@Override
	public void set(int i, int j, double value) {
		int rl = locationAtRow(i);
		if (rl > -1) {
			SparseVector row = rowAtLoc(rl);
			int cl = row.location(j);
			if (cl > -1) {
				row.setAtLoc(cl, value);
			}
		}
	}

	public void setAll(double value) {
		for (int i = 0; i < rows.length; i++) {
			rows[i].setAll(value);
		}
	}

	@Override
	public void setColDim(int colDim) {
		this.col_dim = colDim;
		for (int i = 0; i < rows.length; i++) {
			rows[i].setDim(colDim);
		}
	}

	public void setEntries(Map<Integer, SparseVector> entries) {
		rowIndexes = new int[entries.keySet().size()];
		rows = new SparseVector[rowIndexes.length];
		int loc = 0;

		for (int row : entries.keySet()) {
			SparseVector rowVec = entries.get(row);
			rowIndexes[loc] = row;
			rows[loc] = rowVec;
			loc++;
		}
		sortByRowIndex();
	}

	public void setRow(int loc, int rowId, SparseVector x) {
		rowIndexes[loc] = rowId;
		rows[loc] = x;
	}

	public void setRow(int rowId, SparseVector x) {
		int loc = locationAtRow(rowId);
		if (loc > -1) {
			setRowAtLoc(loc, x);
		}
	}

	@Override
	public void setRow(int rowId, Vector x) {
		int rowLoc = locationAtRow(rowId);
		if (rowLoc > -1) {
			setRowAtLoc(rowLoc, x);
		}
	}

	@Override
	public void setRowAtLoc(int loc, Vector x) {
		rows[loc] = (SparseVector) x;
	}

	@Override
	public void setRowDim(int rowDim) {
		this.row_dim = rowDim;
	}

	public void sortByRowIndex() {
		quicksort();
	}

	private void swapRows(int i, int j) {
		int temp1 = rowIndexes[i];
		int temp2 = rowIndexes[j];
		rowIndexes[i] = temp2;
		rowIndexes[j] = temp1;

		SparseVector temp3 = rows[i];
		SparseVector temp4 = rows[j];

		rows[i] = temp4;
		rows[j] = temp3;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[row dim:\t%d]\n", rowDim()));
		sb.append(String.format("[col dim:\t%d]\n", colDim()));
		for (int i = 0; i < rowIndexes.length && i < 15; i++) {
			sb.append(String.format("%dth: %s\n", i + 1, rows[i]));
		}
		return sb.toString().trim();
	}

	public double value(int rowId, int colId) {
		return row(rowId).value(colId);
	}

	public double valueAlways(int rowId, int colId) {
		double ret = 0;
		int rowLoc = locationAtRow(rowId);
		if (rowLoc > -1) {
			ret = rows[rowLoc].valueAlways(colId);
		}
		return ret;
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
		oos.writeInt(row_dim);
		oos.writeInt(col_dim);
		oos.writeInt(rows.length);

		for (int i = 0; i < rows.length; i++) {
			oos.writeInt(rowIndexes[i]);
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
