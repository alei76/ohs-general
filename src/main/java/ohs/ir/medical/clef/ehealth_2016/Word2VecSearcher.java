package ohs.ir.medical.clef.ehealth_2016;

import java.util.List;

import ohs.ir.weight.TermWeighting;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.utils.Generics;

public class Word2VecSearcher {

	private Word2VecModel model;

	private double[] qVec;

	private SparseVector simVec;

	public Word2VecSearcher(Word2VecModel model) {
		this.model = model;

		qVec = new double[model.sizeOfVector()];

		simVec = new SparseVector(ArrayUtils.arrayRange(model.getVocab().size()));
	}

	public Counter<String> search(List<String> words, int top_k) {
		int num_words = 0;

		ArrayUtils.setAll(qVec, 0);

		Counter<String> wordCnts = Generics.newCounter();

		for (String word : words) {
			wordCnts.incrementCount(word, 1);
		}

		for (String word : wordCnts.keySet()) {
			int w = model.getVocab().indexOf(word);

			if (w < 0) {
				continue;
			}

			// double word_cnt = wordCnts.getCount(word);
			// double sent_freq = model.getVocab().getWordDocFreq(w);
			// double num_sents = model.getVocab().getNumDocs();
			//
			// double tf = TermWeighting.tf(word_cnt);
			// double idf = TermWeighting.idf(num_sents, sent_freq);
			// double tfidf = tf * idf;

			double[] vec = model.getVector(word);
			if (vec.length > 0) {
				ArrayMath.addAfterScale(vec, 1, qVec, 1, qVec);
				num_words++;
			}
		}

		Counter<String> ret = Generics.newCounter();

		if (num_words > 0) {

			ArrayMath.scale(qVec, 1f / num_words, qVec);

			computeSimilarity(qVec);

			simVec.sortByValue();

			for (int i = 0; i < top_k && i < model.getVocab().size(); i++) {
				int w = simVec.indexAtLoc(i);
				double sim = simVec.valueAtLoc(i);
				ret.setCount(model.getVocab().getWord(w), sim);
			}
			simVec.sortByIndex();
		}

		return ret;

	}

	public void computeSimilarity(double[] qVec) {
		simVec.setAll(0);

		for (int i = 0; i < model.getVocab().size(); i++) {
			// simVec.setAtLoc(i, ArrayMath.dotProduct(qVec, model.getVector(i)));
			simVec.setAtLoc(i, ArrayMath.cosine(qVec, model.getVector(i)));
		}
	}

	public Counter<String> search(String word, int top_k) {
		List<String> words = Generics.newArrayList();
		words.add(word);

		return search(words, top_k);
	}
}
