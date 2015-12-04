package ohs.ir.task.medical;

import com.medallia.word2vec.Word2VecModel;

import ohs.math.ArrayMath;
import ohs.types.Counter;
import ohs.types.Indexer;

public class Word2VecSearcher {
	private Indexer<String> wordIndexer;

	private int layerSize;

	private double[][] vectors;

	public Word2VecSearcher(Word2VecModel model) {
		wordIndexer = new Indexer<>();
		for (int i = 0; i < model.getVocab().size(); i++) {
			wordIndexer.add(model.getVocab().get(i));
		}
		
		

		System.out.println(model.getVocab().size());
		System.out.println(wordIndexer.size());

		this.layerSize = model.getLayerSize();
		this.vectors = model.getVectors();

		for (int i = 0; i < vectors.length; i++) {
			ArrayMath.unitVector(vectors[i], vectors[i]);
		}
	}

	public double[][] getVectors() {
		return vectors;
	}

	public double[] getVector(String word) {
		int w = wordIndexer.indexOf(word);
		double[] ret = null;
		if (w > -1) {
			ret = vectors[w];
		}
		return ret;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public int getLayerSize() {
		return layerSize;
	}

	public Counter<String> search(String word) {
		Counter<String> ret = new Counter<String>();

		int w = wordIndexer.indexOf(word);

		if (w > -1) {
			for (int i = 0; i < vectors.length; i++) {
				if (i == w) {
					continue;
				}
				String word2 = wordIndexer.getObject(i);
				double dot_product = ArrayMath.dotProduct(vectors[w], vectors[i]);
				ret.incrementCount(word2, dot_product);
			}
		}

		return ret;
	}
}
