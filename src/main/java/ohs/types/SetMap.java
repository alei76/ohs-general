package ohs.types;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import ohs.utils.Generics;

public class SetMap<K, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7366754514015096846L;

	protected Map<K, Set<V>> entries;

	private Generics.SetType st;

	public SetMap() {
		this(100, Generics.MapType.HASH_MAP, Generics.SetType.HASH_SET);
	}

	public SetMap(Generics.MapType mt, Generics.SetType st) {
		this(100, mt, st);
	}

	public SetMap(int size, Generics.MapType mt, Generics.SetType st) {
		entries = Generics.newMap(mt, size);
		this.st = st;
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
			set = Generics.newSet(st);
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
