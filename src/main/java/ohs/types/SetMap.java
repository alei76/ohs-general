package ohs.types;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SetMap<K, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7366754514015096846L;

	protected Map<K, Set<V>> entries;

	private boolean useTreeMap;

	private boolean useTreeSet;

	public SetMap() {
		this(false, false);
	}

	public SetMap(boolean useTreeMap, boolean useTreeSet) {
		this.useTreeMap = useTreeMap;
		this.useTreeSet = useTreeSet;

		entries = useTreeMap ? new TreeMap<K, Set<V>>() : new HashMap<K, Set<V>>();
	}

	public void addAll(SetMap<K, V> input) {
		for (K key : input.keySet()) {
			Set<V> set = ensure(key);
			for (V val : input.get(key, false)) {
				set.add(val);
			}
		}
	}

	public void clear() {
		for (K key : entries.keySet()) {
			Set<V> set = entries.get(key);
			set.clear();
		}
		entries.clear();
	}

	public boolean contains(K key, V value) {
		boolean ret = false;
		Set<V> set = entries.get(key);
		if (set != null && set.contains(value)) {
			ret = true;
		}
		return ret;
	}

	public boolean containsKey(K key) {
		return entries.containsKey(key);
	}

	protected Set<V> ensure(K key) {
		Set<V> set = entries.get(key);
		if (set == null) {
			set = useTreeSet ? new TreeSet<V>() : new HashSet<V>();
			entries.put(key, set);
		}
		return set;
	}

	public Set<V> get(K key) {
		return get(key, true);
	}

	public Set<V> get(K key, boolean createIfAbsent) {
		return createIfAbsent ? ensure(key) : entries.get(key);
	}

	public SetMap<V, K> invert() {
		SetMap<V, K> ret = new SetMap<V, K>(useTreeMap, useTreeSet);
		for (K key : keySet()) {
			for (V value : get(key, true)) {
				ret.put(value, key);
			}
		}
		return ret;
	}

	public Set<K> keySet() {
		return entries.keySet();
	}

	public void put(K key, Set<V> values) {
		for (V value : values) {
			put(key, value);
		}
	}

	public void put(K key, V value) {
		ensure(key).add(value);
	}

	public Set<V> remove(K key) {
		return entries.remove(key);
	}

	public int size() {
		return entries.size();
	}

	public String toString() {
		return toString(100, 20);
	}

	public String toString(int num_print_keys, int num_print_values) {
		StringBuffer sb = new StringBuffer();
		int numKeys = 0;
		for (K key : entries.keySet()) {
			if (++numKeys > num_print_keys) {
				break;
			}

			sb.append(key.toString() + " => ");
			Set<V> set = entries.get(key);
			int size = set.size();
			int numValues = 0;
			for (V value : set) {
				sb.append(value.toString() + (++numValues >= size ? "" : ", "));
				if (numValues > num_print_keys) {
					sb.append("...");
					break;
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

}
