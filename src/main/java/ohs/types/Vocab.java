package ohs.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ohs.io.FileUtils;
import ohs.utils.StopWatch;

public class Vocab {

	private Indexer<String> wordIndexer;

	private int[] word_cnts;

	private int[] word_doc_freqs;

	public Vocab() {

	}

	public Vocab(Indexer<String> wordIndexer, int[] word_cnts, int[] word_doc_freqs) {
		this.wordIndexer = wordIndexer;
		this.word_cnts = word_cnts;
		this.word_doc_freqs = word_doc_freqs;
	}

	public double getWordCount(int w) {
		return word_cnts[w];
	}

	public int[] getWordCounts() {
		return word_cnts;
	}

	public double getWordDocFreq(int w) {
		return word_doc_freqs[w];
	}

	public int[] getWordDocFreqs() {
		return word_doc_freqs;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public void read(ObjectInputStream ois) throws Exception {
		wordIndexer = FileUtils.readStrIndexer(ois);
		word_cnts = FileUtils.readIntArray(ois);
		word_doc_freqs = FileUtils.readIntArray(ois);
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
		FileUtils.writeIntArray(oos, word_cnts);
		FileUtils.writeIntArray(oos, word_doc_freqs);
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream ois = FileUtils.openObjectOutputStream(fileName);
		write(ois);
		ois.close();

	}

}
