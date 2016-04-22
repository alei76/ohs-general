package ohs.eden.linker;

import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.string.sim.EditDistance;
import ohs.string.sim.SequenceFactory;
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

	private Counter<Integer> catPageCnts = null;

	private SetMap<Integer, Integer> parentToChilds = null;

	private SetMap<Integer, Integer> childToParents = null;

	private int root_id = 192834;

	private int health_id = 153550;

	private Trie<String> trie;

	private Set<Integer> mainCats;

	private EditDistance<Character> ed = new EditDistance<Character>();

	private Set<Integer> leaves;

	private void topDown(Stack<Integer> path) {

		int parent_id = path.peek();
		String parent = idToCat.getValue(parent_id);

		List<String> catPath = Generics.newArrayList(path.size());
		for (int catid : path) {
			catPath.add(idToCat.getValue(catid));
		}

		Set<Integer> children = parentToChilds.get(parent_id, false);

		if (children == null || catPath.size() >= 5) {
			trie.insert(catPath);
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
			if (c == health_id) {
				// List<String> catPath = Generics.newArrayList(path.size());
				//
				// for (int i = 0; i < path.size(); i++) {
				// int loc = path.size() - i - 1;
				// catPath.add(idToCat.getValue(path.get(loc)));
				// }

				trie.insert(catPath);
			}
			return;
		}

		Counter<Integer> catCnts = Generics.newCounter();

		for (int catid : path) {
			catCnts.incrementCount(catid, 1);
		}

		Counter<Integer> pageCnts = Generics.newCounter();

		for (int p : parents) {
			pageCnts.setCount(p, catPageCnts.getCount(p));
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

	private boolean isValid(int catid) {
		String cat = idToCat.getValue(catid);

		if (cat.startsWith("Commons_category")

				|| cat.startsWith("Hidden_categories")

		// || cat.contains("Wikipedia")

		) {
			System.out.println(cat);

			return false;
		}

		return true;
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

		Stack<Integer> path = new Stack<Integer>();
		path.push(health_id);

		topDown(path);

		List<String> catPaths = Generics.newArrayList();

		for (Node<String> node : trie.getLeafNodes()) {
			catPaths.add(node.getKeyPath("\t"));
		}

		Collections.sort(catPaths);

		FileUtils.writeStrCollection(ELPath.WIKI_DIR + "wiki_cat_tree.txt", catPaths);
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
			catPaths.add(node.getKeyPath(" -> "));
		}

		Collections.sort(catPaths);

		FileUtils.writeStrCollection(ELPath.WIKI_DIR + "wiki_cat_tree.txt", catPaths);
	}

	public void read() throws Exception {
		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "encoded_wiki_categorylink.ser.gz");
			parentToChilds = FileUtils.readIntSetMap(ois);
			ois.close();

			childToParents = Generics.newSetMap();

			for (int p : parentToChilds.keySet()) {
				for (int c : parentToChilds.get(p)) {
					childToParents.put(c, p);
				}
			}

		}

		catPageCnts = Generics.newCounter();

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "encoded_wiki_category.ser.gz");
			List<Integer> ids = FileUtils.readIntList(ois);
			List<String> titles = FileUtils.readStrList(ois);
			List<Integer> catPages = FileUtils.readIntList(ois);
			List<Integer> catSubcats = FileUtils.readIntList(ois);
			ois.close();

			idToCat = Generics.newBidMap(ids.size());

			for (int i = 0; i < ids.size(); i++) {
				idToCat.put(ids.get(i), titles.get(i));
				catPageCnts.setCount(ids.get(i), catPages.get(i));
			}

			TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_category.csv.gz");
			while (reader.hasNext()) {
				/*
				 * "id"\t"title"
				 */
				String[] parts = reader.next().split("\t");
				parts = StrUtils.unwrap(parts);

				int id = Integer.parseInt(parts[0]);
				String cat_title = parts[1];
				int cat_pages = Integer.parseInt(parts[2]);
				int cat_subcats = Integer.parseInt(parts[3]);

				ids.add(id);
				titles.add(cat_title);
				catPages.add(cat_pages);
				catSubcats.add(cat_subcats);

			}
			reader.close();
		}

		mainCats = Generics.newHashSet(parentToChilds.get(root_id));
		mainCats.add(root_id);

		{
			leaves = Generics.newHashSet();

			for (int c : idToCat.getKeys()) {
				int cnt = (int) catPageCnts.getCount(c);
				if (parentToChilds.get(c, false) == null && childToParents.get(c, false) != null && cnt > 0) {
					leaves.add(c);
				}
			}

			// for (int c : leaves) {
			// System.out.println(idToCat.getValue(c));
			// }
		}
	}

}
