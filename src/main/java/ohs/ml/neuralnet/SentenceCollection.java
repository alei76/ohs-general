package ohs.ml.neuralnet;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.types.Indexer;
import ohs.types.Vocab;
import ohs.utils.Generics;

public class SentenceCollection {

	private Indexer<String> wordIndexer;

	private int[] word_cnts;

	private int[] doc_freqs;

	private int num_docs;

	private int[][] sents;

	private int[] samples;

	public SentenceCollection() {

	}

	public void create(List<String> docs) {
		wordIndexer = Generics.newIndexer();

		int num_sents = 0;

		for (String doc : docs) {
			num_sents += doc.split("[\n]+").length;
		}

		sents = new int[num_sents][];
		int loc = 0;

		for (String doc : docs) {
			for (String sent : doc.split("[\n]+")) {
				sents[loc] = wordIndexer.getIndexes(sent.toLowerCase().split("[ ]+"));
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

	public int[] getRandomContext(int[] loc, int context_size) {
		// int sloc = ArrayMath.random(0, sents.length);
		// int wloc = ArrayMath.random(0, sents[sloc].length);

		int sloc = loc[0];
		int wloc = loc[1];

		int[] sent = sents[sloc];

		List<String> words = Arrays.asList(wordIndexer.getObjects(sent));

		List<Integer> context = Generics.newArrayList();

		int start = Math.max(0, wloc - context_size);
		for (int i = start; i < wloc; i++) {
			context.add(sent[i]);
		}

		if (wloc + 1 < sent.length) {
			int end = Math.min(sent.length, wloc + context_size + 1);
			for (int i = wloc + 1; i < end; i++) {
				context.add(sent[i]);
			}
		}

		int[] ret = ArrayUtils.copy(context);

		return ret;
	}

	public int[] getRandomWordLoc() {
		int sloc = ArrayMath.random(0, sents.length);
		int wloc = ArrayMath.random(0, sents[sloc].length);
		return new int[] { sloc, wloc };
	}

	public int[][] getSents() {
		return sents;
	}

	public Vocab getVocab() {
		return new Vocab(wordIndexer, word_cnts, doc_freqs, num_docs);
	}

	public int getWord(int[] loc) {
		return sents[loc[0]][loc[1]];
	}

	public void makeSampleTable(int table_size) {
		double[] probs = new double[wordIndexer.size()];
		ArrayUtils.copy(word_cnts, probs);
		ArrayMath.pow(probs, 0.75, probs);
		ArrayMath.cumulateAfterNormalize(probs, probs);
		samples = ArrayMath.sample(probs, table_size);
	}

	public int sampleWord() {
		return ArrayMath.sampleRandom(samples);
	}

}
