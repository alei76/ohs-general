package ohs.eden.keyphrase;

import java.util.Iterator;
import java.util.List;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		dh.processPapers();
		// dh.processReports();
		dh.check();
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

	public void processReports() throws Exception {
		TextFileReader reader = new TextFileReader(KPPath.REPORT_DUMP_FILE);
		List<String> labels = Generics.newArrayList();

		// TextFileWriter writer = new
		// TextFileWriter(KWPath.PAPER_KEYWORD_FILE);

		Counter<String> kwCounts = Generics.newCounter();

		while (reader.hasNext()) {
			// if (reader.getNumLines() > 10) {
			// break;
			// }

			String line = reader.next();
			String[] parts = line.split("\t");

			for (int i = 0; i < parts.length; i++) {
				if (parts[i].length() > 1) {
					parts[i] = parts[i].substring(1, parts[i].length() - 1);
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

				String kwkStr = parts[5];
				String kweStr = parts[6];
				// String[] kors = kwkStr.split(";");
				// String[] engs = kweStr.split(";");
				String abk = parts[7];
				String abs = parts[8];

				// int short_len = Math.min(kors.length, engs.length);
				// int long_len = Math.max(kors.length, engs.length);

				List<String> kors = Generics.newArrayList();
				List<String> engs = Generics.newArrayList();

				for (String kw : kwkStr.split(";")) {
					if (kw.length() > 0) {
						kors.add(kw);
					}
				}

				for (String kw : kweStr.split(";")) {
					if (kw.length() > 0) {
						engs.add(kw);
					}
				}

				if (kors.size() == engs.size() && kors.size() > 0) {
					// writer.write("KOR:\t" + StrUtils.join("\t", kors) +
					// "\n");
					// writer.write("ENG:\t" + StrUtils.join("\t", engs) +
					// "\n\n");

					for (int i = 0; i < kors.size(); i++) {
						String kor = kors.get(i);
						String eng = engs.get(i);
						kwCounts.incrementCount(kor + "\t" + eng, 1);
					}
				}
			}
		}
		reader.close();
		// writer.close();

		FileUtils.write(KPPath.REPORT_KEYWORD_FILE, kwCounts, true);
	}

	public void processPapers() throws Exception {
		TextFileReader reader = new TextFileReader(KPPath.PAPER_DUMP_FILE);
		List<String> labels = Generics.newArrayList();

		// TextFileWriter writer1 = new
		// TextFileWriter(KPPath.PAPER_KEYWORD_FILE);
		// TextFileWriter writer2 = new
		// TextFileWriter(KPPath.PAPER_CONTEXT_FILE);

		CounterMap<String, String> cm1 = Generics.newCounterMap();
		CounterMap<String, String> cm2 = Generics.newCounterMap();
		CounterMap<String, String> cm3 = Generics.newCounterMap();

		while (reader.hasNext()) {
			// if (reader.getNumLines() > 100000) {
			// break;
			// }

			String line = reader.next();
			String[] parts = line.split("\t");

			for (int i = 0; i < parts.length; i++) {
				if (parts[i].length() > 1) {
					parts[i] = parts[i].substring(1, parts[i].length() - 1);
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

				String cn = parts[0];
				String korKeywordStr = parts[8];
				String engKeywordStr = parts[9];
				// String[] kors = kwkStr.split(";");
				// String[] engs = kweStr.split(";");
				String korAbs = parts[10];
				String engAbs = parts[11];

				// int short_len = Math.min(kors.length, engs.length);
				// int long_len = Math.max(kors.length, engs.length);

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

				if (kors.size() == 0 || kors.size() != engs.size()) {
					continue;
				}

				for (int i = 0; i < kors.size(); i++) {
					String kor = kors.get(i);
					String eng = engs.get(i);
					cm1.incrementCount(kor + "\t" + eng.toLowerCase(), cn, 1);
				}

				// writer.write("KOR:\t" + StrUtils.join("\t", kors) +
				// "\n");
				// writer.write("ENG:\t" + StrUtils.join("\t", engs) +
				// "\n\n");

				// System.out.println(korKeywordStr);
				// System.out.println(kors);
				// System.out.println();

				Counter<String> korAbsWordCounts = Generics.newCounter();
				Counter<String> engAbsWordCounts = Generics.newCounter();

				for (String word : StrUtils.split("[\\s\\p{Punct}]+", korAbs)) {
					word = word.trim();
					if (word.length() > 0) {
						korAbsWordCounts.incrementCount(word, 1);
					}
				}

				for (String word : StrUtils.split("[\\s\\p{Punct}]+", engAbs)) {
					word = word.trim();
					if (word.length() > 0) {
						engAbsWordCounts.incrementCount(word, 1);
					}
				}

				if (korAbsWordCounts.size() > 0) {
					cm2.setCounter(cn, korAbsWordCounts);
				}

				if (engAbsWordCounts.size() > 0) {
					cm3.setCounter(cn, engAbsWordCounts);
				}

				// Counter<String> kwCounts = Generics.newCounter();
				//
				// for (int i = 0; i < kors.size(); i++) {
				// String kor = kors.get(i);
				// String eng = engs.get(i);
				// kwCounts.incrementCount(kor + "\t" + eng, 1);
				// }

				// writer1.write(String.format("Keywords:\t%s",
				// kwCounts.toStringSortedByValues(true, true, kwCounts.size(),
				// " ")));
				// writer1.write("\nKor Abs:\t"
				// + korAbsWordCounts.toStringSortedByValues(true, false,
				// korAbsWordCounts.size(), " "));
				// writer1.write("\nEng Abs:\t"
				// + engAbsWordCounts.toStringSortedByValues(true, false,
				// engAbsWordCounts.size(), " "));
				// writer1.write("\n");

			}
		}
		reader.close();
		// writer.close();

		Iterator<String> iter = cm1.keySet().iterator();
		while (iter.hasNext()) {
			String inKey = iter.next();
			Counter<String> c = cm1.getCounter(inKey);
			if (c.totalCount() < 5) {
				iter.remove();
			}
		}

		FileUtils.write(KPPath.PAPER_KEYWORD_FILE, cm1, null, true);
		FileUtils.write(KPPath.PAPER_KOREAN_CONTEXT_FILE, cm2, null, true);
		FileUtils.write(KPPath.PAPER_ENGLISH_CONTEXT_FILE, cm3, null, true);

		// FileUtils.write(KWPath.PAPER_KEYWORD_FILE, kwCounts, true);
	}

}
