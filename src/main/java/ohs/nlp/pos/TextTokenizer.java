package ohs.nlp.pos;

import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;

public class TextTokenizer {

	public static final String LINE_SEP = System.getProperty("line.separator");

	public TextTokenizer() {

	}

	public KDocument tokenize(String text) {
		String[] lines = text.split(LINE_SEP);
		KSentence[] sents = new KSentence[lines.length];
		for (int i = 0, loc = 0; i < lines.length; i++) {
			String line = lines[i];
			String[] words = line.split("[\\s]+");
			MultiToken[] mts = new MultiToken[words.length];
			for (int j = 0; j < words.length; j++) {
				Token t = new Token(loc, words[j]);
				MultiToken mt = new MultiToken(loc, new Token[] { t });
				mts[j] = mt;
				loc += t.length();
			}
			sents[i] = new KSentence(mts);
			loc++;
		}
		return new KDocument(sents);
	}
}
