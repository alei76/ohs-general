package ohs.ir.medical.general;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import ohs.io.FileUtils;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.utils.Generics;

public class WordCountBox {

	public static Counter<String> getDocFreqs(IndexReader ir, String field) throws Exception {
		Counter<String> ret = new Counter<String>();

		Fields fs = MultiFields.getFields(ir);
		if (fs != null) {
			Terms terms = fs.terms(field);
			TermsEnum termsEnum = terms.iterator();
			BytesRef text;
			while ((text = termsEnum.next()) != null) {
				Term term = new Term(field, text.utf8ToString());
				double df = ir.docFreq(term);
				ret.incrementCount(term.text(), df);
			}
		}

		// for (String word : c) {
		// Term term = new Term(field, word);
		// double df = ir.docFreq(term);
		// ret.setCount(word, df);
		// }
		return ret;
	}

	public static Counter<String> getDocFreqs(IndexReader ir, String field, Collection<String> c) throws Exception {
		Counter<String> ret = new Counter<String>();
		for (String word : c) {
			Term term = new Term(field, word);
			double df = ir.docFreq(term);
			ret.setCount(word, df);
		}
		return ret;
	}

	public static SparseVector getDocFreqs(IndexReader ir, String field, Indexer<String> wordIndexer) throws Exception {
		SparseVector ret = new SparseVector(wordIndexer.size());
		for (int i = 0; i < wordIndexer.size(); i++) {
			String word = wordIndexer.getObject(i);
			Term term = new Term(field, word);
			double df = ir.docFreq(term);
			ret.incrementAtLoc(i, i, df);
		}
		return ret;
	}

	public static WordCountBox getWordCountBox(IndexReader ir, SparseVector docScores, Indexer<String> wordIndexer) throws Exception {
		return getWordCountBox(ir, docScores, wordIndexer, CommonFieldNames.CONTENT);
	}

	public static WordCountBox getWordCountBox(IndexReader ir, SparseVector docScores, Indexer<String> wordIndexer, String field)
			throws Exception {
		Set<Integer> fbWords = Generics.newHashSet();
		CounterMap<Integer, Integer> cm = Generics.newCounterMap(docScores.size());
		ListMap<Integer, Integer> docWords = Generics.newListMap(docScores.size());

		for (int j = 0; j < docScores.size(); j++) {
			int docid = docScores.indexAtLoc(j);
			double score = docScores.valueAtLoc(j);
			Document doc = ir.document(docid);

			Terms t = ir.getTermVector(docid, field);
			int word_size = (int) t.size();

			if (t == null) {
				continue;
			}

			TermsEnum ts = t.iterator();

			BytesRef br = null;
			PostingsEnum pe = null;
			Counter<Integer> wcs = Generics.newCounter(word_size);
			Map<Integer, Integer> locWords = Generics.newHashMap();

			while ((br = ts.next()) != null) {
				pe = ts.postings(pe, PostingsEnum.ALL);

				if (pe.nextDoc() != 0) {
					throw new AssertionError();
				}

				String word = br.utf8ToString();
				// if (word.startsWith("<N") && word.endsWith(">")) {
				// continue;
				// }
				// if (word.contains("<N")) {
				// continue;
				// }

				int w = wordIndexer.getIndex(word);
				int freq = pe.freq();
				wcs.incrementCount(w, freq);

				for (int k = 0; k < freq; k++) {
					final int position = pe.nextPosition();
					locWords.put(position, w);
				}
			}
			cm.setCounter(docid, wcs);

			List<Integer> locs = Generics.newArrayList(locWords.keySet());
			Collections.sort(locs);

			List<Integer> words = Generics.newArrayList();

			for (int loc : locs) {
				words.add(locWords.get(loc));
			}

			docWords.put(docid, words);

			for (int w : wcs.keySet()) {
				fbWords.add(w);
			}
		}

		SparseMatrix dwcs = VectorUtils.toSpasreMatrix(cm);

		Counter<Integer> c1 = Generics.newCounter();
		Counter<Integer> c2 = Generics.newCounter();

		for (int w : fbWords) {
			String word = wordIndexer.getObject(w);
			Term term = new Term(field, word);
			double cnt = ir.totalTermFreq(term);
			double df = ir.docFreq(term);
			c1.setCount(w, cnt);
			c2.setCount(w, df);
		}

		SparseVector collWordCounts = VectorUtils.toSparseVector(c1);
		SparseVector docFreqs = VectorUtils.toSparseVector(c2);

		double cnt_sum_in_coll = ir.getSumTotalTermFreq(CommonFieldNames.CONTENT);

		WordCountBox ret = new WordCountBox(dwcs, collWordCounts, cnt_sum_in_coll, docFreqs, ir.maxDoc(), docWords);
		ret.setWordIndexer(wordIndexer);
		return ret;
	}

	public static Counter<String> getWordCounts(IndexReader ir, int docid, String field) throws Exception {
		Terms t = ir.getTermVector(docid, field);

		if (t == null) {
			return new Counter<String>();
		}

		TermsEnum te = t.iterator();
		BytesRef br = null;
		PostingsEnum pe = null;
		Counter<String> ret = Generics.newCounter();

		while ((br = te.next()) != null) {
			pe = te.postings(pe, PostingsEnum.ALL);

			if (pe.nextDoc() != 0) {
				throw new AssertionError();
			}

			String word = br.utf8ToString();
			// if (word.contains("<N")) {
			// continue;
			// }

			int freq = pe.freq();
			ret.incrementCount(word, freq);
			// for (int k = 0; k < freq; k++) {
			// final int position = postingsEnum.nextPosition();
			// locWords.put(position, w);
			// }
		}
		return ret;
	}

	public static Counter<String> getWordCounts(IndexReader ir, String field, Counter<String> c) throws Exception {
		Counter<String> ret = Generics.newCounter();
		for (String word : c.keySet()) {
			Term term = new Term(field, word);
			double cnt = ir.totalTermFreq(term);
			ret.setCount(word, cnt);
		}
		return ret;
	}

	private Indexer<String> wordIndexer;

	private SparseMatrix docWordCnts;

	private SparseVector collWordCnts;

	private SparseVector collDocFreqs;

	private ListMap<Integer, Integer> docWords;

	private double cnt_sum_in_coll;

	private double num_docs_in_coll;

	public void write(ObjectOutputStream oos) throws Exception {
		FileUtils.writeStrIndexer(oos, wordIndexer);
		FileUtils.writeIntListMap(oos, docWords);

		collWordCnts.writeObject(oos);
		collDocFreqs.writeObject(oos);

		oos.writeDouble(cnt_sum_in_coll);
		oos.writeDouble(num_docs_in_coll);
	}

	public void read(ObjectInputStream ois) throws Exception {
		wordIndexer = FileUtils.readStrIndexer(ois);
		docWords = FileUtils.readIntListMap(ois);

		{
			CounterMap<Integer, Integer> cm = Generics.newCounterMap();

			for (int docid : docWords.keySet()) {
				Counter<Integer> c = Generics.newCounter();

				for (int w : docWords.get(docid)) {
					c.incrementCount(w, 1);
				}
				cm.setCounter(docid, c);
			}

			docWordCnts = VectorUtils.toSparseMatrix(cm);
		}

		collWordCnts = new SparseVector();
		collWordCnts.read(ois);

		collDocFreqs = new SparseVector();
		collDocFreqs.read(ois);

		cnt_sum_in_coll = ois.readDouble();
		num_docs_in_coll = ois.readDouble();

	}

	public WordCountBox(SparseMatrix docWordCounts, SparseVector collWordCounts, double cnt_sum_in_coll, SparseVector docFreqs,
			double num_docs_in_coll, ListMap<Integer, Integer> docWords) {
		super();
		this.docWordCnts = docWordCounts;
		this.collWordCnts = collWordCounts;
		this.cnt_sum_in_coll = cnt_sum_in_coll;
		this.collDocFreqs = docFreqs;
		this.num_docs_in_coll = num_docs_in_coll;
		this.docWords = docWords;
	}

	public SparseVector getDocFreqs() {
		return collDocFreqs;
	}

	public double getCountSum() {
		return cnt_sum_in_coll;
	}

	public SparseVector getCollWordCounts() {
		return collWordCnts;
	}

	public SparseMatrix getDocWordCounts() {
		return docWordCnts;
	}

	public ListMap<Integer, Integer> getDocWords() {
		return docWords;
	}

	public double getNumDocs() {
		return num_docs_in_coll;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public void setCollWordCounts(SparseVector collWordCounts) {
		this.collWordCnts = collWordCounts;
	}

	public void setCountSum(double cnt_sum_in_col) {
		this.cnt_sum_in_coll = cnt_sum_in_col;
	}

	public void setDocWordCounts(SparseMatrix docWordCounts) {
		this.docWordCnts = docWordCounts;
	}

	public void setWordIndexer(Indexer<String> wordIndexer) {
		this.wordIndexer = wordIndexer;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		return sb.toString();
	}

}
