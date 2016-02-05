package ohs.ir.medical.clef.ehealth_2015;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.Section;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.MIRPath;
import ohs.tree.trie.Node;
import ohs.tree.trie.Trie;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.Generics;

public class ESABuilder {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ESABuilder m = new ESABuilder();
		// m.build1();
		m.build2();

		System.out.println("process ends.");
	}

	private MedicalEnglishAnalyzer analyzer;

	private MediaWikiParser parser;

	public ESABuilder() throws Exception {
		analyzer = new MedicalEnglishAnalyzer();

		parser = new MediaWikiParserFactory(Language.english).createParser();
	}

	public void build1() throws Exception {
		System.out.println("build ESA using TRIE.");

		Counter<String> wordConceptFreqs = new Counter<String>();
		double num_concepts = 0;

		TextFileReader reader = new TextFileReader(new File(MIRPath.ICD10_HIERARCHY_PAGE_FILE));

		Trie<String> trie = new Trie<String>();

		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			String chapter = parts[0];
			String section = parts[1];
			String subSection = parts[2];
			String title = parts[3];
			String wikiText = parts[4];

			String[] keys = new String[] { chapter, section, subSection, title };

			Node<String> node = trie.insert(keys);

			Counter<String> wordCounts = (Counter<String>) node.getData();

			if (wordCounts == null) {
				wordCounts = new Counter<String>();
				node.setData(wordCounts);
			}

			ParsedPage page = parser.parse(wikiText);

			Set<String> wordSet = Generics.newHashSet();

			for (int j = 0; j < page.getSections().size(); j++) {
				Section sec = page.getSection(j);
				String secTitle = sec.getTitle();

				// if (secTitle == null) {
				// continue;
				// }

				List<String> words = AnalyzerUtils.getWords(sec.getText(), analyzer);
				for (String word : words) {
					wordCounts.incrementCount(word, 1);
					wordSet.add(word);
				}
			}

			for (String word : wordSet) {
				wordConceptFreqs.incrementCount(word, 1);
			}

			num_concepts++;
		}
		reader.close();

		List<Node<String>> leafNodes = trie.getLeafNodes();
		TextFileWriter writer = new TextFileWriter(MIRPath.ICD10_ESA_FILE);

		Counter<String> counter = new Counter<String>();

		for (int i = 0; i < leafNodes.size(); i++) {
			Node<String> node = leafNodes.get(i);
			Counter<String> wordCounts = (Counter<String>) node.getData();

			if (node.getDepth() != 5 || wordCounts.size() == 0) {
				continue;
			}

			counter.incrementAll(wordCounts);

			double norm = 0;
			for (String word : wordCounts.keySet()) {
				double cnt = wordCounts.getCount(word);
				double tf = 1 + Math.log(cnt);
				double concept_freq = wordConceptFreqs.getCount(word);
				double tfidf = cnt * Math.log((num_concepts + 1) / concept_freq);
				wordCounts.setCount(word, tfidf);
				norm += (tfidf * tfidf);
			}

			norm = Math.sqrt(norm);
			wordCounts.scale(1f / norm);

			String keyPath = node.getKeyPath("\t");
			StringBuffer sb = new StringBuffer();
			for (String word : wordCounts.getSortedKeys()) {
				double tfidf = wordCounts.getCount(word);
				sb.append(String.format("%s:%f ", word, tfidf));
			}
			writer.write(keyPath + "\t" + sb.toString().trim() + "\n");
		}
		writer.close();

		double num_unique_words = wordConceptFreqs.size();
		double num_words = counter.totalCount();
		double avg_words_in_concept = num_words / num_concepts;

		System.out.printf("Concepts:\t%d\n", leafNodes.size());
		System.out.printf("Unique words:\t%d\n", (int) num_unique_words);
		System.out.printf("Words:\t%d\n", (int) num_words);
		System.out.printf("Avg. words per concept:\t%f\n", avg_words_in_concept);
	}

	public void build2() throws Exception {
		System.out.println("build ESA.");
		double num_concepts = 0;

		CounterMap<String, String> conceptWordCounts = Generics.newCounterMap();
		Counter<String> wordConceptFreqs = new Counter<String>();

		TextFileReader reader = new TextFileReader(MIRPath.ICD10_HIERARCHY_PAGE_FILE);

		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			String chapter = parts[0];
			String section = parts[1];
			String subSection = parts[2];
			String title = parts[3];
			String wikiText = parts[4].replace("<NL>", "\n");

			if (conceptWordCounts.containsKey(title)) {
				continue;
			}

			// if (wikiText.contains("Redirect")) {
			// System.out.println();
			// }

			ParsedPage page = parser.parse(wikiText);

			String ss = page.getText();

			Counter<String> wordCounts = new Counter<String>();

			List<String> words = AnalyzerUtils.getWords(ss, analyzer);
			for (String word : words) {
				wordCounts.incrementCount(word, 1);
			}

			conceptWordCounts.setCounter(title, wordCounts);

			for (String word : wordCounts.keySet()) {
				wordConceptFreqs.incrementCount(word, 1);
			}

			num_concepts++;
		}
		reader.close();

		double num_unique_words = wordConceptFreqs.size();
		double num_words = conceptWordCounts.totalCount();
		double avg_words_in_concept = num_words / num_concepts;

		System.out.printf("Concepts:\t%d\n", (int) conceptWordCounts.size());
		System.out.printf("Unique words:\t%d\n", (int) num_unique_words);
		System.out.printf("Words:\t%d\n", (int) num_words);
		System.out.printf("Avg. words per concept:\t%f\n", avg_words_in_concept);

		// TextFileWriter writer = new TextFileWriter(MIRPath.ICD10_ESA_FILE);

		for (String concept : conceptWordCounts.keySet()) {
			Counter<String> wordCounts = conceptWordCounts.getCounter(concept);

			double norm = 0;
			for (String word : wordCounts.keySet()) {
				double cnt = wordCounts.getCount(word);
				double tf = 1 + Math.log(cnt);
				double concept_freq = wordConceptFreqs.getCount(word);
				double tfidf = cnt * Math.log((num_concepts + 1) / concept_freq);
				wordCounts.setCount(word, tfidf);
				norm += (tfidf * tfidf);
			}

			norm = Math.sqrt(norm);
			wordCounts.scale(1f / norm);

			// String keyPath = node.getKeyPath("\t");
			// StringBuffer sb = new StringBuffer();
			// for (String word : wordCounts.getSortedKeys()) {
			// double tfidf = wordCounts.getCount(word);
			// sb.append(String.format("%s:%f ", word, tfidf));
			// }
			// writer.write(keyPath + "\t" + sb.toString().trim() + "\n");
		}
		// writer.close();

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(8);

		FileUtils.writeStrCounterMap(MIRPath.ICD10_ESA_FILE, conceptWordCounts.invert(), null, false);
	}
}
