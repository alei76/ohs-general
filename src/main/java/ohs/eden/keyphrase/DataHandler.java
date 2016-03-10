package ohs.eden.keyphrase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kr.co.shineware.nlp.komoran.core.analyzer.Komoran;
import kr.co.shineware.util.common.model.Pair;
import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class DataHandler {

	public static final String NONE = "<none>";

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.mergeDumps();
		dh.tagPOS();
		// dh.extractKeywordData();
		System.out.println("process ends.");
	}

	public void extractPatterns() {
		List<String> labels = Generics.newArrayList();
		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_POS_FILE);
		while (reader.hasNext()) {

			String line = reader.next();
			String[] parts = line.split("\t");

			if (reader.getNumLines() == 1) {
				for (String p : parts) {
					labels.add(p);
				}
			} else {
				if (parts.length != labels.size()) {
					continue;
				}

				removeSurroundings(parts);

				String type = parts[0];
				String cn = parts[1];
				String korKwdStr = parts[2];
				String engKwdStr = parts[3];
				String korTitle = parts[4];
				String engTitle = parts[5];
				String korAbs = parts[6];
				String engAbs = parts[7];

				System.out.println(korKwdStr);
			}
		}
		reader.close();
	}

	private void removeSurroundings(String[] parts) {
		for (int j = 0; j < parts.length; j++) {
			if (parts[j].length() > 1) {
				parts[j] = parts[j].substring(1, parts[j].length() - 1);
			}
		}
	}

	private void appendSurroundings(String[] parts) {
		for (int j = 0; j < parts.length; j++) {
			parts[j] = String.format("\"%s\"", parts[j]);
		}
	}

	public void extractKeywordData() throws Exception {
		String[] inFileNames = { KPPath.PAPER_DUMP_FILE, KPPath.REPORT_DUMP_FILE };

		SetMap<String, String> keywordDocs = Generics.newSetMap();

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

				removeSurroundings(parts);

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
								keywordDocs.put(kor + "\t" + eng, cn);
							}
						}
					} else {
						for (String kor : kors) {
							keywordDocs.put(kor + "\t" + NONE, cn);
						}

						for (String eng : engs) {
							keywordDocs.put(NONE + "\t" + eng, cn);
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

	private String getText(List<List<List<Pair<String, String>>>> result) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < result.size(); i++) {
			List<List<Pair<String, String>>> ll = result.get(i);
			for (int j = 0; j < ll.size(); j++) {
				List<Pair<String, String>> l = ll.get(j);

				for (int k = 0; k < l.size(); k++) {
					Pair<String, String> pair = l.get(k);
					sb.append(String.format("%s#S#%s", pair.getFirst().replace(" ", "_"), pair.getSecond()));
					if (k != l.size() - 1) {
						sb.append("#P#");
					}
				}

				if (j != ll.size() - 1) {
					sb.append(" ");
				}
			}
			if (i != ll.size() - 1) {
				sb.append("\n");
			}
		}

		return sb.toString().trim();
	}

	private Counter<String> getWordCounts(String text) {
		Counter<String> ret = Generics.newCounter();
		text = text.replace(". ", " .\n");
		for (String sent : text.split("\n")) {
			for (String word : StrUtils.split(sent)) {
				ret.incrementCount(word.toLowerCase(), 1);
			}
		}
		return ret;
	}

	public void mergeDumps() {
		TextFileWriter writer = new TextFileWriter(KPPath.SINGLE_DUMP_FILE);
		writer.write("TYPE\tCN\tKOR_KWD\tENG_KWD\tENG_TITLE\tKOR_TITLE\tKOR_ABS\tENG_ABS");

		String[] inFileNames = { KPPath.PAPER_DUMP_FILE, KPPath.REPORT_DUMP_FILE, KPPath.PATENT_DUMP_FILE };

		for (int i = 0; i < inFileNames.length; i++) {
			String inFileName = inFileNames[i];

			TextFileReader reader = new TextFileReader(inFileName);
			List<String> labels = Generics.newArrayList();

			while (reader.hasNext()) {
				String line = reader.next();
				String[] parts = line.split("\t");

				removeSurroundings(parts);

				if (reader.getNumLines() == 1) {
					for (String p : parts) {
						labels.add(p);
					}
				} else {
					if (parts.length != labels.size()) {
						continue;
					}

					String cn = parts[0];
					String korTitle = "";
					String engTitle = "";
					String korAbs = "";
					String engAbs = "";
					String korKeywordStr = "";
					String engKeywordStr = "";
					String type = "";

					if (i == 0) {
						korTitle = parts[4];
						engTitle = parts[5];
						korAbs = parts[10];
						engAbs = parts[11];
						korKeywordStr = parts[8];
						engKeywordStr = parts[9];
						type = "paper";
					} else if (i == 1) {
						korTitle = parts[1];
						engTitle = parts[2];
						korAbs = parts[7];
						engAbs = parts[8];

						korKeywordStr = parts[5];
						engKeywordStr = parts[6];
						type = "report";
					} else if (i == 2) {
						String applno = parts[0];
						korTitle = parts[1];
						engTitle = parts[2];
						cn = parts[3];
						korAbs = parts[4];
						String cm = parts[5];
						type = "patent";
					}

					List<String> korKwds = getKeywords(korKeywordStr);
					List<String> engKwds = getKeywords(engKeywordStr);

					korKeywordStr = StrUtils.join(";", korKwds);
					engKeywordStr = StrUtils.join(";", engKwds);

					List<String> res = Generics.newArrayList();
					res.add(String.format("\"%s\"", type));
					res.add(String.format("\"%s\"", cn));
					res.add(String.format("\"%s\"", korKeywordStr));
					res.add(String.format("\"%s\"", engKeywordStr));
					res.add(String.format("\"%s\"", korTitle));
					res.add(String.format("\"%s\"", engTitle));
					res.add(String.format("\"%s\"", korAbs));
					res.add(String.format("\"%s\"", engAbs));

					String output = StrUtils.join("\t", res);

					writer.write(String.format("\n%s", output));
				}
			}
			reader.close();
		}
		writer.close();
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

				Set<String> kwdSet = kwdSets[j];
				List<String> founds = Generics.newArrayList();

				for (String kwd : kwdSet) {
					if (abs.contains(kwd)) {
						founds.add(kwd);
					}
				}

				if (founds.size() > 0) {
					writer.write(String.format("Keywords:\t%s\n", StrUtils.join("\t", founds)));
					writer.write(String.format("ABS:\t%s\n\n", abs));
				}

			}
		}
		reader.close();
		writer.close();
	}

	public void tagPOS() {
		Komoran komoran = new Komoran("lib/models-full/");

		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_FILE);
		reader.setPrintNexts(false);

		TextFileWriter writer = new TextFileWriter(KPPath.SINGLE_DUMP_POS_FILE);
		List<String> labels = Generics.newArrayList();
		int num_docs = 0;

		while (reader.hasNext()) {
			reader.print(100000);

			// if (num_docs > 10000) {
			// break;
			// }

			String line = reader.next();
			String[] parts = line.split("\t");

			if (reader.getNumLines() == 1) {
				for (String p : parts) {
					labels.add(p);
				}
				writer.write(line);
			} else {
				if (parts.length != labels.size()) {
					continue;
				}

				for (int j = 0; j < parts.length; j++) {
					if (parts[j].length() > 1) {
						parts[j] = parts[j].substring(1, parts[j].length() - 1);
					}
				}

				String type = parts[0];
				String cn = parts[1];
				String korKwdStr = parts[2];
				String engKwdStr = parts[3];
				String korTitle = parts[4];
				String engTitle = parts[5];
				String korAbs = parts[6];
				String engAbs = parts[7];

				List<String> korKwds = getKeywords(korKwdStr);

				if (type.equals("patent")) {
					break;
				}

				if (!type.equals("patent")) {
					if (korTitle.length() == 0 || korAbs.length() == 0) {
						continue;
					}
					if (korKwds.size() == 0) {
						continue;
					}
				}

				for (int i = 0; i < korKwds.size(); i++) {
					String kwd = korKwds.get(i);
					kwd = getText(komoran.analyze(kwd, 1));
					korKwds.set(i, kwd);
				}

				korKwdStr = StrUtils.join(";", korKwds);

				parts[2] = korKwdStr;
				parts[4] = getText(komoran.analyze(korTitle, 1));
				parts[6] = getText(komoran.analyze(korAbs, 1));

				for (int i = 0; i < parts.length; i++) {
					parts[i] = String.format("\"%s\"", parts[i]);
				}
				writer.write("\n" + StrUtils.join("\t", parts));

				num_docs++;
			}
		}
		reader.printLast();
		reader.close();
		writer.close();
	}

}
