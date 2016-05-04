package ohs.types;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.utils.Generics;
import ohs.utils.Generics.ListType;
import ohs.utils.Generics.MapType;

public class ListMap<K, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7366754514015096846L;

	protected Map<K, List<V>> entries;

	private ListType lt;

	private MapType mt;

	public ListMap() {
		this(10000, Generics.MapType.HASH_MAP, Generics.ListType.ARRAY_LIST);
	}

	public void trimToSize() {
		Map<K, List<V>> temp = Generics.newMap(mt, entries.size());

		for (K k : entries.keySet()) {
			List<V> l = entries.get(k);
			List<V> nl = Generics.newArrayList(l.size());

			for (V v : l) {
				nl.add(v);
			}
			temp.put(k, nl);

			l.clear();
			l = null;
		}
		entries = temp;
	}

	public ListMap(int size, MapType mt, ListType lt) {
		entries = Generics.newMap(mt, size);

		this.mt = mt;
		this.lt = lt;
	}

	public void clear() {
		Iterator<K> iter = entries.keySet().iterator();
		while (iter.hasNext()) {
			K key = iter.next();
			entries.get(key).clear();
		}
		entries.clear();
	}

	public boolean containsKey(K key) {
		return entries.containsKey(key);
	}

	protected List<V> ensure(K key) {
		List<V> list = entries.get(key);
		if (list == null) {
			list = Generics.newList(lt);
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

	public List<V> get(K key, boolean addIfUnseen) {
		return addIfUnseen ? ensure(key) : entries.get(key);
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

	public void put(K key, List<V> values) {
		entries.put(key, values);
	}

	public void put(K key, V value) {
		ensure(key).add(value);
	}

	public List<V> remove(K key) {
		return entries.remove(key);
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

	@Override
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
