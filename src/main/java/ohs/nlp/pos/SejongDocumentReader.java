package ohs.nlp.pos;

import java.util.Iterator;

import ohs.io.TextFileReader;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;

public class SejongDocumentReader implements Iterator<KDocument> {

	private TextFileReader reader;

	public SejongDocumentReader(String fileName) throws Exception {
		reader = new TextFileReader(fileName);
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		SejongDocumentReader r = new SejongDocumentReader(NLPPath.POS_DATA_FILE);
		while (r.hasNext()) {
			KDocument doc = r.next();
		}

		System.out.println("process ends.");
	}

	private KDocument doc;

	private int num_docs;

	private void buildDoc(String text) {
		KDocument ret = KDocument.parse(text);
		MultiToken[] mts = ret.getTokens();
		for (int i = 0, loc = 0; i < mts.length; i++) {
			MultiToken mt = mts[i];
			mt.setStart(loc);
			for (int j = 0; j < mt.size(); j++) {
				Token t = mt.getToken(j);
				t.setStart(loc);
				loc += t.length();
			}
			loc++;
		}
	}

	@Override
	public boolean hasNext() {
		StringBuffer sb = new StringBuffer();

		while (reader.hasNext()) {
			String line = reader.next();
			if (line.startsWith("</doc>")) {
				break;
			} else {
				if (!line.startsWith("<doc id")) {
					sb.append(line + "\n");
				}
			}
		}

		String text = sb.toString().trim();

		boolean hasNext = false;

		if (text.length() > 0) {
			doc = KDocument.parse(text);
			MultiToken[] mts = doc.getTokens();
			for (int i = 0, loc = 0; i < mts.length; i++) {
				MultiToken mt = mts[i];
				mt.setStart(loc);
				for (int j = 0; j < mt.size(); j++) {
					Token t = mt.getToken(j);
					t.setStart(loc);
					loc += t.length();
				}
				loc++;
			}
			hasNext = true;
			num_docs++;
		} else {
			doc = null;
		}

		return hasNext;
	}

	@Override
	public KDocument next() {
		return doc;
	}

	public void close() {
		reader.close();
	}

}
