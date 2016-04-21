package ohs.eden.linker;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.tree.trie.hash.Trie;
import ohs.tree.trie.hash.Trie.SearchResult;
import ohs.tree.trie.hash.Trie.SearchResult.MatchType;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class WikiCsvDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", WikiCsvDataHandler.class.getName());

		WikiCsvDataHandler d = new WikiCsvDataHandler();
		// d.encodeTitles();
		// d.encodeCategories();
		// d.encodeCategoryLinks();
		// d.encodeRedirects();
		d.getMedicalCategories();

		System.out.printf("[%s] ends.\n", WikiCsvDataHandler.class.getName());
	}

	public void encodeCategories() throws Exception {
		List<Integer> ids = Generics.newArrayList();
		List<String> titles = Generics.newArrayList();
		List<Integer> catPages = Generics.newArrayList();
		List<Integer> catSubcats = Generics.newArrayList();

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

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(ELPath.WIKI_DIR + "encoded_wiki_category.ser.gz");
		FileUtils.writeIntCollection(oos, ids);
		FileUtils.writeStrCollection(oos, titles);
		FileUtils.writeIntCollection(oos, catPages);
		FileUtils.writeIntCollection(oos, catSubcats);
		oos.close();
	}

	public void encodeCategoryLinks() throws Exception {
		BidMap<Integer, String> idTitleMap = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "encoded_wiki_title.ser.gz");
			idTitleMap = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		BidMap<Integer, String> idCatMap = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "encoded_wiki_category.ser.gz");
			idCatMap = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		SetMap<Integer, Integer> sm = Generics.newSetMap();

		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_categorylink.csv.gz");
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = StrUtils.unwrap(line.split("\t"));

			int cl_from = Integer.parseInt(parts[0]);
			String cl_to = parts[1];
			String cl_type = parts[2];

			if (!cl_type.equals("subcat")) {
				continue;
			}

			String title_from = idTitleMap.getValue(cl_from);
			Integer child_id = idCatMap.getKey(title_from);
			Integer parent_id = idCatMap.getKey(cl_to);

			if (child_id == null || parent_id == null) {
				continue;
			}
			sm.put(parent_id, child_id);
		}
		reader.close();

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(ELPath.WIKI_DIR + "encoded_wiki_categorylink.ser.gz");
		FileUtils.writeIntSetMap(oos, sm);
		oos.close();
	}

	public void encodeRedirects() throws Exception {
		BidMap<Integer, String> idTitleMap = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "wiki_title_encoded.ser.gz");
			idTitleMap = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_redirect.csv");
		Map<Integer, Integer> map = Generics.newHashMap();

		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			parts = normalize(parts);

			String from = parts[0];
			String to = parts[1];

			Integer from_id = idTitleMap.getKey(from);
			Integer to_id = idTitleMap.getKey(to);

			if (from_id == null || to_id == null) {
				continue;
			}

			map.put(from_id, to_id);
		}
		reader.close();

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(ELPath.WIKI_DIR + "wiki_redirect_encoded.ser.gz");
		FileUtils.writeIntMap(oos, map);
		oos.close();

		// FileUtils.writeStrCounterMap(ELPath.WIKI_DIR + "wiki_categorylink_encoded.txt", cm);
		// FileUtils.writeStrCounter(ELPath.WIKI_DIR + "wiki_categorylink_encoded.txt", cc);
	}

	public void encodeTitles() throws Exception {
		BidMap<Integer, String> idPageMap = Generics.newBidMap();

		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_title.csv.gz");
		while (reader.hasNext()) {
			/*
			 * "id"\t"title"
			 */
			String[] parts = reader.next().split("\t");
			parts = normalize(parts);
			int page_id = Integer.parseInt(parts[0]);
			String page_title = parts[1];
			idPageMap.put(page_id, page_title);
		}
		reader.close();

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(ELPath.WIKI_DIR + "encoded_wiki_title.ser.gz");
		FileUtils.writeIntStrBidMap(oos, idPageMap);
		oos.close();
	}

	public void getMedicalCategories() throws Exception {

		SetMap<Integer, Integer> parentToChilds = null;
		SetMap<Integer, Integer> childToParents = null;

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

		BidMap<Integer, String> idToCat = null;
		Counter<Integer> catPageCnts = Generics.newCounter();

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
				catPageCnts.setCount(ids.get(i), catPageCnts.getCount(i));
			}
		}

		{
			for (int c : childToParents.keySet()) {
				String child = idToCat.getValue(c);
				Set<Integer> ps = childToParents.get(c);

				if (ps.size() > 1) {
					StringBuffer sb = new StringBuffer();
					sb.append("# " + child + "\n");
					for (int p : ps) {
						int cat_pages = (int) catPageCnts.getCount(p);
						String parent = idToCat.getValue(p);
						sb.append(String.format("-> %s, %d\n", parent, cat_pages));
					}

					System.out.println(sb.toString());
				}
			}
		}

		int root_id = 192834;
		int health_id = 153550;

		// System.out.println(parentToChilds.get(health_id));

		Set<Integer> mainCats = Generics.newHashSet(parentToChilds.get(root_id));
		mainCats.add(root_id);

		// for (int id : mainTopics) {
		// System.out.println(idToCat.getValue(id));
		// }

		Stack<Integer> path = new Stack<Integer>();
		path.push(health_id);

		TextFileWriter writer = new TextFileWriter(ELPath.WIKI_DIR + "wiki_cat_tree.txt");

		Trie<String> trie = Trie.newTrie();

		SetMap<Integer, Integer> levelToCats = Generics.newSetMap();

		getMedicalConcepts(path, mainCats, parentToChilds, idToCat, levelToCats, trie);

		writer.close();

		// FileUtils.writeStrCounter(ELPath.WIKI_DIR + "wiki_cat_tree.txt", leaves);
	}

	private void getMedicalConcepts(Stack<Integer> path, Set<Integer> mainCats,

			SetMap<Integer, Integer> parentToChilds, BidMap<Integer, String> idToCat, SetMap<Integer, Integer> levelToCats,
			Trie<String> trie) {

		int parent_id = path.peek();
		int level = path.size();

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

				if (levelToCats.contains(catPath.length + 1, child_id)) {
					continue;
				}

				levelToCats.put(catPath.length + 1, child_id);

				path.push(child_id);
				getMedicalConcepts(path, mainCats, parentToChilds, idToCat, levelToCats, trie);
				path.pop();
			}
		}
	}

	private String[] normalize(String[] parts) {
		String[] ret = new String[parts.length];
		for (int i = 0; i < parts.length; i++) {
			String s = parts[i];
			s = s.substring(1, s.length() - 1);
			ret[i] = s;
		}
		return ret;
	}

}
