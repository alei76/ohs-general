package ohs.ir.medical.clef.ehealth_2016;

import java.util.List;
import java.util.Set;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.utils.Generics;

public class Word2VecSearcher {

	private Word2VecModel model;

	private double[] q;

	private SparseVector sims;

	private Set<String> stopwords;

	public Word2VecSearcher(Word2VecModel model, Set<String> stopwords) {
		this.model = model;
		this.stopwords = stopwords;

		q = new double[model.sizeOfVector()];

		sims = new SparseVector(ArrayUtils.arrayRange(model.getVocab().size()));
	}

	public void computeSimilarity(double[] q) {
		sims.setAll(0);

		for (int i = 0; i < model.getVocab().size(); i++) {
			// sims.setAtLoc(i, ArrayMath.dotProduct(q, model.getVector(i)));
			sims.setAtLoc(i, ArrayMath.cosine(q, model.getVector(i)));
		}
	}

	public Word2VecModel getModel() {
		return model;
	}

	public Counter<String> search(Counter<String> wordCnts, int top_k) {
		int num_words = 0;
		ArrayUtils.setAll(q, 0);

		for (String word : wordCnts.keySet()) {
			if (stopwords.contains(word)) {
				continue;
			}

			int w = model.getVocab().indexOf(word);

			if (w < 0) {
				continue;
			}

			double[] vec = model.getVector(word);
			if (vec.length > 0) {
				ArrayMath.add(vec, q, q);
				num_words++;
			}
		}

		Counter<String> ret = Generics.newCounter();

		if (num_words > 0) {
			ArrayMath.scale(q, 1f / num_words, q);

			computeSimilarity(q);

			sims.sortByValue();

			for (int i = 0; i < top_k && i < model.getVocab().size(); i++) {
				int w = sims.indexAtLoc(i);
				double sim = sims.valueAtLoc(i);
				ret.setCount(model.getVocab().getWord(w), sim);
			}
			sims.sortByIndex();
		}

		return ret;
	}

	public Counter<String> search(List<String> words, int top_k) {
		Counter<String> wordCnts = Generics.newCounter();
		for (String word : words) {
			word = word.toLowerCase();
			wordCnts.incrementCount(word, 1);
		}
		return search(wordCnts, top_k);

	}

	public Counter<String> search(String word, int top_k) {
		List<String> words = Generics.newArrayList();
		words.add(word);

		return search(words, top_k);
	}
}
