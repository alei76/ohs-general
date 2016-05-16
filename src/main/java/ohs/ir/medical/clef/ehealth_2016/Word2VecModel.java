package ohs.ir.medical.clef.ehealth_2016;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.List;

import com.medallia.word2vec.Searcher.UnknownWordException;

import ohs.io.FileUtils;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.Vocab;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class Word2VecModel {

	public static void interact(Word2VecModel model) throws IOException, UnknownWordException {
		Word2VecSearcher searcher = new Word2VecSearcher(model);

		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			while (true) {
				System.out.print("Enter word or sentence (EXIT to break): ");
				String line = br.readLine();
				if (line.toLowerCase().equals("exit")) {
					break;
				}

				List<String> words = StrUtils.split(StrUtils.normalizePunctuations(line));

				Counter<String> res = searcher.search(words, 20);

				System.out.println(res.toStringSortedByValues(true, true, 20, "\n"));
				System.out.println();
			}
		}
	}

	/**
	 * Runs the example
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		Word2VecModel model = new Word2VecModel();
		model.readObject("../../data/medical_ir/wiki/wiki_medical_word2vec_model.ser.gz");

		interact(model);

		System.out.println("process ends.");
	}

	private Vocab vocab;

	private double[][] vecs;

	public double[] getVector(int w) {
		if (w < 0) {
			return new double[0];
		} else {
			return vecs[w];
		}
	}

	public double[] getVector(String word) {
		return getVector(vocab.indexOf(word));
	}

	public Vocab getVocab() {
		return vocab;
	}

	public void readObject(ObjectInputStream ois) throws Exception {
		vocab = new Vocab();
		vocab.readObject(ois);

		vecs = FileUtils.readDoubleMatrix(ois);

		for (int i = 0; i < vecs.length; i++) {
			ArrayMath.unitVector(vecs[i], vecs[i]);
		}
	}

	public void readObject(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		readObject(ois);
		ois.close();
	}

	public int sizeOfVector() {
		return vecs[0].length;
	}

}
