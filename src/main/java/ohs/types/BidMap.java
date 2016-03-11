package ohs.types;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ohs.utils.Generics;

public class BidMap<K, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 263626040745113538L;
	private Map<K, V> keyToValue;
	private Map<V, K> valueToKey;

	public BidMap() {
		this(Generics.MapType.HASH_MAP, 1000);
	}

	public BidMap(Generics.MapType mt, int size) {
		keyToValue = Generics.newMap(mt, size);
		valueToKey = Generics.newMap(mt, size);
	}

	public BidMap(int size) {
		this(Generics.MapType.HASH_MAP, size);
	}

	public BidMap(Map<K, V> keyToValue, Map<V, K> valueToKey) {
		this.keyToValue = keyToValue;
		this.valueToKey = valueToKey;
	}

	public boolean contains(K key, V value) {
		return keyToValue.containsKey(key) && valueToKey.containsKey(value);
	}

	public K getKey(V value) {
		return valueToKey.get(value);
	}

	public Set<K> getKeys() {
		return keyToValue.keySet();
	}

	public Map<K, V> getKeyToValue() {
		return keyToValue;
	}

	public V getValue(K key) {
		return keyToValue.get(key);
	}

	public Set<V> getValues() {
		return valueToKey.keySet();
	}

	public Map<V, K> getValueToKey() {
		return valueToKey;
	}

	public void put(K key, V value) {
		keyToValue.put(key, value);
		valueToKey.put(value, key);
	}

	public int size() {
		return keyToValue.size();
	}

	@Override
	public String toString() {
		return toString(keyToValue.size());
	}

	public String toString(int num_print_keys) {
		StringBuffer sb = new StringBuffer();

		Iterator<K> iter = keyToValue.keySet().iterator();
		int cnt = 0;

		sb.append(String.format("entry size:\t%d", keyToValue.size()));
		sb.append(String.format("\nNo\tKey\tValue"));

		while (iter.hasNext() && cnt++ < num_print_keys) {
			K key = iter.next();
			V value = keyToValue.get(key);
			sb.append(String.format("\n%d\t%s\t%s", cnt, key.toString(), value.toString()));
		}
		return sb.toString();
	}
}
