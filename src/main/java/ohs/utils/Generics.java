package ohs.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.DeepMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.Pair;
import ohs.types.SetMap;
import ohs.types.Triple;

/**
 * from Stanford Core NLP
 * 
 * A collection of utilities to make dealing with Java generics less painful and
 * verbose. For example, rather than declaring
 *
 * <pre>
 * {@code  Map<String,ClassicCounter<List<String>>> = new HashMap<String,ClassicCounter<List<String>>>()}
 * </pre>
 *
 * you just call <code>Generics.newHashMap()</code>:
 *
 * <pre>
 * {@code Map<String,ClassicCounter<List<String>>> = Generics.newHashMap()}
 * </pre>
 *
 * Java type-inference will almost always just <em>do the right thing</em>
 * (every once in a while, the compiler will get confused before you do, so you
 * might still occasionally have to specify the appropriate types).
 *
 * This class is based on the examples in Brian Goetz's article
 * <a href="http://www.ibm.com/developerworks/library/j-jtp02216.html">Java
 * theory and practice: The pseudo-typedef antipattern</a>.
 *
 * @author Ilya Sherman
 */
public class Generics {

	public static enum ListType {
		ARRAY_LIST, LINKED_LIST;
	}

	public static enum MapType {
		HASH_MAP, TREE_MAP, WEAK_HASH_MAP, IDENTIY_HASH_MAP;
	}

	public static enum SetType {
		HASH_SET, TREE_SET, WEAK_HASH_SET, IDENTITY_HASH_SET;
	}

	/* Collections */
	public static <E> ArrayList<E> newArrayList() {
		return new ArrayList<E>();
	}

	public static <E> ArrayList<E> newArrayList(Collection<? extends E> c) {
		return new ArrayList<E>(c);
	}

	public static <E> ArrayList<E> newArrayList(int size) {
		return new ArrayList<E>(size);
	}

	public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
		return new ConcurrentHashMap<K, V>();
	}

	public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(int initialCapacity) {
		return new ConcurrentHashMap<K, V>(initialCapacity);
	}

	public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(int initialCapacity, float loadFactor,
			int concurrencyLevel) {
		return new ConcurrentHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel);
	}

	public static <E> Counter<E> newCounter() {
		return new Counter<E>();
	}

	public static <E> Counter<E> newCounter(Counter<? extends E> c) {
		return new Counter<E>(c);
	}

	public static <E> Counter<E> newCounter(int size) {
		return new Counter<E>(size);
	}

	public static <K, V> CounterMap<K, V> newCounterMap() {
		return new CounterMap<K, V>();
	}

	public static <K, V> CounterMap<K, V> newCounterMap(CounterMap<K, V> cm) {
		return new CounterMap<K, V>(cm);
	}

	public static <K, V> CounterMap<K, V> newCounterMap(int size) {
		return new CounterMap<K, V>(size);
	}

	public static <K, V> ListMap<K, V> newListMap() {
		return new ListMap<K, V>();
	}

	public static <K, V> ListMap<K, V> newListMap(int size) {
		return new ListMap<K, V>(size, MapType.HASH_MAP, ListType.ARRAY_LIST);
	}

	public static <K, V> SetMap<K, V> newSetMap() {
		return new SetMap<K, V>();
	}

	public static <K, V> SetMap<K, V> newSetMap(int size) {
		return new SetMap<K, V>(size, MapType.HASH_MAP, SetType.HASH_SET);
	}

	public static <K, E, V> DeepMap<K, E, V> newDeepMap() {
		return new DeepMap<K, E, V>();
	}

	public static <K, E, V> DeepMap<K, E, V> newDeepMap(int size) {
		return new DeepMap<K, E, V>(size, MapType.HASH_MAP, MapType.HASH_MAP);
	}

	/* Maps */
	public static <K, V> Map<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	public static <K, V> Map<K, V> newHashMap(int initialCapacity) {
		return new HashMap<K, V>(initialCapacity);
	}

	public static <K, V> Map<K, V> newHashMap(Map<? extends K, ? extends V> m) {
		return new HashMap<K, V>(m);
	}

	public static <E> Set<E> newHashSet() {
		return new HashSet<E>();
	}

	public static <E> Set<E> newHashSet(Collection<? extends E> c) {
		return new HashSet<E>(c);
	}

	public static <E> Set<E> newHashSet(int initialCapacity) {
		return new HashSet<E>(initialCapacity);
	}

	public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
		return new IdentityHashMap<K, V>();
	}

	public static <K, V> IdentityHashMap<K, V> newIdentityHashMap(int size) {
		return new IdentityHashMap<K, V>(size);
	}

	public static <K> Set<K> newIdentityHashSet() {
		return Collections.newSetFromMap(Generics.<K, Boolean> newIdentityHashMap());
	}

	public static <K> Set<K> newIdentityHashSet(int size) {
		return Collections.newSetFromMap(Generics.<K, Boolean> newIdentityHashMap(size));
	}

	public static <T> Indexer<T> newIndexer() {
		return new Indexer<T>();
	}

	public static <E> LinkedList<E> newLinkedList() {
		return new LinkedList<E>();
	}

	public static <E> LinkedList<E> newLinkedList(Collection<? extends E> c) {
		return new LinkedList<E>(c);
	}

	public static <E> List<E> newList(ListType t) {
		return newList(t, 0);
	}

	public static <E> List<E> newList(ListType t, int size) {
		List<E> ret = null;
		if (t == ListType.ARRAY_LIST) {
			ret = size > 0 ? newArrayList(size) : newArrayList();
		} else if (t == ListType.LINKED_LIST) {
			ret = newLinkedList();
		}
		return ret;
	}

	public static <K, V> Map<K, V> newMap(MapType mt) {
		return newMap(mt, 0);
	}

	public static <K, V> Map<K, V> newMap(MapType mt, int size) {
		Map<K, V> ret = null;
		if (mt == MapType.HASH_MAP) {
			ret = size > 0 ? newHashMap(size) : newHashMap();
		} else if (mt == MapType.TREE_MAP) {
			ret = newTreeMap();
		} else if (mt == MapType.WEAK_HASH_MAP) {
			ret = size > 0 ? newWeakHashMap(size) : newWeakHashMap();
		} else if (mt == MapType.IDENTIY_HASH_MAP) {
			ret = size > 0 ? newIdentityHashMap(size) : newIdentityHashMap();
		}
		return ret;
	}

	/* Other */
	public static <T1, T2> Pair<T1, T2> newPair(T1 first, T2 second) {
		return new Pair<T1, T2>(first, second);
	}

	public static <E> Set<E> newSet(SetType t) {
		return newSet(t, 0);
	}

	public static <E> Set<E> newSet(SetType t, int size) {
		Set<E> ret = null;
		if (t == SetType.HASH_SET) {
			ret = size > 0 ? newHashSet(size) : newHashSet();
		} else if (t == SetType.TREE_SET) {
			ret = newTreeSet();
		} else if (t == SetType.IDENTITY_HASH_SET) {
			ret = size > 0 ? newIdentityHashSet(size) : newIdentityHashSet();
		}
		return ret;
	}

	public static <E> Stack<E> newStack() {
		return new Stack<E>();
	}

	public static <K, V> TreeMap<K, V> newTreeMap() {
		return new TreeMap<K, V>();
	}

	public static <E> TreeSet<E> newTreeSet() {
		return new TreeSet<E>();
	}

	public static <E> TreeSet<E> newTreeSet(Comparator<? super E> comparator) {
		return new TreeSet<E>(comparator);
	}

	public static <E> TreeSet<E> newTreeSet(SortedSet<E> s) {
		return new TreeSet<E>(s);
	}

	// public static <E> Index<E> newIndex() {
	// return new HashIndex<E>();
	// }

	public static <T1, T2, T3> Triple<T1, T2, T3> newTriple(T1 first, T2 second, T3 third) {
		return new Triple<T1, T2, T3>(first, second, third);
	}

	public static <K, V> WeakHashMap<K, V> newWeakHashMap() {
		return new WeakHashMap<K, V>();
	}

	public static <K, V> WeakHashMap<K, V> newWeakHashMap(int size) {
		return new WeakHashMap<K, V>(size);
	}

	public static <T> WeakReference<T> newWeakReference(T referent) {
		return new WeakReference<T>(referent);
	}

	// public static <T> Interner<T> newInterner() {
	// return new Interner<T>();
	// }

	// public static <T> SynchronizedInterner<T>
	// newSynchronizedInterner(Interner<T> interner) {
	// return new SynchronizedInterner<T>(interner);
	// }
	//
	// public static <T> SynchronizedInterner<T>
	// newSynchronizedInterner(Interner<T> interner, Object mutex) {
	// return new SynchronizedInterner<T>(interner, mutex);
	// }

	private Generics() {
	} // static class
}
