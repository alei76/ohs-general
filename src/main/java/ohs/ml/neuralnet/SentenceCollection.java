package ohs.ml.neuralnet;

import java.util.List;
import java.util.Set;

import ohs.math.ArrayMath;
import ohs.types.Indexer;
import ohs.types.Vocab;
import ohs.utils.Generics;

public class SentenceCollection {

	private Indexer<String> wordIndexer;

	private int[] word_cnts;

	private int[] doc_freqs;

	private int num_docs;

	private int[][] sents;

	public SentenceCollection() {

	}

	public void build(List<String> docs) {
		Indexer<String> wordIndexer = Generics.newIndexer();

		int num_sents = 0;

		for (String doc : docs) {
			num_sents += doc.split("[\n]+").length;
		}

		sents = new int[ ][];
		int loc = 0;

		for (String doc : docs) {
			for (String sent : doc.split("[\n]+")) {
				sents[loc] = wordIndexer.getIndexes(sent.split("[ ]+"));
				loc++;
			}
		}

		word_cnts = new int[wordIndexer.size()];
		doc_freqs = new int[wordIndexer.size()];

		Set<Integer> wordSet = Generics.newHashSet();

		for (int[] sent : sents) {
			wordSet.clear();
			for (int w : sent) {
				word_cnts[w]++;
				wordSet.add(w);
			}

			for (int w : wordSet) {
				doc_freqs[w]++;
			}
		}
		
	}

	public int[][] getSents() {
		return sents;
	}

	public Vocab getVocab() {
		return new Vocab(wordIndexer, word_cnts, doc_freqs, num_docs);
	}

	public void getRandomContext(int context_size) {

		int sloc = ArrayMath.random(0, sents.length);
		int wloc = ArrayMath.random(0, sents[sloc].length);

		{
			int start = Math.max(0, wloc - context_size);
		}

	}

}
