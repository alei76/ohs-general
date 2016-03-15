package ohs.nlp;

import java.util.List;

import ohs.ling.types.Document;
import ohs.ling.types.MultiToken;
import ohs.utils.StrUtils;

public class TextTokenizer {

	public static final String LINE_SEP = System.getProperty("line.separator");

	public TextTokenizer() {

	}

	public Document tokenize(String text) {

		String[] lines = text.split(LINE_SEP);

		for (int i = 0, loc = 0; i < lines.length; i++) {
			String line = lines[i];
			String[] words = line.split("[\\s]+");

			for (int j = 0; j < words.length; j++) {
				Token t = new Token(0, words[j])
				MultiToken mt = new MultiToken(loc++, new Token[]{);
			}
		}

		return null;
	}
}
