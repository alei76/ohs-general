package ohs.types;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DeepMap<K, E, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7366754514015096846L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

	protected Map<K, Map<E, V>> entries;

	private boolean useTreeMap;

	public DeepMap() {
		this(false);
	}

	public DeepMap(boolean useTreeMap) {
		this.useTreeMap = useTreeMap;
		entries = useTreeMap ? new TreeMap<K, Map<E, V>>() : new HashMap<K, Map<E, V>>();
	}

	public boolean containsKey(K key) {
		return entries.containsKey(key);
	}

	public boolean containsKeys(K key1, E key2) {
		Map<E, V> map = entries.get(key1);
		if (map == null) {
			return false;
		}
		if (!map.containsKey(key2)) {
			return false;
		}
		return true;
	}

	protected Map<E, V> ensure(K key) {
		Map<E, V> map = entries.get(key);
		if (map == null) {
			map = useTreeMap ? new TreeMap<E, V>() : new HashMap<E, V>();
			entries.put(key, map);
		}
		return map;
	}

	public Map<E, V> get(K key) {
		return get(key, true);
	}

	public Map<E, V> get(K key, boolean createIfAbsent) {
		return createIfAbsent ? ensure(key) : entries.get(key);
	}

	public V get(K key, E elem, boolean createIfAbsent) {
		return get(key, createIfAbsent).get(elem);
	}

	public Set<K> keySet() {
		return entries.keySet();
	}

	public Set<E> keySet(K key) {
		return entries.get(key).keySet();
	}

	public void put(K key, E elem, V value) {
		Map<E, V> map = ensure(key);
		map.put(elem, value);
	}

	public void put(K key, Map<E, V> map) {
		entries.put(key, map);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (K key : entries.keySet()) {
			sb.append(key.toString() + " => ");
			Map<E, V> map = entries.get(key);
			int size = map.size();
			int num = 0;
			for (E elem : map.keySet()) {
				V value = map.get(elem);
				sb.append(elem.toString() + ":" + value.toString() + (++num >= size ? "" : " "));
			}
			sb.append("\n");
		}

		return sb.toString();
	}

}
