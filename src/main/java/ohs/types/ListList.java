package ohs.types;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import ohs.utils.Generics;
import ohs.utils.Generics.ListType;

public class ListList<V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7366754514015096846L;

	private List<List<V>> entries;

	private ListType lt2;

	private ListType lt1;

	public ListList() {
		this(10000, Generics.ListType.ARRAY_LIST, Generics.ListType.ARRAY_LIST);
	}

	public ListList(int size, ListType lt1, ListType lt2) {
		entries = Generics.newList(lt1, size);

		this.lt1 = lt1;
		this.lt2 = lt2;
	}

	public void add(int i, List<V> values) {
		ensure(i).addAll(values);
	}

	public void add(int i, V value) {
		ensure(i).add(value);
	}

	public void add(List<V> values) {
		entries.add(values);
	}

	public void set(int i, List<V> values) {
		ensure(i);
		entries.set(i, values);
	}

	public void clear() {
		Iterator<List<V>> iter = entries.iterator();
		while (iter.hasNext()) {
			List<V> l = iter.next();
			l.clear();

		}
		entries.clear();
	}

	private List<V> ensure(int i) {
		if (entries.size() < i) {
			int size = i - entries.size();
			for (int j = 0; j < size; j++) {
				entries.add(Generics.newArrayList());
			}
		}
		return entries.get(i);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ListList other = (ListList) obj;
		if (entries == null) {
			if (other.entries != null)
				return false;
		} else if (!entries.equals(other.entries))
			return false;
		return true;
	}

	public List<V> get(int i, boolean addIfUnseen) {
		return addIfUnseen ? ensure(i) : entries.get(i);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entries == null) ? 0 : entries.hashCode());
		return result;
	}

	public int size() {
		return entries.size();
	}

	public int sizeOfEntries() {
		int ret = 0;
		for (List<V> l : entries) {
			ret += l.size();
		}
		return ret;
	}

	@Override
	public String toString() {
		return toString(20);
	}

	public String toString(int print_size) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < entries.size(); i++) {
			if ((i + 1) == print_size) {
				break;
			}

			List<V> l = entries.get(i);
			sb.append(String.format("%d: %s", i, l));
			if (i != entries.size() - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public void trimToSize() {
		List<List<V>> temp = Generics.newList(lt1, entries.size());

		for (List<V> l : entries) {
			List<V> nl = Generics.newList(lt2, l.size());

			for (V v : l) {
				nl.add(v);
			}
			temp.add(nl);

			l.clear();
			l = null;
		}
		entries = temp;
	}

}
