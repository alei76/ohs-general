package ohs.tree.trie.hash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import ohs.utils.StrUtils;

/******************************************************************************
 *  Compilation:  javac TST.java
 *  Execution:    java TST < words.txt
 *  Dependencies: StdIn.java
 *
 *  Symbol table with string keys, implemented using a ternary search
 *  trie (TST).
 *
 *
 *  % java TST < shellsST.txt
 *  keys(""):
 *  by 4
 *  sea 6
 *  sells 1
 *  she 0
 *  shells 3
 *  shore 7
 *  the 5
 *
 *  longestPrefixOf("shellsort"):
 *  shells
 *
 *  keysWithPrefix("shor"):
 *  shore
 *
 *  keysThatMatch(".he.l."):
 *  shells
 *
 *  % java TST
 *  theory the now is the time for all good men
 *
 *  Remarks
 *  --------
 *    - can't use a key that is the empty string ""
 *
 ******************************************************************************/

/**
 * The <tt>TST</tt> class represents an symbol table of key-value pairs, with string keys and generic values. It supports the usual
 * <em>put</em>, <em>get</em>, <em>contains</em>, <em>delete</em>, <em>size</em>, and <em>is-empty</em> methods. It also provides
 * character-based methods for finding the string in the symbol table that is the <em>longest prefix</em> of a given prefix, finding all
 * strings in the symbol table that <em>start with</em> a given prefix, and finding all strings in the symbol table that <em>match</em> a
 * given pattern. A symbol table implements the <em>associative array</em> abstraction: when associating a value with a key that is already
 * in the symbol table, the convention is to replace the old value with the new value. Unlike {@link java.util.Map}, this class uses the
 * convention that values cannot be <tt>null</tt>&mdash;setting the value associated with a key to <tt>null</tt> is equivalent to deleting
 * the key from the symbol table.
 * <p>
 * This implementation uses a ternary search trie.
 * <p>
 * For additional documentation, see <a href="http://algs4.cs.princeton.edu/52trie">Section 5.2</a> of <i>Algorithms, 4th Edition</i> by
 * Robert Sedgewick and Kevin Wayne.
 */
public class TST<V> {
	public static class Node<V> {

		private char c; // character
		private Node<V> parent;

		private Node<V> left, middle, right; // left, middle, and right subtries

		private V value; // value associated with string

		private int level = 0;

		public Node() {

		}

		public Node(char c, Node<V> parent, Node<V> left, Node<V> middle, Node<V> right, V value) {
			super();
			this.c = c;
			this.parent = parent;
			this.left = left;
			this.middle = middle;
			this.right = right;
			this.value = value;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Node other = (Node) obj;
			if (c != other.c)
				return false;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
			return true;
		}

		public char getCharacter() {
			return c;
		}

		public Node<V> getLeft() {
			return left;
		}

		public int getLevel() {
			return level;
		}

		public Node<V> getMiddle() {
			return middle;
		}

		public Node<V> getParent() {
			return parent;
		}

		public Node<V> getRight() {
			return right;
		}

		public V getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + c;
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
			return result;
		}

		public void setCharacter(char c) {
			this.c = c;
		}

		public void setLeft(Node<V> left) {
			this.left = left;
		}

		public void setLevel(int level) {
			this.level = level;
		}

		public void setMiddle(Node<V> middle) {
			this.middle = middle;
		}

		public void setParent(Node<V> parent) {
			this.parent = parent;
		}

		public void setRight(Node<V> right) {
			this.right = right;
		}

		public void setValue(V v) {
			this.value = v;
		}

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();

			List<String> chs = new ArrayList<String>();
			List<String> levels = new ArrayList<String>();
			Node<V> node = this;

			while (node != null) {
				chs.add(node.getCharacter() + "");
				levels.add(node.getLevel() + "");
				node = node.getParent();
			}

			Collections.reverse(chs);
			Collections.reverse(levels);

			sb.append(String.format("level:\t%s\n", StrUtils.join(" -> ", levels)));
			sb.append(String.format("char:\t%s\n", StrUtils.join(" -> ", chs)));

			// sb.append(String.format("value:\t%s\n", value.toString()));
			// sb.append(String.format("parent char:\t%s", parent.c));
			// sb.append(String.format("parent value:\t%s", parent.val.toString()));
			// sb.append(String.format("left char:\t%s", left.c));
			// sb.append(String.format("left value:\t%s", left.val.toString()));
			// sb.append(String.format("middle char:\t%s", middle.c));
			// sb.append(String.format("middle value:\t%s", middle.val.toString()));
			// sb.append(String.format("right char:\t%s", right.c));
			// sb.append(String.format("right value:\t%s", right.val.toString()));
			return sb.toString();
		}
	}

	private int N; // size

	private Node<V> root; // root of TST

	/**
	 * Initializes an empty string symbol table.
	 */
	public TST() {
	}

	public void clear() {

	}

	private void collect(Node<V> x, StringBuilder prefix, int i, String pattern, Queue<String> queue) {
		if (x == null)
			return;
		char c = pattern.charAt(i);
		if (c == '.' || c < x.getCharacter())
			collect(x.getLeft(), prefix, i, pattern, queue);
		if (c == '.' || c == x.getCharacter()) {
			if (i == pattern.length() - 1 && x.getValue() != null)
				queue.add(prefix.toString() + x.getCharacter());
			if (i < pattern.length() - 1) {
				collect(x.getMiddle(), prefix.append(x.getCharacter()), i + 1, pattern, queue);
				prefix.deleteCharAt(prefix.length() - 1);
			}
		}
		if (c == '.' || c > x.getCharacter())
			collect(x.getRight(), prefix, i, pattern, queue);
	}

	// all keys in subtrie rooted at x with given prefix
	private void collect(Node<V> x, StringBuilder prefix, List<String> queue) {
		if (x == null)
			return;
		collect(x.getLeft(), prefix, queue);
		if (x.getValue() != null)
			queue.add(prefix.toString() + x.getCharacter());
		collect(x.getMiddle(), prefix.append(x.getCharacter()), queue);
		prefix.deleteCharAt(prefix.length() - 1);
		collect(x.getRight(), prefix, queue);
	}

	/**
	 * Does this symbol table contain the given key?
	 * 
	 * @param key
	 *            the key
	 * @return <tt>true</tt> if this symbol table contains <tt>key</tt> and <tt>false</tt> otherwise
	 * @throws NullPointerException
	 *             if <tt>key</tt> is <tt>null</tt>
	 */
	public boolean contains(String key) {
		return get(key) != null;
	}

	// return subtrie corresponding to given key
	private Node<V> get(Node<V> x, String key, int d) {
		if (key == null)
			throw new NullPointerException();
		if (key.length() == 0)
			throw new IllegalArgumentException("key must have length >= 1");
		if (x == null)
			return null;
		char c = key.charAt(d);
		if (c < x.getCharacter())
			return get(x.getLeft(), key, d);
		else if (c > x.getCharacter())
			return get(x.getRight(), key, d);
		else if (d < key.length() - 1)
			return get(x.getMiddle(), key, d + 1);
		else
			return x;
	}

	/**
	 * Returns the value associated with the given key.
	 * 
	 * @param key
	 *            the key
	 * @return the value associated with the given key if the key is in the symbol table and <tt>null</tt> if the key is not in the symbol
	 *         table
	 * @throws NullPointerException
	 *             if <tt>key</tt> is <tt>null</tt>
	 */
	public V get(String key) {
		if (key == null)
			throw new NullPointerException();
		if (key.length() == 0)
			throw new IllegalArgumentException("key must have length >= 1");
		Node<V> x = get(root, key, 0);
		if (x == null)
			return null;
		return x.getValue();
	}

	public List<Node<V>> getLeaves() {
		List<Node<V>> ret = new ArrayList<Node<V>>();
		getLeaves(root, ret);
		return ret;
	}

	private void getLeaves(Node<V> node, List<Node<V>> leaves) {
		if (node != null) {
			if (node.getLeft() == null && node.getMiddle() == null && node.getRight() == null) {
				leaves.add(node);
			} else {
				getLeaves(node.getLeft(), leaves);
				getLeaves(node.getMiddle(), leaves);
				getLeaves(node.getRight(), leaves);
			}
		}
	}

	public Node<V> getNode(String key) {
		if (key == null)
			throw new NullPointerException();
		if (key.length() == 0)
			throw new IllegalArgumentException("key must have length >= 1");
		Node<V> x = get(root, key, 0);
		return x;
	}

	public Node<V> getRoot() {
		return root;
	}

	/**
	 * Returns all keys in the symbol table as an <tt>Iterable</tt>. To iterate over all of the keys in the symbol table named <tt>st</tt>,
	 * use the foreach notation: <tt>for (Key key : st.keys())</tt>.
	 * 
	 * @return all keys in the sybol table as an <tt>Iterable</tt>
	 */
	public List<String> keys() {
		List<String> queue = new ArrayList<String>(size());
		collect(root, new StringBuilder(), queue);
		return queue;
	}

	/**
	 * Returns all of the keys in the symbol table that match <tt>pattern</tt>, where . symbol is treated as a wildcard character.
	 * 
	 * @param pattern
	 *            the pattern
	 * @return all of the keys in the symbol table that match <tt>pattern</tt>, as an iterable, where . is treated as a wildcard character.
	 */
	public Iterable<String> keysThatMatch(String pattern) {
		Queue<String> queue = new LinkedList<String>();
		collect(root, new StringBuilder(), 0, pattern, queue);
		return queue;
	}

	/**
	 * Returns all of the keys in the set that start with <tt>prefix</tt>.
	 * 
	 * @param prefix
	 *            the prefix
	 * @return all of the keys in the set that start with <tt>prefix</tt>, as an iterable
	 */
	public Iterable<String> keysWithPrefix(String prefix) {
		List<String> queue = new LinkedList<String>();
		Node<V> x = get(root, prefix, 0);
		if (x == null)
			return queue;
		if (x.getValue() != null)
			queue.add(prefix);
		collect(x.getMiddle(), new StringBuilder(prefix), queue);
		return queue;
	}

	/**
	 * Returns the string in the symbol table that is the longest prefix of <tt>query</tt>, or <tt>null</tt>, if no such string.
	 * 
	 * @param query
	 *            the query string
	 * @return the string in the symbol table that is the longest prefix of <tt>query</tt>, or <tt>null</tt> if no such string
	 * @throws NullPointerException
	 *             if <tt>query</tt> is <tt>null</tt>
	 */
	public String longestPrefixOf(String query) {
		if (query == null || query.length() == 0)
			return null;
		int length = 0;
		Node<V> x = root;
		int i = 0;
		while (x != null && i < query.length()) {
			char c = query.charAt(i);
			if (c < x.getCharacter())
				x = x.getLeft();
			else if (c > x.getCharacter())
				x = x.getRight();
			else {
				i++;
				if (x.getValue() != null)
					length = i;
				x = x.getMiddle();
			}
		}
		return query.substring(0, length);
	}

	private Node<V> put(Node<V> p, Node<V> x, String key, V val, int d) {
		char c = key.charAt(d);
		if (x == null) {
			x = new Node<V>();
			x.setCharacter(c);
			x.setLevel(d);
		}

		// System.out.printf("%s: %d, %c\n", key, d, c);

		if (c < x.getCharacter()) {
			x.setLeft(put(x, x.getLeft(), key, val, d));
		} else if (c > x.getCharacter()) {
			x.setRight(put(x, x.getRight(), key, val, d));
		} else if (d < key.length() - 1) {
			x.setMiddle(put(x, x.getMiddle(), key, val, d + 1));
		} else {
			x.setValue(val);
		}
		x.setParent(p);
		return x;
	}

	/**
	 * Inserts the key-value pair into the symbol table, overwriting the old value with the new value if the key is already in the symbol
	 * table. If the value is <tt>null</tt>, this effectively deletes the key from the symbol table.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * @throws NullPointerException
	 *             if <tt>key</tt> is <tt>null</tt>
	 */
	public void put(String key, V val) {
		if (!contains(key)) {
			N++;
		}

		// System.out.println("#############");
		root = put(root, root, key, val, 0);
		root.setParent(null);
	}

	public void setRoot(Node<V> root) {
		this.root = root;
	}

	public void setSize(int size) {
		this.N = size;
	}

	/**
	 * Returns the number of key-value pairs in this symbol table.
	 * 
	 * @return the number of key-value pairs in this symbol table
	 */
	public int size() {
		return N;
	}

	public int sizeOfAllNodes() {
		int ret = 0;

		return ret;
	}

}