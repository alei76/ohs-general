package ohs.nlp.pos;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ohs.io.TextFileReader;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.utils.Generics;

public class SejongReader implements Iterator<KDocument> {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		SejongReader r = new SejongReader(NLPPath.POS_DATA_FILE);
		while (r.hasNext()) {
			KDocument doc = r.next();
		}
		r.close();

		System.out.println("process ends.");
	}

	private TextFileReader reader;

	private KDocument doc;

	private int num_docs;

	private Set<String> posSet;

	public SejongReader(String dataFileName) throws Exception {
		reader = new TextFileReader(dataFileName);

		posSet = Generics.newHashSet();

		for (SJTag tag : SJTag.values()) {
			posSet.add(tag.toString());
		}

	}

	public void close() {
		reader.close();
	}

	private void filter(KDocument doc) {
		List<KSentence> sents = Generics.newArrayList();

		for (KSentence sent : doc.getSentences()) {
			boolean isValid = true;
			for (Token tok : sent.getTokens()) {
				MultiToken mt = (MultiToken) tok;
				for (Token t : mt.getTokens()) {
					String pos = t.get(TokenAttr.POS);
					if (!posSet.contains(pos)) {
						isValid = false;
						break;
					}
				}
				if (!isValid) {
					break;
				}
			}

			if (isValid) {
				sents.add(sent);
			}
		}
		doc.setSentences(sents.toArray(new KSentence[sents.size()]));

		SejongParser.enumerateStarts(doc);
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
			doc = SejongParser.parseDocument(text);

			filter(doc);

			if (doc.size() > 0) {
				hasNext = true;
			}
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

}
