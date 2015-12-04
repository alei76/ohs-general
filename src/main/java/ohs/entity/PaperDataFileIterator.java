package ohs.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ohs.io.TextFileReader;
import ohs.types.BidMap;

public class PaperDataFileIterator implements Iterator<Map<PaperAttr, String>> {

	private TextFileReader reader;

	private int num_papers = 0;

	private Map<PaperAttr, String> attrValueMap;

	private Map<String, PaperAttr> attrMap;

	private final String DOCUMENT_START = "@NEW_DOCUMENT";

	private boolean readFromLine = false;

	public PaperDataFileIterator(String fileName) {
		reader = new TextFileReader(fileName, "euc-kr");

		attrMap = new HashMap<String, PaperAttr>();

		for (PaperAttr pa : PaperAttr.values()) {
			attrMap.put(pa.toString(), pa);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		reader.close();
	}

	public int getNumPapers() {
		return num_papers;
	}

	public boolean hasNext() {
		List<String> lines = new ArrayList<String>();

		if (num_papers == 0) {
			reader.hasNext();
			String line = reader.next();
			if (!line.equals(DOCUMENT_START)) {
				return false;
			}
		}

		while (reader.hasNext()) {
			String line = reader.next();
			if (line.equals(DOCUMENT_START)) {
				break;
			} else {
				lines.add(line);
			}
		}

		BidMap<Integer, PaperAttr> locAttrMap = new BidMap<Integer, PaperAttr>();

		for (int j = 0; j < lines.size(); j++) {
			String s = lines.get(j);
			if (s.startsWith("#")) {
				s = s.substring(1);
				int idx = s.indexOf("=");
				String attr = s.substring(0, idx);
				PaperAttr pa = attrMap.get(attr);

				if (pa != null) {
					locAttrMap.put(j, pa);
				}
			}
		}

		List<Integer> locs = new ArrayList<Integer>();
		locs.addAll(locAttrMap.getKeys());
		locs.add(lines.size());

		attrValueMap = new HashMap<PaperAttr, String>();

		for (int j = 0; j < locs.size() - 1; j++) {
			int start = locs.get(j);
			int end = locs.get(j + 1);

			PaperAttr attr = locAttrMap.getValue(start);
			StringBuffer sb = new StringBuffer();
			for (int k = start; k < end; k++) {
				String s = lines.get(k);
				if (s.startsWith("#")) {
					int idx = s.indexOf("=");
					s = s.substring(idx + 1);
				}
				sb.append(s + "\n");
			}
			String value = sb.toString().trim();

			if (value.length() > 0 && !value.equals("null")) {
				attrValueMap.put(attr, value);
			}
		}

		if (attrValueMap.size() > 0) {
			num_papers++;
		}

		return attrValueMap.size() > 0 ? true : false;
	}

	@Override
	public Map<PaperAttr, String> next() {
		return attrValueMap;
	}
}