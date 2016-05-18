package ohs.ir.medical.general;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import ohs.io.FileUtils;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.math.ArrayMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.Pair;
import ohs.utils.Generics;
import ohs.utils.StopWatch;

public class WordCountBox {

	public static Map<Integer, Pair<Integer, Integer>> cache = Generics.newWeakHashMap();

	public static Counter<String> getDocFreqs(IndexSearcher is, String field) throws Exception {
		IndexReader ir = is.getIndexReader();

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

	public static Counter<String> getDocFreqs(IndexSearcher is, String field, Collection<String> c) throws Exception {
		IndexReader ir = is.getIndexReader();
		Counter<String> ret = new Counter<String>();
		for (String word : c) {
			Term term = new Term(field, word);
			double df = ir.docFreq(term);
			ret.setCount(word, df);
		}
		return ret;
	}

	public static SparseVector getDocFreqs(IndexSearcher is, String field, Indexer<String> wordIndexer) throws Exception {
		IndexReader ir = is.getIndexReader();
		SparseVector ret = new SparseVector(wordIndexer.size());
		for (int i = 0; i < wordIndexer.size(); i++) {
			String word = wordIndexer.getObject(i);
			Term term = new Term(field, word);
			double df = ir.docFreq(term);
			ret.incrementAtLoc(i, i, df);
		}
		return ret;
	}

	public static WordCountBox getWordCountBox(IndexSearcher is, SparseVector docScores, Indexer<String> wordIndexer) throws Exception {
		return getWordCountBox(is, docScores, wordIndexer, CommonFieldNames.CONTENT);
	}

	public static WordCountBox getWordCountBox(IndexSearcher is, SparseVector docScores, Indexer<String> wordIndexer, String field)
			throws Exception {

		IndexReader ir = is.getIndexReader();
		Set<Integer> wordSet = Generics.newHashSet();
		Map<Integer, SparseVector> docToWordCnts = Generics.newHashMap();
		ListMap<Integer, Integer> docToWords = Generics.newListMap(docScores.size());
		// StopWatch stopWatch = StopWatch.newStopWatch();

		for (int j = 0; j < docScores.size(); j++) {
			int docid = docScores.indexAtLoc(j);
			// double score = docScores.valueAtLoc(j);
			// Document doc = ir.document(docid);
			// String content = doc.get(CommonFieldNames.CONTENT);

			Terms t = ir.getTermVector(docid, field);

			if (t == null) {
				continue;
			}

			int vec_size = (int) t.size();

			TermsEnum ts = t.iterator();

			BytesRef br = null;
			PostingsEnum pe = null;

			int loc = 0;
			SparseVector wordCnts = new SparseVector(vec_size);
			Map<Integer, Integer> locToWord = Generics.newHashMap();

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
				int cnt = pe.freq();
				int doc_freq = ts.docFreq();

				wordCnts.incrementAtLoc(loc++, w, cnt);

				for (int k = 0; k < cnt; k++) {
					// System.out.printf("%d\t%s\n", position, word);
					locToWord.put(pe.nextPosition(), w);
				}
			}
			wordCnts.sortByIndex();

			docToWordCnts.put(docid, wordCnts);

			List<Integer> locs = Generics.newArrayList(locToWord.keySet());
			Collections.sort(locs);

			List<Integer> words = Generics.newArrayList(locToWord.size());

			for (int l : locs) {
				words.add(locToWord.get(l));
			}

			docToWords.put(docid, words);

			for (int w : wordCnts.indexes()) {
				wordSet.add(w);
			}
		}

		SparseMatrix dwcs = new SparseMatrix(docToWordCnts);

		SparseVector collWordCnts = new SparseVector(wordSet.size());
		SparseVector docFreqs = new SparseVector(wordSet.size());
		int loc = 0;

		// if (doc_freq > 1) {
		// System.out.println();
		// }

		for (int w : wordSet) {
			Pair<Integer, Integer> p = cache.get(w);
			int cnt = 0;
			int doc_freq = 0;

			if (p == null) {
				String word = wordIndexer.getObject(w);
				Term term = new Term(field, word);
				TermContext termContext = TermContext.build(is.getTopReaderContext(), term);
				cnt = (int) termContext.totalTermFreq();
				doc_freq = termContext.docFreq();
			} else {
				cnt = p.getFirst();
				doc_freq = p.getSecond();
			}

			collWordCnts.incrementAtLoc(loc, w, cnt);
			docFreqs.incrementAtLoc(loc, w, doc_freq);
			loc++;
		}

		// System.out.println(stopWatch.stop());

		double cnt_sum_in_coll = ir.getSumTotalTermFreq(CommonFieldNames.CONTENT);

		WordCountBox ret = new WordCountBox(dwcs, collWordCnts, cnt_sum_in_coll, docFreqs, ir.maxDoc(), docToWords);
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

	private SparseMatrix docToWordCnts;

	private SparseVector collWordCnts;

	private SparseVector docFreqs;

	private ListMap<Integer, Integer> docToWords;

	private double cnt_sum_in_coll;

	private double num_docs_in_coll;

	public WordCountBox(SparseMatrix docWordCnts, SparseVector collWordCnts, double cnt_sum_in_coll, SparseVector docFreqs,
			double num_docs_in_coll, ListMap<Integer, Integer> docToWords) {
		super();
		this.docToWordCnts = docWordCnts;
		this.collWordCnts = collWordCnts;
		this.cnt_sum_in_coll = cnt_sum_in_coll;
		this.docFreqs = docFreqs;
		this.num_docs_in_coll = num_docs_in_coll;
		this.docToWords = docToWords;
	}

	public SparseVector getCollWordCounts() {
		return collWordCnts;
	}

	public double getCountSum() {
		return cnt_sum_in_coll;
	}

	public SparseVector getDocFreqs() {
		return docFreqs;
	}

	public SparseMatrix getDocToWordCounts() {
		return docToWordCnts;
	}

	public ListMap<Integer, Integer> getDocToWords() {
		return docToWords;
	}

	public double getNumDocs() {
		return num_docs_in_coll;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public void readObject(ObjectInputStream ois) throws Exception {
		wordIndexer = FileUtils.readStrIndexer(ois);
		docToWords = FileUtils.readIntListMap(ois);

		{
			CounterMap<Integer, Integer> cm = Generics.newCounterMap();

			for (int docid : docToWords.keySet()) {
				Counter<Integer> c = Generics.newCounter();

				for (int w : docToWords.get(docid)) {
					c.incrementCount(w, 1);
				}
				cm.setCounter(docid, c);
			}

			docToWordCnts = VectorUtils.toSparseMatrix(cm);
		}

		collWordCnts = new SparseVector();
		collWordCnts.read(ois);

		docFreqs = new SparseVector();
		docFreqs.read(ois);

		cnt_sum_in_coll = ois.readDouble();
		num_docs_in_coll = ois.readDouble();

	}

	public void setCollWordCounts(SparseVector collWordCnts) {
		this.collWordCnts = collWordCnts;
	}

	public void setCountSum(double cnt_sum_in_col) {
		this.cnt_sum_in_coll = cnt_sum_in_col;
	}

	public void setDocToWordCounts(SparseMatrix docToWordCnts) {
		this.docToWordCnts = docToWordCnts;
	}

	public void setWordIndexer(Indexer<String> wordIndexer) {
		this.wordIndexer = wordIndexer;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		return sb.toString();
	}

	public void writeObject(ObjectOutputStream oos) throws Exception {
		FileUtils.writeStrIndexer(oos, wordIndexer);
		FileUtils.writeIntListMap(oos, docToWords);

		collWordCnts.writeObject(oos);
		docFreqs.writeObject(oos);

		oos.writeDouble(cnt_sum_in_coll);
		oos.writeDouble(num_docs_in_coll);
	}

}
