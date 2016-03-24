package ohs.tree.trie.array;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeMap;

import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KDocumentCollection;
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
public class ODATrie {
	/**
	 * Relocation Strategy RANDOM is a heuristic monte carlo relocation method, BRUTE_FORCE scans until it finds a suitable location (not
	 * recommended)
	 */
	public enum RelocMethod {
		BRUTE_FORCE, RANDOM
	};

	public static final int FAIL = -1;
	public static final int EMPTY = 0;
	public static final int ROOT = 1;

	// Just a simple example usage.
	public static void main(String[] args) throws Exception {

		{
			ODATrie trie = new ODATrie();

			String[] ss = { "bachelor#", "jar#", "badge#", "baby#" };

			for (int i = 0; i < ss.length; i++) {
				trie.insert(ss[i]);
				// trie.search(ss[i]);
			}

		}

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
		// trie.display();
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
	protected IntegerArrayList children;
	protected Random random;
	protected byte[] empty_slots;// bit for each empty slot

	private boolean use_empty_array;
	protected IntegerArrayList base;
	protected IntegerArrayList check;
	protected IntegerArrayList tail;

	private TreeMap<Integer, Integer> data_addresses;

	private int head;

	private int DA_SIZE = 10;

	public ODATrie() {
		base = new IntegerArrayList();
		check = new IntegerArrayList();
		tail = new IntegerArrayList();

		base.ensureCapacityPadding(DA_SIZE);
		check.ensureCapacityPadding(DA_SIZE);
		tail.ensureCapacityPadding(DA_SIZE);

		base.set(ROOT, ROOT);

	}

	public int[] getIntegers(String word) {
		int[] x = new int[word.length()];
		int a = Character.codePointAt(new char[] { 'a' }, 0) - 2;

		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			if (c == '#') {
				x[i] = 1;
			} else {
				x[i] = word.codePointAt(i);
				x[i] -= a;
			}
		}
		return x;
	}

	public int search(String word) {
		return search(getIntegers(word));
	}

	public void insert(String word) {
		insert(getIntegers(word));
	}

	private int pos = 1;

	public void insert(int[] x) {
		int s = ROOT;
		for (int h = 0; h < x.length; h++) {
			int c = x[h];
			int t = base.get(s) + c;

			if (t < 0) {
				break;
			}

			int chk = check.get(t);

			if (chk == s) {

			} else {
				if (chk == 0) {
					base.set(t, -pos);
					check.set(t, s);
					pos = x[h + 1];
				} else {

				}
			}
			s = t;
		}
	}

	public int search(int[] x) {
		int s = ROOT;

		for (int h = 0; h < x.length; h++) {
			if (base.size() < s) {
				s = FAIL;
			} else {
				int t = base.get(s) + x[h];
				if (t >= 0 && check.get(t) == s) {
					s = FAIL;
					break;
				}
				s = t;
			}
		}
		return s;
	}

}