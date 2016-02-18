package ohs.eden.linker;

import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.tree.trie.Trie;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class CategoryTreeBuilder {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", CategoryTreeBuilder.class.getName());

		CategoryTreeBuilder d = new CategoryTreeBuilder();
		d.readData();
		d.build();

		System.out.printf("[%s] ends.\n", CategoryTreeBuilder.class.getName());
	}

	private SetMap<Integer, Integer> parentChildMap;

	private BidMap<Integer, String> idCatMap;

	private int root_id = 192834;

	private int health_id = 153550;

	private SetMap<Integer, Integer> childParentMap;

	private Trie<String> trie;

	private TextFileWriter writer;

	private Set<Integer> mainTopics;

	public void build() throws Exception {

		writer = new TextFileWriter(ELPath.WIKI_DIR + "wiki_cat_tree.txt");

		for (int c : childParentMap.keySet()) {
			goUp(c);
		}

		// trie = Trie.newTrie();

		// Stack<Integer> path = new Stack<Integer>();
		// path.push(health_id);
		//
		// goDown(path);

		// writer.close();

		// FileUtils.writeStrCounter(ELPath.WIKI_DIR + "wiki_cat_tree.txt", catPathCnts);

		// goDown(idPath);

		// mainTopics = parentChildMap.get(root_id);
		// mainTopics.remove(health_id);

		// goDown(health_id);

	}

	private void goDown(int c) {
		List<Integer> catPath = Generics.newArrayList();
		catPath.add(c);

		Set<Integer> visited = Generics.newHashSet();

		goDown(catPath, visited);

		writer.close();
	}

	private void goDown(List<Integer> catPath, Set<Integer> visited) {
		int c = catPath.get(catPath.size() - 1);
		Set<Integer> children = parentChildMap.get(c);

		if (visited.contains(c)) {
			List<String> list = Generics.newArrayList();

			for (int cc : catPath) {
				list.add(idCatMap.getValue(cc));
			}
			String catPathStr = StrUtils.join("->", list);
			// System.out.println(catPathStr);
		} else if (mainTopics.contains(c)) {
			List<String> list = Generics.newArrayList();

			for (int cc : catPath) {
				list.add(idCatMap.getValue(cc));
			}
			String catPathStr = StrUtils.join("->", list);
			System.out.println(catPathStr);
		} else {
			visited.add(c);

			if (children.size() == 0) {
				List<String> list = Generics.newArrayList();

				for (int cc : catPath) {
					list.add(idCatMap.getValue(cc));
				}
				Collections.reverse(list);
				String catPathStr = StrUtils.join("->", list);
				writer.write(catPathStr + "\n");
			} else {
				for (int child_id : children) {
					List<Integer> catPath2 = Generics.newArrayList(catPath);
					Set<Integer> visited2 = Generics.newHashSet(visited);
					catPath2.add(child_id);

					goDown(catPath2, visited2);
				}
			}
		}

	}

	private void goDown(Stack<Integer> idPath) {

		int parent_id = idPath.peek();

		Set<Integer> children = parentChildMap.get(parent_id, true);

		if (children.size() == 0) {
			StringBuffer sb = new StringBuffer();

			List<String> list = Generics.newArrayList();
			for (int i = 0; i < idPath.size(); i++) {
				int id = idPath.get(i);
				list.add(idCatMap.getValue(id));
			}

			trie.insert(list);

		} else {
			Set<Integer> mainToipcs = parentChildMap.get(root_id, false);

			for (int child_id : children) {
				if (idPath.contains(child_id)) {
					continue;
				}

				if (mainToipcs.contains(child_id)) {
					continue;
				}

				String child = idCatMap.getValue(child_id);

				idPath.push(child_id);

				goDown(idPath);

				idPath.pop();
			}
		}
	}

	private void goUp(int c) {
		List<Integer> catPath = Generics.newArrayList();
		catPath.add(c);

		Set<Integer> visited = Generics.newHashSet();
		// visited.add(c);

		goUp(catPath, visited);
	}

	private void goUp(List<Integer> catPath, Set<Integer> visited) {
		int c = catPath.get(catPath.size() - 1);

		if (visited.contains(c)) {
			// List<String> list = Generics.newArrayList();
			// for (int cc : catPath) {
			// list.add(idCatMap.getValue(cc));
			// }
			// String catPathStr = StrUtils.join("->", list);
			// System.out.println(catPathStr);
		} else {
			visited.add(c);

			Set<Integer> parents = childParentMap.get(c);

			if (parentChildMap.get(root_id).contains(c) && c == health_id) {
				List<String> list = Generics.newArrayList();

				for (int cc : catPath) {
					list.add(idCatMap.getValue(cc));
				}
				String catPathStr = StrUtils.join("->", list);
				// System.out.println(catPathStr);

				writer.write(catPathStr + "\n");
			} else {
				if (parents.size() > 0) {
					for (int p : parents) {
						List<Integer> catPath2 = Generics.newArrayList(catPath);
						catPath2.add(p);

						Set<Integer> visited2 = Generics.newHashSet(visited);

						goUp(catPath2, visited2);

					}
				}
			}
		}

	}

	public void readData() throws Exception {

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "wiki_categorylink_encoded.ser.gz");
			parentChildMap = FileUtils.readIntSetMap(ois);
			ois.close();
		}
		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "wiki_category_encoded.ser.gz");
			idCatMap = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		childParentMap = Generics.newSetMap();

		for (int p : parentChildMap.keySet()) {
			for (int c : parentChildMap.get(p)) {
				childParentMap.put(c, p);
			}
		}
	}

}
