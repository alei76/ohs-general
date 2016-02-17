package ohs.eden.linker;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.types.BidMap;
import ohs.types.CounterMap;
import ohs.types.SetMap;
import ohs.utils.Generics;

public class WikiCsvDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", WikiCsvDataHandler.class.getName());

		WikiCsvDataHandler d = new WikiCsvDataHandler();
		d.encodeCategoryLinks();

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

	public void encodeCategoryLinks() throws Exception {
		BidMap<String, Integer> idPageMap = readTitles();

		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_categorylink.csv");
		// TextFileWriter writer = new TextFileWriter(ELPath.WIKI_DIR + "wiki_categorylink_encoded.txt");
		CounterMap<String, String> cm = Generics.newCounterMap();
		SetMap<Integer, Integer> sm = Generics.newSetMap();

		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			parts = normalize(parts);
			int cl_from = Integer.parseInt(parts[0]);
			String child = idPageMap.getKey(cl_from);
			String cl_to = parts[1];
			String parent = cl_to;
			Integer cl_to_id = idPageMap.getValue(cl_to);
			String cl_type = parts[2];

			if (cl_to_id == null || !cl_type.equals("subcat")) {
				// System.out.println(line);
				continue;
			}

			// cm.incrementCount(String.format("#%s#", parent), String.format("#%s#", child), 1);
			cm.incrementCount(String.format("<%s>", parent), String.format("<%s>", child), 1);

			// writer.write(String.format("%s\t%s\n", cl_from_str, cl_to));
		}
		reader.close();
		// writer.close();

		// Counter<String> cc = Generics.newCounter();
		//
		// for (String parent : cm.keySet()) {
		// Counter<String> c = cm.getCounter(parent);
		// cc.setCount(parent, 0);
		// for (String child : c.keySet()) {
		// cc.setCount(child, 0);
		// }
		// }
		//
		// for (String parent : cm.keySet()) {
		// Counter<String> c = cm.getCounter(parent);
		// for (String child : c.keySet()) {
		// cc.incrementCount(child, 1);
		// }
		// }

		FileUtils.writeStrCounterMap(ELPath.WIKI_DIR + "wiki_categorylink_encoded.txt", cm);
		// FileUtils.writeStrCounter(ELPath.WIKI_DIR + "wiki_categorylink_encoded.txt", cc);
	}

	public void readCategories() {
		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_title.csv");
		BidMap<Integer, String> idPageMap = Generics.newBidMap();
		while (reader.hasNext()) {
			/*
			 * "id" "cat_title" "cat_pages" "cat_subcats"
			 */
			String[] parts = reader.next().split("\t");
			parts = normalize(parts);

			int id = Integer.parseInt(parts[0]);
			String cat_title = parts[1];
			int cat_pages = Integer.parseInt(parts[2]);
			int cat_subcats = Integer.parseInt(parts[3]);
			idPageMap.put(Integer.parseInt(parts[0]), parts[1]);
		}
		reader.close();
	}

	public BidMap<String, Integer> readTitles() {
		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_title.csv");
		BidMap<String, Integer> idPageMap = Generics.newBidMap();

		while (reader.hasNext()) {
			/*
			 * "id"\t"title"
			 */
			String[] parts = reader.next().split("\t");
			parts = normalize(parts);
			idPageMap.put(parts[1], Integer.parseInt(parts[0]));
		}
		reader.close();
		return idPageMap;
	}

}
