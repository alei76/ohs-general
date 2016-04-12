package ohs.eden.name;

import org.tartarus.snowball.ext.PorterStemmer;

import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.types.Counter;
import ohs.utils.Generics;

public class NameFeatureExtractor {
	private GramGenerator korGG = new GramGenerator(3);

	private GramGenerator engGG = new GramGenerator(4);

	private PorterStemmer stemmer = new PorterStemmer();

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
			s = s.toLowerCase();

			for (Gram g : engGG.generateQGrams(s)) {
				ret.incrementCount(g.getString(), 1);
			}

			for (String word : s.split(" ")) {
				stemmer.setCurrent(word);
				stemmer.stem();
				ret.incrementCount(String.format("stm=%s", stemmer.getCurrent()), 1);
			}

			ret.incrementCount(getLengthFeat(1, s.length()), 1);
		}
		return ret;
	}

	private Counter<String> extractKorFeatures(String s) {
		Counter<String> ret = Generics.newCounter();
		if (s.length() > 0) {
			s = s.toLowerCase();

			for (int i = 0; i < s.length(); i++) {
				ret.incrementCount(s.charAt(i) + "", 1);
			}

			for (Gram g : korGG.generateQGrams(s.toLowerCase())) {
				ret.incrementCount(g.getString(), 1);
			}
			ret.incrementCount(getLengthFeat(0, s.length()), 1);
		}
		return ret;
	}

	private String getLengthFeat(int type, int len) {
		return String.format("len_%s=%d", type == 0 ? "kor" : "eng", len);
	}
}
