package ohs.eden.linker;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class WikiCsvDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", WikiCsvDataHandler.class.getName());

		WikiCsvDataHandler d = new WikiCsvDataHandler();
		d.encodeTitles();
		// d.encodeCategories();
		// d.encodeCategoryLinks();
		// d.encodeRedirects();
		// d.map();

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

	public void map() throws Exception {
		BidMap<Integer, String> idToTitle = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "encoded_wiki_title.ser.gz");
			idToTitle = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		BidMap<Integer, String> idToCat = null;

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
				// catPageCnts.setCount(ids.get(i), catPages.get(i));
			}
		}

		Set<Integer> healthSet = Generics.newHashSet();

		{
			TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_cat_tree.txt");
			while (reader.hasNext()) {
				String[] cats = reader.next().split("\t");

				for (String cat : cats) {
					healthSet.add(idToCat.getKey(cat));
				}
			}
			reader.close();
		}

		{

			List<Integer> ids = Generics.newArrayList();

			TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_categorylink.csv.gz");
			while (reader.hasNext()) {
				String line = reader.next();
				String[] parts = StrUtils.unwrap(line.split("\t"));

				int cl_from = Integer.parseInt(parts[0]);
				String cl_to = parts[1];
				String cl_type = parts[2];

				if (!cl_type.equals("page")) {
					continue;
				}

				Integer parent_id = idToCat.getKey(cl_to);

				if (healthSet.contains(parent_id)) {
					ids.add(cl_from);
				}
			}
			reader.close();

			TextFileWriter writer = new TextFileWriter(ELPath.WIKI_DIR + "wiki_page_health.txt");
			for (int i = 0; i < ids.size(); i++) {
				int id = ids.get(i);
				String title = idToTitle.getValue(id);
				String[] parts = new String[] { id + "", title };
				parts = StrUtils.wrap(parts);
				writer.write(StrUtils.join("\t", parts));

				if (i != ids.size() - 1) {
					writer.write("\n");
				}
			}
			writer.close();

		}
	}

	public void encodeCategoryLinks() throws Exception {
		BidMap<Integer, String> idToTitle = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "encoded_wiki_title.ser.gz");
			idToTitle = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		BidMap<Integer, String> idToCat = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "encoded_wiki_category.ser.gz");
			idToCat = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		SetMap<Integer, Integer> parentToChildren = Generics.newSetMap();

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

			String title_from = idToTitle.getValue(cl_from);
			Integer child_id = idToCat.getKey(title_from);
			Integer parent_id = idToCat.getKey(cl_to);

			if (child_id == null || parent_id == null) {
				continue;
			}
			parentToChildren.put(parent_id, child_id);
		}
		reader.close();

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(ELPath.WIKI_DIR + "encoded_wiki_categorylink.ser.gz");
		FileUtils.writeIntSetMap(oos, parentToChildren);
		oos.close();
	}

	public void encodeRedirects() throws Exception {
		BidMap<Integer, String> idToTitle = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(ELPath.WIKI_DIR + "wiki_title_encoded.ser.gz");
			idToTitle = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_redirect.csv");
		Map<Integer, Integer> map = Generics.newHashMap();

		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = StrUtils.unwrap(line.split("\t"));
			String from = parts[0];
			String to = parts[1];

			Integer from_id = idToTitle.getKey(from);
			Integer to_id = idToTitle.getKey(to);

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

		Counter<String> c = Generics.newCounter();

		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_title.csv.gz");
		while (reader.hasNext()) {
			/*
			 * "id"\t"title"
			 */
			String[] parts = StrUtils.unwrap(reader.next().split("\t"));

			int page_id = Integer.parseInt(parts[0]);
			String page_title = parts[1];
			idPageMap.put(page_id, page_title);

			c.incrementCount(page_title, 1);
		}
		reader.close();
		
		System.out.println(c);

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(ELPath.WIKI_DIR + "encoded_wiki_title.ser.gz");
		FileUtils.writeIntStrBidMap(oos, idPageMap);
		oos.close();
	}

}
