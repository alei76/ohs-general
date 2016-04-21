package ohs.eden.linker;

import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import ohs.io.FileUtils;
import ohs.string.sim.EditDistance;
import ohs.string.sim.SequenceFactory;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.SetMap;
import ohs.utils.Generics;

public class CategoryTreeBuilder {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", CategoryTreeBuilder.class.getName());

		CategoryTreeBuilder ctb = new CategoryTreeBuilder();
		ctb.build();

		System.out.printf("[%s] ends.\n", CategoryTreeBuilder.class.getName());
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

	private boolean bottomUp(Stack<Integer> path) {
		int c = path.peek();
		String cat = idToCat.getValue(c);

		Set<Integer> parents = childToParents.get(c, false);

		if (mainCats.contains(c) || parents == null) {
			if (c == health_id) {
				List<String> catPath = Generics.newArrayList();
				for (int i = 0; i < path.size(); i++) {
					catPath.add(idToCat.getValue(path.get(i)));
				}
				Collections.reverse(catPath);

				trie.insert(catPath);

				return true;
			} else {
				return false;
			}
		}

		// int p = -1;
		//
		// if (parents.size() == 0) {
		// p = parents.iterator().next();
		// } else {
		// Counter<Integer> pageScores = Generics.newCounter();
		//
		// for (int nc : parents) {
		// String nCat = idToCat.getValue(nc);
		//
		// if (nCat.startsWith("Commons_category_") || nCat.startsWith("Hidden_")) {
		// continue;
		// }
		// pageScores.setCount(nc, 0);
		// }
		//
		// if (pageScores.size() > 0) {
		// for (int nc : pageScores.keySet()) {
		// pageScores.setCount(nc, catPageCnts.getCount(nc));
		// }
		//
		// if (pageScores.max() > 0) {
		// p = pageScores.argMax();
		// }
		//
		// if (p == -1) {
		// for (int nc : pageScores.keySet()) {
		// double sim = ed.getSimilarity(SequenceFactory.newCharSequences(cat, idToCat.getValue(nc)));
		// pageScores.setCount(nc, sim);
		// }
		//
		// p = pageScores.argMax();
		// }
		// }
		// }

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

			String nCat = idToCat.getValue(p);
			if (nCat.startsWith("Commons_category_") || nCat.startsWith("Hidden_")) {
				continue;
			}

			path.push(p);

			if (bottomUp(path)) {
				return true;
			}

			path.pop();
		}

		return false;
	}

	public void build() throws Exception {

		read();

		// for (int id : mainTopics) {
		// System.out.println(idToCat.getValue(id));
		// }

		// Stack<Integer> path = new Stack<Integer>();
		// path.push(health_id);

		// TextFileWriter writer = new TextFileWriter(ELPath.WIKI_DIR + "wiki_cat_tree.txt");

		trie = Trie.newTrie();
		int num_nodes = 0;
		for (int c : leaves) {
			if (++num_nodes % 100 == 0) {
				System.out.printf("\r[%d/%d]", num_nodes, leaves.size());
			}

			Stack<Integer> path = new Stack<Integer>();
			path.push(c);

			bottomUp(path);
		}

		System.out.printf("\r[%d/%d]\n", num_nodes, leaves.size());

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

	private void topDown(Stack<Integer> path) {

		int parent_id = path.peek();

		Counter<String> catCnts = Generics.newCounter();
		Counter<String> mainCatCnts = Generics.newCounter();

		String[] catPath = new String[path.size()];

		for (int i = 0; i < path.size(); i++) {
			int catid = path.get(i);
			String cat = idToCat.getValue(catid);
			catPath[i] = cat;

			catCnts.incrementCount(cat, 1);

			if (mainCats.contains(catid)) {
				mainCatCnts.incrementCount(cat, 1);
			}
		}

		if (catCnts.max() > 1) {
			return;
		}

		if (mainCatCnts.size() > 1) {
			return;
		}

		Set<Integer> children = parentToChilds.get(parent_id, false);

		if (children == null) {
			trie.insert(catPath);

		} else {
			for (int child_id : children) {
				String child = idToCat.getValue(child_id);

				path.push(child_id);
				topDown(path);
				path.pop();
			}
		}
	}

}
