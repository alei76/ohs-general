package com.medallia.word2vec;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.medallia.word2vec.thrift.Word2VecModelThrift;
import com.medallia.word2vec.util.Common;

import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.math.ArrayUtils;

/**
 * Represents the Word2Vec model, containing vectors for each word
 * <p/>
 * Instances of this class are obtained via:
 * <ul>
 * <li>{@link #trainer()}
 * <li>{@link #fromThrift(Word2VecModelThrift)}
 * </ul>
 *
 * @see {@link #forSearch()}
 */
public class Word2VecModel {
	private final static long ONE_GB = 1024 * 1024 * 1024;

	/**
	 * @return {@link Word2VecModel} created from the binary representation output by the open source C version of word2vec using the given
	 *         byte order.
	 */

	public static Word2VecModel fromSerFile(String fileName) throws Exception {

		ObjectInputStream ois = IOUtils.openObjectInputStream(fileName);
		int[] dims = IOUtils.readIntegerArray(ois);
		int vocabSize = dims[0];
		int layerSize = dims[1];

		List<String> vocab = IOUtils.readStrings(ois);
		double[][] vectors = IOUtils.readDoubleMatrix(ois);
		ois.close();

		int[] dim = ArrayUtils.dimensions(vectors);

		System.out.printf("read [%d] words and [%d, %d] matrix at [%s]\n", vocab.size(), dim[0], dim[1], fileName);

		return new Word2VecModel(vocab, layerSize, vectors);

	}

	/**
	 * @return {@link Word2VecModel} read from a file in the text output format of the Word2Vec C open source project.
	 */
	public static Word2VecModel fromTextFile(File file) throws IOException {
		List<String> lines = Common.readToList(file);
		return fromTextFile(file.getAbsolutePath(), lines);
	}

	/**
	 * @return {@link Word2VecModel} from the lines of the file in the text output format of the Word2Vec C open source project.
	 */
	@VisibleForTesting
	public static Word2VecModel fromTextFile(String filename, List<String> lines) throws IOException {
		int vocabSize = Integer.parseInt(lines.get(0).split(" ")[0]);
		int layerSize = Integer.parseInt(lines.get(0).split(" ")[1]);

		List<String> vocab = new ArrayList<String>();
		double[][] vectors = new double[vocabSize][];

		Preconditions.checkArgument(vocabSize == lines.size() - 1,
				"For file '%s', vocab size is %s, but there are %s word vectors in the file", filename, vocabSize, lines.size() - 1);

		for (int n = 1; n < lines.size(); n++) {
			String[] values = lines.get(n).split(" ");
			vocab.add(values[0]);

			// Sanity check
			Preconditions.checkArgument(layerSize == values.length - 1,
					"For file '%s', on line %s, layer size is %s, but found %s values in the word vector", filename, n, layerSize,
					values.length - 1);
			double[] vector = new double[layerSize];
			for (int d = 1; d < values.length; d++) {
				vector[d - 1] = Double.parseDouble(values[d]);
			}
			vectors[n - 1] = vector;
		}

		return new Word2VecModel(vocab, layerSize, vectors);
	}

	/**
	 * @return {@link Word2VecTrainerBuilder} for training a model
	 */
	public static Word2VecTrainerBuilder trainer() {
		return new Word2VecTrainerBuilder();
	}

	final List<String> vocab;

	final int layerSize;

	final double[][] vectors;

	Word2VecModel(List<String> vocab, int layerSize, double[][] vectors) {
		this.vocab = vocab;
		this.layerSize = layerSize;
		this.vectors = vectors;
	}

	/**
	 * @return {@link Searcher} for searching
	 */
	public Searcher forSearch() {
		return new SearcherImpl(this);
	}

	public int getLayerSize() {
		return layerSize;
	}

	public double[][] getVectors() {
		return vectors;
	}

	/**
	 * @return Vocab
	 */
	public List<String> getVocab() {
		return vocab;
	}

	/**
	 * Saves the model as a bin file that's compatible with the C version of Word2Vec
	 */

	public void toSerFile(String outputFileName) throws Exception {
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(outputFileName);
		int[] dims = new int[] { vocab.size(), layerSize };
		IOUtils.write(oos, dims);
		IOUtils.write(oos, vocab);
		IOUtils.write(oos, vectors);
		oos.close();

	}

	public void toTextFile(String outputFile) throws IOException {
		final Charset cs = Charset.forName("UTF-8");

		TextFileWriter writer = new TextFileWriter(outputFile);
		writer.write(String.format("%d %d\n", vocab.size(), layerSize));

		for (int i = 0; i < vocab.size(); ++i) {
			writer.write(String.format("%s", vocab.get(i)));
			double[] vector = vectors[i];
			for (int j = 0; j < layerSize; ++j) {
				writer.write(" " + Double.toString(vector[j]));
			}
			writer.write("\n");
		}
		writer.close();
	}

}
