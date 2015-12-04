package com.medallia.word2vec.neuralnetwork;

import java.util.Map;

import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener;
import com.medallia.word2vec.huffman.HuffmanCoding.HuffmanNode;

import ohs.types.Vocab;

/**
 * Trainer for neural network using skip gram
 */
class SkipGramModelTrainer extends NeuralNetworkTrainer {

	/** {@link Worker} for {@link SkipGramModelTrainer} */
	private class SkipGramWorker extends Worker {
		private SkipGramWorker(int randomSeed, int iter, int[][] batch) {
			super(randomSeed, iter, batch);
		}

		@Override
		void trainSentence(int[] sent) {
			int len = sent.length;

			for (int loc = 0; loc < len; loc++) {
				int word = sent[loc];
				HuffmanNode huffmanNode = huffmanNodes.get(word);

				for (int c = 0; c < layer1_size; c++) {
					neu1[c] = 0;
					neu1e[c] = 0;
				}

				nextRandom = incrementRandom(nextRandom);

				int b = (int) (((nextRandom % window) + nextRandom) % window);

				for (int a = b; a < window * 2 + 1 - b; a++) {
					if (a == window)
						continue;
					int c = loc - window + a;

					if (c < 0 || c >= len)
						continue;
					for (int d = 0; d < layer1_size; d++)
						neu1e[d] = 0;

					int l1 = huffmanNodes.get(sent[c]).idx;

					if (config.useHierarchicalSoftmax) {
						for (int d = 0; d < huffmanNode.code.length; d++) {
							double f = 0;
							int l2 = huffmanNode.point[d];
							// Propagate hidden -> output
							for (int e = 0; e < layer1_size; e++)
								f += syn0[l1][e] * syn1[l2][e];

							if (f <= -MAX_EXP || f >= MAX_EXP)
								continue;
							else
								f = EXP_TABLE[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];
							// 'g' is the gradient multiplied by the learning rate
							double g = (1 - huffmanNode.code[d] - f) * alpha;

							// Propagate errors output -> hidden
							for (int e = 0; e < layer1_size; e++)
								neu1e[e] += g * syn1[l2][e];
							// Learn weights hidden -> output
							for (int e = 0; e < layer1_size; e++)
								syn1[l2][e] += g * syn0[l1][e];
						}
					}

					handleNegativeSampling(huffmanNode);

					// Learn weights input -> hidden
					for (int d = 0; d < layer1_size; d++) {
						syn0[l1][d] += neu1e[d];
					}
				}
			}
		}
	}

	SkipGramModelTrainer(NeuralNetworkConfig config, Vocab counts, Map<Integer, HuffmanNode> huffmanNodes,
			TrainingProgressListener listener) {
		super(config, counts, huffmanNodes, listener);
	}

	@Override
	Worker createWorker(int randomSeed, int iter, int[][] batch) {
		return new SkipGramWorker(randomSeed, iter, batch);
	}
}