package ohs.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeepListMap<K, V, F> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7366754514015096846L;

	protected Map<K, ListMap<V, F>> entries;

	public DeepListMap() {
		entries = new HashMap<K, ListMap<V, F>>();
	}

	public boolean containsKey(K key1) {
		return entries.containsKey(key1);
	}

	protected ListMap<V, F> ensure(K key1) {
		ListMap<V, F> ret = entries.get(key1);
		if (ret == null) {
			ret = new ListMap<V, F>();
			entries.put(key1, ret);
		}
		return ret;
	}

	protected List<F> ensure(K key1, V key2) {
		return ensure(key1).get(key2, true);
	}

	public ListMap<V, F> get(K key1) {
		return get(key1, true);
	}

	public ListMap<V, F> get(K key1, boolean createIfAbsent) {
		return createIfAbsent ? ensure(key1) : entries.get(key1);
	}

	public List<F> get(K key1, V key2, boolean createIfAbsent) {
		List<F> ret = null;
		if (createIfAbsent) {
			ret = ensure(key1, key2);
		} else {
			ListMap<V, F> temp = entries.get(key1);
			if (temp != null) {
				ret = temp.get(key2);
			}
		}
		return ret;
	}

	public Set<K> keySet() {
		return entries.keySet();
	}

	public void put(K key, ListMap<V, F> value) {
		entries.put(key, value);
	}

	public void put(K key1, V key2, F value) {
		ensure(key1, key2).add(value);
	}

	public int size() {
		return entries.size();
	}

	public String toString() {
		return toString(30);
	}

	public String toString(int num_print_entries) {
		StringBuffer sb = new StringBuffer();
		int cnt = 0;
		int cnt2 = 0;
		int cnt3 = 0;

		List<K> keys1 = new ArrayList<K>(entries.keySet());

		for (int i = 0; i < keys1.size() && i < num_print_entries; i++) {
			ListMap<V, F> innerEntries = entries.get(keys1.get(i));

			List<V> keys2 = new ArrayList<V>(innerEntries.keySet());

			for (int j = 0; j < keys2.size() && j < num_print_entries; j++) {
				List<F> values = innerEntries.get(keys2.get(j), false);

				sb.append(keys1.get(i));
				sb.append(" -> " + keys2.get(j));
				for (int k = 0; k < values.size() && k < num_print_entries; k++) {
					if (k == 0) {
						sb.append(" ->");
					}
					sb.append(" " + values.get(k));
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

}
