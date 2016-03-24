package ohs.tree.trie.array;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeMap;

import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.pos.NLPPath;
import ohs.nlp.pos.SejongReader;
import ohs.types.IntegerArrayList;

/**
 * Double Array Trie
 * 
 * Abstract Double Array Trie. Implements many of the DA Trie methods, but use DATrieMem for an in-memory trie or DATrieDisk for a
 * persistent version.
 * 
 * The input are strings of integers (with a minimum and maximum value for each integer) and an associated address (this is just an integer
 * as well) which should refer to the data associated with a string (i.e. the a unique index for the data).
 * 
 * Notice, this implementation does not provide a good relocation strategy for conflicting elements. Instead we use a heuristic monte carlo
 * approacah to find a suitable location. Preliminary experiments on a large corpus (> 1 million items) found that this implementation
 * wastes ~15% of the space it uses. Strategies described ins the original paper could improve this quite a bit
 * 
 * Following description:
 * 
 * Aoe, J. An Efficient Digital Search Algorithm by Using a Double-Array Structure. IEEE Transactions on Software Engineering. Vol. 15, 9
 * (Sep 1989). pp. 1066-1077.
 * 
 * @author Jeshua Bratman
 */
public class KDATrie2 {
	/**
	 * Relocation Strategy RANDOM is a heuristic monte carlo relocation method, BRUTE_FORCE scans until it finds a suitable location (not
	 * recommended)
	 */
	public enum RelocMethod {
		BRUTE_FORCE, RANDOM
	};

	public static final int FAIL = -1;
	public static final int EMPTY = 0;
	public static final int HEAD = 1;

	// Just a simple example usage.
	public static void main(String[] args) throws Exception {

		KDATrie2 trie = new KDATrie2(1, 100000);

		int cnt = 0;
		SejongReader r = new SejongReader(NLPPath.POS_DATA_FILE, NLPPath.POS_TAG_SET_FILE);
		while (r.hasNext()) {
			KDocument doc = r.next();

			for (int i = 2; i < doc.size(); i++) {
				KSentence sent = doc.getSentence(i);
				for (MultiToken mt : sent.toMultiTokens()) {
					String text = mt.getText();
					char[] chs = text.toCharArray();
					trie.insert(text, cnt++);
				}
			}
		}
		r.close();

		// KDATrie test = new KDATrie(1, 128);
		//
		// test.insert(new int[] { 1, 5, 6, 7 }, 14);
		// test.insert(new int[] { 2, 2 }, 19);
		// test.insert(new int[] { 1, 5, 2 }, 9);
		// test.insert(new int[] { 2, 5, 2 }, 3);
		// test.insert(new int[] { 1, 2, 5 }, 8);
		// test.insert(new int[] { 1, 2, 5 }, 8);
		//
		// System.out.println(test.find(new int[] { 1, 2, 5 }));
		// ;
		trie.display();
		// test.disp();
	}

	/*
	 * Somewhat arbitrary parameters.
	 */
	public RelocMethod RELOC_METHOD = RelocMethod.BRUTE_FORCE;
	// maximum percentage of array to consider when picking a random base
	protected final double RANDOM_BASE_MAX = .9;

	// by what factor to expand arrays
	protected final double ARRAY_LENGTHEN_FACTOR = 2;
	// proporition of array until choosing a new random base
	protected final double RELOCATION_BOREDOM = .0001;
	// proportion of array that must be search before expanding array
	protected final double RELOCATION_LENGTHEN_THRESH = .5;

	protected final int INITIAL_ARRAY_SIZE = 1000;
	protected int minChar;

	protected int maxChar;
	protected int array_size;
	protected IntegerArrayList children;
	protected Random random;
	protected byte[] empty_slots;// bit for each empty slot

	private boolean use_empty_array;
	protected IntegerArrayList base;
	protected IntegerArrayList check;
	private TreeMap<Integer, Integer> data_addresses;

	private int head;

	/**
	 * Initialize DA trie with default min/max chars (sufficient if input is in ascii)
	 */
	public KDATrie2() {
		this(1, 128);
	}

	/**
	 * @param minChar
	 *            minimum character value (must be > 0)
	 * @param maxChar
	 *            maximum character value
	 */
	public KDATrie2(int minChar, int maxChar) {
		if (minChar < 1)
			throw new IllegalArgumentException("Minimum character value must be >= 1.");
		this.minChar = minChar;
		this.maxChar = maxChar;

		random = new Random();
		empty_slots = new byte[100];
		use_empty_array = false;
		array_size = INITIAL_ARRAY_SIZE;

		base = new IntegerArrayList(array_size);
		check = new IntegerArrayList(array_size);
		children = new IntegerArrayList(maxChar);

		base.setAutoGrowth(true);
		check.setAutoGrowth(true);
		children.setAutoGrowth(true);

		data_addresses = new TreeMap<Integer, Integer>();
		head = 1;

		base.set(head, 1);
		check.set(base.get(head), head);

		ensureEmptySize();
	}

	/**
	 * Adds a child of state by following transition c.
	 * 
	 * @param state
	 *            start state
	 * @param c
	 *            transition to follow
	 * @return
	 */
	protected int addChild(int state, int c) throws IOException {
		int stateb = base.get(state);
		int b = getEmptyBase(stateb);
		int child = stateb + c;

		String s1 = String.copyValueOf(Character.toChars(c));

		base.set(child, b);
		writeCheckW(child, state);
		return child;
	}

	/**
	 * Checks if a state can fit at base. ASSUMES getchildren was called
	 * 
	 * @param state
	 * @param base
	 * @return
	 * @throws IOException
	 */
	private boolean checkSafeBase(int state, int base) throws IOException {
		// loop through children that aren't 0
		boolean safe = true;
		for (int c = minChar; c < maxChar; c++) {
			if (children.get(c) > 0) {
				// make sure we haven't gone out of bounds
				if (base >= array_size || base + c >= array_size) {
					safe = false;
					break;
				}
				// check if this new base CAN'T fit this child
				if (!empty(base + c)) {
					safe = false;
					break;
				}
			}
		}
		return safe;
	}

	protected void close() {
	}

	public void disp() {
		for (int i = HEAD; i < 60; i++) {
			System.out.printf("%d\t[%d %d]\t[%d]\n", i, this.base.get(i), this.readAddress(i), this.check.get(i));
		}
	}

	/**
	 * Display using dotty. You must have the dot program in the path.
	 */
	public void display() {
		File temp;
		// BufferedWriter out;
		int count = 0;
		int max = 100;
		try {
			// temp = File.createTempFile("disp", ".dot");
			// out = new BufferedWriter(new FileWriter(temp));
			// create the graph
			String digraph = "digraph dispgraph { \n";
			// BFS across trie
			int state;
			LinkedList<Integer> queue = new LinkedList<Integer>();
			queue.add(HEAD);

			while (!queue.isEmpty()) {
				state = queue.poll();
				final IntegerArrayList children = getChildren(state);
				for (int i = 0; i < children.size(); i++) {
					if (children.get(i) > 0) {
						if (count++ > max)
							break;

						int c = children.get(i);
						digraph += "\"" + state + "," + readAddress(state) + "\" -> \"" + c + "," + readAddress(c) + "\"[label=\"" + i
								+ "\"]\n";
						queue.add(c);
					}
				}
				if (count > max)
					break;
			}
			digraph += "}\n";

			System.out.println(digraph);
			// out.write(digraph);
			// // display the graph
			// out.flush();
			// String tmp_filename = temp.toString();
			// String command = "/usr/bin/dot -Tps " + tmp_filename + " -o " + tmp_filename + ".ps";
			// // System.out.println(command);
			// Process p = Runtime.getRuntime().exec(command);
			//
			// // wait for dotty to close
			// try {
			// p.waitFor();
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			//
			// command = "evince " + tmp_filename + ".ps";
			// // System.out.println(command);
			// p = Runtime.getRuntime().exec(command);
			//
			// try {
			// p.waitFor();
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			// temp.delete();
			// out.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	protected boolean empty(int b) throws IOException {
		if (use_empty_array) {
			byte mask = (byte) (1 << (b % 8));
			return (empty_slots[b / 8] & mask) != 0;
		} else {
			return check.get(b) == EMPTY;
		}
	}

	// ==================================================

	protected void ensureEmptySize() {
		if (use_empty_array && empty_slots.length < array_size / 8) {
			byte[] temp = empty_slots;
			empty_slots = new byte[(int) Math.ceil(array_size / 8.0)];
			for (int i = 0; i < temp.length; i++) {
				empty_slots[i] = temp[i];
			}
		}
	}

	private void ensureLengthW(int length) {
		// ensureLength(length);
		// ensureEmptySize();
	}

	/**
	 * Return state by following string.
	 * 
	 * @param ar
	 *            string to search for (as an array of ints)
	 * @return state associated with string
	 * @throws IOException
	 */
	public int find(int[] ar) throws IOException {
		return find(ar, KDATrie2.HEAD);
	}

	public int find(int[] ar, int state) throws IOException {
		int length = ar.length;
		for (int i = 0; i < length; i++) {
			state = getChild(state, ar[i]);
			if (state == FAIL || state == EMPTY)
				return EMPTY;
		}
		return state;
	}

	/**
	 * Finds a safe base to move state to. This means that the new base is empty and the children of state fit as offsets from base.
	 * 
	 * @param state
	 * @param required_child
	 *            in addition to current children, ensure space for this child
	 * @return
	 */
	protected int findSafeBase(int state, int required_child) throws IOException {
		getChildren(state);// sets children variable
		children.set(required_child, 1);// just some number > 0
		int loops = 0;

		int b;
		if (this.RELOC_METHOD == RelocMethod.BRUTE_FORCE) {
			b = base.get(state) - array_size / 5;// getBase();{
		} else {
			b = getRandomBase();
		}
		if (b < 1)
			b = 1;
		// loop until we find a safe base
		while (true) {
			if (checkSafeBase(state, b))
				return b;
			else {
				// bookeeping
				loops++;
				// choose next base
				if (b + maxChar >= array_size)
					b = 1;
				else
					b++;
				if (RELOC_METHOD == RelocMethod.RANDOM) {
					if ((loops % (array_size * RELOCATION_BOREDOM)) == 0)
						b = getRandomBase();
				}
				// lengthen arrays if we loop too long
				if (loops > array_size * RELOCATION_LENGTHEN_THRESH) {
					// System.out.println("I have looped "+loops+" times, resizing");
					b = array_size;
					ensureLengthW((int) (array_size * this.ARRAY_LENGTHEN_FACTOR));
					return b;// just put this entry at the end of the old array
					// size
				}
			}
		}
	}

	/**
	 * Returns the address associated with a state (i.e. the data)
	 * 
	 * @param state
	 * @return
	 * @throws IOException
	 */
	public int getAddress(int state) throws IOException {
		return readAddress(state);
	}

	public int getArraySize() {
		return array_size;
	}

	/**
	 * Follow transition from state and retrieve the character. Returns EMPTY if the state at the end of the transition is unset Returns
	 * FAIL if the state at the end of the transition is owned by another state
	 * 
	 * @param state
	 * @param character
	 * @return child node or FAIL or EMPTY
	 */
	protected int getChild(int state, int character) throws IOException {
		int t = base.get(state) + character;
		int chk = check.get(t);

		if (chk == state)
			return t;
		else if (chk == EMPTY)
			return EMPTY;
		else
			return FAIL;
	}

	/**
	 * Get list of children from state and set member variable
	 * 
	 * @param s
	 * @return
	 * @throws IOException
	 */
	protected final IntegerArrayList getChildren(int state) throws IOException {
		int child;
		for (int i = minChar; i < maxChar; i++) {
			child = base.get(state) + i;
			if (child >= array_size) {
				for (int j = i; j < maxChar; j++) {
					children.set(j, EMPTY);
				}

				break;
			}
			if (check.get(child) == state) {
				children.set(i, child);
			} else {
				children.set(i, EMPTY);
			}
		}
		return children;
	};

	protected int getEmptyBase(int start_base) throws IOException {
		int b = start_base;
		while (!empty(b)) {
			// if (b >= array_size - maxChar)
			// b = 1;
			// else
			b++;
		}
		return b;
	};

	// ==================================================
	// IMPLEMENTATION

	/**
	 * Get a base where we can put a new state
	 * 
	 * @return
	 * @throws IOException
	 */
	protected int getRandomBase() throws IOException {
		int b;
		b = random.nextInt((int) (array_size * RANDOM_BASE_MAX));
		return getEmptyBase(b);
	}

	/**
	 * Insert string into Trie
	 * 
	 * @param ar
	 *            string to insert (as array of integers)
	 * @param address:
	 *            address associated with this string that is, we store a single integer that represents the data associated with this
	 *            string
	 * @throws IOException
	 */
	public void insert(int[] ar, int address) throws IOException {
		int state = HEAD;
		int last_state;
		for (int i = 0; i < ar.length; i++) {
			// System.out.println(ar[i]);
			int c = ar[i];

			String s = String.copyValueOf(Character.toChars(c));
			System.out.println(s);
			last_state = state;
			state = getChild(state, c);
			if (state == FAIL) {
				int new_base = findSafeBase(last_state, c);
				relocateState(last_state, new_base);
				state = (int) addChild(last_state, c);
			} else if (state == EMPTY) {
				state = (int) addChild(last_state, c);
			}
		}
		writeAddress(state, address);
	}

	public void insert(String word, int addr) throws IOException {
		int[] ar = new int[word.length()];
		for (int i = 0; i < word.length(); i++) {
			ar[i] = word.codePointAt(i);
			// System.out.println(String.copyValueOf(Character.toChars(word.codePointAt(i))));
		}
		insert(ar, addr);
	}

	/**
	 * Check if the state reached by following c is owned by s
	 * 
	 * @param s
	 *            start state
	 * @param c
	 *            transition
	 * @return
	 * @throws IOException
	 */
	protected boolean owned(int s, int c) throws IOException {
		return check.get(base.get(s) + c) == s;
	}

	public void printDensity() {
		double free = 0;
		int used = 0;
		for (int i = 0; i < array_size; i++) {
			if (check.get(i) == EMPTY)
				free += 1;
			else
				used += 1;
		}
		System.out.println("used: " + used);
		System.out.println("free: " + (int) free);
		System.out.println("total size: " + array_size);
		System.out.println("Density: " + (1 - (free / array_size)));
	}

	protected int readAddress(int s) {
		if (data_addresses.containsKey(s)) {
			return data_addresses.get(s);
		} else {
			return EMPTY;
		}
	}

	/**
	 * Move state to new base and resolve children. This function is part of resolveConflict which find a safe new base. NOTE: if the new
	 * base is not safe then this function may mess up the tree.
	 * 
	 * @param state
	 * @param new_base
	 */
	protected void relocateState(int state, int new_base) throws IOException {
		getChildren(state);
		int statebase = base.get(state);
		for (int c = minChar; c < maxChar; c++) {
			if (children.get(c) == EMPTY)
				continue;

			// update children
			int old_child = statebase + c;
			int new_child = new_base + c;
			// set owner of new location
			writeCheckW(new_base + c, state);

			// copy the node
			base.set(new_child, base.get(old_child));

			for (int d = minChar; d < maxChar; d++) {
				if (owned(old_child, d))
					writeCheckW(base.get(old_child) + d, new_base + c);
			}
			writeCheckW(old_child, EMPTY);

			// move data address for child
			writeAddress(new_child, readAddress(old_child));
			writeAddress(old_child, EMPTY);
		}
		base.set(state, new_base);
	}

	protected void setEmpty(int b, boolean empty) {
		if (use_empty_array) {
			byte mask = (byte) (1 << (b % 8));
			if (empty)
				empty_slots[b / 8] &= ~mask;
			else
				empty_slots[b / 8] |= mask;
		}
	}

	protected void writeAddress(int s, int target) {
		if (target != EMPTY)
			data_addresses.put(s, target);
	}

	/**
	 * Write to the check file.
	 * 
	 * @param s
	 *            state
	 * @param v
	 *            owner
	 * @throws IOException
	 */
	private void writeCheckW(int s, int v) throws IOException {
		check.set(s, v);
		setEmpty(s, v == EMPTY);
	}
}