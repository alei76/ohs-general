package ohs.string.search.ppss;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.string.search.ppss.Gram.Type;
import ohs.types.Counter;

/**
 * 
 * An inverted index for (gram,string) pairs.
 * 
 * @author Heung-Seon Oh
 * 
 * 
 */
public class GramInvertedIndex implements Serializable {

	private Map<String, GramPostings> map;

	public GramInvertedIndex() {
		map = new HashMap<String, GramPostings>();
	}

	public boolean containsKey(String g) {
		return map.containsKey(g);
	}

	private GramPostings ensure(String g) {
		GramPostings ret = map.get(g);
		if (ret == null) {
			ret = new GramPostings();
			map.put(g, ret);
		}
		return ret;
	}

	public GramPostings get(String g, boolean createIfNotPresented) {
		return createIfNotPresented ? ensure(g) : map.get(g);
	}

	public Map<String, GramPostings> getIndex() {
		return map;
	}

	private void insert1(GramInvertedIndex L, String line) {

	}

	private void insert2(GramInvertedIndex L, String line) {

	}

	public Set<String> keySet() {
		return map.keySet();
	}

	public void read(BufferedReader reader) throws IOException {
		int num_read = GramUtils.getNumLinesToRead(reader);

		GramInvertedIndex L = this;

		for (int i = 0; i < num_read; i++) {
			GramPostings gp = null;

			{
				String line = reader.readLine();
				String[] parts = line.split(" -> ");
				String p1 = parts[0];
				String p2 = parts[1];

				p1 = p1.substring(1, p1.length() - 1);
				p2 = p2.substring(1, p2.length() - 1);

				int loc = p1.lastIndexOf(", ");

				String g = p1.substring(0, loc);

				String[] splits = parts[1].split("\\) \\(");

				gp = L.get(g, true);

				for (int j = 0; j < splits.length; j++) {
					String split = splits[j];
					split = split.substring(1, split.length() - 1);

					String[] toks = split.split(", ");
					int rid = Integer.parseInt(toks[0]);
					int start = Integer.parseInt(toks[1]);
					Type type = Type.parse(toks[2]);
					gp.getEntries().add(new GramPostingEntry(rid, start, type));
				}
			}

			String[] lines = new String[2];
			lines[0] = reader.readLine();
			lines[1] = reader.readLine();

			for (int j = 0; j < lines.length; j++) {
				String line = lines[j];
				String[] parts = line.split(" -> ");
				if (parts.length == 2) {
					String p1 = parts[0];
					String p2 = parts[1];
					p2 = p2.substring(1, p2.length() - 1);

					Type type = Type.parse(p1);

					String[] splits = p2.split("\\) \\(");

					for (int k = 0; k < splits.length; k++) {
						String split = splits[k];

						String[] toks = split.split(", ");
						int len = Integer.parseInt(toks[0]);
						int loc = Integer.parseInt(toks[1]);

						gp.getTypeLengthLocs().put(type, len, loc);
					}
				}
			}
		}
	}

	public String toString() {
		StringBuffer ret = new StringBuffer();
		List<String> keys = new ArrayList<String>(map.keySet());

		for (int i = 0; i < keys.size(); i++) {
			String g = keys.get(i);
			GramPostings p = map.get(g);
			ret.append(g + " -> ");
			String[] splits = p.toString(true).split("\n");

			for (int j = 0; j < splits.length; j++) {
				ret.append(splits[j]);

				if (j != splits.length - 1) {
					ret.append("\n");
				}
			}

			if (i != keys.size() - 1) {
				ret.append("\n");
			}
		}
		return ret.toString().trim();
	}

	public void write(BufferedWriter writer) throws IOException {
		writer.write(String.format("## Gram Inverted Index\t%d\n", map.size()));

		List<String> grams = new ArrayList<String>();

		{
			Counter<String> c = new Counter<String>();
			for (String g : map.keySet()) {
				int size = map.get(g).size();
				c.setCount(g, size);
			}
			grams = c.getSortedKeys();
		}

		for (int i = 0; i < grams.size(); i++) {
			String g = grams.get(i);

			StringBuffer sb = new StringBuffer();
			GramPostings p = map.get(g);
			sb.append(String.format("(%s, %d) -> ", g, p.getEntries().size()));
			String[] splits = p.toString(true).split("\n");
			for (int j = 0; j < splits.length; j++) {
				sb.append(splits[j]);

				if (j != splits.length - 1) {
					sb.append("\n");
				}
			}

			if (i != grams.size() - 1) {
				sb.append("\n");
			}

			writer.write(sb.toString());
			writer.flush();
		}
	}

}
