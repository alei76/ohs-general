package ohs.types;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Indexer<E> extends AbstractList<E> implements Serializable {
	private static final long serialVersionUID = -8769544079136550516L;
	protected List<E> objs;
	protected Map<E, Integer> idxs;

	public Indexer() {
		objs = new ArrayList<E>();
		idxs = new HashMap<E, Integer>();
	}

	public Indexer(Collection<? extends E> c) {
		this();
		for (E a : c) {
			getIndex(a);
		}
	}

	public Indexer(int size) {
		objs = new ArrayList<E>(size);
		idxs = new HashMap<E, Integer>(size);
	}

	@Override
	public boolean add(E elem) {
		if (contains(elem)) {
			return false;
		}
		idxs.put(elem, size());
		objs.add(elem);
		return true;
	}

	@Override
	public void clear() {
		objs.clear();
		idxs.clear();
	}

	@Override
	public boolean contains(Object o) {
		return idxs.keySet().contains(o);
	}

	public E get(int index) {
		return getObject(index);
	}

	public int getIndex(E e) {
		return getIndex(e, -1);
	}

	public int getIndex(E e, int unknown) {
		if (e == null)
			return unknown;
		Integer index = idxs.get(e);
		if (index == null) {
			index = size();
			objs.add(e);
			idxs.put(e, index);
		}
		return index;
	}

	public int[] getIndexes(E[] objs) {
		return getIndexes(objs, -1);
	}

	public int[] getIndexes(E[] objs, int unknown) {
		int[] ret = new int[objs.length];
		for (int i = 0; i < objs.length; i++) {
			ret[i] = getIndex(objs[i], unknown);
		}
		return ret;
	}

	public List<Integer> getIndexes(List<E> objs) {
		return getIndexes(objs, -1);
	}

	public List<Integer> getIndexes(List<E> objs, int unknown) {
		List<Integer> ret = new ArrayList<Integer>();
		for (int i = 0; i < objs.size(); i++) {
			ret.add(getIndex(objs.get(i), unknown));
		}
		return ret;
	}

	public E getObject(int index) {
		return objs.get(index);
	}

	public List<E> getObjects() {
		return objs;
	}

	public E[] getObjects(int[] ids) {
		if (size() == 0)
			throw new IllegalArgumentException("bad");
		int n = ids.length;
		Class c = objs.get(0).getClass();
		E[] os = (E[]) Array.newInstance(c, n);
		for (int i = 0; i < n; i++)
			os[i] = ids[i] == -1 ? null : getObject(ids[i]);
		return os;
	}

	public List<E> getObjects(Collection<Integer> ids) {
		List<E> ret = new ArrayList<E>();
		for (int id : ids) {
			ret.add(getObject(id));
		}
		return ret;
	}

	public int[] indexesOf(E[] objs) {
		return indexesOf(objs, -1);
	}

	public int[] indexesOf(E[] objs, int unknown) {
		int[] ret = new int[objs.length];
		for (int i = 0; i < objs.length; i++) {
			ret[i] = indexOf(objs[i], unknown);
		}
		return ret;
	}

	public List<Integer> indexesOf(List<Object> objs, int unknown) {
		List<Integer> ret = new ArrayList<>(objs.size());
		for (int i = 0; i < objs.size(); i++) {
			ret.add(indexOf(objs.get(i)));
		}
		return ret;
	}

	@Override
	public int indexOf(Object obj) {
		return indexOf(obj, -1);
	}

	public int indexOf(Object o, int unknown) {
		Integer index = idxs.get(o);
		if (index == null)
			return unknown;
		return index;
	}

	@Override
	public int size() {
		return objs.size();
	}

}
