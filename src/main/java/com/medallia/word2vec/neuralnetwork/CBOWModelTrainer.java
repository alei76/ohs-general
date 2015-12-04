package com.medallia.word2vec.neuralnetwork;

import java.util.Map;

import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener;
import com.medallia.word2vec.huffman.HuffmanCoding.HuffmanNode;

import ohs.types.Vocab;

/**
 * Trainer for neural network using continuous bag of words
 */
class CBOWModelTrainer extends NeuralNetworkTrainer {

	/** {@link Worker} for {@link CBOWModelTrainer} */
	private class CBOWWorker extends Worker {
		private CBOWWorker(int randomSeed, int iter, int[][] batch) {
			super(randomSeed, iter, batch);
		}

		@Override
		void trainSentence(int[] sent) {
			int senLen = sent.length;

			// Map<Integer, String> map = new HashMap<>();
			//
			// for (String word : huffmanNodes.keySet()) {
			// HuffmanNode hn = huffmanNodes.get(word);
			// map.put(hn.idx, word);
			// }

			for (int sentPos = 0; sentPos < senLen; sentPos++) {
				int word = sent[sentPos];
				HuffmanNode huffmanNode = huffmanNodes.get(word);

				for (int c = 0; c < layer1_size; c++) {
					neu1[c] = 0;
					neu1e[c] = 0;
				}

				// ArrayUtils.setAll(neu1, 0);
				// ArrayUtils.setAll(neu1e, 0);

				nextRandom = incrementRandom(nextRandom);

				int b = (int) ((nextRandom % window) + window) % window;

				// in -> hidden
				int cw = 0;
				for (int a = b; a < window * 2 + 1 - b; a++) {
					if (a == window)
						continue;
					int c = sentPos - window + a;
					if (c < 0 || c >= senLen)
						continue;
					int idx = huffmanNodes.get(sent[c]).idx;

					for (int d = 0; d < layer1_size; d++) {
						neu1[d] += syn0[idx][d];
					}

					// ArrayMath.addAfterScale(syn0[idx], neu1, 1, 1, neu1);

					cw++;
				}

				if (cw == 0)
					continue;

				for (int c = 0; c < layer1_size; c++)
					neu1[c] /= cw;

				// ArrayMath.scale(neu1, 1f / cw);

				if (config.useHierarchicalSoftmax) {
					// System.out.printf("cword:%s\n", word);
					//
					// for (int d = 0; d < huffmanNode.code.length; d++) {
					// int l2 = huffmanNode.point[d];
					// String pWord = map.get(l2);
					// HuffmanNode phn = huffmanNodes.get(pWord);
					// System.out.printf("%d, pid:%d, pword:%s, pcount:%d\n", d, l2, pWord, phn.count);
					// }

					// System.out.println();

					for (int d = 0; d < huffmanNode.code.length; d++) {
						double f = 0;
						int l2 = huffmanNode.point[d];
						// Propagate hidden -> output
						for (int c = 0; c < layer1_size; c++)
							f += neu1[c] * syn1[l2][c];
						if (f <= -MAX_EXP || f >= MAX_EXP)
							continue;
						else
							f = EXP_TABLE[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];
						// 'g' is the gradient multiplied by the learning rate
						double g = (1 - huffmanNode.code[d] - f) * alpha;
						// Propagate errors output -> hidden
						for (int c = 0; c < layer1_size; c++)
							neu1e[c] += g * syn1[l2][c];

						// ArrayMath.addAfterScale(neu1e, syn1[l2], 1, g, neu1e);

						// Learn weights hidden -> output
						for (int c = 0; c < layer1_size; c++)
							syn1[l2][c] += g * neu1[c];

						// ArrayMath.addAfterScale(syn1[l2], neu1, 1, g, syn1[l2]);

					}
				}

				handleNegativeSampling(huffmanNode);

				// hidden -> in
				for (int a = b; a < window * 2 + 1 - b; a++) {
					if (a == window)
						continue;
					int c = sentPos - window + a;
					if (c < 0 || c >= senLen)
						continue;
					int idx = huffmanNodes.get(sent[c]).idx;
					for (int d = 0; d < layer1_size; d++)
						syn0[idx][d] += neu1e[d];

					// ArrayMath.addAfterScale(syn0[idx], neu1e, 1, 1, syn0[idx]);
				}
			}
		}
	}

	CBOWModelTrainer(NeuralNetworkConfig config, Vocab counts, Map<Integer, HuffmanNode> huffmanNodes, TrainingProgressListener listener) {
		super(config, counts, huffmanNodes, listener);
	}

	@Override
	Worker createWorker(int randomSeed, int iter, int[][] batch) {
		return new CBOWWorker(randomSeed, iter, batch);
	}
}