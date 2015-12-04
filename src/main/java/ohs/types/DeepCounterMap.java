package ohs.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DeepCounterMap<K, V, F> implements Serializable {

	private Map<K, CounterMap<V, F>> dcm;

	public DeepCounterMap() {
		dcm = new HashMap<K, CounterMap<V, F>>();
	}

	public DeepCounterMap(DeepCounterMap<K, V, F> dcm) {
		incrementAll(dcm);
	}

	// public void setCount(Pair<K,V> pair) {
	//
	// }

	/**
	 * Finds the key with maximum count. This is a linear operation, and ties are broken arbitrarily.
	 * 
	 * @return a key with minumum count
	 */
	public Triple<K, V, F> argMax() {
		double maxCount = Double.NEGATIVE_INFINITY;
		Triple<K, V, F> maxKey = null;
		for (Map.Entry<K, CounterMap<V, F>> e1 : dcm.entrySet()) {
			for (Map.Entry<V, Counter<F>> e2 : e1.getValue().getEntrySet()) {
				Counter<F> counter = e2.getValue();
				F localMax = counter.argMax();
				if (counter.getCount(localMax) > maxCount || maxKey == null) {
					maxKey = new Triple<K, V, F>(e1.getKey(), e2.getKey(), localMax);
					maxCount = counter.getCount(localMax);
				}
			}
		}

		return maxKey;
	}

	public boolean containsKey(K key) {
		return dcm.containsKey(key);
	}

	protected CounterMap<V, F> ensureCounterMap(K key) {
		CounterMap<V, F> cm = dcm.get(key);
		if (cm == null) {
			cm = new CounterMap<V, F>();
			dcm.put(key, cm);
		}
		return cm;
	}

	/**
	 * Gets the total count of the given key, or zero if that key is not present. Does not create any objects.
	 */
	public double getCount(K key) {
		CounterMap<V, F> cm = dcm.get(key);
		if (cm == null)
			return 0.0;
		return cm.totalCount();
	}

	/**
	 * Gets the count of the given (key, value) entry, or zero if that entry is not present. Does not create any objects.
	 */
	public double getCount(K key1, V key2) {
		Counter<F> cm = dcm.get(key1).getCounter(key2);
		if (cm == null)
			return 0.0;
		return cm.totalCount();
	}

	/**
	 * Gets the sub-counter for the given key. If there is none, a counter is created for that key, and installed in the CounterMap. You
	 * can, for example, add to the returned empty counter directly (though you shouldn't). This is so whether the key is present or not,
	 * modifying the returned counter has the same effect (but don't do it).
	 */
	public CounterMap<V, F> getCounterMap(K key) {
		return ensureCounterMap(key);
	}

	public Set<Entry<K, CounterMap<V, F>>> getEntrySet() {
		return dcm.entrySet();
	}

	public Counter<K> getInnerCountSums() {
		Counter<K> ret = new Counter<K>();
		for (K k1 : dcm.keySet()) {
			ret.setCount(k1, dcm.get(k1).totalCount());
		}
		return ret;
	}

	public void incrementAll(DeepCounterMap<K, V, F> dmc) {
		for (Map.Entry<K, CounterMap<V, F>> e1 : dmc.getEntrySet()) {
			K k1 = e1.getKey();
			CounterMap<V, F> cm = e1.getValue();
			for (Map.Entry<V, Counter<F>> e2 : cm.getEntrySet()) {
				V k2 = e2.getKey();
				Counter<F> c = e2.getValue();
				for (Entry<F, Double> e3 : c.getEntrySet()) {
					F k3 = e3.getKey();
					incrementCount(k1, k2, k3, e3.getValue());
				}
			}
		}
	}

	/**
	 * Increments the count for a particular (key, value) pair.
	 */
	public void incrementCount(K key1, V key2, F key3, double count) {
		ensureCounterMap(key1).incrementCount(key2, key3, count);
	}

	/**
	 * True if there are no dcm in the CounterMap (false does not mean totalCount > 0)
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Returns the keys that have been inserted into this CounterMap.
	 */
	public Set<K> keySet() {
		return dcm.keySet();
	}

	public void normalize() {
		for (K k1 : keySet()) {
			CounterMap<V, F> cm = getCounterMap(k1);
			for (V k2 : cm.keySet()) {
				cm.getCounter(k2).normalize();
			}
		}
	}

	public void prune(final Set<K> toRemove) {
		for (final K e : toRemove) {
			removeKey(e);
		}
	}

	public void pruneExcept(final Set<K> toKeep) {
		final List<K> toRemove = new ArrayList<K>();
		for (final K key : dcm.keySet()) {
			if (!toKeep.contains(key))
				toRemove.add(key);
		}
		for (final K e : toRemove) {
			removeKey(e);
		}
	}

	public void removeKey(K oldIndex) {
		dcm.remove(oldIndex);

	}

	/**
	 * Scale all dcm in <code>CounterMap</code> by <code>scaleFactor</code>
	 * 
	 * @param scaleFactor
	 */
	public void scale(double scaleFactor) {
		for (K k1 : keySet()) {
			CounterMap<V, F> cm = getCounterMap(k1);
			for (V k2 : cm.keySet()) {
				cm.getCounter(k2).scale(scaleFactor);
			}
		}
	}

	/**
	 * Sets the count for a particular (key, value) pair.
	 */
	public void setCount(K key1, V key2, F key3, double count) {
		CounterMap<V, F> cm = ensureCounterMap(key1);
		cm.setCount(key2, key3, count);
	}

	public void setCounter(K newIndex, CounterMap<V, F> cm) {
		dcm.put(newIndex, cm);

	}

	/**
	 * The number of keys in this CounterMap (not the number of key-value dcm -- use totalSize() for that)
	 */
	public int size() {
		return dcm.size();
	}

	@Override
	public String toString() {
		return toString(50, 50, 50);
	}

	public String toString(int num_key1, int num_key2, int num_key3) {
		StringBuilder sb = new StringBuilder("[\n");
		int numKeys = 0;

		for (K k1 : getInnerCountSums().getSortedKeys()) {
			CounterMap<V, F> cm = dcm.get(k1);
			if (++numKeys > num_key1) {
				break;
			}

			for (V k2 : cm.getInnerCountSums().getSortedKeys()) {
				Counter<F> c = cm.getCounter(k2);
				if (++numKeys > num_key2) {
					break;
				}
				sb.append(String.format("%s:%d\t%s:%d -> ", k1, (int) cm.totalCount(), k2, (int) c.totalCount()));
				sb.append(c.toStringSortedByValues(true, false, num_key3));
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Returns the total of all counts in sub-counters. This implementation is linear; it recalculates the total each time.
	 */
	public double totalCount() {
		double total = 0.0;
		for (Map.Entry<K, CounterMap<V, F>> entry : dcm.entrySet()) {
			total += entry.getValue().totalCount();
		}
		return total;
	}

	/**
	 * Returns the total number of (key, value) dcm in the CounterMap (not their total counts).
	 */
	public int totalSize() {
		int total = 0;
		for (Map.Entry<K, CounterMap<V, F>> e1 : dcm.entrySet()) {
			for (Map.Entry<V, Counter<F>> e2 : e1.getValue().getEntrySet()) {
				total += e2.getValue().size();
			}
		}
		return total;
	}

}
