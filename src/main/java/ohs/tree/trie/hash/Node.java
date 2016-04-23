package ohs.tree.trie.hash;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import ohs.utils.Generics;

public class Node<K> implements Serializable {

	public static enum Type {
		ROOT, NON_LEAF, LEAF;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 6414918490989194051L;

	protected int id;
	protected K key;
	protected int cnt = 0;
	protected Map<K, Node<K>> children;
	protected Node<K> parent;
	protected Object data;

	public Node() {

	}

	public void setKey(K key) {
		this.key = key;
	}

	public void setCount(int cnt) {
		this.cnt = cnt;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Node(Node<K> parent, K key, Object data, int id) {
		this.parent = parent;
		this.key = key;
		this.children = null;
		this.data = data;
		this.cnt = 0;
		this.id = id;
	}

	public void addChild(Node<K> node) {
		if (children == null) {
			children = new HashMap<K, Node<K>>();
		}
		children.put(node.getKey(), node);
	}

	// public Node(Node<K> parent, K key, int depth, int id) {
	// this(parent, key, depth, null, id);
	// }

	// public void write(ObjectOutputStream ois) {
	//
	// }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (id != other.id)
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
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

	public int getCount() {
		return cnt;
	}

	public Object getData() {
		return data;
	}

	public int getDepth() {
		int ret = 1;
		Node<K> node = this;
		while (!node.isRoot()) {
			node = node.getParent();
			ret++;
		}
		return ret;
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
		if (parent == null) {
			ret = Type.ROOT;
		} else if (children == null) {
			ret = Type.LEAF;
		}
		return ret;
	}

	public boolean hasChild(K key) {
		return children == null || !children.containsKey(key) ? false : true;
	}

	public boolean hasData() {
		return data == null ? false : true;
	}

	public boolean hasChildren() {
		return children == null || children.size() == 0 ? false : true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	public boolean hasParent() {
		return parent == null ? false : true;
	}

	public void incrementCount() {
		cnt++;
	}

	public boolean isLeaf() {
		return getType() == Type.LEAF ? true : false;
	}

	public boolean isRoot() {
		return getType() == Type.ROOT ? true : false;
	}

	public void read(ObjectInputStream ois) throws Exception {
		id = ois.readInt();
		key = (K) ois.readObject();
		cnt = ois.readInt();
		data = ois.readObject();
	}

	public void setChildren(Map<K, Node<K>> children) {
		this.children = children;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public void setParent(Node<K> parent) {
		this.parent = parent;
	}

	// public boolean isLeaf() {
	// return children == null ? true : false;
	// }

	// public boolean isRoot() {
	// return depth == 1 ? true : false;
	// }

	public int sizeOfChildren() {
		return children == null ? 0 : children.size();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		Type type = getType();

		sb.append(String.format("id:\t%s\n", id));
		sb.append(String.format("type:\t%s\n", type));
		sb.append(String.format("count:\t%d\n", cnt));
		sb.append(String.format("depth:\t%d\n", getDepth()));
		sb.append(String.format("key:\t%s\n", key == null ? "null" : key.toString()));

		if (type != Type.ROOT) {
			sb.append(String.format("key path:\t%s\n", "R->" + getKeyPath("->")));
		}
		sb.append(String.format("children:\t%d\n", getChildren().size()));
		int no = 0;
		for (Node<K> child : getChildren().values()) {
			sb.append(String.format("  %dth %s -> %d children\n", ++no, child.key, child.getChildren().size()));
		}
		return sb.toString().trim();
	}

	public void trimToSize() {
		if (children != null) {
			Map<K, Node<K>> temp = Generics.newHashMap(children.size());
			for (Entry<K, Node<K>> e : children.entrySet()) {
				temp.put(e.getKey(), e.getValue());
			}
			children = temp;
		}
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(id);
		oos.writeObject(key);
		oos.writeInt(cnt);
		oos.writeObject(data);
		oos.flush();
	}

}
