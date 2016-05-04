package ohs.ir.wiki;

import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import ohs.io.FileUtils;
import ohs.ir.medical.general.MIRPath;
import ohs.string.sim.EditDistance;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

public class CategoryTreeBuilder {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		CategoryTreeBuilder ctb = new CategoryTreeBuilder();
		// ctb.buildBottomUp();
		ctb.buildTopDown();

		System.out.println("process ends.");
	}

	private BidMap<Integer, String> idToCat = null;

	private Counter<Integer> pageCnts = null;

	private SetMap<Integer, Integer> parentToChildren = null;

	private SetMap<Integer, Integer> childToParents = null;

	private int root_id = 192834;
	//
	// private int health_id = 153550;

	private Trie<Integer> trie;

	private Set<Integer> mainCats;

	private EditDistance<Character> ed = new EditDistance<Character>();

	private Set<Integer> leaves;

	private SetMap<Integer, Integer> pageToCats;

	private Counter<Integer> subCatCnts;

	private void bottomUp(Stack<Integer> path, Trie<Integer> trie) {
		int c = path.peek();
		String cat = idToCat.getValue(c);

		Set<Integer> parents = childToParents.get(c, false);

		if (parents == null) {
			return;
		}

		List<Integer> path2 = Generics.newArrayList(path);
		Collections.reverse(path2);

		List<String> catPath = Generics.newArrayList(path2.size());

		for (int i = 0; i < path2.size(); i++) {
			catPath.add(idToCat.getValue(path2.get(i)));
		}

		if (mainCats.contains(c)) {
			trie.insert(path2);
			return;
		}

		Counter<Integer> catCnts = Generics.newCounter();

		for (int cid : path) {
			catCnts.incrementCount(cid, 1);
		}

		Counter<Integer> pageCnts = Generics.newCounter();

		for (int p : parents) {
			pageCnts.setCount(p, pageCnts.getCount(p));
		}

		for (int p : pageCnts.getSortedKeys()) {
			if (catCnts.containsKey(p)) {
				continue;
			}

			if (leaves.contains(p)) {
				continue;
			}

			if (!isValid(p)) {
				continue;
			}

			path.push(p);

			bottomUp(path, trie);

			path.pop();
		}
	}

	public void buildBottomUp() throws Exception {

		read();

		int num_nodes = 0;

		StopWatch stopWatch = StopWatch.newStopWatch();

		int[] roots = { 349052, 198457 };

		Set<Integer> rootSet = Generics.newHashSet();

		for (int id : roots) {
			rootSet.add(id);
		}

		Set<String> catPaths = Generics.newTreeSet();

		for (int c : leaves) {
			if (++num_nodes % 1000 == 0) {
				System.out.printf("\r[%d/%d, %s]", num_nodes, leaves.size(), stopWatch.stop());
				break;
			}

			if (!isValid(c)) {
				continue;
			}

			Stack<Integer> path = new Stack<Integer>();
			path.push(c);

			Trie<Integer> trie = Trie.newTrie();

			bottomUp(path, trie);

			List<Node<Integer>> leaves = trie.getLeafNodes();

			for (int i = 0; i < leaves.size(); i++) {
				List<Integer> keyPath = leaves.get(i).getKeyPath();
				boolean is_valid = false;

				for (int j = 0; j < keyPath.size(); j++) {
					if (rootSet.contains(keyPath.get(j))) {
						is_valid = true;
						break;
					}
				}

				if (is_valid) {
					List<String> catPath = Generics.newArrayList();

					for (int j = 0; j < keyPath.size(); j++) {
						catPath.add(idToCat.getValue(keyPath.get(j)));
					}

					catPaths.add(StrUtils.join("\t", catPath));

				}

			}
			System.out.println();
		}

		System.out.printf("\r[%d/%d, %s]\n", num_nodes, leaves.size(), stopWatch.stop());

		FileUtils.writeStrCollection(MIRPath.WIKI_DIR + "wiki_cat_tree_bottom-up.txt", catPaths);
	}

	public void buildTopDown() throws Exception {
		read();

		trie = Trie.newTrie();

		/*
		 * 349052 -> Diseases_and_disorders 198457 -> Medicine
		 */

		int[] roots = { 349052, 198457 };

		List<String> catPaths = Generics.newArrayList();

		for (int i = 0; i < roots.length; i++) {
			// root_id = idToCat.getKey(roots[i]);
			Stack<Integer> path = new Stack<Integer>();
			path.push(roots[i]);

			topDown(path);

			for (Node<Integer> node : trie.getLeafNodes()) {
				List<Integer> keyPath = node.getKeyPath();

				List<String> catPath = Generics.newArrayList();

				for (int cid : keyPath) {
					catPath.add(idToCat.getValue(cid));
				}
				catPaths.add(StrUtils.join("\t", catPath));
			}
		}

		Set<String> res = Generics.newTreeSet();
		res.addAll(catPaths);

		FileUtils.writeStrCollection(MIRPath.WIKI_DIR + "wiki_cat_tree_top-down.txt", res);
	}

	private boolean isValid(int catid) {
		String cat = idToCat.getValue(catid);

		if (cat == null ||

				cat.startsWith("Commons_category")

				|| cat.startsWith("Hidden_categories")

		// || cat.contains("Wikipedia")

		) {
			// System.out.println(cat);

			return false;
		}

		return true;
	}

	public void read() throws Exception {
		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_catlinks.ser.gz");
			parentToChildren = FileUtils.readIntSetMap(ois);
			ois.close();

			childToParents = Generics.newSetMap();

			for (int p : parentToChildren.keySet()) {
				for (int c : parentToChildren.get(p)) {
					childToParents.put(c, p);
				}
			}

		}

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_cats.ser.gz");
			List<Integer> ids = FileUtils.readIntList(ois);
			List<String> titles = FileUtils.readStrList(ois);
			List<Integer> catPages = FileUtils.readIntList(ois);
			List<Integer> catSubcats = FileUtils.readIntList(ois);
			ois.close();

			idToCat = Generics.newBidMap(ids.size());
			pageCnts = Generics.newCounter();
			subCatCnts = Generics.newCounter();

			for (int i = 0; i < ids.size(); i++) {
				idToCat.put(ids.get(i), titles.get(i));
				pageCnts.setCount(ids.get(i), catPages.get(i));
				subCatCnts.setCount(ids.get(i), catSubcats.get(i));
			}

		}

		mainCats = Generics.newHashSet(parentToChildren.get(root_id));
		// mainCats.add(root_id);

		for (int id : mainCats) {
			System.out.println(idToCat.getValue(id));
		}

		{
			leaves = Generics.newHashSet();

			for (int c : idToCat.getKeys()) {
				int cnt = (int) pageCnts.getCount(c);
				if (parentToChildren.get(c, false) == null && childToParents.get(c, false) != null && cnt > 0) {
					leaves.add(c);
				}
			}

			// for (int c : leaves) {
			// System.out.println(idToCat.getValue(c));
			// }
		}
	}

	private void topDown(Stack<Integer> path) {

		int parent_id = path.peek();
		String parent = idToCat.getValue(parent_id);

		List<String> catPath = Generics.newArrayList(path.size());
		for (int catid : path) {
			catPath.add(idToCat.getValue(catid));
		}

		Set<Integer> children = parentToChildren.get(parent_id, false);

		if (children == null || catPath.size() >= 10) {
			if (pageCnts.getCount(parent_id) > 0) {
				trie.insert(path);
			}
		} else {
			for (int child_id : children) {
				String child = idToCat.getValue(child_id);

				if (!isValid(child_id)) {
					continue;
				}

				if (path.contains(child_id)) {
					continue;
				}

				if (mainCats.contains(child_id)) {
					continue;
				}

				if (child_id == root_id) {
					continue;
				}

				path.push(child_id);
				topDown(path);
				path.pop();
			}
		}
	}

}
