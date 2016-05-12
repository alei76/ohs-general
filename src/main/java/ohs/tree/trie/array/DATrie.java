package ohs.tree.trie.array;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

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
 * wastes ~15% of the space it uses. Strategies described in the original paper could improve this quite a bit
 * 
 * Following description:
 * 
 * Aoe, J. An Efficient Digital Search Algorithm by Using a Double-Array Structure. IEEE Transactions on Software Engineering. Vol. 15, 9
 * (Sep 1989). pp. 1066-1077.
 * 
 * @author Jeshua Bratman
 */
public abstract class DATrie {
	/**
	 * Relocation Strategy RANDOM is a heuristic monte carlo relocation method, BRUTE_FORCE scans until it finds a suitable location (not
	 * recommended)
	 */
	public enum RelocMethod {
		BRUTE_FORCE, RANDOM
	};

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

	public static final int FAIL = -1;
	public static final int EMPTY = 0;
	public static final int HEAD = 1;

	protected int minChar;
	protected int maxChar;

	protected int array_size;
	protected int[] children;
	protected Random random;
	protected byte[] empty_slots;// bit for each empty slot
	private boolean use_empty_array;

	/**
	 * Initialize DA trie with default min/max chars (sufficient if input is in ascii)
	 */
	public DATrie() {
		this(1, 128);
	}

	/**
	 * @param minChar
	 *            minimum character value (must be > 0)
	 * @param maxChar
	 *            maximum character value
	 */
	public DATrie(int minChar, int maxChar) {
		if (minChar < 1)
			throw new IllegalArgumentException("Minimum character value must be >= 1.");
		this.minChar = minChar;
		this.maxChar = maxChar;
		children = new int[maxChar];
		random = new Random();
		empty_slots = new byte[100];
		use_empty_array = false;
	}

	public void Init() {
		this.ensureEmptySize();
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
			last_state = state;
			state = (int) getChild(state, ar[i]);
			if (state == FAIL) {
				int new_base = findSafeBase(last_state, ar[i]);
				relocateState(last_state, new_base);
				state = (int) addChild(last_state, ar[i]);
			} else if (state == EMPTY) {
				state = (int) addChild(last_state, ar[i]);
			}
		}
		writeAddress(state, address);
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
		return find(ar, DATrie.HEAD);
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
	};

	// ==================================================
	// ABSTRACT INTERFACE

	protected abstract int readBase(int s) throws IOException;

	protected abstract void writeBase(int s, int v) throws IOException;

	protected abstract int readCheck(int s) throws IOException;

	protected abstract void writeCheck(int s, int v) throws IOException;

	protected abstract int readAddress(int s) throws IOException;

	protected abstract void writeAddress(int s, int target) throws IOException;

	protected void close() {
	};

	// ==================================================
	// IMPLEMENTATION

	protected boolean empty(int b) throws IOException {
		if (use_empty_array) {
			byte mask = (byte) (1 << (b % 8));
			return (empty_slots[b / 8] & mask) != 0;
		} else {
			return readCheck(b) == EMPTY;
		}
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

	protected void ensureEmptySize() {
		if (use_empty_array && empty_slots.length < array_size / 8) {
			byte[] temp = empty_slots;
			empty_slots = new byte[(int) Math.ceil(array_size / 8.0)];
			for (int i = 0; i < temp.length; i++) {
				empty_slots[i] = temp[i];
			}
		}
	}

	protected int getEmptyBase(int start_base) throws IOException {
		int b = start_base;
		while (!empty(b)) {
			if (b >= array_size - maxChar)
				b = 1;
			else
				b++;
		}
		return b;
	}

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
	 * Write to the check file.
	 * 
	 * @param s
	 *            state
	 * @param v
	 *            owner
	 * @throws IOException
	 */
	private void writeCheckW(int s, int v) throws IOException {
		this.writeCheck(s, v);
		setEmpty(s, v == EMPTY);
	}

	/**
	 * Ensure the arrays are long enough. The wrapper method ensureLengthW extends the empty_slots array if necessary
	 * 
	 * @param length
	 */
	protected abstract void ensureLength(int length);

	private void ensureLengthW(int length) {
		this.ensureLength(length);
		this.ensureEmptySize();
	}

	protected abstract void setLength(int length);

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
		return readCheck(readBase(s) + c) == s;
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
		int statebase = readBase(state);
		for (int c = minChar; c < maxChar; c++) {
			if (children[c] == EMPTY)
				continue;

			// update children
			int old_child = statebase + c;
			int new_child = new_base + c;
			// set owner of new location
			writeCheckW(new_base + c, state);

			// copy the node
			writeBase(new_child, readBase(old_child));

			for (int d = minChar; d < maxChar; d++) {
				if (owned(old_child, d))
					writeCheckW(readBase(old_child) + d, new_base + c);
			}
			writeCheckW(old_child, EMPTY);

			// move data address for child
			writeAddress(new_child, readAddress(old_child));
			writeAddress(old_child, EMPTY);
		}
		writeBase(state, new_base);
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
		children[required_child] = 1;// just some number > 0
		int loops = 0;

		int b;
		if (this.RELOC_METHOD == RelocMethod.BRUTE_FORCE) {
			b = readBase(state) - array_size / 5;// getBase();{
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
			if (children[c] > 0) {
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

	/**
	 * Follow transition from state and retrieve the character. Returns EMPTY if the state at the end of the transition is unset Returns
	 * FAIL if the state at the end of the transition is owned by another state
	 * 
	 * @param state
	 * @param character
	 * @return child node or FAIL or EMPTY
	 */
	protected int getChild(int state, int character) throws IOException {
		ensureLengthW(state);
		int t = readBase(state) + character;
		int check = readCheck(t);
		if (check == state)
			return t;
		else if (check == EMPTY)
			return EMPTY;
		else
			return FAIL;
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
		int stateb = readBase(state);
		int b = getEmptyBase(stateb);
		int child = stateb + c;
		writeBase(child, b);
		writeCheckW(child, state);
		return child;
	}

	/**
	 * Get list of children from state and set member variable
	 * 
	 * @param s
	 * @return
	 * @throws IOException
	 */
	protected final int[] getChildren(int state) throws IOException {
		int child;
		for (int i = minChar; i < maxChar; i++) {
			child = readBase(state) + i;
			if (child >= array_size) {
				for (int j = i; j < maxChar; j++)
					children[j] = EMPTY;
				break;
			}
			if (readCheck(child) == state) {
				children[i] = child;
			} else {
				children[i] = EMPTY;
			}
		}
		return children;
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
				final int[] children = getChildren(state);
				for (int i = 0; i < children.length; i++) {
					if (children[i] > 0) {
						if (count++ > max)
							break;
						digraph += "\"" + state + "," + readAddress(state) + "\" -> \"" + children[i] + "," + readAddress(children[i])
								+ "\"[label=\"" + i + "\"]\n";
						queue.add(children[i]);
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

	public void disp() {
		for (int i = HEAD; i < 60; i++) {
			try {
				System.out.printf("%d\t[%d %d]\t[%d]\n", i, this.readBase(i), this.readAddress(i), this.readCheck(i));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}