package ohs.eden.name;

import java.util.Set;

import org.tartarus.snowball.ext.PorterStemmer;

import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.types.Counter;
import ohs.utils.Generics;

public class NameFeatureExtractor {
	private GramGenerator korGG = new GramGenerator(3);

	private GramGenerator engGG = new GramGenerator(4);

	private PorterStemmer stemmer = new PorterStemmer();

	public static Set<Character> clues;

	static {
		clues = Generics.newHashSet();
		clues.add('대');
		clues.add('학');
		clues.add('교');
		clues.add('연');
		clues.add('구');
		clues.add('원');
		clues.add('소');
		clues.add('주');
		clues.add('식');
		clues.add('유');
		clues.add('한');
		clues.add('회');
		clues.add('사');
		clues.add('협');
		clues.add('회');

	}

	public NameFeatureExtractor() {

	}

	public Counter<String> extract(String kor, String eng) {
		Counter<String> ret = Generics.newCounter();
		ret.incrementAll(extractKorFeatures(kor));
		ret.incrementAll(extractEngFeatures(eng));
		return ret;
	}

	private Counter<String> extractEngFeatures(String s) {
		Counter<String> ret = Generics.newCounter();
		if (s.length() > 0) {

			for (Gram g : engGG.generateQGrams(s.toLowerCase())) {
				ret.incrementCount(String.format("eg=%s", g.getString()), 1);
			}

			String[] words = s.split(" ");
			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < words.length; i++) {
				String word = words[i];
				stemmer.setCurrent(word);
				stemmer.stem();
				ret.incrementCount(String.format("es=%s", stemmer.getCurrent().toLowerCase()), 1);
				ret.incrementCount(String.format("ew=%s", word.toLowerCase()), 1);

				// String shape = getWordShape(word);
				// ret.incrementCount(String.format("e_wshp=%s", shape), 1);
				// sb.append(shape);

				if (i != words.length - 1) {
					sb.append(" ");
				}
			}

			// ret.incrementCount(String.format("e_sshp=%s", sb.toString()), 1);
			ret.incrementCount(getLengthFeat(1, s.length()), 1);
		}
		return ret;
	}

	private String getWordShape(String word) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			if (Character.isAlphabetic(c)) {
				if (Character.isUpperCase(c)) {
					sb.append("X");
				} else {
					sb.append("x");
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private Counter<String> extractKorFeatures(String s) {
		Counter<String> ret = Generics.newCounter();
		if (s.length() > 0) {
			s = s.replaceAll("[\\s]+", "").toLowerCase();

			for (int i = 0; i < s.length(); i++) {
				ret.incrementCount(String.format("kc=%s", s.charAt(i) + ""), 1);
			}

			for (Gram g : korGG.generateQGrams(s.toLowerCase())) {
				ret.incrementCount(String.format("kg=%s", g.getString()), 1);
			}

			ret.incrementCount(String.format("kw=%s", s), 1);

			// StringBuffer sb = new StringBuffer();
			//
			// for (int i = 0; i < s.length(); i++) {
			// char c = s.charAt(i);
			// if (clues.contains(c)) {
			// sb.append("X");
			// } else {
			// sb.append(c);
			// }
			// }

			// ret.incrementCount(String.format("k_sshp=%s", sb.toString()), 1);
			ret.incrementCount(getLengthFeat(0, s.length()), 1);

		}
		return ret;
	}

	private String getLengthFeat(int type, int len) {
		return String.format("%sl=%d", type == 0 ? "k" : "e", len);
	}
}
