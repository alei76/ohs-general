package ohs.tree.trie;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class Node<K> implements Serializable {

	public static enum Type {
		ROOT, NON_LEAF, LEAF;
	}

	protected Map<K, Node<K>> children;
	protected int count;
	protected int depth;
	protected K key;
	protected Node<K> parent;
	protected Object data;

	// public Node(Node<K> parent, K key, int depth, int id) {
	// this(parent, key, depth, null, id);
	// }

	// public void write(ObjectOutputStream ois) {
	//
	// }

	protected int id;

	public Node(Node<K> parent, K key, int depth, Object data, int id) {
		this.parent = parent;
		this.key = key;
		this.depth = depth;
		this.children = null;
		this.data = data;
		this.count = 1;
		this.id = id;
	}

	public void addChild(Node<K> node) {
		if (children == null) {
			children = new HashMap<K, Node<K>>();
		}
		children.put(node.getKey(), node);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Node)) {
			return false;
		}
		Node other = (Node) obj;
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		} else if (!parent.equals(other.parent)) {
			return false;
		}
		return true;
	}

	public Node<K> getChild(K key) {
		return children.get(key);
	}

	public Map<K, Node<K>> getChildren() {
		Map<K, Node<K>> ret = children;
		if (ret == null) {
			ret = new HashMap<K, Node<K>>();
		}
		return ret;
	}

	public double getCount() {
		return count;
	}

	public Object getData() {
		return data;
	}

	public int getDepth() {
		return depth;
	}

	public int getID() {
		return id;
	}

	public K getKey() {
		return key;
	}

	public List<K> getKeyPath() {
		List<K> ret = new ArrayList<K>();
		for (Node<K> parent : getNodePath()) {
			ret.add(parent.getKey());
		}
		return ret;
	}

	public String getKeyPath(String delim) {
		List<K> keyPath = getKeyPath();
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < keyPath.size(); i++) {
			K key = keyPath.get(i);
			if (i == 0) {
				sb.append(key.toString());
			} else {
				sb.append(delim + key.toString());
			}
		}
		return sb.toString();
	}

	public List<Node<K>> getLeafNodesUnder() {
		List<Node<K>> ret = new ArrayList<Node<K>>();
		for (Node<K> node : getNodesUnder()) {
			if (node.getType() == Type.LEAF) {
				ret.add(node);
			}
		}
		return ret;
	}

	public List<Node<K>> getNodePath() {
		List<Node<K>> ret = getParents();
		ret.add(this);
		return ret;
	}

	public List<Node<K>> getNodesUnder() {
		return getNodesUnder(null);
	}

	public List<Node<K>> getNodesUnder(Set<Node<K>> toRemove) {
		Set<Node<K>> visited = new HashSet<Node<K>>();
		Stack<Node<K>> fringe = new Stack<Node<K>>();
		fringe.add(this);

		while (!fringe.empty()) {
			Node<K> node = fringe.pop();

			if (visited.contains(node)) {
				continue;
			}

			if (toRemove != null && toRemove.contains(node)) {
				continue;
			}

			visited.add(node);
			for (Node<K> child : node.getChildren().values()) {
				fringe.push(child);
			}
		}

		return new ArrayList<Node<K>>(visited);
	}

	public Node<K> getParent() {
		return parent;
	}

	public List<Node<K>> getParents() {
		return getParents(-1);
	}

	public List<Node<K>> getParents(int max_dist_from_node) {
		List<Node<K>> ret = new ArrayList<Node<K>>();
		Node<K> node = this;

		while (node.hasParent() && node.getParent().getType() != Type.ROOT) {
			if (max_dist_from_node != -1 && ret.size() == max_dist_from_node) {
				break;
			}
			node = node.getParent();
			ret.add(node);
		}

		Collections.reverse(ret);
		return ret;
	}

	public Type getType() {
		Type ret = Type.NON_LEAF;
		if (depth == 1) {
			ret = Type.ROOT;
		} else if (children == null) {
			ret = Type.LEAF;
		}
		return ret;
	}

	public boolean hasChild(K key) {
		return children == null || !children.containsKey(key) ? false : true;
	}

	public boolean hasChildren() {
		return children == null || children.size() == 0 ? false : true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (key == null ? 0 : key.hashCode());
		result = prime * result + (parent == null ? 0 : parent.hashCode());
		return result;
	}

	public boolean hasParent() {
		return parent == null ? false : true;
	}

	public void increaseCount() {
		increaseCount(1);
	}

	public void increaseCount(int increment) {
		count += increment;
	}

	// public boolean isLeaf() {
	// return children == null ? true : false;
	// }

	// public boolean isRoot() {
	// return depth == 1 ? true : false;
	// }

	public boolean isLeaf() {
		return getType() == Type.LEAF ? true : false;
	}

	public boolean isRoot() {
		return getType() == Type.ROOT ? true : false;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		Type type = getType();

		sb.append(String.format("ID:\t%s\n", id));
		sb.append(String.format("Type\t%s\n", type));
		sb.append(String.format("Count:\t%d\n", count));
		sb.append(String.format("Depth:\t%d\n", depth));
		sb.append(String.format("Key:\t%s\n", key == null ? "null" : key.toString()));

		if (type != Type.ROOT) {
			StringBuffer sb2 = new StringBuffer();
			List<Node<K>> nodes = getNodePath();
			for (int i = 0; i < nodes.size(); i++) {
				Node<K> node = nodes.get(i);
				sb2.append(i == nodes.size() - 1 ? node.getKey().toString() : node.getKey().toString() + "->");
			}
			sb.append(String.format("Key Path\t%s\n", sb2.toString()));
		}

		sb.append(String.format("Children:\t%d\n", getChildren().size()));
		int no = 0;
		for (Node<K> child : getChildren().values()) {
			sb.append(String.format("  %dth %s -> %d children\n", ++no, child.key, child.getChildren().size()));
		}
		return sb.toString().trim();
	}
}
