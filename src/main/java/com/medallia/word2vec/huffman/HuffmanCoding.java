package com.medallia.word2vec.huffman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multiset;
import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener;
import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener.Stage;

import ohs.types.Vocab;

/**
 * Word2Vec library relies on a Huffman encoding scheme
 * <p>
 * Note that the generated codes and the index of the parents are both used in the hierarchical softmax portions of the neural network
 * training phase
 * <p>
 */
public class HuffmanCoding {
	/** Node */
	public static class HuffmanNode {
		/** Array of 0's and 1's */
		public final byte[] code;
		/** Array of parent node index offsets */
		public final int[] point;
		/** Index of the Huffman node */
		public final int idx;
		/** Frequency of the token */
		public final int count;

		private HuffmanNode(byte[] code, int[] point, int idx, int count) {
			this.code = code;
			this.point = point;
			this.idx = idx;
			this.count = count;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(String.format("idx:\t%d\n", idx));
			sb.append(String.format("count:\t%d\n", count));
			sb.append(String.format("code:"));
			for (int i = 0; i < code.length; i++) {
				sb.append(String.format("\t%d", code[i]));
			}
			sb.append("\n");
			sb.append(String.format("point:"));
			for (int i = 0; i < point.length; i++) {
				sb.append(String.format("\t%d", point[i]));
			}

			return sb.toString();
		}
	}

	private final Vocab vocab;
	private final TrainingProgressListener listener;

	/**
	 * @param vocab
	 *            {@link Multiset} of tokens, sorted by frequency descending
	 * @param listener
	 *            Progress listener
	 */
	public HuffmanCoding(Vocab vocab, TrainingProgressListener listener) {
		this.vocab = vocab;
		this.listener = listener;
	}

	/**
	 * Populate the count, binary, and parentNode arrays with the Huffman tree This uses the linear time method assuming that the count
	 * array is sorted
	 */
	private void createTree(int numTokens, long[] count, byte[] binary, int[] parentNode) throws InterruptedException {
		int min1i;
		int min2i;
		int pos1 = numTokens - 1;
		int pos2 = numTokens;

		// Map<Integer, String> map = new HashMap<>();
		// {
		// int id = 0;
		// for (Entry<String> e : vocab.entrySet()) {
		// map.put(id++, e.getElement());
		// }
		// }

		// Construct the Huffman tree by adding one node at a time
		for (int a = 0; a < numTokens - 1; a++) {
			// First, find two smallest nodes 'min1, min2'
			if (pos1 >= 0) {
				if (count[pos1] < count[pos2]) {
					min1i = pos1;
					pos1--;
				} else {
					min1i = pos2;
					pos2++;
				}
			} else {
				min1i = pos2;
				pos2++;
			}

			if (pos1 >= 0) {
				if (count[pos1] < count[pos2]) {
					min2i = pos1;
					pos1--;
				} else {
					min2i = pos2;
					pos2++;
				}
			} else {
				min2i = pos2;
				pos2++;
			}

			int newNodeIdx = numTokens + a;
			count[newNodeIdx] = count[min1i] + count[min2i];
			parentNode[min1i] = newNodeIdx;
			parentNode[min2i] = newNodeIdx;
			binary[min2i] = 1;

			// System.out.printf("Min1:\tpos:%d, id:%d, word:%s, parent:%d, code:%d\n", pos1, min1i, map.get(min1i), parentNode[min1i],
			// (int) binary[min1i]);
			// System.out.printf("Min2:\tpos:%d, id:%d, word:%s, parent:%d, cdoe:%d\n", pos2, min2i, map.get(min2i), parentNode[min2i],
			// (int) binary[min2i]);
			// System.out.println();

			if (a % 1_000 == 0 || a == numTokens - 2) {
				if (Thread.currentThread().isInterrupted())
					throw new InterruptedException("Interrupted while encoding huffman tree");
				listener.update(Stage.CREATE_HUFFMAN_ENCODING, (0.5 * a) / numTokens);
			}
		}
	}

	/**
	 * @return {@link Map} from each given token to a {@link HuffmanNode}
	 */
	public Map<Integer, HuffmanNode> encode() throws InterruptedException {
		final int numTokens = vocab.getWordIndexer().size();

		int[] parentNode = new int[numTokens * 2 + 1];
		byte[] binary = new byte[numTokens * 2 + 1];
		long[] count = new long[numTokens * 2 + 1];
		int i = 0;

		for (int w = 0; w < numTokens; w++) {
			count[i] = (long) vocab.getWordCount(w);
			i++;
		}
		Preconditions.checkState(i == numTokens, "Expected %s to match %s", i, numTokens);
		for (i = numTokens; i < count.length; i++)
			count[i] = (long) 1e15;

		createTree(numTokens, count, binary, parentNode);

		return encode(binary, parentNode);
	}

	/**
	 * @return Ordered map from each token to its {@link HuffmanNode}, ordered by frequency descending
	 */
	private Map<Integer, HuffmanNode> encode(byte[] binary, int[] parentNode) throws InterruptedException {
		int numTokens = vocab.getWordIndexer().size();

		// Now assign binary code to each unique token
		Map<Integer, HuffmanNode> result = new HashMap<Integer, HuffmanNode>();

		// Map<Integer, String> map = new HashMap<>();
		// {
		// int id = 0;
		// for (Entry<String> e : vocab.entrySet()) {
		// map.put(id++, e.getElement());
		// }
		// }

		int nodeIdx = 0;
		for (int w = 0; w < vocab.getWordIndexer().size(); w++) {
			int curNodeIdx = nodeIdx;
			ArrayList<Byte> code = new ArrayList<>();
			ArrayList<Integer> points = new ArrayList<>();
			while (true) {
				code.add(binary[curNodeIdx]);
				points.add(curNodeIdx);
				curNodeIdx = parentNode[curNodeIdx];
				if (curNodeIdx == numTokens * 2 - 2)
					break;
			}
			int codeLen = code.size();
			final int count = (int) vocab.getWordCount(w);
			final byte[] rawCode = new byte[codeLen];
			final int[] rawPoints = new int[codeLen + 1];

			// System.out.printf("## id:%d, word:%s\n", nodeIdx, map.get(nodeIdx));
			// for (int i = 0; i < codeLen; i++) {
			// System.out.printf("depth:%d, pcode:%d, pid:%d, pword:%s\n", i, (int)code.get(i), points.get(i), map.get(points.get(i)));
			// }
			// System.out.println();

			rawPoints[0] = numTokens - 2;
			for (int i = 0; i < codeLen; i++) {
				rawCode[codeLen - i - 1] = code.get(i);
				rawPoints[codeLen - i] = points.get(i) - numTokens;
			}

			// for (int i = 0; i < codeLen; i++) {
			// System.out.printf("depth:%d, pcode:%d, pid:%d, pword:%s\n", i, rawCode[i], rawPoints[i], map.get(rawPoints[i]));
			// }
			// System.out.println();

			// String token = e.getElement();
			result.put(w, new HuffmanNode(rawCode, rawPoints, nodeIdx, count));

			if (nodeIdx % 1_000 == 0) {
				if (Thread.currentThread().isInterrupted())
					throw new InterruptedException("Interrupted while encoding huffman tree");
				listener.update(Stage.CREATE_HUFFMAN_ENCODING, 0.5 + (0.5 * nodeIdx) / numTokens);
			}

			nodeIdx++;
		}

		return result;
	}
}
