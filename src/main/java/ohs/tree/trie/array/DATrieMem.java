package ohs.tree.trie.array;

import java.util.TreeMap;

/**
 * Pure memory implementation of Double-Array Trie. Stores array as -- well -- array and a tree structure to store address data.
 * 
 * @author Jeshua Bratman
 * 
 */
public class DATrieMem extends DATrie {
	protected int[] base;
	protected int[] check;
	private TreeMap<Integer, Integer> data_addresses;
	private int head;

	public DATrieMem() {
		this(1, 128);
	}

	public DATrieMem(int minChar, int maxChar) {
		super(minChar, maxChar);
		array_size = super.INITIAL_ARRAY_SIZE;
		base = new int[(int) array_size];
		check = new int[(int) array_size];
		data_addresses = new TreeMap<Integer, Integer>();
		head = 1;
		base[head] = 1;
		check[base[head]] = head;
		children = new int[maxChar];
		super.Init();
	}

	protected void setLength(int length) {
		lengthenArrays(length);
	}

	protected void ensureLength(int length) {
		while (array_size < length) {
			lengthenArrays((int) (array_size * ARRAY_LENGTHEN_FACTOR));
		}
	}

	protected int readBase(int s) {
		return base[s];
	}

	protected int readCheck(int s) {
		return check[s];
	}

	protected void writeBase(int s, int v) {
		base[s] = v;
	}

	protected void writeCheck(int s, int v) {
		check[s] = v;
	}

	protected int readAddress(int s) {
		if (data_addresses.containsKey(s)) {
			return data_addresses.get(s);
		} else {
			return EMPTY;
		}
	}

	protected void writeAddress(int s, int target) {
		if (target != EMPTY)
			data_addresses.put(s, target);
	}

	private void lengthenArrays(int new_size) {
		int[] new_base = new int[(int) (new_size)];
		int[] new_check = new int[(int) (new_size)];
		for (int i = 0; i < array_size; i++) {
			new_base[i] = base[i];
			new_check[i] = check[i];
		}
		base = new_base;
		check = new_check;
		this.array_size = new_size;
	}

	// ==================================================

	public void printDensity() {
		double free = 0;
		int used = 0;
		for (int i = 0; i < array_size; i++) {
			if (check[i] == EMPTY)
				free += 1;
			else
				used += 1;
		}
		System.out.println("used: " + used);
		System.out.println("free: " + (int) free);
		System.out.println("total size: " + array_size);
		System.out.println("Density: " + (1 - (free / array_size)));
	}

	// Just a simple example usage.
	public static void main(String[] args) throws Exception {
		// {
		// DATrieMem trie = new DATrieMem(1, 10000);
		//
		// int cnt = 0;
		// SejongReader r = new SejongReader(NLPPath.POS_DATA_FILE, NLPPath.POS_TAG_SET_FILE);
		// while (r.hasNext()) {
		// KDocument doc = r.next();
		//
		// for (int i = 0; i < doc.size(); i++) {
		// MTSentence sent = doc.getSentence(i);
		// for (MultiToken mt : sent.toMultiTokens()) {
		// String text = mt.getText();
		// int[] ws = new int[text.length()];
		// for (int j = 0; j < ws.length; j++) {
		// ws[j] = text.charAt(j);
		// }
		// trie.insert(ws, cnt++);
		// }
		// }
		// }
		// r.close();
		// }
		
		{
			DATrieMem trie = new DATrieMem(1, 128);

			trie.insert(new int[] { 1, 5, 6, 7 }, 14);
			trie.insert(new int[] { 2, 2 }, 19);
			trie.insert(new int[] { 1, 5, 2 }, 9);
			trie.insert(new int[] { 2, 5, 2 }, 3);
			trie.insert(new int[] { 1, 2, 5 }, 8);
			trie.insert(new int[] { 1, 2, 5 }, 8);

			System.out.println(trie.find(new int[] { 1, 2, 5 }));
			;
			trie.display();
		}

		{
			DATrieMem trie = new DATrieMem(1, 128);

			trie.insert(new int[] { 1, 5, 6, 7 }, 14);
			trie.insert(new int[] { 2, 2 }, 19);
			trie.insert(new int[] { 1, 5, 2 }, 9);
			trie.insert(new int[] { 2, 5, 2 }, 3);
			trie.insert(new int[] { 1, 2, 5 }, 8);
			trie.insert(new int[] { 1, 2, 5 }, 8);

			System.out.println(trie.find(new int[] { 1, 2, 5 }));
			;
			trie.display();
		}
		// test.disp();
	}
}