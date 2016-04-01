package ohs.nlp.pos;

import java.util.List;
import java.util.regex.Pattern;

import ohs.nlp.ling.types.KDocument;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class TextTokenizer {

	private enum Type {

	}

	private Pattern p1 = Pattern.compile("[\\p{Punct}]+");

	private Pattern p2 = Pattern.compile("[\\p{Digit}]+");

	public TextTokenizer() {

	}

	public List<String> splitSentences(String text) {
		List<String> lines = Generics.newArrayList();
		for (String line : text.split("[\n]+")) {
			lines.add(line);
		}

		return lines;
	}

	public KDocument tokenize(String text) {
		List<String> sents = splitSentences(text);

		return KDocument.newDocument(sents.toArray(new String[sents.size()]));
	}

	private String tokenize01(String text) {
		StringBuffer sb = new StringBuffer();

		List<String> toks = StrUtils.split(text);

		for (int i = 0; i < toks.size(); i++) {
			String tok = toks.get(i);
		}

		for (int i = 0; i < text.length(); i++) {
			String s = new String(new char[] { text.charAt(i) });

			if (p1.matcher(s).find()) {

			} else if (p2.matcher(s).find()) {

			}
		}

		return text;
	}

	private String tokenize02(String t) {
		StringBuffer sb = new StringBuffer();

		return sb.toString();
	}

}
