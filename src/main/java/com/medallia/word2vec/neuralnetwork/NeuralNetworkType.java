package com.medallia.word2vec.neuralnetwork;

import java.util.Map;

import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener;
import com.medallia.word2vec.huffman.HuffmanCoding.HuffmanNode;

import ohs.types.Vocab;

/**
 * Supported types for the neural network
 */
public enum NeuralNetworkType {
	/** Faster, slightly better accuracy for frequent words */
	CBOW {
		@Override
		NeuralNetworkTrainer createTrainer(NeuralNetworkConfig config, Vocab counts, Map<Integer, HuffmanNode> huffmanNodes,
				TrainingProgressListener listener) {
			return new CBOWModelTrainer(config, counts, huffmanNodes, listener);
		}

		@Override
		public double getDefaultInitialLearningRate() {
			return 0.05;
		}
	},
	/** Slower, better for infrequent words */
	SKIP_GRAM {
		@Override
		NeuralNetworkTrainer createTrainer(NeuralNetworkConfig config, Vocab counts, Map<Integer, HuffmanNode> huffmanNodes,
				TrainingProgressListener listener) {
			return new SkipGramModelTrainer(config, counts, huffmanNodes, listener);
		}

		@Override
		public double getDefaultInitialLearningRate() {
			return 0.025;
		}
	},;

	/** @return New {@link NeuralNetworkTrainer} */
	abstract NeuralNetworkTrainer createTrainer(NeuralNetworkConfig config, Vocab counts, Map<Integer, HuffmanNode> huffmanNodes,
			TrainingProgressListener listener);

	/** @return Default initial learning rate */
	public abstract double getDefaultInitialLearningRate();
}