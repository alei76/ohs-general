package ohs.tree.trie.hash;

import java.io.File;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ohs.tree.trie.hash.Node.Type;

public class Trie<K> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8031071859567911644L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Process begins.");

		System.out.println("Process ends.");
	}

	public static <K> Trie<K> newTrie() {
		return new Trie<K>();
	}

	private int depth = 1;

	private int size = 1;

	private Node<K> root;

	public Trie() {
		root = new Node<K>(null, null, null, 0);
	}

	public void delete(K[] keys) {
		Node<K> node = search(keys);
		if (node.hasParent()) {
			Node<K> parent = node.getParent();
			parent.getChildren().remove(node.getKey());
		}
	}

	public Node<K> findLCA(Node<K> node1, Node<K> node2) {
		return null;
	}

	public int getDepth() {
		return depth;
	}

	public List<Node<K>> getLeafNodes() {
		return root.getLeafNodesUnder();
	}

	public Map<Integer, Node<K>> getNodeMap() {
		Map<Integer, Node<K>> ret = new HashMap<Integer, Node<K>>();
		for (Node<K> node : getNodes()) {
			ret.put(node.getID(), node);
		}
		return ret;
	}

	public List<Node<K>> getNodes() {
		return root.getNodesUnder();
	}

	public List<Node<K>> getNodesAtLevel(int level) {
		List<Node<K>> ret = new ArrayList<Node<K>>();

		for (Node<K> node : getNodes()) {
			if (node.getDepth() == level) {
				ret.add(node);
			}
		}
		return ret;
	}

	public Node<K> getRoot() {
		return root;
	}

	public Node<K> insert(K[] keys) {
		return insert(keys, 0, keys.length);
	}

	public Node<K> insert(K[] keys, int start, int end) {
		return insert(Arrays.asList(keys), start, end);
	}

	public Node<K> insert(List<K> keys) {
		return insert(keys, 0, keys.size());
	}

	public Node<K> insert(List<K> keys, int start, int end) {
		Node<K> node = root;

		for (int i = start; i < end; i++) {
			K key = keys.get(i);
			Node<K> child;
			if (node.hasChild(key)) {
				child = node.getChild(key);
			} else {
				child = new Node<K>(node, key, null, size++);
				node.addChild(child);
			}
			node = child;
			node.incrementCount();
		}
		node.incrementCount();

		int d = end - start + 1;
		depth = Math.max(depth, d);
		return node;
	}

	public Set<K> keySet() {
		Set<K> ret = new TreeSet<K>();
		for (Node<K> node : root.getNodesUnder()) {
			if (node.getType() != Type.ROOT) {
				ret.add(node.getKey());
			}
		}
		return ret;
	}

	public Set<K> keySetAtLevel(int level) {
		Set<K> ret = new HashSet<K>();
		for (Node<K> node : getNodes()) {
			if (node.getDepth() == level) {
				ret.add(node.getKey());
			}
		}
		return ret;
	}

	public void read(File inputFile) {

	}

	public Node<K> search(K[] keys) {
		return search(keys, 0, keys.length);
	}

	// public void SmithWatermanScorer(){
	// KTrie<Integer> trie = new KTrie<Integer>();
	// for (int topicId : topicIds) {
	// int[] path = info.path(topicId);
	// trie.insert(ArrayUtils.toArray(path));
	// }
	//
	// topic_parent = new SetMap<Integer, Integer>();
	// topic_child = new SetMap<Integer, Integer>();
	//
	// for (int topicId : topicIds) {
	// int[] path = info.path(topicId);
	// Node<Integer> node = trie.search(ArrayUtils.toArray(path));
	// List<Integer> neighborIds = new ArrayList<Integer>();
	// while (node.hasParent() && !node.parent().isRoot()) {
	// node = node.parent();
	// if (node.count() == 1) {
	// neighborIds.add(node.key());
	// } else {
	// break;
	// }
	// }
	//
	// if (node.count() == 1) {
	// int[] childIds = info.children(topicId);
	// for (int childId : childIds) {
	// topic_child.put(topicId, childId);
	// }
	// }
	// }
	// }

	public Node<K> search(K[] keys, int start, int end) {
		return search(Arrays.asList(keys), start, end);
	}

	public Node<K> search(List<K> keys, int start, int end) {
		Node<K> node = root;
		for (int i = start; i < end; i++) {
			K key = keys.get(i);
			if (node.hasChild(key)) {
				node = node.getChild(key);
			} else {
				node = null;
				break;
			}
		}
		return node;
	}

	public int size() {
		return size;
	}

	@Override
	public String toString() {
		return toString(2);
	}

	public String toString(int max_depth) {
		NumberFormat nf = NumberFormat.getInstance();
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("depth:\t%s\n", nf.format(depth)));
		sb.append(String.format("node size:\t%s\n", nf.format(size)));
		toString(root, sb, max_depth, 1);
		return sb.toString().trim();
	}

	private void toString(Node<K> node, StringBuffer sb, int max_depth, int depth) {
		if (node.hasChildren() && depth <= max_depth) {
			int cnt = 0;
			for (Node<K> child : node.getChildren().values()) {
				sb.append("\n");
				for (int j = 0; j < child.getDepth(); j++) {
					sb.append("  ");
				}
				sb.append(String.format("(%d, %d) -> %s", child.getDepth(), cnt++, child.getKey()));
				toString(child, sb, max_depth, depth + 1);
			}
		}
	}

	public void trimToSize() {
		trimToSize(root);
	}

	private void trimToSize(Node<K> node) {
		for (Node<K> child : node.getChildren().values()) {
			trimToSize(child);
		}
		node.trimToSize();
	}

	public void write(File outputFile) {

	}
}
