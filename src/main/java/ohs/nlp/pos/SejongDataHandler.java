package ohs.nlp.pos;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.nlp.ling.types.KCollection;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class SejongDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		SejongDataHandler sdh = new SejongDataHandler();
		// sdh.extractPosData();
		sdh.tagPOS();

		System.out.println("process ends.");
	}

	public void tagPOS() throws Exception {
		TextFileReader reader = new TextFileReader(NLPPath.POS_DATA_FILE);
		reader.setPrintNexts(true);

		KCollection col = new KCollection();

		while (reader.hasNext()) {
			reader.print(10000);

			if (reader.getNumNexts() > 10) {
				break;
			}
			List<String> lines = reader.getNextLines();

			List<MultiToken> mts = Generics.newArrayList();

			Token t = new Token(0, "<S>");
			t.setValue(TokenAttr.POS, "<S>");

			mts.add(new MultiToken(0, new Token[] { t }));

			for (int i = 0, loc = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				String[] parts = line.split("\t");

				String[] toks = parts[parts.length - 1].split(" \\+ ");
				Token[] ts = new Token[toks.length];

				for (int j = 0; j < toks.length; j++) {
					String[] two = StrUtils.split2Two("/", toks[j]);
					String word = two[0];
					String pos = two[1];
					t = new Token(loc, word);
					t.setValue(TokenAttr.POS, pos);
					ts[j] = t;
					loc += word.length();
				}
				loc++;

				MultiToken mt = new MultiToken(ts[0].getStart(), ts);
				mts.add(mt);
			}

			t = new Token(0, "</S>");
			t.setValue(TokenAttr.POS, "</S>");

			mts.add(new MultiToken(0, new Token[] { t }));

			KSentence sent = new KSentence(mts.toArray(new MultiToken[mts.size()]));
			col.add(sent.toDocument());
		}
		reader.printLast();
		reader.close();

		col.write(NLPPath.POS_SENT_COL_FILE);
	}

	public void extractPosData() throws Exception {

		String inputFileName = NLPPath.POS_RAW_DATA_FILE;

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

			List<String> lines = Generics.newArrayList();

			while ((line = br.readLine()) != null) {

				if (line.startsWith(startTag)) {
					lines.add(line);
				} else {
					if (lines.size() > 0) {
						sb.append(String.join("\n", lines));
						sb.append("\n\n");

						lines.clear();
						lines = Generics.newArrayList();
					}
				}
			}

			writer.write(sb.toString().trim() + "\n\n");
		}
		br.close();
		writer.close();

		// writer.close();

	}

}
