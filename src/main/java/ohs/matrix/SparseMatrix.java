package ohs.matrix;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ohs.io.IOUtils;
import ohs.types.Counter;
import ohs.types.CounterMap;

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

	public static SparseMatrix read(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		ObjectInputStream ois = IOUtils.openObjectInputStream(fileName);
		SparseMatrix ret = readStream(ois);
		ois.close();
		return ret;
	}

	public static List<SparseMatrix> readList(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		List<SparseMatrix> ret = new ArrayList<SparseMatrix>();

		ObjectInputStream ois = IOUtils.openObjectInputStream(fileName);
		int size = ois.readInt();
		for (int i = 0; i < size; i++) {
			SparseMatrix mat = readStream(ois);
			ret.add(mat);
		}
		ois.close();
		System.out.printf("read [%d] matrices.\n", ret.size());
		return ret;
	}

	public static SparseMatrix readStream(ObjectInputStream ois) throws Exception {
		int rowDim = ois.readInt();
		int colDim = ois.readInt();
		int label = ois.readInt();
		int rowSize = ois.readInt();

		int[] rowIndexes = new int[rowSize];
		SparseVector[] rowVectors = new SparseVector[rowSize];

		for (int i = 0; i < rowSize; i++) {
			rowIndexes[i] = ois.readInt();
			rowVectors[i] = SparseVector.readStream(ois);
		}
		SparseMatrix ret = new SparseMatrix(rowDim, colDim, label, rowIndexes, rowVectors);
		ret.sortByRowIndex();

		return ret;
	}

	public static void write(String fileName, List<SparseMatrix> xs) throws Exception {
		System.out.printf("write to [%s].\n", fileName);

		ObjectOutputStream oos = IOUtils.openObjectOutputStream(fileName);
		oos.writeInt(xs.size());
		for (int i = 0; i < xs.size(); i++) {
			SparseMatrix vector = xs.get(i);
			vector.write(oos);
		}
		oos.close();

		System.out.printf("write [%d] matrices.\n", xs.size());
	}

	private int rowDim;

	private int colDim;

	private int label;

	private int[] rowIndexes;

	private SparseVector[] rows;

	public SparseMatrix(int rowDim, int colDim, int label, int[] rowIndexes, SparseVector[] rows) {
		this.rowDim = rowDim;
		this.colDim = colDim;
		this.label = label;
		this.rowIndexes = rowIndexes;
		this.rows = rows;
		// sortByRowIndex();
	}

	public SparseMatrix(int rowDim, int colDim, int label, Map<Integer, SparseVector> entries) {
		this.rowDim = rowDim;
		this.colDim = colDim;
		this.label = label;

		setEntries(entries);
	}

	public SparseMatrix(Map<Integer, SparseVector> entries) {
		this(-1, -1, -1, entries);
	}

	public int colDim() {
		return colDim;
	}

	public SparseVector column(int colId) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();

		for (int i = 0; i < rowSize(); i++) {
			int index = rowIndexes[i];
			SparseVector row = rows[i];
			int loc = row.location(colId);
			if (loc < 0) {
				continue;
			}
			double value = row.valueAtLoc(loc);
			indexList.add(index);
			valueList.add(value);
		}

		int[] indexes = new int[indexList.size()];
		double[] values = new double[valueList.size()];
		double sum = 0;
		for (int i = 0; i < indexList.size(); i++) {
			indexes[i] = indexList.get(i);
			values[i] = valueList.get(i);
			sum += values[i];
		}
		SparseVector ret = new SparseVector(indexes, values, label(), rowDim());
		ret.setSum(sum);
		return ret;
	}

	public SparseVector columnSums() {
		Counter<Integer> counter = new Counter<Integer>();

		for (int i = 0; i < rowSize(); i++) {
			SparseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				int colId = row.indexAtLoc(j);
				double value = row.valueAtLoc(j);
				counter.incrementCount(colId, value);
			}
		}
		return new SparseVector(counter, label(), rowDim());
	}

	public SparseMatrix copy() {
		int[] newRowIndexes = new int[rowIndexes.length];
		SparseVector[] newRowVectors = new SparseVector[rowIndexes.length];
		for (int i = 0; i < rowIndexes.length; i++) {
			newRowIndexes[i] = rowIndexes[i];
			newRowVectors[i] = rows[i].copy();
		}
		return new SparseMatrix(rowDim(), colDim(), label(), newRowIndexes, newRowVectors);
	}

	public Map<Integer, SparseVector> entries() {
		Map<Integer, SparseVector> ret = new HashMap<Integer, SparseVector>();
		for (int i = 0; i < rowIndexes.length; i++) {
			ret.put(rowIndexes[i], rows[i]);
		}
		return ret;
	}

	public int indexAtRowLoc(int loc) {
		return rowIndexes[loc];
	}

	public int label() {
		return label;
	}

	public int locationAtRow(int rowId) {
		return Arrays.binarySearch(rowIndexes, rowId);
	}

	public void normalizeColumns() {
		int maxColIndex = 0;

		for (int i = 0; i < rows.length; i++) {
			SparseVector row = rows[i];
			int lastIndex = row.indexAtLoc(row.size() - 1);
			if (maxColIndex < lastIndex) {
				maxColIndex = lastIndex;
			}
		}

		double[] columnSums = new double[maxColIndex + 1];

		for (int i = 0; i < rows.length; i++) {
			SparseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				int colIndex = row.indexAtLoc(j);
				double value = row.valueAtLoc(j);
				columnSums[colIndex] += value;
			}
		}

		for (int i = 0; i < rows.length; i++) {
			SparseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				int colId = row.indexAtLoc(j);
				double value = row.valueAtLoc(j);
				double sum = columnSums[colId];

				if (sum != 0) {
					row.setAtLoc(j, colId, value / sum);
				}
			}
			row.summation();
		}
	}

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

	public Vector row(int rowId) {
		int loc = locationAtRow(rowId);
		if (loc < 0) {
			throw new IllegalArgumentException("not found");
		}
		return rows[loc];
	}

	public SparseVector rowAlways(int rowId) {
		SparseVector ret = new SparseVector(0);
		int loc = locationAtRow(rowId);
		if (loc > -1) {
			ret = rows[loc];
		}
		return ret;
	}

	public SparseVector rowAtLoc(int loc) {
		return rows[loc];
	}

	public int rowDim() {
		return rowDim;
	}

	public int[] rowIndexes() {
		return rowIndexes;
	}

	@Override
	public Vector[] rows() {
		return rows;
	}

	public int rowSize() {
		return rowIndexes.length;
	}

	public void rowSummation() {
		for (Vector row : rows) {
			row.summation();
		}
	}

	public SparseVector rowSums() {
		SparseVector ret = new SparseVector(rowSize());
		ret.setDim(rowDim());

		double totalSum = 0;
		for (int i = 0; i < rowIndexes.length; i++) {
			int rowId = rowIndexes[i];
			SparseVector row = rows[i];
			double sum = row.sum();
			ret.setAtLoc(i, rowId, sum);
			totalSum += sum;
		}
		ret.setSum(totalSum);
		return ret;
	}

	public void set(int rowId, int colId, double value) {
		int rowLoc = locationAtRow(rowId);
		if (rowLoc > -1) {
			SparseVector row = (SparseVector) vectorAtRowLoc(rowLoc);
			int colLoc = row.location(colId);
			if (colLoc > -1) {
				row.setAtLoc(colLoc, value);
			}
		}
	}

	public void setAll(double value) {
		for (int i = 0; i < rows.length; i++) {
			rows[i].setAll(value);
		}
	}

	public void setColDim(int colDim) {
		this.colDim = colDim;
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

	public void setLabel(int label) {
		this.label = label;
	}

	public void setRow(int loc, int rowId, SparseVector x) {
		rowIndexes[loc] = rowId;
		rows[loc] = x;
	}

	public void setRow(int rowId, SparseVector x) {
		int loc = locationAtRow(rowId);
		if (loc > -1) {
			setVectorAtRowLoc(loc, x);
		}
	}

	public void setRow(int rowId, Vector x) {
		int rowLoc = locationAtRow(rowId);
		if (rowLoc > -1) {
			setVectorAtRowLoc(rowLoc, x);
		}

	}

	public void setRowDim(int rowDim) {
		this.rowDim = rowDim;
	}

	public void setVectorAtRowLoc(int loc, Vector x) {
		rows[loc] = (SparseVector) x;
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

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[label:\t%d]\n", label()));
		sb.append(String.format("[row dim:\t%d]\n", rowDim()));
		sb.append(String.format("[col dim:\t%d]\n", colDim()));
		for (int i = 0; i < rowIndexes.length && i < 15; i++) {
			sb.append(String.format("%dth: %s\n", i + 1, rows[i]));
		}
		return sb.toString().trim();
	}

	public SparseMatrix transpose() {
		CounterMap<Integer, Integer> counterMap = new CounterMap<Integer, Integer>();

		for (int i = 0; i < rows.length; i++) {
			int rowIndex = rowIndexes[i];
			SparseVector row = rows[i];
			for (int j = 0; j < row.size(); j++) {
				int colIndex = row.indexAtLoc(j);
				double value = row.valueAtLoc(j);
				counterMap.incrementCount(colIndex, rowIndex, value);
			}
		}

		int[] rowIds = new int[counterMap.keySet().size()];
		SparseVector[] rows = new SparseVector[rowIds.length];
		int loc = 0;

		for (int rowId : counterMap.keySet()) {
			Counter<Integer> col_value = counterMap.getCounter(rowId);

			int[] ids = new int[col_value.keySet().size()];
			double[] values = new double[ids.length];

			int loc2 = 0;
			for (Entry<Integer, Double> entry : col_value.entrySet()) {
				int colId = entry.getKey();
				double value = entry.getValue();
				ids[loc2] = colId;
				values[loc2] = value;
				loc2++;
			}

			rowIds[loc] = rowId;
			SparseVector row = new SparseVector(ids, values, rowId, rowDim());
			row.sortByIndex();
			rows[loc++] = row;

		}

		return new SparseMatrix(colDim(), rowDim(), label(), rowIds, rows);
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

	public SparseVector vectorAtRowLoc(int loc) {
		return rows[loc];
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(rowDim());
		oos.writeInt(colDim());
		oos.writeInt(label());
		oos.writeInt(rowSize());

		for (int i = 0; i < rowSize(); i++) {
			oos.writeInt(indexAtRowLoc(i));
			vectorAtRowLoc(i).write(oos);
		}
	}

	public void write(String fileName) throws Exception {
		System.out.printf("write to [%s].\n", fileName);
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(fileName);
		write(oos);
		oos.close();
	}

}
