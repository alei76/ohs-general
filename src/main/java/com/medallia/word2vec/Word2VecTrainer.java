package com.medallia.word2vec;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.primitives.Doubles;
import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener;
import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener.Stage;
import com.medallia.word2vec.huffman.HuffmanCoding;
import com.medallia.word2vec.huffman.HuffmanCoding.HuffmanNode;
import com.medallia.word2vec.neuralnetwork.NeuralNetworkConfig;
import com.medallia.word2vec.neuralnetwork.NeuralNetworkTrainer.NeuralNetworkModel;
import com.medallia.word2vec.util.AC;
import com.medallia.word2vec.util.ProfilingTimer;

import ohs.types.Vocab;

/** Responsible for training a word2vec model */
class Word2VecTrainer {
	/**
	 * @return {@link Multiset} containing unique tokens and their counts
	 */
	private static Multiset<String> count(Iterable<String> tokens) {
		Multiset<String> counts = HashMultiset.create();
		for (String token : tokens)
			counts.add(token);
		return counts;
	}

	private final Vocab vocab;

	private final NeuralNetworkConfig neuralNetworkConfig;

	Word2VecTrainer(Vocab vocab, NeuralNetworkConfig neuralNetworkConfig) {
		this.vocab = vocab;
		this.neuralNetworkConfig = neuralNetworkConfig;
	}

	/** Train a model using the given data */
	Word2VecModel train(Log log, TrainingProgressListener listener, int[][] sents) throws InterruptedException {
		try (ProfilingTimer timer = ProfilingTimer.createLoggingSubtasks(log, "Training word2vec")) {
			// final Multiset<String> counts;
			//
			// try (AC ac = timer.start("Acquiring word frequencies")) {
			// listener.update(Stage.ACQUIRE_VOCAB, 0.0);
			// counts = (vocab.isPresent()) ? vocab.get() : count(Iterables.concat(sents));
			// }

			// try (AC ac = timer.start("Filtering and sorting vocabulary")) {
			// listener.update(Stage.FILTER_SORT_VOCAB, 0.0);
			// vocab.getWordCounts().pruneKeysBelowThreshold(minFrequency);
			// }

			final Map<Integer, HuffmanNode> huffmanNodes;
			try (AC task = timer.start("Create Huffman encoding")) {
				huffmanNodes = new HuffmanCoding(vocab, listener).encode();
			}

			final NeuralNetworkModel model;
			try (AC task = timer.start("Training model %s", neuralNetworkConfig)) {
				model = neuralNetworkConfig.createTrainer(vocab, huffmanNodes, listener).train(sents);
			}

			return new Word2VecModel(vocab.getWordIndexer().getObjects(), model.layerSize(), model.vectors());
		}
	}
}
