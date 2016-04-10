package ohs.string.sim;

public interface StringScorer<E> {

	public double getDistance(Sequence<E> s, Sequence<E> t);

	default public double getDistance(Sequence<E>[] st) {
		return getDistance(st[0], st[1]);
	}

	public double getSimilarity(Sequence<E> s, Sequence<E> t);

	default public double getSimilarity(Sequence<E>[] st) {
		return getSimilarity(st[0], st[1]);
	}

}
