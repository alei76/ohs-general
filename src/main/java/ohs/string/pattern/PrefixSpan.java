package ohs.string.pattern;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import ohs.entity.ENTPath;
import ohs.entity.StringPartitioner;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.string.search.ppss.Gram;
import ohs.types.Counter;
import ohs.types.ListMap;
import ohs.types.Pair;

/**
 * 
 * A class for mining sequential patterns. This implementation is ported from Taku Kudo's C++ version.
 * 
 * Reference:
 * 
 * J. Pei, J. Han, H. Pinto, Q. Chen, U. Dayal, and M.-C. Hsu, PrefixSpan: Mining Sequential Patterns Efficiently by Prefix-Projected
 * Pattern Growth Proc. 2001 Int. Conf. on Data Engineering (ICDE'01), Heidelberg, Germany, April 2001.
 * http://www.cs.sfu.ca/~peijian/personal/publications/span.pdf
 * 
 * Taku Kudo, http://www.chasen.org/~taku/index.html.en
 * 
 * @author Heung-Seon Oh
 * 
 */

public class PrefixSpan {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("process begins.");

		List<String[]> records = new ArrayList<String[]>();

		StringPartitioner p = new StringPartitioner(3, false);

		TextFileReader reader = new TextFileReader(ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			String korName = parts[0];
			Gram[] grams = p.partition(korName);

			String[] items = new String[grams.length];

			for (int i = 0; i < grams.length; i++) {
				items[i] = grams[i].getString();
			}

			records.add(items);

			if (records.size() == 10000) {
				break;
			}
		}
		reader.close();

		PrefixSpan s = new PrefixSpan();
		s.setRecords(records);
		s.mine();
		s.write(ENTPath.DATA_DIR + "patterns.txt");
		System.out.println("process ends.");
	}

	private List<String[]> records;

	private Stack<Pair<String, Integer>> patterns;
	private ListMap<String, Integer> patMap;
	private int min_support;
	private int min_pat_len;
	private int max_pat_len;
	private boolean all;
	private boolean m_where = true;
	private String delimiter;
	private boolean m_verbose = true;

	public PrefixSpan() {
		this(5, 2, 6, false, "/");
	}

	public PrefixSpan(int min_support, int min_pat_len, int max_pat_len, boolean all, String delimiter) {
		this.min_support = min_support;
		this.min_pat_len = min_pat_len;
		this.max_pat_len = max_pat_len;
		this.all = all;
		this.delimiter = delimiter;
	}

	public ListMap<String, Integer> getPatterns() {
		return patMap;
	}

	public void mine() {
		patterns = new Stack<Pair<String, Integer>>();
		patMap = new ListMap<String, Integer>();

		// if (this.verbose) {
		// this.sb.append(this.transaction.size() + "\n");
		// }
		List<Pair> root = new ArrayList<Pair>();

		for (int i = 0; i < records.size(); i++) {
			root.add(new Pair(i, -1));
		}

		project(root);

	}

	private void project(List<Pair> projected) {

		if (all) {
			report(projected);
		}

		// Map<String, List<IntPair>> counter = new TreeMap<String, List<IntPair>>();
		ListMap<String, Pair> c = new ListMap<String, Pair>();

		for (int i = 0; i < projected.size(); i++) {
			Pair info = projected.get(i);
			int id = (int) info.getFirst();
			int pos = (int) info.getSecond();
			String[] items = records.get(id);

			Map<String, Integer> tmp = new HashMap<String, Integer>();

			for (int j = pos + 1; j < items.length; j++) {
				String item = items[j];
				if (!tmp.containsKey(item)) {
					tmp.put(item, j);
				}
			}

			for (String item : tmp.keySet()) {
				int new_pos = tmp.get(item);
				c.put(item, new Pair(id, new_pos));
			}
		}

		{
			Iterator<String> iter = c.keySet().iterator();
			while (iter.hasNext()) {
				String item = iter.next();
				List<Pair> locs = c.get(item);

				if (locs.size() < min_support) {
					iter.remove();
				}
			}
		}

		if (!all && c.size() == 0) {
			report(projected);
			return;
		}

		{
			for (String item : c.keySet()) {
				List<Pair> locs = c.get(item);
				if (patterns.size() < max_pat_len) {
					patterns.push(new Pair<String, Integer>(item, locs.size()));
					project(locs);
					patterns.pop();
				}
			}
		}
	}

	public void read(File file) {
		// InputStream in = new InputStream(inFile);
		// String line;
		//
		// while ((line = in.readLine()) != null) {
		// String[] toks = line.split("\t");
		// String[] terms = toks[4].split(" ");
		// List<String> termList = new ArrayList<String>();
		//
		// for (int i = 1; i < terms.length - 1; i++) {
		// String term = terms[i];
		// termList.add(term);
		// }
		// this.transaction.add(termList);
		// }
		// in.close();
	}

	private void report(List<Pair> projected) {
		if (projected == null || min_pat_len > patterns.size()) {
			return;
		}

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < patterns.size(); i++) {
			sb.append(patterns.get(i).getFirst());
			if (i != patterns.size() - 1) {
				sb.append("->");
			}
		}

		// sb.append("\t" + patterns.get(patterns.size() - 1).getSecond());

		String key = sb.toString();

		if (!patMap.containsKey(key)) {
			List<Integer> ids = new ArrayList<Integer>();
			for (int i = 0; i < projected.size(); i++) {
				ids.add((int) projected.get(i).getFirst());
			}
			patMap.set(key, ids);
		}

		// StringBuffer sb = new StringBuffer();
		//
		// if (this.where) {
		//
		// sb.append("<patterns>\n");
		//
		// if (all) {
		// sb.append("<freq>"
		// + this.pattern.get(this.pattern.size() - 1).second
		// + "</freq>\n");
		// sb.append("<what>");
		//
		// for (int i = 0; i < this.pattern.size(); i++) {
		// sb.append((i > 0 ? " " : "") + this.pattern.get(i).first);
		// }
		// } else {
		// sb.append("<what>");
		// for (int i = 0; i < this.pattern.size(); i++) {
		// sb.append((i > 0 ? " " : "") + this.pattern.get(i).first
		// + "/" + this.pattern.get(i).second);
		// }
		// }
		//
		// sb.append("</what>\n");
		// sb.append("<where>");
		// for (int i = 0; i < projected.size(); i++) {
		// sb.append((i > 0 ? " " : "") + projected.get(i).first);
		// }
		//
		// sb.append("</where>\n");
		// sb.append("</patterns>\n");
		// System.out.println(sb.toString());
		// } else {
		// if (all) {
		// sb.append(this.pattern.get(this.pattern.size() - 1).second);
		// for (int i = 0; i < this.pattern.size(); i++) {
		// sb.append(" " + this.pattern.get(i).first);
		// }
		// } else {
		// String str = "";
		// for (int i = 0; i < this.pattern.size(); i++) {
		// sb.append((i > 0 ? " " : "") + this.pattern.get(i).first
		// + "/" + this.pattern.get(i).second);
		// str += this.pattern.get(i).first + "/"
		// + this.pattern.get(i).second + " ";
		// }
		//
		// pList.add(str.trim());
		//
		// }
		//
		// sb.append("\n");
		// }
	}

	public void setRecords(List<String[]> records) {
		this.records = records;
	}

	public void write(String fileName) {
		TextFileWriter writer = new TextFileWriter(fileName);
		int patternid = 0;

		writer.write("Pattern\tOccurred Records \tRecord IDs\n");

		Counter<String> c = new Counter<String>();

		for (String pat : patMap.keySet()) {
			List<Integer> locs = patMap.get(pat);
			c.incrementCount(pat, locs.size());
		}

		List<String> pats = c.getSortedKeys();

		for (int i = 0; i < pats.size(); i++) {
			String pat = pats.get(i);
			List<Integer> recordIds = patMap.get(pat);

			StringBuffer sb = new StringBuffer();

			sb.append(String.format("%s\t%d\t", pat, recordIds.size()));

			for (int j = 0; j < recordIds.size(); j++) {
				sb.append(recordIds.get(j));
				if (j != recordIds.size() - 1) {
					sb.append(" ");
				}
			}
			writer.write(sb.toString() + "\n");
		}
		writer.close();
	}
}
