package ohs.ir.wiki;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.medical.general.MIRPath;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class WikiDbDumpDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", WikiDbDumpDataHandler.class.getName());

		WikiDbDumpDataHandler d = new WikiDbDumpDataHandler();
		// d.encodeTitles();
		// d.encodeCategories();
		// d.encodeCategoryLinks();
		// d.encodeRedirects();
		// d.map();

		System.out.printf("[%s] ends.\n", WikiDbDumpDataHandler.class.getName());
	}

	public void encodeCategories() throws Exception {
		List<Integer> ids = Generics.newArrayList();
		List<String> titles = Generics.newArrayList();
		List<Integer> catPages = Generics.newArrayList();
		List<Integer> catSubcats = Generics.newArrayList();

		TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_cats.txt.gz");
		while (reader.hasNext()) {
			/*
			 * "id"\t"title"
			 */
			String[] parts = reader.next().split("\t");
			parts = StrUtils.unwrap(parts);

			int id = Integer.parseInt(parts[0]);
			String cat_title = parts[1];

			if (cat_title.length() == 0) {
				continue;
			}

			int cat_pages = Integer.parseInt(parts[2]);
			int cat_subcats = Integer.parseInt(parts[3]);

			ids.add(id);
			titles.add(cat_title);
			catPages.add(cat_pages);
			catSubcats.add(cat_subcats);
		}
		reader.close();

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(MIRPath.WIKI_DIR + "wiki_cats.ser.gz");
		FileUtils.writeIntCollection(oos, ids);
		FileUtils.writeStrCollection(oos, titles);
		FileUtils.writeIntCollection(oos, catPages);
		FileUtils.writeIntCollection(oos, catSubcats);
		oos.close();
	}

	public void encodeCategoryLinks() throws Exception {
		BidMap<Integer, String> idToCat = null;

		{
			Counter<String> c = Generics.newCounter();
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_cats.ser.gz");
			List<Integer> ids = FileUtils.readIntList(ois);
			List<String> titles = FileUtils.readStrList(ois);
			List<Integer> catPages = FileUtils.readIntList(ois);
			List<Integer> catSubcats = FileUtils.readIntList(ois);
			ois.close();

			idToCat = Generics.newBidMap(ids.size());

			for (int i = 0; i < ids.size(); i++) {
				idToCat.put(ids.get(i), titles.get(i));
			}
		}

		Map<Integer, String> idToTitle = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_titles.ser.gz");
			idToTitle = FileUtils.readIntStrMap(ois);
			ois.close();

		}

		SetMap<Integer, Integer> parentToChildren = Generics.newSetMap();
		SetMap<Integer, Integer> catToCats = Generics.newSetMap();

		TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_catlinks.txt.gz");
		while (reader.hasNext()) {
			String line = reader.next();

			String[] parts = StrUtils.unwrap(line.split("\t"));

			if (parts.length != 3) {
				System.out.println(line);
				continue;
			}

			int pageid = Integer.parseInt(parts[0]);
			int parent_id = Integer.parseInt(parts[1]);
			String cl_type = parts[2];

			if (cl_type.equals("subcat")) {
				String child = idToTitle.get(pageid);
				if (idToCat.containsValue(child)) {
					int child_id = idToCat.getKey(child);
					parentToChildren.put(parent_id, child_id);
				}
			}
		}
		reader.close();

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(MIRPath.WIKI_DIR + "wiki_catlinks.ser.gz");
		FileUtils.writeIntSetMap(oos, parentToChildren);
		// FileUtils.writeIntSetMap(oos, pageToCats);
		oos.close();
	}

	public void encodeRedirects() throws Exception {
		Map<Integer, String> idToTitle = null;
		SetMap<String, Integer> titleToIds = Generics.newSetMap();

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_titles.ser.gz");
			idToTitle = FileUtils.readIntStrMap(ois);
			ois.close();

			for (Entry<Integer, String> e : idToTitle.entrySet()) {
				titleToIds.put(e.getValue(), e.getKey());
			}
		}

		TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_redirects.txt.gz");
		Map<Integer, Integer> map = Generics.newHashMap();

		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = StrUtils.unwrap(line.split("\t"));
			int from_id = Integer.parseInt(parts[0]);
			String from = parts[1];
			String to = parts[2];

			Set<Integer> toIds = titleToIds.get(to, false);

			if (toIds == null || toIds.size() > 1) {
				continue;
			}

			map.put(from_id, toIds.iterator().next());
		}
		reader.close();

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(MIRPath.WIKI_DIR + "wiki_redirects.ser.gz");
		FileUtils.writeIntMap(oos, map);
		oos.close();

		// FileUtils.writeStrCounterMap(MIRPath.WIKI_DIR + "wiki_categorylink_encoded.txt", cm);
		// FileUtils.writeStrCounter(MIRPath.WIKI_DIR + "wiki_categorylink_encoded.txt", cc);
	}

	public void encodeTitles() throws Exception {
		Map<Integer, String> idToTitle = Generics.newHashMap();

		TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_titles.txt.gz");
		while (reader.hasNext()) {
			/*
			 * "id"\t"title"
			 */
			String[] parts = StrUtils.unwrap(reader.next().split("\t"));
			int pageid = Integer.parseInt(parts[0]);
			String title = parts[1];

			idToTitle.put(pageid, title);

		}
		reader.close();

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(MIRPath.WIKI_DIR + "wiki_titles.ser.gz");
		FileUtils.writeIntStrMap(oos, idToTitle);
		oos.close();
	}

	public void map() throws Exception {
		BidMap<Integer, String> idToTitle = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "encoded_wiki_title.ser.gz");
			idToTitle = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		BidMap<Integer, String> idToCat = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "encoded_wiki_category.ser.gz");
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
			TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_cat_tree.txt");
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

			TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_catlinks.txt.gz");
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

			TextFileWriter writer = new TextFileWriter(MIRPath.WIKI_DIR + "wiki_page_health.txt");
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

}
