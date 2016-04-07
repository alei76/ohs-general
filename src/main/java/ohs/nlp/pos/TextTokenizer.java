package ohs.nlp.pos;

import java.util.List;
import java.util.regex.Pattern;

import ohs.nlp.ling.types.KDocument;
import ohs.utils.Generics;
import ohs.utils.StrUtils;
import ohs.utils.UnicodeUtils;

public class TextTokenizer {

	private enum Type {

	}

	private Pattern punctPat = Pattern.compile("[\\p{Punct}]+");

	private Pattern digitPat = Pattern.compile("[\\p{Digit}]+");

	private Pattern engPat = Pattern.compile("[\\p{Digit}]+");

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
		StringBuffer sb = new StringBuffer();

		for (String line : text.split("[\n]+")) {
			for (String word : line.split("[\\s]+")) {
				word = tokenizeWord(word);
				sb.append(word);

				if (word.endsWith("다 .") || word.endsWith("다.")) {
					sb.append("\n");
				} else {
					sb.append(" ");
				}
			}
			sb.append("\n");
		}
		return KDocument.newDocument(sb.toString().trim().split("\n"));
	}

	private String tokenizeLine(String line) {
		StringBuffer sb = new StringBuffer();
		List<String> words = StrUtils.split(line);

		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			word = tokenizeWord(word);

			sb.append(word);

			if (i != words.size() - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	private String tokenizeWord(String word) {
		StringBuffer sb = new StringBuffer();
		int len = word.length();

		for (int i = 0; i < len; i++) {
			char c = word.charAt(i);

			if (c == '.') {
				int end = i + 1;
				if (end == len) {
					// char pc = StrUtils.value(i - 1 >= 0, word.charAt(i - 1), '#');
					char pc = '#';
					if (i - 1 >= 0) {
						pc = word.charAt(i - 1);
					}

					if (pc == '다') {
						sb.append(" ");
					}
				}
				sb.append(word.substring(i, end));

				i = end - 1;
			} else if (c == '\"' || c == '\'' || c == '{' || c == '}' || c == '(' || c == ')') {
				int end = i + 1;
				while (end < len) {
					char nc = word.charAt(end);
					if (c == nc) {
						end++;
					} else {
						break;
					}
				}

				if (sb.length() > 0) {
					sb.append(" ");
				}

				sb.append(word.substring(i, end));

				i = end - 1;
			} else if (UnicodeUtils.isEnglish(c)) {
				int end = i + 1;
				while (end < len) {
					char nc = word.charAt(end);
					if (UnicodeUtils.isEnglish(nc) || nc == '-') {
						end++;
					} else {
						break;
					}
				}

				if (sb.length() > 0) {
					sb.append(" ");
				}

				sb.append(word.substring(i, end));

				i = end - 1;
			} else if (UnicodeUtils.isKorean(c)) {
				int end = i + 1;
				while (end < len) {
					char nc = word.charAt(end);
					if (UnicodeUtils.isKorean(nc)) {
						end++;
					} else if (nc == '-') {
						if (end == len - 1) {
							break;
						} else {
							end++;
						}
					}

					else {
						break;
					}
				}

				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(word.substring(i, end));
				i = end - 1;
			} else if (UnicodeUtils.isNumber(c)) {
				int end = i + 1;
				while (end < len) {
					char nc = word.charAt(end);

					if (UnicodeUtils.isNumber(nc)) {
						end++;
					} else if (nc == '-' || nc == ',' || nc == '/' || nc == '.') {
						if (end == len - 1) {
							break;
						} else {
							end++;
						}
					} else {
						break;
					}
				}

				if (sb.length() > 0) {
					sb.append(" ");
				}

				sb.append(word.substring(i, end));
				i = end - 1;
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

}
