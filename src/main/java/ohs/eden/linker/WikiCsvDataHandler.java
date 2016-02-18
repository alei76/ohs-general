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
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.SetMap;
import ohs.utils.Generics;

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

	private String[] normalize(String[] parts) {
		String[] ret = new String[parts.length];
		for (int i = 0; i < parts.length; i++) {
			String s = parts[i];
			s = s.substring(1, s.length() - 1);
			ret[i] = s;
		}
		return ret;
	}

	public void getMedicalCategories() throws Exception {

		SetMap<Integer, Integer> parentChildMap = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "wiki_categorylink_encoded.ser.gz");
			parentChildMap = FileUtils.readIntSetMap(ois);
			ois.close();
		}

		BidMap<Integer, String> idCatMap = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "wiki_category_encoded.ser.gz");
			idCatMap = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		int root_id = 192834;
		int health_id = 153550;

		System.out.println(parentChildMap.get(health_id));

		Set<Integer> mainTopicIds = parentChildMap.get(root_id);

		for (int id : mainTopicIds) {
			System.out.println(idCatMap.getValue(id));
		}

		Stack<Integer> path = new Stack<Integer>();
		path.push(health_id);

		TextFileWriter writer = new TextFileWriter(ELPath.WIKI_DIR + "wiki_cat_tree.txt");

		Counter<String> pathCnts = Generics.newCounter();

		getMedicalConcepts(path, parentChildMap, idCatMap, pathCnts);

		writer.close();
	}

	private List<Integer> getMedicalConcepts(Stack<Integer> idPath, SetMap<Integer, Integer> parentChildMap,
			BidMap<Integer, String> idCatMap, Counter<String> visited) {

		int parent_id = idPath.peek();

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < idPath.size(); i++) {
			sb.append(idCatMap.getValue(idPath.get(i)));
			if (i != idPath.size() - 1) {
				sb.append("->");
			}
		}

		sb.append(idCatMap.getValue(parent_id));

		List<Integer> ret = Generics.newArrayList();

		String catPath = sb.toString();

		if (visited.containsKey(catPath)) {
			return ret;
		}

		visited.incrementCount(catPath, 1);

		Set<Integer> children = parentChildMap.get(parent_id, true);

		if (children.size() == 0) {
			for (int i = 0; i < idPath.size(); i++) {
				ret.add(idPath.get(i));
			}
			idPath.pop();
		} else {
			for (int child_id : children) {
				String child = idCatMap.getValue(child_id);
			}
		}

		return ret;

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

	public void encodeCategoryLinks() throws Exception {
		BidMap<Integer, String> idTitleMap = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "wiki_title_encoded.ser.gz");
			idTitleMap = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		BidMap<Integer, String> idCatMap = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "wiki_category_encoded.ser.gz");
			idCatMap = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		SetMap<Integer, Integer> sm = Generics.newSetMap();

		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_categorylink.csv");
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			parts = normalize(parts);
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

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(ELPath.WIKI_DIR + "wiki_categorylink_encoded.ser.gz");
		FileUtils.writeIntSetMap(oos, sm);
		oos.close();
	}

	public void encodeCategories() throws Exception {
		BidMap<Integer, String> idPageMap = Generics.newBidMap();

		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_category.csv");
		while (reader.hasNext()) {
			/*
			 * "id"\t"title"
			 */
			String[] parts = reader.next().split("\t");
			parts = normalize(parts);

			int id = Integer.parseInt(parts[0]);
			String cat_title = parts[1];
			int cat_pages = Integer.parseInt(parts[2]);
			int cat_subcats = Integer.parseInt(parts[3]);
			idPageMap.put(id, cat_title);
		}
		reader.close();
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(ELPath.WIKI_DIR + "wiki_category_encoded.ser.gz");
		FileUtils.writeIntStrBidMap(oos, idPageMap);
		oos.close();
	}

	public void encodeTitles() throws Exception {
		BidMap<Integer, String> idPageMap = Generics.newBidMap();

		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_title.csv");
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

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(ELPath.WIKI_DIR + "wiki_title_encoded.ser.gz");
		FileUtils.writeIntStrBidMap(oos, idPageMap);
		oos.close();
	}

}
