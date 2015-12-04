package ohs.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import fig.basic.Pair;

/**
 * Maintains counts of (key, value) pairs. The map is structured so that for every key, one can get a counter over values. Example usage:
 * keys might be words with values being POS tags, and the count being the number of occurences of that word/tag pair. The sub-counters
 * returned by getCounter(word) would be count distributions over tags for that word.
 * 
 * @author Dan Klein
 */
public class CounterMap<K, V> implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		CounterMap<String, String> bigramCounterMap = new CounterMap<String, String>();
		bigramCounterMap.incrementCount("people", "run", 1);
		bigramCounterMap.incrementCount("cats", "growl", 2);
		bigramCounterMap.incrementCount("cats", "scamper", 3);
		System.out.println(bigramCounterMap);
		System.out.println("Entries for cats: " + bigramCounterMap.getCounter("cats"));
		System.out.println("Entries for dogs: " + bigramCounterMap.getCounter("dogs"));
		System.out.println("Count of cats scamper: " + bigramCounterMap.getCount("cats", "scamper"));
		System.out.println("Count of snakes slither: " + bigramCounterMap.getCount("snakes", "slither"));
		System.out.println("Total size: " + bigramCounterMap.totalSize());
		System.out.println("Total count: " + bigramCounterMap.totalCount());
		System.out.println(bigramCounterMap);
	}

	private Map<K, Counter<V>> counterMap;

	public CounterMap() {
		counterMap = new HashMap<K, Counter<V>>();
	}

	public CounterMap(CounterMap<K, V> cm) {
		this();
		incrementAll(cm);
	}

	/**
	 * Finds the key with maximum count. This is a linear operation, and ties are broken arbitrarily.
	 * 
	 * @return a key with minumum count
	 */
	public Pair<K, V> argMax() {
		double maxCount = Double.NEGATIVE_INFINITY;
		Pair<K, V> maxKey = null;
		for (Map.Entry<K, Counter<V>> entry : counterMap.entrySet()) {
			Counter<V> counter = entry.getValue();
			V localMax = counter.argMax();
			if (counter.getCount(localMax) > maxCount || maxKey == null) {
				maxKey = new Pair<K, V>(entry.getKey(), localMax);
				maxCount = counter.getCount(localMax);
			}
		}
		return maxKey;
	}

	// public void setCount(Pair<K,V> pair) {
	//
	// }

	public void clear() {
		for (Counter<V> c : counterMap.values()) {
			c.clear();
		}
		counterMap.clear();
	}

	public boolean containsKey(K key) {
		return counterMap.containsKey(key);
	}

	protected Counter<V> ensureCounter(K key) {
		Counter<V> valueCounter = counterMap.get(key);
		if (valueCounter == null) {
			valueCounter = new Counter<V>();
			counterMap.put(key, valueCounter);
		}
		return valueCounter;
	}

	/**
	 * Gets the total count of the given key, or zero if that key is not present. Does not create any objects.
	 */
	public double getCount(K key) {
		Counter<V> valueCounter = counterMap.get(key);
		if (valueCounter == null)
			return 0.0;
		return valueCounter.totalCount();
	}

	/**
	 * Gets the count of the given (key, value) entry, or zero if that entry is not present. Does not create any objects.
	 */
	public double getCount(K key, V value) {
		Counter<V> valueCounter = counterMap.get(key);
		if (valueCounter == null)
			return 0.0;
		return valueCounter.getCount(value);
	}

	/**
	 * Gets the sub-counter for the given key. If there is none, a counter is created for that key, and installed in the CounterMap. You
	 * can, for example, add to the returned empty counter directly (though you shouldn't). This is so whether the key is present or not,
	 * modifying the returned counter has the same effect (but don't do it).
	 */
	public Counter<V> getCounter(K key) {
		return ensureCounter(key);
	}

	public Set<Map.Entry<K, Counter<V>>> getEntrySet() {
		// TODO Auto-generated method stub
		return counterMap.entrySet();
	}

	public Counter<K> getInnerCountSums() {
		Counter<K> ret = new Counter<K>();
		for (K key : counterMap.keySet()) {
			ret.setCount(key, counterMap.get(key).totalCount());
		}
		return ret;
	}

	public Iterator<Pair<K, V>> getPairIterator() {

		class PairIterator implements Iterator<Pair<K, V>> {

			Iterator<K> outerIt;
			Iterator<V> innerIt;
			K curKey;

			public PairIterator() {
				outerIt = keySet().iterator();
			}

			private boolean advance() {
				if (innerIt == null || !innerIt.hasNext()) {
					if (!outerIt.hasNext()) {
						return false;
					}
					curKey = outerIt.next();
					innerIt = getCounter(curKey).keySet().iterator();
				}
				return true;
			}

			public boolean hasNext() {
				return advance();
			}

			public Pair<K, V> next() {
				advance();
				assert curKey != null;
				return Pair.newPair(curKey, innerIt.next());
			}

			public void remove() {
				// TODO Auto-generated method stub

			}

		}
		;
		return new PairIterator();
	}

	public void incrementAll(CounterMap<K, V> cMap) {
		for (Map.Entry<K, Counter<V>> entry : cMap.counterMap.entrySet()) {
			K key = entry.getKey();
			Counter<V> innerCounter = entry.getValue();
			for (Map.Entry<V, Double> innerEntry : innerCounter.entrySet()) {
				V value = innerEntry.getKey();
				incrementCount(key, value, innerEntry.getValue());
			}
		}
	}

	public void incrementAll(CounterMap<K, V> cMap, double scaleFactor) {
		for (Map.Entry<K, Counter<V>> entry : cMap.counterMap.entrySet()) {
			K key = entry.getKey();
			Counter<V> innerCounter = entry.getValue();
			for (Map.Entry<V, Double> innerEntry : innerCounter.entrySet()) {
				V value = innerEntry.getKey();
				incrementCount(key, value, scaleFactor * innerEntry.getValue());
			}
		}
	}

	public void incrementAll(Map<K, V> map, double count) {
		for (Map.Entry<K, V> entry : map.entrySet()) {
			incrementCount(entry.getKey(), entry.getValue(), count);
		}
	}

	/**
	 * Increments the count for a particular (key, value) pair.
	 */
	public void incrementCount(K key, V value, double count) {
		Counter<V> valueCounter = ensureCounter(key);
		valueCounter.incrementCount(value, count);
	}

	/**
	 * Constructs reverse CounterMap where the count of a pair (k,v) is the count of (v,k) in the current CounterMap
	 * 
	 * @return
	 */
	public CounterMap<V, K> invert() {
		CounterMap<V, K> invertCounterMap = new CounterMap<V, K>();
		for (K key : this.keySet()) {
			Counter<V> keyCounts = this.getCounter(key);
			for (V val : keyCounts.keySet()) {
				double count = keyCounts.getCount(val);
				invertCounterMap.setCount(val, key, count);
			}
		}
		return invertCounterMap;
	}

	/**
	 * True if there are no entries in the CounterMap (false does not mean totalCount > 0)
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean isEqualTo(CounterMap<K, V> map) {
		boolean tmp = true;
		CounterMap<K, V> bigger = map.size() > size() ? map : this;
		for (K k : bigger.keySet()) {
			tmp &= map.getCounter(k).isEqualTo(getCounter(k));
		}
		return tmp;
	}

	/**
	 * Returns the keys that have been inserted into this CounterMap.
	 */
	public Set<K> keySet() {
		return counterMap.keySet();
	}

	public void normalize() {
		for (K key : keySet()) {
			getCounter(key).normalize();
		}
	}

	public void normalizeWithDiscount(double discount) {
		for (K key : keySet()) {
			Counter<V> ctr = getCounter(key);
			double totalCount = ctr.totalCount();
			for (V value : ctr.keySet()) {
				ctr.setCount(value, (ctr.getCount(value) - discount) / totalCount);
			}
		}
	}

	public Set<V> outerKeySet() {
		Set<V> ret = new HashSet<V>();
		for (Counter<V> c : counterMap.values()) {
			ret.addAll(c.keySet());
		}
		return ret;
	}

	public void prune(final Set<K> toRemove) {
		for (final K e : toRemove) {
			removeKey(e);
		}
	}

	public void pruneExcept(final Set<K> toKeep) {
		final List<K> toRemove = new ArrayList<K>();
		for (final K key : counterMap.keySet()) {
			if (!toKeep.contains(key))
				toRemove.add(key);
		}
		for (final K e : toRemove) {
			removeKey(e);
		}
	}

	public void removeKey(K oldIndex) {
		counterMap.remove(oldIndex);

	}

	/**
	 * Scale all entries in <code>CounterMap</code> by <code>scaleFactor</code>
	 * 
	 * @param scaleFactor
	 */
	public void scale(double scaleFactor) {
		for (K key : keySet()) {
			Counter<V> counts = getCounter(key);
			counts.scale(scaleFactor);
		}
	}

	/**
	 * Sets the count for a particular (key, value) pair.
	 */
	public void setCount(K key, V value, double count) {
		Counter<V> valueCounter = ensureCounter(key);
		valueCounter.setCount(value, count);
	}

	public void setCounter(K newIndex, Counter<V> counter) {
		counterMap.put(newIndex, counter);

	}

	/**
	 * The number of keys in this CounterMap (not the number of key-value entries -- use totalSize() for that)
	 */
	public int size() {
		return counterMap.size();
	}

	public int sizeOfMaxInnerCounter() {
		int ret = 0;
		for (Counter<V> c : counterMap.values()) {
			if (ret < c.size()) {
				ret = c.size();
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		return toString(50, 50);
	}

	public String toString(int numPrintRows, int numPrintColumns) {
		StringBuilder sb = new StringBuilder("[\n");
		int numKeys = 0;

		for (K key : getInnerCountSums().getSortedKeys()) {
			Counter<V> inner = counterMap.get(key);
			if (++numKeys > numPrintRows) {
				sb.append("...\n");
				break;
			}
			sb.append(String.format("   %s:%d -> ", key, (int) inner.totalCount()));
			sb.append(inner.toStringSortedByValues(true, false, numPrintColumns));
			sb.append("\n");
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Returns the total of all counts in sub-counters. This implementation is linear; it recalculates the total each time.
	 */
	public double totalCount() {
		double total = 0.0;
		for (Map.Entry<K, Counter<V>> entry : counterMap.entrySet()) {
			Counter<V> counter = entry.getValue();
			total += counter.totalCount();
		}
		return total;
	}

	/**
	 * Returns the total number of (key, value) entries in the CounterMap (not their total counts).
	 */
	public int totalSize() {
		int total = 0;
		for (Map.Entry<K, Counter<V>> entry : counterMap.entrySet()) {
			Counter<V> counter = entry.getValue();
			total += counter.size();
		}
		return total;
	}

}
