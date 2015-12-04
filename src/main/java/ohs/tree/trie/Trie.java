package ohs.tree.trie;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ohs.tree.trie.Node.Type;

public class Trie<K> {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Process begins.");

		System.out.println("Process ends.");
	}

	private int depth = 1;

	private int size = 1;

	private Node<K> root;

	public Trie() {
		root = new Node<K>(null, null, depth, null, 0);
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
		Node<K> node = root;
		node.increaseCount();

		for (K key : keys) {
			Node<K> child;
			if (node.hasChild(key)) {
				child = node.getChild(key);
				child.increaseCount();
			} else {
				child = new Node<K>(node, key, node.getDepth() + 1, null, size++);
				node.addChild(child);
			}
			node = child;
			if (node.getDepth() > depth) {
				depth = node.getDepth();
			}
		}

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

	// public void SmithWatermanScorer(){
	// Trie<Integer> trie = new Trie<Integer>();
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

	public void read(File inputFile) {

	}

	private void recursive(Node<K> node, StringBuffer sb, int depthToPrint) {
		if (node.hasChildren() && node.getDepth() < depthToPrint) {
			int cnt = 0;
			for (Node<K> child : node.getChildren().values()) {
				sb.append("\n");
				for (int j = 0; j < child.getDepth(); j++) {
					sb.append("  ");
				}
				sb.append(String.format("(%d, %d) -> %s", child.getDepth(), ++cnt, child.getKey()));
				recursive(child, sb, depthToPrint);
			}
		}
	}

	public Node<K> search(K[] keys) {
		Node<K> node = root;

		for (K key : keys) {
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

	public String toString() {
		return toString(2);
	}

	public String toString(int depthToPrint) {
		NumberFormat nf = NumberFormat.getInstance();
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("Depth:\t%s\n", nf.format(depth)));
		sb.append(String.format("Node Size:\t%s\n", nf.format(size)));

		recursive(root, sb, depthToPrint);

		return sb.toString().trim();
	}

	public void write(File outputFile) {

	}
}
