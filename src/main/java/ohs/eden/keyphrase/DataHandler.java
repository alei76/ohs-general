package ohs.eden.keyphrase;

import java.util.Arrays;
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
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.DeepCounterMap;
import ohs.types.SetMap;
import ohs.types.StrPair;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class DataHandler {

	public static final String NONE = "<none>";

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.mergeDumps();
		// dh.tagPOS();
		// dh.makeKeywordData();
		// dh.makeTitleData();

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

		for (StrPair kwdp : data.getKeywordIndexer().getObjects()) {

			for (String kwd : kwdp.asArray()) {
				if (kwd.equals(NONE)) {
					continue;
				}

				List<ohs.types.Pair<String, String>> pairs = ext.extract(kwd);

				for (ohs.types.Pair<String, String> pair : pairs) {
					cm.incrementCount(pair.getFirst(), kwdp.getFirst() + "#" + pair.getSecond().toLowerCase(), 1);
					dcm.incrementCount(pair.getFirst(), pair.getSecond().toLowerCase(), kwdp.getFirst(), 1);
				}
			}
		}

		FileUtils.writeStrCounterMap(KPPath.KEYWORD_ABBR_FILE, cm);

		System.out.println(dcm);

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
					String f = pair.getFirst().replace(" ", "_");
					String s = pair.getSecond();

					if (s.length() == 0) {
						continue;
					}

					sb.append(String.format("%s%s%s", f, Token.DELIM_TOKEN, s));

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

	public void makeKeywordData() throws Exception {
		SetMap<String, String> kwdToDocs = Generics.newSetMap();

		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_POS_FILE);
		reader.setPrintNexts(false);

		List<String> labels = Generics.newArrayList();

		while (reader.hasNext()) {
			reader.print(100000);

			String line = reader.next();

			String[] parts = line.split("\t");

			if (reader.getNumLines() == 1) {
				labels = Arrays.asList(parts);
			} else {
				if (parts.length != labels.size()) {
					continue;
				}

				parts = StrUtils.unwrap(parts);

				String type = parts[0];
				String cn = parts[1];
				String korKwdStr = parts[2].split(" => ")[0];
				String korKwdStrPos = parts[2].split(" => ")[1];
				String engKwdStr = parts[3];
				String korTitle = parts[4];
				String engTitle = parts[5];
				String korAbs = parts[6];
				String engAbs = parts[7];

				if (type.equals("patent")) {
					break;
				}

				korKwdStr = StrUtils.unwrap(korKwdStr);
				korKwdStrPos = StrUtils.unwrap(korKwdStrPos);

				List<String> korKwds = getKeywords(korKwdStr);
				List<String> engKwds = getKeywords(engKwdStr);

				if (korKwds.size() == 0 && engKwds.size() == 0) {

				} else if (korKwds.size() == engKwds.size()) {
					for (int j = 0; j < korKwds.size(); j++) {
						String kor = korKwds.get(j);
						String eng = engKwds.get(j);
						String[] p = StrUtils.wrap(StrUtils.asArray(kor, eng));
						kwdToDocs.put(StrUtils.join("\t", p), cn);
					}
				} else {
					for (String kwd : korKwds) {
						String[] p = StrUtils.wrap(StrUtils.asArray(kwd, ""));
						kwdToDocs.put(StrUtils.join("\t", p), cn);
					}

					for (String kwd : engKwds) {
						String[] p = StrUtils.wrap(StrUtils.asArray("", kwd));
						kwdToDocs.put(StrUtils.join("\t", p), cn);
					}
				}
			}
		}
		reader.printLast();
		reader.close();

		FileUtils.writeStrSetMap(KPPath.KEYWORD_DATA_FILE, kwdToDocs);
	}

	public void makeTitleData() throws Exception {

		List<String> labels = Generics.newArrayList();

		EnglishAnalyzer analyzer = new EnglishAnalyzer();

		CounterMap<String, String> cm = Generics.newCounterMap();

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

				parts = StrUtils.unwrap(parts);

				String type = parts[0];
				String cn = parts[1];
				String korKwdStr = parts[2].split(" => ")[0];
				String korKwdStrPos = parts[2].split(" => ")[1];
				String engKwdStr = parts[3];
				String korTitle = parts[4];
				String engTitle = parts[5];
				String korAbs = parts[6];
				String engAbs = parts[7];

				if (type.equals("patent")) {
					break;
				}

				korKwdStr = StrUtils.unwrap(korKwdStr);
				korKwdStrPos = StrUtils.unwrap(korKwdStrPos);

				if (korKwdStr.length() == 0 && engKwdStr.length() == 0) {
					continue;
				}

				Counter<String> c = Generics.newCounter();

				KDocument doc = TaggedTextParser.parse(korTitle + "\\n" + korAbs);
				//
				for (Token t : doc.getSubTokens()) {
					String word = t.getValue(TokenAttr.WORD);
					String pos = t.getValue(TokenAttr.POS);
					if (pos.startsWith("N")) {
						c.incrementCount(word.toLowerCase(), 1);
					}
				}

				c.incrementAll(AnalyzerUtils.getWordCounts(engTitle, analyzer));

				if (c.size() > 0) {
					cm.setCounter(cn, c);
				}
			}
		}
		reader.printLast();
		reader.close();
		// writer.close();

		FileUtils.writeStrCounterMap(KPPath.TITLE_DATA_FILE, cm);

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

				// System.out.println(line);

				if (reader.getNumLines() == 1) {
					parts = StrUtils.unwrap(parts);

					for (String p : parts) {
						labels.add(p);
					}
				} else {
					if (parts.length != labels.size()) {
						System.out.println(line);
						continue;
					}

					parts = StrUtils.unwrap(parts);

					String cn = parts[0];
					String korTitle = "";
					String engTitle = "";
					String korAbs = "";
					String engAbs = "";
					String korKwdStr = "";
					String engKwdStr = "";
					String type = "";

					if (i == 0) {
						korTitle = parts[1];
						engTitle = parts[2];
						korAbs = parts[5];
						engAbs = parts[6];
						korKwdStr = parts[3];
						engKwdStr = parts[4];
						type = "paper";
					} else if (i == 1) {
						korTitle = parts[1];
						engTitle = parts[2];
						korAbs = parts[5];
						engAbs = parts[6];
						korKwdStr = parts[3];
						engKwdStr = parts[4];
						type = "report";
					} else if (i == 2) {
						String applno = parts[0];
						korTitle = parts[1];
						engTitle = parts[2];
						cn = parts[3];
						korAbs = parts[4];
						type = "patent";
					}

					List<String> korKwds = getKeywords(korKwdStr);
					List<String> engKwds = getKeywords(engKwdStr);

					String[] res = new String[] { type, cn, StrUtils.join(";", korKwds), StrUtils.join(";", engKwds), korTitle, engTitle,
							korAbs, engAbs };
					String output = StrUtils.join("\t", StrUtils.wrap(res));

					writer.write(String.format("\n%s", output));
				}
			}
			reader.close();
		}
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

				parts = StrUtils.unwrap(parts);

				String type = parts[0];
				String cn = parts[1];
				String korKwdStr = parts[2];
				String engKwdStr = parts[3];
				String korTitle = parts[4].replace("\n", "\\n").replace("\t", "\\t");
				String engTitle = parts[5];
				String korAbs = parts[6].replace("\n", "\\n").replace("\t", "\\t");
				String engAbs = parts[7];

				List<String> korKwds1 = getKeywords(korKwdStr);
				List<String> korKwds2 = Generics.newArrayList(korKwds1.size());
				List<String> engKwds = getKeywords(engKwdStr);

				// if (type.equals("patent")) {
				// break;
				// }

				if ((korTitle.length() == 0 && korAbs.length() == 0) || (engTitle.length() == 0 && engAbs.length() == 0)) {
					continue;
				}

				for (int i = 0; i < korKwds1.size(); i++) {
					String kwd = korKwds1.get(i);
					kwd = getText(komoran.analyze(kwd, 1));
					korKwds2.add(kwd);
				}

				parts[2] = String.format("\"%s\" => \"%s\"", StrUtils.join(";", korKwds1), StrUtils.join(";", korKwds2));
				parts[4] = getText(komoran.analyze(korTitle, 1)).replace("\n", "\\n").replace("\t", "\\t");
				// parts[6] = getText(komoran.analyze(korAbs, 1)).replace("\n", "\\n").replace("\t", "\\t");

				parts = StrUtils.wrap(parts);

				writer.write("\n" + StrUtils.join("\t", parts));

				num_docs++;
			}
		}
		reader.printLast();
		reader.close();
		writer.close();
	}

}
