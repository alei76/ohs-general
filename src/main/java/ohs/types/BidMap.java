package ohs.types;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BidMap<K, V> implements Serializable {

	private Map<K, V> keyToValue;
	private Map<V, K> valueToKey;

	public BidMap() {
		this(false);
	}

	public BidMap(boolean useTreeMap) {
		keyToValue = useTreeMap ? new TreeMap<K, V>() : new HashMap<K, V>();
		valueToKey = useTreeMap ? new TreeMap<V, K>() : new HashMap<V, K>();
	}

	public BidMap(Map<K, V> keyToValue, Map<V, K> valueToKey) {
		this.keyToValue = keyToValue;
		this.valueToKey = valueToKey;
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
