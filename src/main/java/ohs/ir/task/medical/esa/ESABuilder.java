package ohs.ir.task.medical.esa;

import java.util.List;

import edu.stanford.nlp.stats.IntCounter;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.task.medical.MIRPath;
import ohs.tree.trie.Node;
import ohs.tree.trie.Trie;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

public class ESABuilder {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ESABuilder m = new ESABuilder();
		m.build();

		System.out.println("process ends.");
	}

	private MedicalEnglishAnalyzer analyzer;

	public ESABuilder() throws Exception {
		analyzer = new MedicalEnglishAnalyzer();
	}

	public void build() throws Exception {
		System.out.println("build ESA model.");

		IntCounter docFreqs = new IntCounter();
		double num_docs = 0;

		TextFileReader reader = new TextFileReader(MIRPath.ICD10_HIERARCHY_PAGE_FILE);

		Trie<String> trie = new Trie<String>();

		Indexer<String> conceptIndexer = new Indexer<String>();
		Indexer<String> wordIndexer = new Indexer<String>();

		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 5) {
				continue;
			}

			String chapter = parts[0];
			String section = parts[1];
			String subSection = parts[2];
			String title = parts[3];
			String wikiText = parts[4];

			// String[] keys = new String[] { chapter, section, subSection, title };
			String[] keys = new String[] { subSection, title };

			Node<String> node = trie.insert(keys);

			IntCounter c = (IntCounter) node.getData();

			if (c == null) {
				c = new IntCounter();
				node.setData(c);
			}

			IntHashSet wordSet = new IntHashSet();

			String[] lines = wikiText.split("<NL>");

			Counter<String> wc = AnalyzerUtils.getWordCounts(wikiText.replace("<NL>", "\n"), analyzer);

			if (wc.size() < 5) {
				continue;
			}

			String concept = node.getKeyPath(" -> ");
			int cptId = conceptIndexer.getIndex(concept);

			// for (List<String> words : wordsList) {
			for (String word : wc.keySet()) {
				int w = wordIndexer.getIndex(word);
				double cnt = wc.getCount(word);
				c.incrementCount(w, cnt);
				wordSet.add(w);
			}
			// }

			for (int w : wordSet) {
				docFreqs.incrementCount(w, 1);
			}

			num_docs++;
		}
		reader.close();

		List<Node<String>> leafNodes = trie.getLeafNodes();

		CounterMap<Integer, Integer> cm = new IntCounterMap();

		for (int i = 0; i < leafNodes.size(); i++) {
			Node<String> node = leafNodes.get(i);
			IntCounter c = (IntCounter) node.getData();

			if (node.getDepth() != 5 || c.size() == 0) {
				continue;
			}

			double norm = 0;
			for (int w : c.keySet()) {
				double cnt = c.getCount(w);
				double tf = 1 + Math.log(cnt);
				double doc_freq = docFreqs.getCount(w);
				double tfidf = cnt * Math.log((num_docs + 1) / doc_freq);
				c.setCount(w, tfidf);
				norm += (tfidf * tfidf);
			}

			norm = Math.sqrt(norm);
			c.scale(1f / norm);

			String concept = node.getKeyPath(" -> ");
			int cptId = conceptIndexer.getIndex(concept);
			cm.setCounter(cptId, c);
		}

		TextFileWriter writer = new TextFileWriter(MIRPath.ICD10_ESA_FILE);

		writer.write(String.format("## concepts\t%d\n", conceptIndexer.size()));

		for (int i = 0; i < conceptIndexer.size(); i++) {
			writer.write(conceptIndexer.getObject(i) + "\n");
		}

		writer.write("\n");
		writer.write(String.format("## words\t%d\n", wordIndexer.size()));

		for (int i = 0; i < wordIndexer.size(); i++) {
			writer.write(wordIndexer.getObject(i) + "\n");
		}

		writer.write("\n");
		writer.write(String.format("## concept-word weights\n"));

		cm = cm.invert();

		for (int w = 0; w < wordIndexer.size(); w++) {
			Counter<Integer> c = cm.getCounter(w);

			StringBuffer sb = new StringBuffer();
			sb.append(w + "\t");

			List<Integer> cpts = c.getSortedKeys();

			for (int j = 0; j < cpts.size(); j++) {
				int cpt = cpts.get(j);
				double weight = c.getCount(cpt);

				sb.append(String.format("%d:%s", cpt, weight + ""));

				if (j != cpts.size() - 1) {
					sb.append("\t");
				}
			}

			writer.write(sb.toString() + "\n");
		}
		writer.close();

		// double num_unique_words = wordConceptFreqs.size();
		// double num_words = counter.totalCount();
		// double avg_words_in_concept = num_words / num_concepts;
		//
		// System.out.printf("Concepts:\t%d\n", (int) leafNodes.size());
		// System.out.printf("Unique words:\t%d\n", (int) num_unique_words);
		// System.out.printf("Words:\t%d\n", (int) num_words);
		// System.out.printf("Avg. words per concept:\t%f\n", avg_words_in_concept);
	}
}
