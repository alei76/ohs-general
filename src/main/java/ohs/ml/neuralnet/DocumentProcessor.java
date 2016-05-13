package ohs.ml.neuralnet;

import java.util.List;

import ohs.types.Indexer;
import ohs.utils.Generics;

public class DocumentProcessor {

	private Indexer<String> wordIndexer;

	private int[] word_cnts;

	private int[] doc_freqs;

	private int num_docs;

	private int num_sents;

	private int num_toks;

	public DocumentProcessor() {
		this(Generics.newIndexer());
	}

	public DocumentProcessor(Indexer<String> wordIndexer) {
		this.wordIndexer = wordIndexer;
	}

	public void process(List<String> docs) {

	}

}
