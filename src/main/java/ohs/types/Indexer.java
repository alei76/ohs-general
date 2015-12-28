package ohs.types;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains a two-way map between a set of objects and contiguous integers from 0 to the number of objects. Use get(i) to look up object i,
 * and indexOf(object) to look up the index of an object.
 * 
 * @author Dan Klein
 */
public class Indexer<E> extends AbstractList<E> implements Serializable {
	private static final long serialVersionUID = -8769544079136550516L;
	protected List<E> objects;
	protected Map<E, Integer> indexes;

	public Indexer() {
		this(100);
	}

	public Indexer(int size) {
		objects = new ArrayList<E>(size);
		indexes = new HashMap<E, Integer>(size);
	}

	public Indexer(Collection<? extends E> c) {
		this();
		for (E a : c)
			getIndex(a);
	}

	/**
	 * @author aria42
	 */
	@Override
	public boolean add(E elem) {
		if (contains(elem)) {
			return false;
		}
		indexes.put(elem, size());
		objects.add(elem);
		return true;
	}

	public void clear() {
		objects.clear();
		indexes.clear();
	}

	/**
	 * Constant time override for contains.
	 */
	@Override
	public boolean contains(Object o) {
		return indexes.keySet().contains(o);
	}

	/**
	 * Return the object with the given index
	 * 
	 * @param index
	 */
	@Override
	@Deprecated
	public E get(int index) {
		return objects.get(index);
	}

	// Return the index of the element
	// If doesn't exist, add it.
	public int getIndex(E e) {
		if (e == null)
			return -1;
		Integer index = indexes.get(e);
		if (index == null) {
			index = size();
			objects.add(e);
			indexes.put(e, index);
		}
		return index;
	}

	public List<E> getObjects(List<Integer> ids) {
		List<E> ret = new ArrayList<E>();
		for (int i = 0; i < ids.size(); i++) {
			ret.add(getObject(ids.get(i)));
		}
		return ret;
	}

	public List<Integer> getIndexes(List<E> objs) {
		List<Integer> ret = new ArrayList<Integer>();
		for (int i = 0; i < objs.size(); i++) {
			ret.add(getIndex(objs.get(i)));
		}
		return ret;
	}

	public E getObject(int index) {
		return objects.get(index);
	}

	// Not really safe; trust them not to modify it
	public List<E> getObjects() {
		return objects;
	}

	public E[] getObjects(int[] is) {
		if (size() == 0)
			throw new IllegalArgumentException("bad");
		int n = is.length;
		Class c = objects.get(0).getClass();
		E[] os = (E[]) Array.newInstance(c, n);
		for (int i = 0; i < n; i++)
			os[i] = is[i] == -1 ? null : getObject(is[i]);
		return os;
	}

	/**
	 * Returns the index of the given object, or -1 if the object is not present in the indexer.
	 * 
	 * @param o
	 * @return
	 */
	@Override
	public int indexOf(Object o) {
		Integer index = indexes.get(o);
		if (index == null)
			return -1;
		return index;
	}

	public List<Integer> indexOf(List<Object> objs) {
		List<Integer> ret = new ArrayList<>(objs.size());
		for (int i = 0; i < objs.size(); i++) {
			ret.add(indexOf(objs.get(i)));
		}
		return ret;
	}

	/**
	 * Returns the number of objects indexed.
	 */
	@Override
	public int size() {
		return objects.size();
	}

}
