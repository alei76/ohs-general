package ohs.ir.wiki;

import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import ohs.eden.linker.ELPath;
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

	private Trie<String> trie;

	private Set<Integer> mainCats;

	private EditDistance<Character> ed = new EditDistance<Character>();

	private Set<Integer> leaves;

	private SetMap<Integer, Integer> pageToCats;

	private void bottomUp(Stack<Integer> path, Trie<String> trie) {
		int c = path.peek();
		String cat = idToCat.getValue(c);

		Set<Integer> parents = childToParents.get(c, false);

		if (parents == null) {
			return;
		}

		List<String> catPath = Generics.newArrayList(path.size());

		for (int i = 0; i < path.size(); i++) {
			int loc = path.size() - i - 1;
			catPath.add(idToCat.getValue(path.get(loc)));
		}

		if (mainCats.contains(c)) {
			// if (c == health_id) {
			// List<String> catPath = Generics.newArrayList(path.size());
			//
			// for (int i = 0; i < path.size(); i++) {
			// int loc = path.size() - i - 1;
			// catPath.add(idToCat.getValue(path.get(loc)));
			// }

			trie.insert(catPath);
			// }
			return;
		}

		Counter<Integer> catCnts = Generics.newCounter();

		for (int catid : path) {
			catCnts.incrementCount(catid, 1);
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

			String nCat = idToCat.getValue(p);
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

		// for (int id : mainTopics) {
		// System.out.println(idToCat.getValue(id));
		// }

		// Stack<Integer> path = new Stack<Integer>();
		// path.push(health_id);

		// TextFileWriter writer = new TextFileWriter(ELPath.WIKI_DIR + "wiki_cat_tree.txt");

		trie = Trie.newTrie();
		int num_nodes = 0;

		StopWatch stopWatch = StopWatch.newStopWatch();

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

			Trie<String> trie2 = Trie.newTrie();

			bottomUp(path, trie2);

			List<Node<String>> leaves = trie2.getLeafNodes();

			System.out.println(idToCat.getValue(c));
			for (int i = 0; i < leaves.size(); i++) {
				System.out.println(leaves.get(i).getKeyPath(" -> "));
			}
			System.out.println();
		}

		System.out.printf("\r[%d/%d, %s]\n", num_nodes, leaves.size(), stopWatch.stop());

		// topDown(path);

		// writer.close();

		// FileUtils.writeStrCounter(ELPath.WIKI_DIR + "wiki_cat_tree.txt", leaves);

		List<String> catPaths = Generics.newArrayList();

		for (Node<String> node : trie.getLeafNodes()) {
			List<String> keyPath = node.getKeyPath();
			
			
			
			catPaths.add(node.getKeyPath(" -> "));
		}

		Collections.sort(catPaths);

		FileUtils.writeStrCollection(ELPath.WIKI_DIR + "wiki_cat_tree.txt", catPaths);
	}

	public void buildTopDown() throws Exception {
		read();

		// for (int id : mainTopics) {
		// System.out.println(idToCat.getValue(id));
		// }

		// Stack<Integer> path = new Stack<Integer>();
		// path.push(health_id);

		// TextFileWriter writer = new TextFileWriter(ELPath.WIKI_DIR + "wiki_cat_tree.txt");

		trie = Trie.newTrie();
		
		/*
		 * 349052 -> Diseases_and_disorders
		 * 198457 -> Medicine
		 */

		int[] roots = { 349052, 198457 };

		List<String> catPaths = Generics.newArrayList();

		for (int i = 0; i < roots.length; i++) {
			// root_id = idToCat.getKey(roots[i]);
			Stack<Integer> path = new Stack<Integer>();
			path.push(roots[i]);

			topDown(path);

			for (Node<String> node : trie.getLeafNodes()) {
				catPaths.add(node.getKeyPath("\t"));
			}
		}

		Set<String> res = Generics.newTreeSet();
		res.addAll(catPaths);

		FileUtils.writeStrCollection(MIRPath.WIKI_DIR + "wiki_cat_tree.txt", res);
	}

	private boolean isValid(int catid) {
		String cat = idToCat.getValue(catid);

		if (cat == null ||

				cat.startsWith("Commons_category")

				|| cat.startsWith("Hidden_categories")

		// || cat.contains("Wikipedia")

		) {
			System.out.println(cat);

			return false;
		}

		return true;
	}

	private Counter<Integer> subCatCnts;

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
				trie.insert(catPath);
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
