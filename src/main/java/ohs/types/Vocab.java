package ohs.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ohs.io.FileUtils;
import ohs.math.ArrayMath;

public class Vocab {

	private Indexer<String> wordIndexer;

	private int[] word_cnts;

	private int[] word_doc_freqs;

	private int num_docs;

	private int word_cnt_sum;

	public Vocab() {

	}

	public Vocab(Indexer<String> wordIndexer, int[] word_cnts, int[] word_doc_freqs, int num_docs) {
		this.wordIndexer = wordIndexer;
		this.word_cnts = word_cnts;
		this.word_doc_freqs = word_doc_freqs;
		this.num_docs = num_docs;
		word_cnt_sum = ArrayMath.sum(word_cnts);
	}

	public int getNumDocs() {
		return num_docs;
	}

	public String getWord(int w) {
		String ret = null;
		if (w >= 0 && w < wordIndexer.size()) {
			ret = wordIndexer.getObject(w);
		}
		return ret;
	}

	public int getWordCount(int w) {
		return word_cnts[w];
	}

	public int getWordCount(String word) {
		int w = wordIndexer.indexOf(word);
		int ret = 0;

		if (w > -1) {
			ret = word_cnts[w];
		}
		return ret;
	}

	public int[] getWordCounts() {
		return word_cnts;
	}

	public int getWordCountSum() {
		return word_cnt_sum;
	}

	public double getWordDocFreq(int w) {
		return word_doc_freqs[w];
	}

	public int getWordDocFreq(String word) {
		int w = wordIndexer.indexOf(word);
		int ret = 0;
		if (w > -1) {
			ret = word_doc_freqs[w];
		}
		return ret;
	}

	public int[] getWordDocFreqs() {
		return word_doc_freqs;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public double getWordProb(int w) {
		return 1f * word_cnts[w] / word_cnt_sum;
	}

	public double getWordProb(String word) {
		double ret = 0;
		int w = wordIndexer.indexOf(word);
		if (w > -1) {
			ret = getWordProb(w);
		}
		return ret;
	}

	public int indexOf(String word) {
		return wordIndexer.indexOf(word);
	}

	public void read(ObjectInputStream ois) throws Exception {
		wordIndexer = FileUtils.readStrIndexer(ois);
		num_docs = ois.readInt();
		word_cnts = FileUtils.readIntArray(ois);
		word_doc_freqs = FileUtils.readIntArray(ois);
		word_cnt_sum = ArrayMath.sum(word_cnts);
	}

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		read(ois);
		ois.close();
	}

	public int size() {
		return wordIndexer.size();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		FileUtils.writeStrIndexer(oos, wordIndexer);
		oos.writeInt(num_docs);
		FileUtils.writeIntArray(oos, word_cnts);
		FileUtils.writeIntArray(oos, word_doc_freqs);
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream ois = FileUtils.openObjectOutputStream(fileName);
		write(ois);
		ois.close();

	}

}
