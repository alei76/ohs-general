package com.medallia.word2vec;

import java.util.List;

import ohs.math.ArrayMath;

/**
 * Represents a word2vec model where all the vectors are normalized to unit length.
 */
public class NormalizedWord2VecModel extends Word2VecModel {

	public static NormalizedWord2VecModel fromWord2VecModel(Word2VecModel model) {
		return new NormalizedWord2VecModel(model.vocab, model.layerSize, model.vectors);
	}

	private NormalizedWord2VecModel(List<String> vocab, int layerSize, double[][] vectors) {
		super(vocab, layerSize, vectors);
		normalize();
	}

	/** Normalizes the vectors in this model */
	private void normalize() {
		for (int i = 0; i < vectors.length; i++) {
			double[] v = vectors[i];
			ArrayMath.unitVector(v, v);
		}
	}
}
