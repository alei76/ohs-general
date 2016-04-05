package ohs.eden.keyphrase;

import java.util.List;

import org.apache.lucene.analysis.en.EnglishAnalyzer;

import kr.co.shineware.nlp.komoran.core.analyzer.Komoran;
import kr.co.shineware.util.common.model.Pair;
import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.nlp.pos.TextTokenizer;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.DeepCounterMap;
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
		// dh.makeKeywordData();
		// dh.makeAbsData();
		// dh.extractKeywordAbbreviations();

		System.out.println("process ends.");
	}

	public void extractKeywordAbbreviations() throws Exception {
		KeywordData data = new KeywordData();

		if (FileUtils.exists(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"))) {
			data.read(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));
		} else {
			data.readText(KPPath.KEYWORD_DATA_FILE);
			data.write(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));
		}

		AbbreviationExtractor ext = new AbbreviationExtractor();

		CounterMap<String, String> cm = Generics.newCounterMap();

		DeepCounterMap<String, String, String> dcm = Generics.newDeepCounterMap();

		for (String kwdStr : data.getKeywordIndexer().getObjects()) {
			String[] two = kwdStr.split("\t");
			String korKwd = two[0];
			String engKwd = two[1];

			for (String kwd : two) {
				if (kwd.equals(NONE)) {
					continue;
				}

				List<ohs.types.Pair<String, String>> pairs = ext.extract(kwd);

				for (ohs.types.Pair<String, String> pair : pairs) {
					cm.incrementCount(pair.getFirst(), korKwd + "#" + pair.getSecond().toLowerCase(), 1);
					dcm.incrementCount(pair.getFirst(), pair.getSecond().toLowerCase(), korKwd, 1);
				}
			}
		}

		FileUtils.writeStrCounterMap(KPPath.KEYWORD_ABBR_FILE, cm);

		System.out.println(dcm);

	}

	public void makeAbsData() throws Exception {

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

				if (korAbs.length() > 0) {

					for (String kwd : korKwdStr.split(";")) {
						KSentence kw = TaggedTextParser.parse(kwd).toSentence();

						for (String word : kw.getValues(TokenAttr.WORD)) {

						}

						System.out.println(kw);
					}

					KDocument abs = TaggedTextParser.parse(korAbs);

					// KDocument input = new KSentence(doc.getSubTokens()).toDocument();

					System.out.println();

				}
			}
		}
		reader.close();

	}

	public void makeKeywordData() throws Exception {
		SetMap<String, String> kwdToDocs = Generics.newSetMap();

		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_FILE);
		reader.setPrintNexts(false);

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

				if (type.equals("patent")) {
					break;
				}

				List<String> korKwds = getKeywords(korKwdStr);
				List<String> engKwds = getKeywords(engKwdStr);

				if (korKwds.size() == 0 && engKwds.size() == 0) {

				} else if (korKwds.size() == engKwds.size()) {
					if (korKwds.size() > 0) {
						for (int j = 0; j < korKwds.size(); j++) {
							String kor = korKwds.get(j);
							String eng = engKwds.get(j);
							kwdToDocs.put(String.format("\"%s\"\t\"%s\"", kor, eng), cn);
						}
					}
				} else {
					for (String kwd : korKwds) {
						kwdToDocs.put(String.format("\"%s\"\t\"%s\"", kwd, ""), cn);
					}

					for (String kwd : engKwds) {
						kwdToDocs.put(String.format("\"%s\"\t\"%s\"", "", kwd), cn);
					}
				}

				num_docs++;
			}
		}
		reader.printLast();
		reader.close();

		FileUtils.writeStrSetMap(KPPath.KEYWORD_DATA_FILE, kwdToDocs);
	}

	public void extractKeywordPatterns() throws Exception {
		Counter<String> patCnts = Generics.newCounter();

		List<String> labels = Generics.newArrayList();
		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_POS_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(10000);

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

				String[] korKwds = korKwdStr.split(";");

				for (String kwd : korKwds) {
					KSentence sent = TaggedTextParser.parse(kwd).getSentence(0);
					String pat = String.join(" ", sent.getValues(TokenAttr.POS));
					patCnts.incrementCount(pat, 1);
				}
			}
		}
		reader.printLast();
		reader.close();

		FileUtils.writeStrCounter(KPPath.KEYWORD_POS_CNT_FILE, patCnts);
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
					sb.append(String.format("%s%s%s", pair.getFirst().replace(" ", "_"), Token.DELIM_TOKEN, pair.getSecond()));
					if (k != l.size() - 1) {
						sb.append(MultiToken.DELIM_MULTI_TOKEN);
					}
				}

				if (j != ll.size() - 1) {
					sb.append("\\t");
				}
			}
			if (i != ll.size() - 1) {
				sb.append("\\n");
			}
		}

		return sb.toString().trim();
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
					String korKwdStr = "";
					String engKwdStr = "";
					String type = "";

					if (i == 0) {
						korTitle = parts[4];
						engTitle = parts[5];
						korAbs = parts[10];
						engAbs = parts[11];
						korKwdStr = parts[8];
						engKwdStr = parts[9];
						type = "paper";
					} else if (i == 1) {
						korTitle = parts[1];
						engTitle = parts[2];
						korAbs = parts[7];
						engAbs = parts[8];

						korKwdStr = parts[5];
						engKwdStr = parts[6];
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

					List<String> korKwds = getKeywords(korKwdStr);
					List<String> engKwds = getKeywords(engKwdStr);

					korKwdStr = StrUtils.join(";", korKwds);
					engKwdStr = StrUtils.join(";", engKwds);

					List<String> res = Generics.newArrayList();
					res.add(String.format("\"%s\"", type));
					res.add(String.format("\"%s\"", cn));
					res.add(String.format("\"%s\"", korKwdStr));
					res.add(String.format("\"%s\"", engKwdStr));
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

	private void removeSurroundings(String[] parts) {
		for (int j = 0; j < parts.length; j++) {
			if (parts[j].length() > 1) {
				parts[j] = parts[j].substring(1, parts[j].length() - 1);
			}
		}
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

				StrUtils.unwrap(parts, "\"", "\"", parts);

				String type = parts[0];
				String cn = parts[1];
				String korKwdStr = parts[2];
				String engKwdStr = parts[3];
				String korTitle = parts[4];
				String engTitle = parts[5];
				String korAbs = parts[6];
				String engAbs = parts[7];

				List<String> korKwds1 = getKeywords(korKwdStr);
				List<String> korKwds2 = Generics.newArrayList(korKwds1.size());

				// if (type.equals("patent")) {
				// break;
				// }

				if (!type.equals("patent")) {
					if (korTitle.length() == 0 || korAbs.length() == 0) {
						continue;
					}
					if (korKwds1.size() == 0) {
						continue;
					}
				}

				for (int i = 0; i < korKwds1.size(); i++) {
					String kwd = korKwds1.get(i);
					kwd = getText(komoran.analyze(kwd, 1));
					korKwds2.add(kwd);
				}

				korKwdStr = StrUtils.join(";", korKwds1);

				parts[2] = korKwdStr + " => " + StrUtils.join(";", korKwds2);
				parts[4] = getText(komoran.analyze(korTitle, 1));
				parts[6] = getText(komoran.analyze(korAbs, 1));

				StrUtils.wrap(parts, "\"", "\"", parts);

				writer.write("\n" + StrUtils.join("\t", parts));

				num_docs++;
			}
		}
		reader.printLast();
		reader.close();
		writer.close();
	}

}
