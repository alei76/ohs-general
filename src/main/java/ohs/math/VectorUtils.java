package ohs.math;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import ohs.matrix.DenseVector;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.matrix.Vector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.utils.Generics;

public class VectorUtils {
	public static void copy(CounterMap<Integer, Integer> a, double[][] b) {
		for (int key1 : a.keySet()) {
			for (Entry<Integer, Double> e : a.getCounter(key1).entrySet()) {
				int key2 = e.getKey();
				double value = e.getValue();
				b[key1][key2] = value;
			}
		}
	}

	public static void copy(Vector src, Vector tar) {
		if (VectorChecker.isSparse(src) && VectorChecker.isSparse(tar)) {
			ArrayUtils.copy(src.indexes(), tar.indexes());
			ArrayUtils.copy(src.values(), tar.values());
			tar.setSum(src.sum());
		} else if (!VectorChecker.isSparse(src) && !VectorChecker.isSparse(tar)) {
			ArrayUtils.copy(src.values(), tar.values());
			tar.setSum(src.sum());
		} else {
			throw new IllegalArgumentException();
		}
	}

	public static SparseVector freqOfFreq(DenseVector x) {
		Counter<Integer> counter = new Counter<Integer>();
		for (int i = 0; i < x.size(); i++) {
			int freq = (int) x.value(i);
			counter.incrementCount(freq, 1);
		}
		SparseVector ret = toSparseVector(counter);
		ret.setLabel(x.label());
		return ret;
	}

	public static void subVector(SparseVector x, int[] indexSet) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();
		double sum = 0;

		for (int index : indexSet) {
			int loc = x.location(index);
			if (loc < 0) {
				continue;
			}

			double value = x.valueAtLoc(loc);
			indexList.add(index);
			valueList.add(value);
			sum += value;
		}

		int[] indexes = new int[indexList.size()];
		double[] values = new double[valueList.size()];

		ArrayUtils.copy(indexList, indexes);
		ArrayUtils.copy(valueList, values);

		x.setIndexes(indexes);
		x.setValues(values);
		x.setSum(sum);
	}

	public static SparseVector subVectorTo(SparseVector x, int[] subset) {
		SparseVector ret = x.copy();
		subVector(x, subset);
		return ret;
	}

	public static Counter<String> toCounter(Counter<Integer> x, Indexer<String> indexer) {
		Counter<String> ret = new Counter<String>();
		for (int index : x.keySet()) {
			double value = x.getCount(index);
			String obj = indexer.getObject(index);
			if (obj == null) {
				continue;
			}
			ret.setCount(obj, value);
		}
		return ret;
	}

	public static Counter<String> toCounter(DenseVector x, Indexer<String> indexer) {
		Counter<String> ret = new Counter<String>();
		for (int i = 0; i < x.size(); i++) {
			double value = x.value(i);
			if (value == 0) {
				continue;
			}
			String obj = indexer.getObject(i);
			ret.setCount(obj, value);
		}
		return ret;
	}

	public static Counter<Integer> toCounter(SparseVector x) {
		Counter<Integer> ret = new Counter<Integer>();
		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			if (index < 0) {
				continue;
			}
			double value = x.valueAtLoc(i);
			ret.setCount(index, value);
		}
		return ret;
	}

	public static Counter<String> toCounter(SparseVector x, Indexer<String> indexer) {
		Counter<String> ret = new Counter<String>();
		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			if (index < 0) {
				continue;
			}

			double value = x.valueAtLoc(i);
			String obj = indexer.getObject(index);

			if (obj == null) {
				continue;
			}
			ret.setCount(obj, value);
		}

		return ret;
	}

	public static Counter<String> toCounter(String s) {
		Counter<String> ret = new Counter<String>();
		for (String tok : s.substring(1, s.length() - 1).split(" ")) {
			String[] two = tok.split(":");
			ret.setCount(two[0], Double.parseDouble(two[1]));
		}
		return ret;
	}

	public static CounterMap<String, String> toCounterMap(CounterMap<Integer, Integer> cm, Indexer<String> rowIndexer,
			Indexer<String> columnIndexer) {
		CounterMap<String, String> ret = new CounterMap<String, String>();
		for (int rowId : cm.keySet()) {
			String rowStr = null;
			if (rowIndexer == null) {
				rowStr = rowId + "";
			} else {
				rowStr = rowIndexer.getObject(rowId);
			}

			for (Entry<Integer, Double> entry : cm.getCounter(rowId).entrySet()) {
				int colId = entry.getKey();
				String colStr = null;

				if (columnIndexer == null) {
					colStr = colId + "";
				} else {
					colStr = columnIndexer.getObject(colId);
				}

				double value = entry.getValue();
				ret.incrementCount(rowStr, colStr, value);
			}
		}
		return ret;
	}

	public static ListMap<Integer, SparseVector> toIndexedList(List<SparseVector> xs) {
		ListMap<Integer, SparseVector> ret = new ListMap<Integer, SparseVector>();
		for (int i = 0; i < xs.size(); i++) {
			SparseVector vector = xs.get(i);
			ret.put(vector.label(), vector);
		}
		return ret;
	}

	public static double[][] toMatrix(CounterMap<Integer, Integer> a, int dim) {
		double[][] ret = new double[dim][dim];
		copy(a, ret);
		return ret;
	}

	public static double[][] toMatrix(CounterMap<Integer, Integer> a, int row_dim, int col_dim) {
		double[][] ret = new double[row_dim][col_dim];
		copy(a, ret);
		return ret;
	}

	public static String toRankedString(SparseVector x, Indexer<String> indexer) {
		StringBuffer sb = new StringBuffer();

		x.sortByValue();
		x.reverse();

		for (int i = 0; i < x.size(); i++) {
			int index = x.indexAtLoc(i);
			int rank = (int) x.valueAtLoc(i);
			sb.append(String.format(" %s:%d", indexer.getObject(index), rank));
		}
		return sb.toString().trim();
	}

	public static SparseMatrix toSparseMatrix(CounterMap<Integer, Integer> cm) {
		int[] rowids = new int[cm.keySet().size()];
		SparseVector[] rows = new SparseVector[cm.keySet().size()];

		int loc = 0;
		for (int key : cm.keySet()) {
			rowids[loc] = key;
			rows[loc] = toSparseVector(cm.getCounter(key));
			loc++;
		}

		SparseMatrix ret = new SparseMatrix(-1, -1, -1, rowids, rows);
		ret.sortByRowIndex();
		return ret;
	}

	public static SparseMatrix toSparseMatrix(CounterMap<String, String> cm, Indexer<String> rowIndexer, Indexer<String> columnIndexer,
			boolean addIfUnseen) {

		List<Integer> rs1 = Generics.newArrayList();
		List<SparseVector> rs2 = Generics.newArrayList();

		for (String rowKey : cm.keySet()) {
			int rowid = -1;
			if (addIfUnseen) {
				rowid = rowIndexer.getIndex(rowKey);
			} else {
				rowid = rowIndexer.indexOf(rowKey);
			}

			if (rowid < 0) {
				continue;
			}

			rs1.add(rowid);
			rs2.add(toSparseVector(cm.getCounter(rowKey), columnIndexer, addIfUnseen));
		}

		int[] rowIds = new int[rs1.size()];
		SparseVector[] rows = new SparseVector[rs1.size()];

		for (int i = 0; i < rowIds.length; i++) {
			rowIds[i] = rs1.get(i);
			rows[i] = rs2.get(i);
		}
		SparseMatrix ret = new SparseMatrix(-1, -1, -1, rowIds, rows);
		ret.sortByRowIndex();
		return ret;
	}

	public static SparseVector toSparseVector(Counter<Integer> x) {
		List<Integer> indexList = new ArrayList<Integer>();
		List<Double> valueList = new ArrayList<Double>();
		for (Entry<Integer, Double> entry : x.entrySet()) {
			int index = entry.getKey();
			double value = entry.getValue();
			indexList.add(index);
			valueList.add(value);
		}
		return toSparseVector(indexList, valueList, 0);
	}

	public static SparseVector toSparseVector(Counter<String> x, Indexer<String> indexer) {
		return toSparseVector(x, indexer, false);
	}

	public static SparseVector toSparseVector(Counter<String> x, Indexer<String> indexer, boolean addIfUnseen) {
		List<Integer> indexes = new ArrayList<Integer>();
		List<Double> values = new ArrayList<Double>();

		for (Entry<String, Double> e : x.entrySet()) {
			String key = e.getKey();
			double value = e.getValue();
			int index = indexer.indexOf(key);

			if (index < 0) {
				if (addIfUnseen) {
					index = indexer.getIndex(key);
				} else {
					continue;
				}
			}
			indexes.add(index);
			values.add(value);
		}
		return toSparseVector(indexes, values, indexer.size());
	}

	public static SparseVector toSparseVector(List<Integer> indexes, List<Double> values, int dim) {
		SparseVector ret = new SparseVector(indexes.size());
		for (int i = 0; i < indexes.size(); i++) {
			ret.incrementAtLoc(i, indexes.get(i), values.get(i));
		}
		ret.setDim(dim);
		ret.sortByIndex();
		return ret;
	}

	public static SparseVector toSparseVector(List<String> x, Indexer<String> indexer, boolean addIfUnseen) {
		Counter<Integer> ret = new Counter<Integer>();

		for (String item : x) {
			int index = indexer.indexOf(item);

			if (index < 0) {
				if (addIfUnseen) {
					index = indexer.getIndex(item);
				} else {
					continue;
				}
			}

			ret.incrementCount(index, 1);
		}
		return toSparseVector(ret);
	}

	public static SparseVector toSparseVector(String s, Indexer<String> indexer) {
		return toSparseVector(toCounter(s), indexer);
	}

	public static SparseMatrix toSpasreMatrix(CounterMap<Integer, Integer> cm) {
		return toSpasreMatrix(cm, -1, -1, -1);
	}

	public static SparseMatrix toSpasreMatrix(CounterMap<Integer, Integer> cm, int rowDim, int colDim, int label) {
		int[] rowIds = new int[cm.keySet().size()];
		SparseVector[] rows = new SparseVector[rowIds.length];
		int loc = 0;
		for (int index : cm.keySet()) {
			rowIds[loc] = index;
			rows[loc] = toSparseVector(cm.getCounter(index));
			rows[loc].setLabel(index);
			if (rows[loc].size() > colDim) {
				colDim = rows[loc].size();
			}
			loc++;
		}
		SparseMatrix ret = new SparseMatrix(rowDim, colDim, label, rowIds, rows);
		ret.sortByRowIndex();
		return ret;
	}

	public static String toSVMFormat(SparseVector x, NumberFormat nf) {
		if (nf == null) {
			nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(8);
			nf.setGroupingUsed(false);
		}

		StringBuffer sb = new StringBuffer();
		sb.append(x.label());
		for (int i = 0; i < x.size(); i++) {
			sb.append(String.format(" %d:%s", x.indexAtLoc(i), nf.format(x.valueAtLoc(i))));
		}
		return sb.toString();
	}
}
