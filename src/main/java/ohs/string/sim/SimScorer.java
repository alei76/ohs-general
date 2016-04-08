package ohs.string.sim;

import java.util.List;

public interface SimScorer {

	default public double getDistance(List<String> s, List<String> t) {
		return getDistance(new StrSequence(s), new StrSequence(t));
	}

	public double getDistance(Sequence s, Sequence t);

	default public double getDistance(String s, String t) {
		return getDistance(new CharSequence(s), new CharSequence(t));
	}

	default public double getSimilarity(List<String> s, List<String> t) {
		return getSimilarity(new StrSequence(s), new StrSequence(t));
	}

	public double getSimilarity(Sequence s, Sequence t);

	default public double getSimilarity(String s, String t) {
		return getSimilarity(new CharSequence(s), new CharSequence(t));
	}

}
