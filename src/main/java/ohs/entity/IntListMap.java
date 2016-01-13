package ohs.entity;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;

public class IntListMap {

	public static void main(String[] args) {

		IntListMap gi = new IntListMap();

		gi.put(1, 1);
		gi.put(2, 1);
		gi.put(2, 2);
		gi.put(2, 2);
		gi.put(2, 3);

		System.out.println(gi.toString());
	}

	private Int2ObjectArrayMap<IntList> entries;

	public IntListMap() {
		entries = new Int2ObjectArrayMap<IntList>();
	}

	public IntListMap(int size) {
		entries = new Int2ObjectArrayMap<IntList>(size);
	}

	public boolean containsKey(int key) {
		return entries.containsKey(key);
	}

	private IntList ensure(int key) {

		IntList ret = entries.get(key);
		if (ret == null) {
			ret = new IntArrayList();
			entries.put(key, ret);
		}
		return ret;
	}

	public IntList get(int key, boolean createIfAbsent) {
		return createIfAbsent ? ensure(key) : entries.get(key);
	}

	public IntSet keySet() {
		return entries.keySet();
	}

	public void put(int key, int value) {
		ensure(key).add(value);
	}

	public void read(ObjectInputStream ois) throws Exception {
		int size1 = ois.readInt();
		entries = new Int2ObjectArrayMap<IntList>(size1);

		for (int i = 0; i < size1; i++) {
			int key = ois.readInt();
			int size2 = ois.readInt();
			IntList values = new IntArrayList(size2);
			for (int j = 0; j < size2; j++) {
				values.add(ois.readInt());
			}
			entries.put(key, values);
		}
	}

	public void set(int key, IntList set) {
		entries.put(key, set);
	}

	public int size() {
		return entries.size();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		List<Integer> keys = new ArrayList<Integer>(entries.keySet());
		for (int i = 0; i < keys.size() && i < 50; i++) {
			int key = keys.get(i);
			List<Integer> values = new ArrayList<Integer>(entries.get(key));
			sb.append(String.format("%d ->", key));
			for (int j = 0; j < values.size() && j < 10; j++) {
				sb.append(String.format(" %d", values.get(j)));
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(entries.size());

		for (int key : entries.keySet()) {
			IntList values = entries.get(key);
			oos.writeInt(key);
			oos.writeInt(values.size());
			for (int value : values) {
				oos.writeInt(value);
			}
		}
		oos.flush();
	}

}
