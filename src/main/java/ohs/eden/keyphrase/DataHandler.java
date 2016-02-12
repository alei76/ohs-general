package ohs.eden.keyphrase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ling.types.TextSpan;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.extractKeywordData();
		// dh.process();
		dh.analyze();
		System.out.println("process ends.");
	}

	public void extractKeywordData() throws Exception {
		String[] inFileNames = { KPPath.PAPER_DUMP_FILE, KPPath.REPORT_DUMP_FILE };

		SetMap<String, String> keywordDocs = Generics.newSetMap();

		TextFileWriter writer = new TextFileWriter(KPPath.ABSTRACT_FILE);

		for (int i = 0; i < inFileNames.length; i++) {
			String inFileName = inFileNames[i];
			String type = i == 0 ? "P" : "R";

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

					String cn = parts[0];
					String korAbs = parts[parts.length - 2];
					String engAbs = parts[parts.length - 1];
					String korKeywordStr = null;
					String engKeywordStr = null;

					if (i == 0) {
						korKeywordStr = parts[8];
						engKeywordStr = parts[9];
					} else if (i == 1) {
						korKeywordStr = parts[5];
						engKeywordStr = parts[6];
					}

					List<String> kors = getKeywords(korKeywordStr);
					List<String> engs = getKeywords(engKeywordStr);

					if (kors.size() == 0 && engs.size() == 0) {

					} else if (kors.size() == engs.size()) {
						if (kors.size() > 0) {
							for (int j = 0; j < kors.size(); j++) {
								String kor = kors.get(j);
								String eng = engs.get(j);
								keywordDocs.put(kor + "\t" + eng, type + "_" + cn);
							}
						}
					} else {
						for (String kor : kors) {
							keywordDocs.put(kor + "\t" + "<none>", type + "_" + cn);
						}

						for (String eng : engs) {
							keywordDocs.put("<none>" + "\t" + eng, type + "_" + cn);
						}
					}

					writer.write(String.format("\"%s\"\t\"%s\"\t\"%s\"\n", cn, korAbs, engAbs));
				}

			}
			reader.close();
		}
		writer.close();

		FileUtils.writeStrSetMap(KPPath.KEYWORD_DATA_FILE, keywordDocs);
		// FileUtils.write(KPPath.PAPER_KOREAN_CONTEXT_FILE, cm2, null, true);
		// FileUtils.write(KPPath.PAPER_ENGLISH_CONTEXT_FILE, cm3, null, true);
		// FileUtils.write(KWPath.PAPER_KEYWORD_FILE, kwCounts, true);

	}

	private List<String> getKeywords(String keywordStr) {
		List<String> ret = Generics.newArrayList();
		for (String kw : keywordStr.split(";")) {
			kw = kw.trim();
			if (kw.length() > 0) {
				ret.add(kw);
			}
		}
		return ret;
	}

	public void analyze() throws Exception {
		KeywordData kwdData = new KeywordData();
		kwdData.read(KPPath.KEYWORD_DATA_FILE.replace(".txt", ".ser"));

		CounterMap<String, String> kwdTypeCounts = Generics.newCounterMap();
		Counter<String> typeCounts = Generics.newCounter();

		for (int kwdid : kwdData.getKeywordDocs().keySet()) {
			String kwd = kwdData.getKeywordIndexer().getObject(kwdid);
			List<Integer> docids = kwdData.getKeywordDocs().get(kwdid);
			for (int docid : docids) {
				String docId = kwdData.getDocIndexer().getObject(docid);
				String[] parts = docId.split("_");
				String type = parts[0];
				String cn = parts[1];
				kwdTypeCounts.incrementCount("KWD", type, 1);
			}
		}

		FileUtils.writeStrCounterMap(KPPath.KEYWORD_TEMP_FILE, kwdTypeCounts);

	}

	public void process() throws Exception {
		KeywordData kwdData = new KeywordData();
		kwdData.read(KPPath.KEYWORD_DATA_FILE.replace(".txt", ".ser"));

		Indexer<String> kwdIndexer = kwdData.getKeywordIndexer();
		ListMap<Integer, Integer> docKeywords = Generics.newListMap();
		for (int kwdid : kwdData.getKeywordDocs().keySet()) {
			List<Integer> docids = kwdData.getKeywordDocs().get(kwdid);

			for (int docid : docids) {
				docKeywords.put(docid, kwdid);
			}
		}

		TextFileReader reader = new TextFileReader(KPPath.ABSTRACT_FILE);
		TextFileWriter writer = new TextFileWriter(KPPath.KEYWORD_EXTRACTOR_DIR + "raw_tagged.txt");

		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");

			for (int i = 0; i < parts.length; i++) {
				parts[i] = parts[i].substring(1, parts[i].length() - 1);
			}

			String cn = parts[0];
			int docid = kwdData.getDocIndexer().indexOf(cn);
			String korAbs = parts[1];
			String engAbs = parts[2];

			List<Integer> kwdids = docKeywords.get(docid, false);

			if (kwdids == null) {
				continue;
			}

			String[] abss = { korAbs, engAbs };
			Set<String>[] kwdSets = new HashSet[2];

			for (int j = 0; j < kwdSets.length; j++) {
				kwdSets[j] = Generics.newHashSet();
			}

			for (int kwdid : kwdids) {
				String keyword = kwdIndexer.getObject(kwdid);
				String korKwd = keyword.split("\t")[0];
				String engKwd = keyword.split("\t")[1];
				kwdSets[0].add(korKwd);
				kwdSets[1].add(engKwd);
			}

			for (int j = 0; j < kwdSets.length; j++) {
				if (j != 0) {
					continue;
				}

				String abs = abss[j];

				try {
					abs = StrUtils.tag(abs, kwdSets[j], "KWD");
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}

				if (!abs.contains("KWD")) {
					continue;
				}

				List<TextSpan> spans = StrUtils.extract(abs, "KWD");

				for (TextSpan span : spans) {
					int start = span.getStart();
					int end = span.getEnd();
					System.out.println(span + " -> " + abs.substring(start, end));
				}
				System.out.println();

				Set<String> kwdSet = kwdSets[j];
				Set<String> founds = Generics.newHashSet();

				if (abs.length() > abss[j].length()) {
					writer.write(kwdSet + "\n");
					writer.write(abs.replace("\\.", "\n"));
					writer.write("\n\n");
				}

			}

		}
		reader.close();
		writer.close();
	}

}
