package ohs.nlp.pos;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.math.stat.descriptive.SynchronizedMultivariateSummaryStatistics;
import org.apache.lucene.util.fst.Outputs;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.nlp.ling.types.KDocumentCollection;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class SejongDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		SejongDataHandler sdh = new SejongDataHandler();
		sdh.extractPosData();
		// sdh.extractPosSentences();
		// sdh.extractCounts();

		System.out.println("process ends.");
	}

	public void extractCounts() throws Exception {
		TextFileReader reader = new TextFileReader(NLPPath.POS_DATA_FILE);
		reader.setPrintNexts(true);

		Counter<String> posCnts = Generics.newCounter();
		Counter<String> wordCnts = Generics.newCounter();
		CounterMap<String, String> posTransCnts = Generics.newCounterMap();
		CounterMap<String, String> wordPosCnts = Generics.newCounterMap();

		Set<String> posSet = Generics.newHashSet();

		for (String line : FileUtils.readLines(NLPPath.POS_TAG_SET_FILE)) {
			String[] parts = line.split("\t");
			posSet.add(parts[0]);
		}

		while (reader.hasNext()) {
			reader.print(10000);

			// if (reader.getNumNexts() > 10) {
			// break;
			// }
			List<String> lines = reader.getNextLines();

			List<MultiToken> mts = Generics.newArrayList();

			for (int i = 0, loc = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				String[] parts = line.split("\t");

				String[] toks = parts[parts.length - 1].split(" \\+ ");
				Token[] ts = new Token[toks.length];

				for (int j = 0; j < toks.length; j++) {
					String[] two = StrUtils.split2Two("/", toks[j]);
					String word = two[0];
					String pos = two[1];
					Token t = new Token(loc, word);
					t.setValue(TokenAttr.POS, pos);
					ts[j] = t;
					loc += word.length();
				}
				loc++;

				MultiToken mt = new MultiToken(ts[0].getStart(), ts);
				mts.add(mt);
			}

			KSentence sent = new KSentence(mts);
			Token[] toks = sent.toTokens();
			boolean isValid = true;

			for (int i = 0; i < toks.length; i++) {
				Token t = toks[i];
				String pos = t.getValue(TokenAttr.POS);
				if (!posSet.contains(pos)) {
					isValid = false;
					break;
				}
			}

			if (!isValid) {
				continue;
			}

			for (int i = 0; i < toks.length; i++) {
				Token t1 = toks[i];
				String word1 = t1.getValue(TokenAttr.WORD);
				String pos1 = t1.getValue(TokenAttr.POS);

				wordCnts.incrementCount(word1, 1);
				posCnts.incrementCount(pos1, 1);
				wordPosCnts.incrementCount(word1, pos1, 1);

				if (i + 1 < toks.length) {
					Token t2 = toks[i + 1];
					String pos2 = t2.getValue(TokenAttr.POS);
					posTransCnts.incrementCount(pos1, pos2, 1);
				}
			}

		}
		reader.printLast();
		reader.close();

		FileUtils.writeStrCounter(NLPPath.DATA_DIR + "word_cnt.txt", wordCnts);
		FileUtils.writeStrCounter(NLPPath.DATA_DIR + "pos_cnt.txt", posCnts);
		FileUtils.writeStrCounterMap(NLPPath.DATA_DIR + "pos_trans_cnt.txt", posTransCnts);
		FileUtils.writeStrCounterMap(NLPPath.DATA_DIR + "word_pos_cnt.txt", wordPosCnts);
	}

	public void extractPosSentences() throws Exception {
		TextFileReader reader = new TextFileReader(NLPPath.POS_DATA_FILE);
		reader.setPrintNexts(true);

		KDocumentCollection col = new KDocumentCollection();

		while (reader.hasNext()) {
			reader.print(10000);

			// if (reader.getNumNexts() > 10) {
			// break;
			// }
			List<String> lines = reader.getNextLines();

			List<MultiToken> mts = Generics.newArrayList();

			for (int i = 0, loc = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				String[] parts = line.split("\t");

				String[] toks = parts[parts.length - 1].split(" \\+ ");
				Token[] ts = new Token[toks.length];

				for (int j = 0; j < toks.length; j++) {
					String[] two = StrUtils.split2Two("/", toks[j]);
					String word = two[0];
					String pos = two[1];
					Token t = new Token(loc, word);
					t.setValue(TokenAttr.POS, pos);
					ts[j] = t;
					loc += word.length();
				}
				loc++;

				MultiToken mt = new MultiToken(ts[0].getStart(), ts);
				mts.add(mt);
			}

			KSentence sent = new KSentence(mts.toArray(new MultiToken[mts.size()]));
			col.add(sent.toDocument());
		}
		reader.printLast();
		reader.close();

		col.write(NLPPath.POS_SENT_COL_FILE);
	}

	public void extractPosData() throws Exception {

		String inputFileName = NLPPath.SEJONG_POS_DATA_FILE;

		String outputFileName = NLPPath.POS_DATA_FILE;

		TextFileWriter writer = new TextFileWriter(outputFileName);

		ZipInputStream zio = new ZipInputStream(new FileInputStream(inputFileName));
		BufferedReader br = new BufferedReader(new InputStreamReader(zio, "utf-16"));
		ZipEntry ze = null;

		while ((ze = zio.getNextEntry()) != null) {
			if (ze.isDirectory()) {
				continue;
			}

			String name = FileUtils.removeExtension(ze.getName());
			String startTag = name + "-";

			StringBuffer sb = new StringBuffer();
			String line = "";

			boolean isDoc = false;

			while ((line = br.readLine()) != null) {

				if (line.startsWith("<body>")) {
					isDoc = true;
					continue;
				} else if (line.startsWith("</body>")) {
					isDoc = false;

					String docText = sb.toString().trim();
					sb = new StringBuffer();

					String[] lines = docText.split("\n");

					List<String> inputs = Generics.newArrayList();
					List<String> outputs = Generics.newArrayList();

					for (int i = 0; i < lines.length; i++) {
						if (lines[i].startsWith(startTag)) {
							String[] parts = lines[i].split("\t");

							String[] subparts = parts[2].split(" \\+ ");

							for (int j = 0; j < subparts.length; j++) {
								String[] two = StrUtils.split2Two("/", subparts[j]);
								String word = two[0];
								String pos = two[1];
								subparts[j] = word + " / " + pos;
							}

							parts[2] = StrUtils.join(" + ", subparts);

							String input = StrUtils.join("\t", parts, 1, 3);
							inputs.add(input);
						} else {
							if (inputs.size() > 0) {
								String output = StrUtils.join("\n", inputs);
								outputs.add(output);
							}
							inputs.clear();
						}
					}

					StringBuffer res = new StringBuffer();
					res.append(String.format("<doc id=%s>\n", name));
					res.append(StrUtils.join("\n\n", outputs));
					res.append("\n</doc>");
					writer.write(res.toString() + "\n\n");
				}

				if (isDoc) {
					sb.append(line + "\n");
				}
			}
		}
		br.close();
		writer.close();
	}

}
