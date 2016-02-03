package ohs.eden.keyphrase;

import java.util.List;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.CounterMap;
import ohs.utils.Generics;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.extractData();
		System.out.println("process ends.");
	}

	public void check() {
		TextFileReader reader = new TextFileReader(KPPath.EDS_DICT_FILE);
		TextFileWriter writer = new TextFileWriter(KPPath.KEYPHRASE_DIR + "eds_dict.txt");
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split(",");
			int num_commas = parts.length - 1;
			if (parts.length != 3 && num_commas > 1) {
				writer.write(line + "\t" + num_commas + "\n");
			}
		}
		reader.close();
		writer.close();
	}

	private CounterMap<String, String> getPaperKeywords(String[] parts) {
		CounterMap<String, String> ret = Generics.newCounterMap();

		String cn = parts[0];
		String korKeywordStr = parts[8];
		String engKeywordStr = parts[9];

		List<String> kors = Generics.newArrayList();
		List<String> engs = Generics.newArrayList();

		for (String kw : korKeywordStr.split(";")) {
			kw = kw.trim();
			if (kw.length() > 0) {
				kors.add(kw);
			}
		}

		for (String kw : engKeywordStr.split(";")) {
			kw = kw.trim();
			if (kw.length() > 0) {
				engs.add(kw);
			}
		}

		if (kors.size() == 0 && engs.size() == 0) {
			return ret;
		}

		if (kors.size() == engs.size()) {
			if (kors.size() > 0) {
				for (int i = 0; i < kors.size(); i++) {
					String kor = kors.get(i);
					String eng = engs.get(i);
					ret.incrementCount(kor + "\t" + eng, cn, 1);
				}
			}
		} else {
			for (int i = 0; i < kors.size(); i++) {
				String kor = kors.get(i);
				ret.incrementCount(kor + "\t" + "<none>", cn, 1);
			}

			for (int i = 0; i < engs.size(); i++) {
				String eng = engs.get(i);
				ret.incrementCount("<none>" + "\t" + eng, cn, 1);
			}
		}
		return ret;
	}

	private CounterMap<String, String> getReportKeywords(String[] parts) {
		CounterMap<String, String> ret = Generics.newCounterMap();
		String cn = parts[0];
		String korKeywordStr = parts[5];
		String engKeywordStr = parts[6];

		korKeywordStr = korKeywordStr.replace("\"", "");
		engKeywordStr = engKeywordStr.replace("\"", "");

		List<String> kors = Generics.newArrayList();
		List<String> engs = Generics.newArrayList();

		for (String kw : korKeywordStr.split(";")) {
			kw = kw.trim();
			if (kw.length() > 0) {
				kors.add(kw);
			}
		}

		for (String kw : engKeywordStr.split(";")) {
			kw = kw.trim();
			if (kw.length() > 0) {
				engs.add(kw);
			}
		}

		if (kors.size() == 0 && engs.size() == 0) {
			return ret;
		}

		if (kors.size() == engs.size()) {
			if (kors.size() > 0) {
				for (int i = 0; i < kors.size(); i++) {
					String kor = kors.get(i);
					String eng = engs.get(i);
					ret.incrementCount(kor + "\t" + eng, cn, 1);
				}
			}
		} else {
			for (int i = 0; i < kors.size(); i++) {
				String kor = kors.get(i);
				ret.incrementCount(kor + "\t" + "<none>", cn, 1);
			}

			for (int i = 0; i < engs.size(); i++) {
				String eng = engs.get(i);
				ret.incrementCount("<none>" + "\t" + eng, cn, 1);
			}
		}
		return ret;
	}

	public void extractData() throws Exception {
		String[] inFileNames = { KPPath.PAPER_DUMP_FILE, KPPath.REPORT_DUMP_FILE };

		CounterMap<String, String> allKeywords = Generics.newCounterMap();

		TextFileWriter writer = new TextFileWriter(KPPath.ABSTRACT_FILE);

		for (int i = 0; i < inFileNames.length; i++) {
			String inFileName = inFileNames[i];

			TextFileReader reader = new TextFileReader(inFileName);
			List<String> labels = Generics.newArrayList();

			while (reader.hasNext()) {
				// if (reader.getNumLines() > 100000) {
				// break;
				// }

				String line = reader.next();
				String[] parts = line.split("\t");

				for (int j = 0; j < parts.length; j++) {
					if (parts[j].length() > 1) {
						parts[j] = parts[j].substring(1, parts[j].length() - 1);
					}
				}

				if (reader.getNumLines() == 1) {
					for (String p : parts) {
						labels.add(p);
					}
				} else {
					if (parts.length != labels.size()) {
						continue;
					}

					if (i == 0) {
						allKeywords.incrementAll(getPaperKeywords(parts));
					} else if (i == 1) {
						allKeywords.incrementAll(getReportKeywords(parts));
					}

					String cn = parts[0];
					String korAbs = parts[parts.length - 2];
					String engAbs = parts[parts.length - 1];
					writer.write(String.format("\"%s\"\t\"%s\"\t\"%s\"\n", cn, korAbs, engAbs));
				}
			}
			reader.close();
			// writer.close();

			// Iterator<String> iter = cm.keySet().iterator();
			// while (iter.hasNext()) {
			// String inKey = iter.next();
			// Counter<String> c = cm.getCounter(inKey);
			// if (c.totalCount() < 5) {
			// iter.remove();
			// }
			// }
		}
		writer.close();

		FileUtils.writeStrCounterMap(KPPath.KEYWORD_FILE, allKeywords, null, true);
		// FileUtils.write(KPPath.PAPER_KOREAN_CONTEXT_FILE, cm2, null, true);
		// FileUtils.write(KPPath.PAPER_ENGLISH_CONTEXT_FILE, cm3, null, true);

		// FileUtils.write(KWPath.PAPER_KEYWORD_FILE, kwCounts, true);
	}

}
