package ohs.tree.trie.array;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import ohs.io.FileUtils;
import ohs.math.ArrayUtils;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KDocumentCollection;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.pos.NLPPath;
import ohs.nlp.pos.SejongReader;
import ohs.types.Counter;
import ohs.types.IntegerArrayList;
import ohs.utils.ByteSize;
import ohs.utils.ByteSize.Type;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

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
		System.out.println("begins.");
		// {
		// DATrieMem trie = new DATrieMem(1, 128);
		//
		// [0:3 1:2 2:4 3:9 4:6 5:13 6:16 7:19 8:1]
		// [0:11 1:2 2:19 3:1]
		// [0:3 1:2 2:5 3:8 4:6 5:1]
		// [1:9 2:6 3:13 4:16 5:19 6:1 7:-32 8:-32 9:2 10:19 11:1 12:8 13:6 14:1]
		// [0:3 1:2 2:3 3:26 4:1]
		//
		// trie.insert(new int[] { 1, 5, 6, 7 }, 14);
		// trie.insert(new int[] { 2, 2 }, 19);
		// trie.insert(new int[] { 1, 5, 2 }, 9);
		// trie.insert(new int[] { 2, 5, 2 }, 3);
		// trie.insert(new int[] { 1, 2, 5 }, 8);
		// trie.insert(new int[] { 1, 2, 5 }, 8);
		//
		// System.out.println(trie.find(new int[] { 1, 2, 5 }));
		// ;
		// trie.display();
		// }

		{

			KDocumentCollection col = new KDocumentCollection();
			int cnt = 0;

			List<String> words = Generics.newArrayList();

			int num_docs = 0;

			SejongReader r = new SejongReader(NLPPath.POS_DATA_FILE, NLPPath.POS_TAG_SET_FILE);
			while (r.hasNext()) {

				if (++num_docs > 100) {
					break;
				}

				KDocument doc = r.next();

				MultiToken[] mts = MultiToken.toMultiTokens(doc.getTokens());

				for (int i = 0; i < mts.length; i++) {
					MultiToken mt = mts[i];
					String text = mt.getText();
					words.add(text);
				}
			}
			r.close();

			Collections.sort(words);

			ODATrie trie = new ODATrie();
			Counter<String> c2 = new Counter<String>();

			for (String word : words) {
				trie.insert(word);
				c2.incrementCount(word, 1);
			}

			long bytes1 = 0;
			long bytes2 = 0;

			{

				for (Entry<String, Double> e : c2.entrySet()) {
					bytes1 += (e.getKey().length() * Character.BYTES);
					bytes1 += Double.BYTES;
				}
			}

			ByteSize bs1 = new ByteSize(bytes1);
			ByteSize bs2 = new ByteSize(trie.bytes());

			Type type = Type.MEGA;

			System.out.printf("Counter:\t%s\n", bs1.size(type));
			System.out.printf("ODATrie:\t%s\n", bs2.size(type));

			System.out.printf("Ratio:\t%s\n", bs1.size(type) / bs2.size(type));

		}

		// {
		// ODATrie trie = new ODATrie();
		//
		// String[] ss = { "bachelor#", "bachelor#", "jar#", "badge#", "baby#" };
		//
		// for (int i = 0; i < ss.length; i++) {
		// trie.insert(ss[i]);
		// // trie.search(ss[i]);
		// }
		// }

		// {
		// ODATrie trie = new ODATrie();
		//
		// String[] ss = { "abc", "abc", "cbd", "cbda", "cbdab", "cbdab" };
		//
		// for (int i = 0; i < ss.length; i++) {
		// trie.insert(ss[i]);
		// // trie.search(ss[i]);
		// }
		//
		// trie.trimToSize();
		//
		// System.out.println(trie.toString());
		//
		// }

		System.out.println("ends.");
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(max_char_size);

		FileUtils.writeIntArray(oos, base.getValues());
		FileUtils.writeIntArray(oos, check.getValues());
		FileUtils.writeIntArray(oos, counts.getValues());

		oos.flush();
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		write(oos);
		oos.close();
	}

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		read(ois);
		ois.close();
	}

	public void read(ObjectInputStream ois) throws Exception {
		max_char_size = ois.readInt();
		base = new IntegerArrayList(FileUtils.readIntArray(ois));
		check = new IntegerArrayList(FileUtils.readIntArray(ois));
		counts = new IntegerArrayList(FileUtils.readIntArray(ois));
		allNextStates = new IntegerArrayList();
	}

	private IntegerArrayList base;

	private IntegerArrayList check;

	private IntegerArrayList counts;

	private IntegerArrayList allNextStates;

	private int DA_SIZE = 10;

	private int minChar = 1;

	private int max_char_size = 10;

	private Random random;

	private RelocMethod RELOC_METHOD = RelocMethod.BRUTE_FORCE;

	private final double RANDOM_BASE_MAX = .9;

	private final double RELOCATION_BOREDOM = .0001;

	private final double RELOCATION_LENGTHEN_THRESH = .5;

	public ODATrie() {
		this(100);
	}

	public ODATrie(int max_char_size) {
		base = new IntegerArrayList();
		check = new IntegerArrayList();
		allNextStates = new IntegerArrayList();
		counts = new IntegerArrayList();

		base.ensureCapacityPadding(DA_SIZE);
		check.ensureCapacityPadding(DA_SIZE);
		allNextStates.ensureCapacityPadding(DA_SIZE);
		counts.ensureCapacityPadding(DA_SIZE);

		base.set(ROOT, ROOT);

		random = new Random();

		this.max_char_size = max_char_size;
	}

	private int addNextState(int state, int c) {
		int b = base.get(state);
		int emp_b = getEmptyBase(b);
		int next_state = b + c;

		base.set(next_state, emp_b);
		check.set(next_state, state);
		return next_state;
	}

	public long bytes() {
		long bytes = 0;
		ByteSize bs1 = ArrayUtils.byteSize(base.getValues());
		ByteSize bs2 = ArrayUtils.byteSize(check.getValues());
		ByteSize bs4 = ArrayUtils.byteSize(counts.getValues());
		bytes = (bs1.size() + bs2.size() + bs4.size());
		ByteSize bs5 = new ByteSize(bytes);
		return bs5.size();

	}

	/**
	 * Checks if a state can fit at base. ASSUMES getchildren was called
	 * 
	 * @param state
	 * @param b
	 * @return
	 * @throws IOException
	 */
	private boolean checkSafeBase(int state, int b) {
		// loop through allNextStates that aren't 0
		boolean safe = true;
		for (int c = minChar; c < max_char_size; c++) {
			if (allNextStates.get(c) > 0) {
				// make sure we haven't gone out of bounds
				if (b >= base.size() || b + c >= base.size()) {
					safe = false;
					break;
				}
				// check if this new base CAN'T fit this child
				if (!isEmpty(b + c)) {
					safe = false;
					break;
				}
			}
		}
		return safe;
	}

	public int find(int[] x) {
		return find(x, ROOT);
	}

	private int find(int[] x, int r) {
		int length = x.length;
		for (int h = 0; h < length; h++) {
			r = getNextState(r, x[h]);
			if (r == FAIL || r == EMPTY)
				return EMPTY;
		}
		return r;
	}

	private int findSafeBase(int state, int required_child) {
		getAllNextStates(state);// sets allNextStates variable
		allNextStates.set(required_child, 1);
		int loops = 0;

		int b;
		if (RELOC_METHOD == RelocMethod.BRUTE_FORCE) {
			b = base.get(state) - base.size() / 5;
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
				if (b + max_char_size >= base.size())
					b = 1;
				else
					b++;
				if (RELOC_METHOD == RelocMethod.RANDOM) {
					if ((loops % (base.size() * RELOCATION_BOREDOM)) == 0)
						b = getRandomBase();
				}
				// lengthen arrays if we loop too long
				if (loops > base.size() * RELOCATION_LENGTHEN_THRESH) {
					// System.out.println("I have looped "+loops+" times, resizing");
					b = base.size();
					// ensureLengthW((int) (array_size * this.ARRAY_LENGTHEN_FACTOR));
					return b;// just put this entry at the end of the old array
					// size
				}
			}
		}
	}

	protected final IntegerArrayList getAllNextStates(int state) {
		int next_state;
		for (int i = minChar; i < max_char_size; i++) {
			next_state = base.get(state) + i;
			if (next_state >= base.size()) {
				for (int j = i; j < max_char_size; j++) {
					allNextStates.set(j, EMPTY);
				}
				break;
			}
			if (check.get(next_state) == state) {
				allNextStates.set(i, next_state);
			} else {
				allNextStates.set(i, EMPTY);
			}
		}
		return allNextStates;
	}

	private int getEmptyBase(int state) {
		while (check.get(state) != EMPTY) {
			state++;
		}
		return state;
	}

	public int getInteger(char c) {
		int a = Character.codePointAt(new char[] { 'a' }, 0) - 2;
		int ret = 0;
		if (c == '#') {
			ret = 1;
		} else {
			ret = Character.codePointAt(new char[] { c }, 0);
			ret -= a;
		}
		return ret;
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

	private int getNextState(int state, int c) {
		int next_state = base.get(state) + c;
		int chk = check.get(next_state);

		if (chk == state) {
			return next_state;
		} else if (chk == EMPTY) {
			return EMPTY;
		} else {
			return FAIL;
		}
	}

	// private int readAddress(int s) {
	// if (data.containsKey(s)) {
	// return data.get(s);
	// } else {
	// return EMPTY;
	// }
	// }
	//
	// private void writeAddress(int s, int target) {
	// if (target != EMPTY)
	// data.put(s, target);
	// }

	private int getRandomBase() {
		int b = random.nextInt((int) (base.size() * RANDOM_BASE_MAX));
		return getEmptyBase(b);
	}

	public void insert(IntegerArrayList x, int d) {
		// System.out.println(x.toString());
		base.setAutoGrowth(true);
		check.setAutoGrowth(true);
		allNextStates.setAutoGrowth(true);
		counts.setAutoGrowth(true);
		// tail.setAutoGrowth(true);

		int state = ROOT;
		int prev_state;
		int len = x.size();
		int c = 0;

		for (int i = 0; i < len; i++) {
			c = x.get(i);
			prev_state = state;
			state = getNextState(state, c);

			max_char_size = Math.max(max_char_size, c);

			if (state == EMPTY) {
				state = addNextState(prev_state, c);
			} else if (state == FAIL) {
				int new_base = findSafeBase(prev_state, c);
				relocateState(prev_state, new_base);
				state = addNextState(prev_state, c);
			}

			// System.out.printf("(%d, %c) -> %d\n", prev_state, StrUtils.decodeByCodePoints(c, 0), state);
		}

		if (d != EMPTY) {
			counts.increment(state, 1);
		}

		base.setAutoGrowth(false);
		check.setAutoGrowth(false);
		allNextStates.setAutoGrowth(false);
		counts.setAutoGrowth(false);
		// tail.setAutoGrowth(false);
	}

	public void insert(String word) {
		insert(new IntegerArrayList(StrUtils.encondeByCodePoints(word)), 1);
	}

	private boolean isEmpty(int state) {
		return check.get(state) == EMPTY;
	}

	protected boolean owned(int s, int c) {
		return check.get(base.get(s) + c) == s;
	}

	protected void relocateState(int state, int new_base) {
		getAllNextStates(state);
		int statebase = base.get(state);
		for (int c = minChar; c < max_char_size; c++) {
			if (allNextStates.get(c) == EMPTY) {
				continue;
			}

			// update allNextStates
			int old_child = statebase + c;
			int new_child = new_base + c;
			// set owner of new location
			check.set(new_base + c, state);

			// copy the node
			base.set(new_child, base.get(old_child));

			for (int d = minChar; d < max_char_size; d++) {
				if (owned(old_child, d)) {
					check.set(base.get(old_child) + d, new_base + c);
				}
			}
			check.set(old_child, EMPTY);

			// move data address for child

			counts.change(new_child, counts.get(old_child));
			counts.set(old_child, EMPTY);
		}

		base.set(state, new_base);
	}

	public void trimToSize() {
		int[] vs = base.getValues();
		int idx = ArrayUtils.lastIndexOf(vs, 0, vs.length, EMPTY, true);

		if (idx > -1) {
			vs = Arrays.copyOf(vs, idx);
			base = new IntegerArrayList(vs);

			vs = check.getValues();
			vs = Arrays.copyOf(vs, idx);
			check = new IntegerArrayList(vs);

			vs = counts.getValues();
			vs = Arrays.copyOf(vs, idx);
			counts = new IntegerArrayList(vs);
		}

	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append(String.format("base:\t%s\n", base.toString()));
		sb.append(String.format("check:\t%s\n", check.toString()));
		sb.append(String.format("data:\t%s\n", counts.toString()));

		ByteSize bs1 = ArrayUtils.byteSize(base.getValues());
		ByteSize bs2 = ArrayUtils.byteSize(check.getValues());
		ByteSize bs3 = ArrayUtils.byteSize(allNextStates.getValues());
		ByteSize bs4 = ArrayUtils.byteSize(counts.getValues());

		ByteSize bs5 = new ByteSize(bs1.size() + bs2.size() + bs3.size() + bs4.size());
		sb.append(String.format("Size:\t%s", bs5.size(Type.MEGA)));

		// int basis = Character.codePointAt(new char[] { 'a' }, 0) - 2;
		// for (int i = 0; i < tail.size(); i++) {
		// sb.append(i);
		// if (i != tail.size() - 1) {
		// sb.append("\t");
		// }
		// }
		//
		// sb.append("\n");
		//
		// for (int i = 0; i < tail.size(); i++) {
		// int c = tail.get(i);
		//
		// if (c == 1) {
		// sb.append("#");
		// } else {
		// c = c + basis;
		// String cc = String.valueOf(Character.toChars(c));
		// sb.append(cc);
		// }
		//
		// if (i != tail.size() - 1) {
		// sb.append("\t");
		// }
		// }

		// sb.append(String.format("\nPOS:\t%d", POS));

		return sb.toString();
	}

}