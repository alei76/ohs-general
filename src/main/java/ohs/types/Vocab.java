package ohs.types;

public class Vocab {

	private Indexer<String> wordIndexer;

	private double[] wordCnts;

	public Vocab(Indexer<String> wordIndexer, double[] wordCnts) {
		this.wordIndexer = wordIndexer;
		this.wordCnts = wordCnts;
	}

	public double getWordCount(int w) {
		return wordCnts[w];
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public int size() {
		return wordIndexer.size();
	}

}
