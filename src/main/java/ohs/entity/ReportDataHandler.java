package ohs.entity;

import ohs.io.TextFileReader;
import ohs.types.BidMap;
import ohs.types.Counter;

public class ReportDataHandler {

	private static boolean isNull(String s) {
		return s.equals("null") ? true : false;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ReportDataHandler dh = new ReportDataHandler();
		dh.process();

		System.out.println("process ends.");
	}

	public void process() throws Exception {
		Counter<String> c = new Counter<String>();

		TextFileReader reader = new TextFileReader(ENTPath.REPORT_TEXT_FILE);

		BidMap<Integer, String> attrMap = new BidMap<Integer, String>();

		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				String[] attrs = line.split("\t");
				for (int k = 0; k < attrs.length; k++) {
					attrMap.put(k, attrs[k]);
				}
			} else {
				BidMap<String, String> map = new BidMap<String, String>();

				String[] values = line.replace("\t", "#\t").split("\t");
				for (int l = 0; l < values.length; l++) {
					String v = values[l];
					v = v.replace("#", "");
					values[l] = v;
					String attr = attrMap.getValue(l);
					map.put(attr, v);
				}

				String pbk = map.getValue("PBK");
				String pbe = map.getValue("PBE");

				if (pbk == null || pbk.length() == 0 || pbk.equals("empty")) {
					continue;
				}

				c.incrementCount(pbk, 1);

			}
		}
		reader.close();

		System.out.println(c.toString());
		System.out.println(c.size());
	}
}
