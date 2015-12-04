package ohs.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ListMap<K, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7366754514015096846L;

	protected Map<K, List<V>> entries;

	public ListMap() {
		this(false);
	}

	public ListMap(boolean useTreeMap) {
		entries = useTreeMap ? new TreeMap<K, List<V>>() : new HashMap<K, List<V>>();
	}

	public boolean containsKey(K key) {
		return entries.containsKey(key);
	}

	protected List<V> ensure(K key) {
		List<V> list = entries.get(key);
		if (list == null) {
			list = new ArrayList<V>();
			entries.put(key, list);
		}
		return list;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ListMap other = (ListMap) obj;
		if (entries == null) {
			if (other.entries != null)
				return false;
		} else if (!entries.equals(other.entries))
			return false;
		return true;
	}

	public List<V> get(K key) {
		return get(key, true);
	}

	public List<V> get(K key, boolean createIfAbsent) {
		return createIfAbsent ? ensure(key) : entries.get(key);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entries == null) ? 0 : entries.hashCode());
		return result;
	}

	public Set<K> keySet() {
		return entries.keySet();
	}

	public int keySize() {
		return entries.size();
	}

	public void put(K key, V value) {
		ensure(key).add(value);
	}

	public List<V> remove(K key) {
		return entries.remove(key);
	}

	public void set(K key, List<V> values) {
		entries.put(key, values);
	}

	public int size() {
		return entries.size();
	}

	public int sizeOfEntries() {
		int ret = 0;
		for (K key : entries.keySet()) {
			ret += entries.get(key).size();
		}
		return ret;
	}

	public String toString() {
		return toString(20);
	}

	public String toString(int printSize) {
		StringBuffer sb = new StringBuffer();
		for (K key : entries.keySet()) {
			sb.append(key.toString() + " => ");
			List<V> List = entries.get(key);
			int size = List.size();
			int numElems = 0;
			for (V value : List) {
				sb.append(value.toString() + (++numElems >= size ? "" : ", "));
				if (numElems > printSize) {
					sb.append("...");
					break;
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public int totalEntrySize() {
		int ret = 0;
		for (K key : entries.keySet()) {
			ret += entries.get(key).size();
		}
		return ret;
	}

}
