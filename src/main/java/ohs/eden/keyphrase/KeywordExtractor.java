package ohs.eden.keyphrase;

import java.lang.reflect.Array;
import java.util.List;

import ohs.io.TextFileReader;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.VectorUtils;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.Generics;

public class KeywordExtractor {

	public static void main(String[] args) {
		System.out.printf("[%s] begins.\n", KeywordExtractor.class.getName());

		KeywordExtractor e = new KeywordExtractor();

		TextFileReader reader = new TextFileReader(KPPath.KEYWORD_EXTRACTOR_DIR + "raw_tagged.txt");
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String keywordStr = lines.get(0);
			String text = lines.get(1);
			e.extract(text);
		}
		reader.close();

		System.out.printf("ends.");
	}

	public void prepare() throws Exception {

	}

	public Counter<String> extract(String text) {
		Counter<String> ret = Generics.newCounter();

		CounterMap<Integer, Integer> cm = Generics.newCounterMap();
		Counter<String> c1 = Generics.newCounter();

		int window_size = 2;

		text = text.replace(". ", ".\n");
		String[] sents = text.split("\n");

		Indexer<String> wordIndexer = Generics.newIndexer();

		for (int i = 0; i < sents.length; i++) {
			String[] words = sents[i].split(" ");
			Integer[] ws = wordIndexer.getIndexes(words);

			for (String word : words) {
				c1.incrementCount(word, 1);
			}

			for (int j = 0; j < ws.length; j++) {
				int w1 = ws[j];

				for (int k = j + 1; k < j + 1 + window_size && k < ws.length; k++) {
					int w2 = ws[k];
					cm.incrementCount(w1, w2, 1);
					cm.incrementCount(w2, w1, 1);
				}
			}
		}

		double[][] mat = VectorUtils.toMatrix(cm, wordIndexer.size());

		ArrayMath.normalizeColumns(mat);

		double[] cents = ArrayUtils.array(wordIndexer.size(), 1f / wordIndexer.size());

		ArrayMath.doRandomWalk(mat, cents, 10, 0.00000001, 0.85);

		Counter<String> c2 = Generics.newCounter();

		for (int i = 0; i < cents.length; i++) {
			String word = wordIndexer.getObject(i);
			c2.setCount(word, cents[i]);
		}

		System.out.println(cm);
		System.out.println(c1);
		System.out.println(c2);

		return ret;
	}

}
