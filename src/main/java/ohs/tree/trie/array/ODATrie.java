package ohs.tree.trie.array;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.SynchronizedMultivariateSummaryStatistics;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.uima.jcas.cas.IntegerList;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.STEditAs;

import gnu.trove.map.TMap;
import kr.co.shineware.util.common.string.StringUtil;
import ohs.types.IntegerArrayList;
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

		// base.ensureCapacityPadding(DA_SIZE);
		// check.ensureCapacityPadding(DA_SIZE);
		// tail.ensureCapacityPadding(DA_SIZE);

		base.set(ROOT, ROOT);

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
	 * @param x
	 */
	public void insert(IntegerArrayList x) {
		base.setAutoGrowth(true);
		check.setAutoGrowth(true);
		tail.setAutoGrowth(true);

		int state = ROOT;
		int next_state;
		int len = x.size();

		for (int i = 0; i < len; i++) {
			int c = x.get(i);
			int b = base.get(state);

			if (b < 0) {
				/*
				 * Case 3: insertion of the new word with a collision; in this case, additional characters must be added to the BASE and
				 * characters must be removed from TAIL array to resolve the collision, but nothing already in the BASE array must be
				 * removed.
				 */
				int tmp = -b;
				IntegerArrayList commons = new IntegerArrayList();

				int loc_in_tail = tmp;
				int loc_in_x = i;

				int last_remain_loc_in_tail = 0;
				for (int m = loc_in_tail; m < tail.size(); m++) {
					if (tail.get(m) == 1) {
						last_remain_loc_in_tail = m + 1;
						break;
					}
				}

				int remain_len_in_input = len - loc_in_x;
				int remain_len_in_tail = last_remain_loc_in_tail - loc_in_tail;

				int min_len = Math.min(remain_len_in_tail, remain_len_in_input);
				int common_cnt = 0;
				while (common_cnt < min_len) {
					if (tail.get(loc_in_tail) == x.get(loc_in_x)) {
						commons.add(x.get(loc_in_x));
					} else {
						break;
					}
					loc_in_tail++;
					loc_in_x++;
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
					neighbors.add(x.get(loc_in_x));

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

					int child_state_2 = base.get(branch_state) + x.get(loc_in_x);
					base.set(child_state_2, -pos);
					check.set(child_state_2, branch_state);

					loc_in_x++;

					for (int prev_loc = loc_in_x; prev_loc < len; prev_loc++) {
						tail.set(pos++, x.set(prev_loc, getInteger('?')));
					}

					System.out.println(tail.toString());
				}
				break;
			} else {
				next_state = b + c;

				if (next_state < 0) {
					break;
				}

				int prev_state = check.get(next_state);

				if (prev_state == 0) {
					/*
					 * Case 1: insertion of the new word when double-array is empty. Case 2: insertion of the new word without any
					 * collisions.
					 */
					base.set(next_state, -pos);
					check.set(next_state, state);

					for (int j = i + 1; j < len; j++) {
						tail.set(pos++, x.get(j));
					}
					break;
				} else if (prev_state != state) {
					/*
					 * Case 4: when insertion of the new word with a collision as in case 3 occurs, values in the BASE array must be moved.
					 */
					int temp_node1 = next_state;
					int temp_node2 = 0;

					IntegerArrayList list1 = new IntegerArrayList();
					IntegerArrayList list2 = new IntegerArrayList();

					for (int j = 0; j < check.size(); j++) {
						int chk = check.get(j);
						if (chk == state) {
							list1.add(j - base.get(state));
						}

						else if (chk == prev_state) {
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
						modify_state = prev_state;
					}

					// System.out.println(list1);
					// System.out.println(list2);
					// System.out.println();

					int temp_base = modify_state;

					int[] qa = x_check(modifyList);

					base.set(modify_state, qa[0]);

					for (int j = 0; j < modifyList.size(); j++) {
						int modify_c = modifyList.get(j);
						temp_node1 = temp_base + modify_c;
						temp_node2 = base.get(modify_state) + modify_c;

						base.set(temp_node2, base.get(temp_node1));
						check.set(temp_node2, check.get(temp_node1));

						if (base.get(temp_node1) < 0) {
							base.set(temp_node1, 0);
							check.set(temp_node1, 0);
						} else {

							System.out.println(toString());
							System.out.println();
							int omega = -1;
							for (int k = 0; k < check.size(); k++) {
								if (check.get(k) == temp_node1) {
									omega = k - base.get(temp_node1);
									break;
								}
							}

							check.set(base.get(temp_node1) + omega, temp_node2);

							System.out.println(toString());
							System.out.println();

							base.set(temp_node1, 0);
							check.set(temp_node1, 0);

							System.out.println(toString());
							System.out.println();
						}
					}

					int temp_node = next_state;
					base.set(temp_node, -pos);
					check.set(temp_node, state);

					for (int j = i + 1; j < len; j++) {
						tail.set(pos++, x.get(j));
					}

					System.out.println(toString());
					System.out.println();
					break;
				} else if (prev_state == state) {
					/*
					 * go to next state if there is no transition problem.
					 */

					state = next_state;
				}
			}
		}

		base.setAutoGrowth(false);
		check.setAutoGrowth(false);
		tail.setAutoGrowth(false);
	}

	public void insert(String word) {
		insert(new IntegerArrayList(getIntegers(word)));
	}

	public int search(IntegerArrayList x) {
		int s = ROOT;
		IntegerArrayList ret = new IntegerArrayList();

		for (int i = 0; i < x.size(); i++) {
			int c = x.get(i);
			if (s < base.size()) {
				int b = base.get(s);

				if (b < 0) {
					int p = -b;
					for (int j = p; j < tail.size(); j++) {
						int ct = tail.get(j);
						if (ct == getInteger('?')) {
							ret.add(ct);
							break;
						} else {
							ret.add(ct);
						}
					}

				} else {
					int t = b + c;
					if (t < check.size()) {
						if (check.get(t) == s) {
							s = t;
							ret.add(c);
						}
					} else {
						return -1;
					}
				}
			} else {
				return -1;
			}
		}
		return s;
	}

	// public int search(String word) {
	// return search(getIntegers(word));
	// }

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

	/**
	 * 
	 * return the minimum integer q such that q > 0 and check[w
	 * 
	 * @param list
	 * @return
	 */
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