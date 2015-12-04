package ohs.ir.task.medical.trec.cds_2014;

import java.io.File;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import ohs.io.IOUtils;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;

public class VocabularyData {

	public static void make(IndexReader indexReader) throws Exception {
		System.out.println("make vocabulary data.");
		Indexer<String> wordIndexer = new Indexer<String>();

		Counter<Integer> wordCounts = new Counter<Integer>();
		Counter<Integer> docFreqs = new Counter<Integer>();

		Fields fields = MultiFields.getFields(indexReader);
		Terms terms = fields.terms(IndexFieldName.CONTENT);

		TermsEnum iterator = terms.iterator();
		BytesRef byteRef = null;

		while ((byteRef = iterator.next()) != null) {
			String word = byteRef.utf8ToString();
			int docFreq = iterator.docFreq();
			double count = iterator.totalTermFreq();
			int w = wordIndexer.getIndex(word);
			wordCounts.setCount(w, count);
			docFreqs.setCount(w, docFreq);
		}

		SparseVector sv1 = VectorUtils.toSparseVector(wordCounts);
		SparseVector sv2 = VectorUtils.toSparseVector(docFreqs);

		sv1.write(CDSPath.WORD_COUNT_FILE);
		sv2.write(CDSPath.WORD_DOC_FREQ_FILE);

		IOUtils.write(CDSPath.WORD_INDEXER_FILE, wordIndexer);

		System.out.printf("vocabulary size:\t%d\n", wordIndexer.size());
	}

	public static VocabularyData read(File vocabularyDir) throws Exception {
		System.out.println("read vocabulary data.");
		Indexer<String> wordIndexer = IOUtils.readIndexer(CDSPath.WORD_INDEXER_FILE);
		SparseVector wordCounts = SparseVector.read(CDSPath.WORD_COUNT_FILE);
		SparseVector wordDocFreqs = SparseVector.read(CDSPath.WORD_DOC_FREQ_FILE);

		return new VocabularyData(wordIndexer, wordCounts, wordDocFreqs);
	}

	private Indexer<String> wordIndexer;

	private SparseVector collWordCountData;

	private SparseVector docFreqData;

	public VocabularyData(Indexer<String> wordIndexer, SparseVector collWordCounts, SparseVector docFreqs) {
		this.wordIndexer = wordIndexer;
		this.collWordCountData = collWordCounts;
		this.docFreqData = docFreqs;
	}

	public SparseVector getCollectionWordCountData() {
		return collWordCountData;
	}

	public SparseVector getDocumentFrequencies() {
		return docFreqData;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}
}
