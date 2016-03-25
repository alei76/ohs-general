package ohs.tree.trie.array;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.parser.lexparser.ChineseCharacterBasedLexicon;
import ohs.types.IntegerArrayList;

public class ODATrie2 {
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
			ODATrie2 trie = new ODATrie2();

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

	private IntegerArrayList base;
	private IntegerArrayList check;
	private IntegerArrayList tail;
	private int DA_SIZE = 10;
	private int pos = 1;

	public ODATrie2() {
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

	public void insert(int[] x) {

		base.setAutoGrowth(true);
		check.setAutoGrowth(true);
		tail.setAutoGrowth(true);

		int s = ROOT;
		int h = x.length;
		for (int i = 0; i < h; i++) {
			int c = x[i];
			int t = base.get(s) + c;
			int t2 = getNextState(s, c);

			if (t < 0) {
				break;
			}
			int chk = check.get(t);

			if (chk == EMPTY) {
				base.set(t, -pos);
				check.set(t, s);

				for (int p = i + 1; p < h; p++) {
					tail.set(pos++, x[p]);
				}
				break;
			} else if (chk == s) {

			}
			s = t;
		}
	}

	private int getNextState(int s, int c) {
		int t = base.get(s) + c;
		int chk = check.get(t);
		if (chk != s) {
			if (chk == EMPTY) {
				t = EMPTY;
			} else {
				t = FAIL;
			}
		}
		return t;

	}

	private int x_check(IntegerArrayList list) {
		int q = ROOT;
		while (q != check.size()) {
			boolean foundMinQ = true;
			for (int i = 0; i < list.size(); i++) {
				int c = list.get(i);
				int qc = q + c;

				if (qc < check.size()) {
					if (check.get(qc) != 0) {
						foundMinQ = false;
						break;
					}
				} else {
					foundMinQ = false;
					break;
				}
			}

			if (foundMinQ) {
				break;
			}
			q++;
		}

		return q;
	}

	private List<String> getCharList() {
		List<String> ret = new ArrayList<String>();

		for (int i = 1; i < pos; i++) {
			int c = tail.get(i);
			int a = Character.codePointAt(new char[] { 'a' }, 0) - 2;
			ret.add(String.valueOf(Character.toChars(c + a)));
		}

		return ret;
	}

	public void insert(String word) {
		insert(getIntegers(word));
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

	public int search(String word) {
		return search(getIntegers(word));
	}

}