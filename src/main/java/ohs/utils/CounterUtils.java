package ohs.utils;

import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

public class CounterUtils {

	public static Counter<String> convert(Counter<Integer> counter, Indexer<String> indexer) {
		Counter<String> ret = new Counter<String>();

		for (int id : counter.keySet()) {
			double count = counter.getCount(id);
			String key = null;
			if (indexer == null) {
				key = id + "";
			} else {
				key = indexer.getObject(id);
			}
			ret.setCount(key, count);
		}
		return ret;
	}

	public static CounterMap<String, String> convert(CounterMap<Integer, Integer> counterMap, Indexer<String> rowIndexer,
			Indexer<String> columnIndexer) {
		CounterMap<String, String> ret = new CounterMap<String, String>();

		for (int rowId : counterMap.keySet()) {
			Counter<Integer> counter = counterMap.getCounter(rowId);
			Counter<String> newCounter = convert(counter, columnIndexer);
			String rowStr = null;
			if (rowIndexer == null) {
				rowStr = rowId + "";
			} else {
				rowStr = rowIndexer.getObject(rowId);
			}
			ret.setCounter(rowStr, newCounter);
		}
		return ret;
	}

	public static Counter<Integer> toIntegerKeys(Counter<String> c) {
		Counter<Integer> ret = new Counter<Integer>();
		for (String key : c.keySet()) {
			double count = c.getCount(key);
			ret.setCount(Integer.parseInt(key), count);
		}
		return ret;
	}

	public static Counter<String> toStringKeys(Counter<Integer> c) {
		Counter<String> ret = new Counter<String>();
		for (int key : c.keySet()) {
			double count = c.getCount(key);
			ret.setCount(Integer.toString(key), count);
		}
		return ret;
	}

}
