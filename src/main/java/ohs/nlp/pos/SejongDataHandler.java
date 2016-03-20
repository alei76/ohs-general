package ohs.nlp.pos;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class SejongDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		SejongDataHandler sdh = new SejongDataHandler();
		// sdh.extractPosData();
		sdh.extractCounts();

		System.out.println("process ends.");
	}

	public void extractCounts() throws Exception {

		Counter<String> posCnts = Generics.newCounter();
		Counter<String> wordCnts = Generics.newCounter();
		CounterMap<String, String> posTransCnts = Generics.newCounterMap();
		CounterMap<String, String> wordPosCnts = Generics.newCounterMap();

		Set<String> posSet = Generics.newHashSet();

		for (String line : FileUtils.readLines(NLPPath.POS_TAG_SET_FILE)) {
			String[] parts = line.split("\t");
			posSet.add(parts[0]);
		}

		CounterMap<String, String> cm = Generics.newCounterMap();

		SejongDocumentReader reader = new SejongDocumentReader(NLPPath.POS_DATA_FILE);
		while (reader.hasNext()) {
			KDocument doc = reader.next();

			for (KSentence sent : doc.getSentences()) {
				for (MultiToken mt : sent.getTokens()) {
					cm.incrementCount(mt.getText(), mt.joinValues(), 1);
				}
			}
		}
		reader.close();

		FileUtils.writeStrCounterMap(NLPPath.WORD_POS_CNT_ILE, cm);

	}

	public void extractPosData() throws Exception {
		TextFileWriter writer = new TextFileWriter(NLPPath.POS_DATA_FILE);
		ZipInputStream zio = new ZipInputStream(new FileInputStream(NLPPath.SEJONG_POS_DATA_FILE));
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
