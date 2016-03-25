package ohs.tree.trie.array;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.uima.jcas.cas.IntegerList;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.STEditAs;

import ohs.types.IntegerArrayList;

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

	private IntegerArrayList base;
	private IntegerArrayList check;
	private IntegerArrayList tail;
	private int DA_SIZE = 10;
	private int pos = 1;

	public ODATrie() {
		base = new IntegerArrayList();
		check = new IntegerArrayList();
		tail = new IntegerArrayList();

		base.ensureCapacityPadding(DA_SIZE);
		check.ensureCapacityPadding(DA_SIZE);
		tail.ensureCapacityPadding(DA_SIZE);

		base.set(ROOT, ROOT);

	}

	private int addNextState(int s, int[] x, int i) {
		int t = base.get(s) + x[i];
		base.set(t, -pos);
		check.set(t, s);
		for (int p = i + 1; p < x.length; p++) {
			tail.set(pos++, x[p]);
		}
		return t;
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

	private int getNextState(int s, int c) {
		int t = base.get(s) + c;
		int chk = check.get(t);

		if (chk == s)
			return t;
		else if (chk == EMPTY)
			return EMPTY;
		else
			return FAIL;

	}

	/**
	 * @param input
	 */
	/**
	 * @param input
	 */
	public void insert(IntegerArrayList input) {
		base.setAutoGrowth(true);
		check.setAutoGrowth(true);
		tail.setAutoGrowth(true);

		int state = ROOT;
		int next_state;
		int input_len = input.size();

		for (int i = 0; i < input_len; i++) {
			int c = input.get(i);
			int b = base.get(state);

			if (b < 0) {
				/*
				 * insertion when conflict occurs
				 */
				int tmp = -b;
				IntegerArrayList commons = new IntegerArrayList();

				int loc_in_tail = tmp;
				int loc_in_input = i;

				int last_remain_loc_in_tail = 0;
				for (int m = loc_in_tail; m < tail.size(); m++) {
					if (tail.get(m) == 1) {
						last_remain_loc_in_tail = m + 1;
						break;
					}
				}

				int remain_len_in_input = input_len - loc_in_input;
				int remain_len_in_tail = last_remain_loc_in_tail - loc_in_tail;

				int min_len = Math.min(remain_len_in_tail, remain_len_in_input);
				int common_cnt = 0;
				while (common_cnt < min_len) {
					if (tail.get(loc_in_tail) == input.get(loc_in_input)) {
						commons.add(input.get(loc_in_input));
					} else {
						break;
					}
					loc_in_tail++;
					loc_in_input++;
					common_cnt++;
				}

				if (commons.size() > 0) {

					/*
					 * add branch state
					 */
					int[] qa = x_check(commons);
					int branch_state = qa[0] + qa[1];
					base.set(state, qa[0]);
					check.set(branch_state, state);

					IntegerArrayList neighbors = new IntegerArrayList();
					neighbors.add(tail.get(loc_in_tail));
					neighbors.add(input.get(loc_in_input));

					qa = x_check(neighbors);

					base.set(branch_state, qa[0]);

					/*
					 * add a child state for remaining characters in tail
					 */

					int child_state_1 = base.get(branch_state) + tail.get(loc_in_tail);
					base.set(child_state_1, -tmp);
					check.set(child_state_1, branch_state);

					loc_in_tail++;

					int new_loc_in_tail = -base.get(child_state_1);

					for (int prev_loc = loc_in_tail; prev_loc < last_remain_loc_in_tail; prev_loc++) {
						tail.set(new_loc_in_tail++, tail.set(prev_loc, getInteger('?')));
					}

					/*
					 * add a child state for remaining characters in input
					 */

					int child_state_2 = base.get(branch_state) + input.get(loc_in_input);
					base.set(child_state_2, -pos);
					check.set(child_state_2, branch_state);

					loc_in_input++;

					for (int prev_loc = loc_in_input; prev_loc < input_len; prev_loc++) {
						tail.set(pos++, input.set(prev_loc, getInteger('?')));
					}

					System.out.println(tail.toString());
				}
				break;
			} else {
				next_state = b + c;

				if (next_state < 0) {
					break;
				}

				int from_state = check.get(next_state);

				if (from_state == EMPTY) {
					base.set(next_state, -pos);
					check.set(next_state, state);

					for (int j = i + 1; j < input_len; j++) {
						tail.set(pos++, input.get(j));
					}
					break;
				} else if (from_state != state) {
					int temp_node1 = next_state;

					IntegerArrayList list1 = new IntegerArrayList();
					IntegerArrayList list2 = new IntegerArrayList();

					for (int j = 0; j < check.size(); j++) {
						int chk = check.get(j);
						if (chk == state) {
							list1.add(j - base.get(state));
						}

						else if (chk == from_state) {
							list2.add(j - chk);
						}
					}

					IntegerArrayList modifyList = null;
					int modify_state = 0;

					if (list1.size() + 1 < list2.size()) {
						modifyList = list1;
						modify_state = state;
					} else {
						modifyList = list2;
						modify_state = from_state;
					}

					System.out.println(list1);
					System.out.println(list2);
					System.out.println();

					int temp_base = modify_state;

					int[] qa = x_check(modifyList);

					base.set(temp_base, qa[0]);

					temp_node1 = temp_base + c;
					int temp_node2 = base.get(temp_base);

				}

				state = next_state;
			}
		}
	}

	public void insert(String word) {
		insert(new IntegerArrayList(getIntegers(word)));
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

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append(String.format("BASE:\t%s\n", base.toString()));
		sb.append(String.format("CHECK:\t%s\n", check.toString()));

		int basis = Character.codePointAt(new char[] { 'a' }, 0) - 2;
		for (int i = 0; i < tail.size(); i++) {
			sb.append(i);
			if (i != tail.size() - 1) {
				sb.append("\t");
			}
		}

		sb.append("\n");

		for (int i = 0; i < tail.size(); i++) {
			int c = tail.get(i);

			if (c == 1) {
				sb.append("#");
			} else {
				c = c + basis;
				String cc = String.valueOf(Character.toChars(c));
				sb.append(cc);
			}

			if (i != tail.size() - 1) {
				sb.append("\t");
			}
		}

		sb.append(String.format("\nPOS:\t%d", pos));

		return sb.toString();
	}

	private int[] x_check(IntegerArrayList list) {
		int min_q = ROOT;
		int c_at_min_q = -1;

		for (int q = ROOT; q < check.size(); q++) {
			boolean found = true;
			for (int i = 0; i < list.size(); i++) {
				c_at_min_q = list.get(i);
				if (check.get(q + c_at_min_q) != 0) {
					found = false;
					break;
				}
			}
			if (found) {
				min_q = q;
				break;
			}
		}

		return new int[] { min_q, c_at_min_q };
	}

}