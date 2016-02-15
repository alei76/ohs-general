package ohs.eden.keyphrase;

import java.util.List;

import ohs.io.TextFileReader;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.Vocab;
import ohs.utils.Generics;
import ohs.utils.TermWeighting;

public class KeywordExtractor {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", KeywordExtractor.class.getName());

		Vocab vocab = new Vocab();
		vocab.read(KPPath.VOCAB_FILE);

		KeywordExtractor e = new KeywordExtractor(vocab);

		TextFileReader reader = new TextFileReader(KPPath.KEYWORD_EXTRACTOR_DIR + "raw_tagged.txt");
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String keywordStr = lines.get(0);
			String text = lines.get(1);

			System.out.println(keywordStr);
			e.extract(text);
		}
		reader.close();

		System.out.printf("ends.");
	}

	private Vocab vocab;

	public KeywordExtractor(Vocab vocab) {
		this.vocab = vocab;
	}

	public Counter<String> extract2(String text) {
		Counter<String> ret = Generics.newCounter();

		CounterMap<Integer, Integer> cm = Generics.newCounterMap();

		int window_size = 2;

		text = text.replace(". ", ".\n");
		String[] sents = text.split("\n");

		Indexer<Integer> docWordIndexer = Generics.newIndexer();

		Counter<String> c1 = Generics.newCounter();
		Counter<Integer> wordCnts = Generics.newCounter();
		Counter<Integer> wordDocFreqs = Generics.newCounter();

		for (int i = 0; i < sents.length; i++) {
			String[] words = sents[i].split(" ");

			List<Integer> ws = Generics.newArrayList();

			for (String word : words) {
				word = word.toLowerCase();
				int w = vocab.getWordIndexer().indexOf(word);
				if (w < 0) {
					continue;
				}

				c1.incrementCount(word, 1);
				int nw = docWordIndexer.getIndex(w);

				ws.add(nw);
				wordCnts.incrementCount(nw, 1);
			}

			int last_j = ws.size() - window_size;
			if (last_j < 0) {
				last_j = ws.size();
			}

			for (int j = 0; j < last_j; j++) {
				int w1 = ws.get(j);
				for (int k = j + 1; k < j + 1 + window_size && k < ws.size(); k++) {
					int w2 = ws.get(k);
					cm.incrementCount(w1, w2, 1);
				}
			}
		}

		for (int nw = 0; nw < docWordIndexer.size(); nw++) {
			int w = docWordIndexer.getObject(nw);
			double doc_freq = vocab.getWordDocFreq(w);
			double word_cnt = vocab.getWordCount(w);
			wordDocFreqs.setCount(nw, doc_freq);
		}

		int num_docs = vocab.getNumDocs();
		int cnt_sum = ArrayMath.sum(vocab.getWordCounts());

		for (int w1 : cm.keySet()) {
			Counter<Integer> c = cm.getCounter(w1);
			double idf1 = TermWeighting.idf(num_docs, wordDocFreqs.getCount(w1));
			for (int w2 : c.keySet()) {
				double cnt = c.getCount(w2);
				double idf2 = TermWeighting.idf(num_docs, wordDocFreqs.getCount(w2));
				double tfidf = idf1 * idf2;
				// double cnt_w = wordDocFreqs.getCount(w2);
				// double prob_w = 1f * cnt_w / cnt_sum;
				c.setCount(w2, tfidf);
			}
		}

		SparseMatrix sm = VectorUtils.toSparseMatrix(cm);

		for (int i = 0; i < sm.rowSize(); i++) {
			VectorMath.unitVector(sm.vectorAtRowLoc(i));
		}

		double[][] mat = ArrayUtils.matrix(docWordIndexer.size());

		for (int i = 0; i < sm.rowSize(); i++) {
			int index1 = sm.indexAtRowLoc(i);
			mat[i][i] = 1;
			for (int j = i + 1; j < sm.rowSize(); j++) {
				int index2 = sm.indexAtRowLoc(j);
				mat[index1][index2] = VectorMath.dotProduct(sm.vectorAtRowLoc(i), sm.vectorAtRowLoc(j));
			}
		}

		ArrayMath.normalizeColumns(mat);

		double[] cents = ArrayUtils.array(docWordIndexer.size(), 1f / docWordIndexer.size());

		ArrayMath.randomWalk(mat, cents, 10, 0.00001, 0.85);

		Counter<String> c2 = Generics.newCounter();

		for (int i = 0; i < cents.length; i++) {
			int w = docWordIndexer.getObject(i);
			String word = vocab.getWordIndexer().getObject(w);
			c2.setCount(word, cents[i]);
		}

		System.out.println(c1);
		System.out.println(c2);
		return ret;
	}

	public Counter<String> extract(String text) {
		Counter<String> ret = Generics.newCounter();

		CounterMap<Integer, Integer> cm = Generics.newCounterMap();

		int window_size = 2;

		text = text.replace(". ", ".\n");
		String[] sents = text.split("\n");

		Indexer<Integer> docWordIndexer = Generics.newIndexer();

		Counter<String> c1 = Generics.newCounter();
		Counter<Integer> wordCnts = Generics.newCounter();
		Counter<Integer> wordDocFreqs = Generics.newCounter();

		for (int i = 0; i < sents.length; i++) {
			String[] words = sents[i].split(" ");

			List<Integer> ws = Generics.newArrayList();

			for (String word : words) {
				word = word.toLowerCase();
				int w = vocab.getWordIndexer().indexOf(word);
				if (w < 0) {
					continue;
				}

				c1.incrementCount(word, 1);
				int nw = docWordIndexer.getIndex(w);

				ws.add(nw);
				wordCnts.incrementCount(nw, 1);
			}

			int last_j = ws.size() - window_size;
			if (last_j < 0) {
				last_j = ws.size();
			}

			for (int j = 0; j < last_j; j++) {
				int w1 = ws.get(j);
				for (int k = j + 1; k < j + 1 + window_size && k < ws.size(); k++) {
					int w2 = ws.get(k);
					cm.incrementCount(w1, w2, 1);
				}
			}
		}

		for (int nw = 0; nw < docWordIndexer.size(); nw++) {
			int w = docWordIndexer.getObject(nw);
			double doc_freq = vocab.getWordDocFreq(w);
			double word_cnt = vocab.getWordCount(w);
			wordDocFreqs.setCount(nw, doc_freq);
		}

		int num_docs = vocab.getNumDocs();
		int cnt_sum = ArrayMath.sum(vocab.getWordCounts());

		for (int w1 : cm.keySet()) {
			Counter<Integer> c = cm.getCounter(w1);
			double idf1 = TermWeighting.idf(num_docs, wordDocFreqs.getCount(w1));
			for (int w2 : c.keySet()) {
				double cnt = c.getCount(w2);
				double idf2 = TermWeighting.idf(num_docs, wordDocFreqs.getCount(w2));
				double tfidf = idf1 * idf2;
				// double cnt_w = wordDocFreqs.getCount(w2);
				// double prob_w = 1f * cnt_w / cnt_sum;
				c.setCount(w2, tfidf);
			}
		}

		SparseMatrix sm = VectorUtils.toSparseMatrix(cm);

		for (int i = 0; i < sm.rowSize(); i++) {
			VectorMath.unitVector(sm.vectorAtRowLoc(i));
		}

		double[][] mat = ArrayUtils.matrix(docWordIndexer.size());

		for (int i = 0; i < sm.rowSize(); i++) {
			int index1 = sm.indexAtRowLoc(i);
			// mat[i][i] = 1;
			for (int j = i + 1; j < sm.rowSize(); j++) {
				int index2 = sm.indexAtRowLoc(j);
				mat[index1][index2] = VectorMath.dotProduct(sm.vectorAtRowLoc(i), sm.vectorAtRowLoc(j));
			}
		}

		ArrayMath.normalizeColumns(mat);

		double[] cents = ArrayUtils.array(docWordIndexer.size(), 1f / docWordIndexer.size());

		ArrayMath.randomWalk(mat, cents, 10, 0.00001, 0.85);

		Counter<String> c2 = Generics.newCounter();

		for (int i = 0; i < cents.length; i++) {
			int w = docWordIndexer.getObject(i);
			String word = vocab.getWordIndexer().getObject(w);
			c2.setCount(word, cents[i]);
		}

		System.out.println(c1);
		System.out.println(c2);
		System.out.println();
		return ret;
	}

	public void prepare() throws Exception {

	}

}
