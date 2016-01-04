package ohs.types;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ohs.entity.ENTPath;
import ohs.io.TextFileReader;
import ohs.tree.trie.TST;
import ohs.tree.trie.TST.Node;

public class StringIndexer extends AbstractList<String> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7056651014088801108L;

	public static void main(String[] args) {
		System.out.println("process begins");
		// build symbol table from standard input

		Set<String> names = new HashSet<String>();

		TextFileReader reader = new TextFileReader(ENTPath.NAME_PERSON_FILE);
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			// if (reader.getNumLines() > 10) {
			// break;
			// }

			String[] parts = line.split("\t");
			int id = Integer.parseInt(parts[0]);
			String name = parts[1];
			String topic = parts[2];
			String catStr = parts[3];
			String variantStr = parts[4];
			names.add(name);
		}
		reader.close();

		StringIndexer st = new StringIndexer();

		for (String name : names) {
			st.add(name);
			// System.out.printf("%d:\t%s\n", i, st.getObject(i));
		}

		System.out.println(names.size());
		System.out.println(st.size());
		System.out.println(st.getObjects().size());

		System.out.println("process ends");
	}

	private TST<Integer> indexes;

	private List<TST.Node<Integer>> objects;

	public StringIndexer() {
		objects = new ArrayList<TST.Node<Integer>>();
		indexes = new TST<Integer>();
	}

	/**
	 * @author aria42
	 */
	public boolean add(String s) {
		if (contains(s)) {
			return false;
		}
		indexes.put(s, size());
		objects.add(indexes.getNode(s));
		return true;
	}

	public void clear() {
		objects.clear();
		indexes.clear();
	}

	/**
	 * Constant time override for contains.
	 */
	public boolean contains(String o) {
		return indexes.contains(o);
	}

	/**
	 * Return the object with the given index
	 * 
	 * @param index
	 */
	@Override
	@Deprecated
	public String get(int index) {
		// return objects.get(index);
		return null;
	}

	// Return the index of the element
	// If doesn't exist, add it.
	public int getIndex(String s) {
		if (s == null)
			return -1;
		Integer index = indexes.get(s);
		if (index == null) {
			index = size();
			indexes.put(s, index);
			objects.add(indexes.getNode(s));
			String obj = getObject(objects.size() - 1);
			if (s.length() != obj.length()) {
				System.out.printf("%s\t%s\n", s, obj);
			}

		}
		return index;
	}

	public List<Integer> getIndexes(List<String> objs) {
		List<Integer> ret = new ArrayList<Integer>();
		for (int i = 0; i < objs.size(); i++) {
			ret.add(getIndex(objs.get(i)));
		}
		return ret;
	}

	public String getObject(int index) {
		Node<Integer> node = objects.get(index);
		StringBuffer sb = new StringBuffer();
		while (node != null) {
			sb.append(node.getCharacter());
			node = node.getParent();
		}
		return sb.reverse().toString();
	}

	// Not really safe; trust them not to modify it
	public List<String> getObjects() {
		List<String> ret = new ArrayList<String>();
		for (int i = 0; i < size(); i++) {
			ret.add(getObject(i));
		}
		return ret;
	}

	public String[] getObjects(int[] is) {
		if (size() == 0)
			throw new IllegalArgumentException("bad");
		int n = is.length;
		Class c = objects.get(0).getClass();
		String[] os = (String[]) Array.newInstance(c, n);
		for (int i = 0; i < n; i++)
			os[i] = is[i] == -1 ? null : getObject(is[i]);
		return os;
	}

	public List<String> getObjects(List<Integer> ids) {
		List<String> ret = new ArrayList<String>();
		for (int i = 0; i < ids.size(); i++) {
			ret.add(getObject(ids.get(i)));
		}
		return ret;
	}

	public List<Integer> indexOf(List<Object> objs) {
		List<Integer> ret = new ArrayList<>(objs.size());
		for (int i = 0; i < objs.size(); i++) {
			ret.add(indexOf(objs.get(i)));
		}
		return ret;
	}

	/**
	 * Returns the index of the given object, or -1 if the object is not present in the indexer.
	 * 
	 * @param o
	 * @return
	 */
	public int indexOf(String o) {
		Integer index = indexes.get(o);
		if (index == null)
			return -1;
		return index;
	}

	/**
	 * Returns the number of objects indexed.
	 */
	@Override
	public int size() {
		return indexes.size();
	}

	public void write(ObjectOutputStream oos) {

	}

}
