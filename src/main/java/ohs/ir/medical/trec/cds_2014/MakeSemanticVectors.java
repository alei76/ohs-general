package ohs.ir.medical.trec.cds_2014;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexReader;

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.Section;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.SearcherUtils;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

public class MakeSemanticVectors {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		MakeSemanticVectors m = new MakeSemanticVectors();
		m.makeVectors();

		System.out.println("process ends.");
	}

	private MedicalEnglishAnalyzer analyzer;

	private MediaWikiParser parser;

	private Set<String> stopSectionTitleSet;

	private String[] targetSectionWords = { "symptom", "treatment", "diagnosis", "SmithWatermanScorer" };

	private IndexReader indexReader;

	public MakeSemanticVectors() throws Exception {
		analyzer = MedicalEnglishAnalyzer.newAnalyzer();

		stopSectionTitleSet = ExtractICD10.getStopSectionTitleSet();

		parser = new MediaWikiParserFactory(Language.english).createParser();

		indexReader = SearcherUtils.getIndexSearcher(CDSPath.INDEX_DIR).getIndexReader();

	}

	private void makeConceptCategoryWeights(CounterMap<Integer, Integer> conceptCategoryCounts, Counter<Integer> conceptCategoryFreqs) {
		for (int conceptId : conceptCategoryCounts.keySet()) {
			Counter<Integer> categoryCounts = conceptCategoryCounts.getCounter(conceptId);
			double norm = 0;
			for (int cat : categoryCounts.keySet()) {
				double tf = categoryCounts.getCount(cat);
				tf = Math.log(tf) + 1;
				double categoryFreq = conceptCategoryFreqs.getCount(cat);
				double numCategories = conceptCategoryCounts.size();
				// double tf = 1 + (count == 0 ? 0 : Math.log(count));
				double idf = categoryFreq == 0 ? 0 : Math.log((numCategories + 1) / categoryFreq);
				double tfidf = tf * idf;
				norm += tfidf * tfidf;
				categoryCounts.setCount(cat, tfidf);
			}
			norm = Math.sqrt(norm);
			categoryCounts.scale(1f / norm);
		}
	}

	public void makeVectors() throws Exception {
		Indexer<String> wordIndexer = FileUtils.readIndexer(CDSPath.WORD_INDEXER_FILE);

		Indexer<String> conceptIndexer = new Indexer<String>();
		Indexer<String> categoryIndexer = new Indexer<String>();

		Counter<String> sectionCounts = new Counter<String>();

		CounterMap<Integer, Integer> conceptWordCounts = new CounterMap<Integer, Integer>();
		CounterMap<Integer, Integer> categoryConceptCounts = new CounterMap<Integer, Integer>();

		TextFileReader reader = new TextFileReader(new File(CDSPath.ICD10_REFINED_HIERARCHY_PAGE_ATTACHED_FILE));

		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();

			String[] parts = lines.get(1).split("\t");
			String code = parts[0];
			String label = parts[1];

			for (int i = 2; i < lines.size(); i++) {
				String line = lines.get(i);
				parts = line.split("\t");

				if (parts.length != 4) {
					continue;
				}

				boolean isRelatedToDisease = Boolean.parseBoolean(parts[1]);
				String wikiTitle = parts[2];
				String wikiText = parts[3].replace("<NL>", "\n");

				if (!isRelatedToDisease) {
					continue;
				}

				String concept = wikiTitle.toLowerCase();

				int conceptId = conceptIndexer.getIndex(concept);

				Counter<Integer> wordCounts = conceptWordCounts.getCounter(conceptId);

				if (wordCounts.size() == 0) {
					ParsedPage page = parser.parse(wikiText);

					for (int j = 0; j < page.getSections().size(); j++) {
						Section section = page.getSection(j);
						String sectionTitle = section.getTitle();

						if (sectionTitle == null || stopSectionTitleSet.contains(concept)) {
							// System.out.println(sectionTitle);
							continue;
						}

						boolean isTargetSection = false;

						for (int k = 0; k < targetSectionWords.length; k++) {
							String targetSectionWord = targetSectionWords[k];
							if (sectionTitle.toLowerCase().contains(targetSectionWord)) {
								isTargetSection = true;
								break;
							}
						}

						if (!isTargetSection) {
							// System.out.println(sectionTitle);
							continue;
						}

						List<String> words = AnalyzerUtils.getWords(section.getText(), analyzer);

						for (String word : words) {
							int w = wordIndexer.indexOf(word);
							if (w > -1) {
								wordCounts.incrementCount(w, 1);
							}
						}
					}

					for (Link link : page.getCategories()) {
						String category = link.getTarget();
						category = category.substring(9);
						int categoryId = categoryIndexer.getIndex(category);
						categoryConceptCounts.incrementCount(categoryId, conceptId, 1);
					}
				}
			}
		}
		reader.close();

		Counter<Integer> conceptFreqs = new Counter<Integer>();

		for (int conceptId : conceptWordCounts.keySet()) {
			Counter<Integer> wordCounts = conceptWordCounts.getCounter(conceptId);
			for (int w : wordCounts.keySet()) {
				conceptFreqs.incrementCount(w, 1);
			}
		}

		Counter<Integer> categoryFreqs = new Counter<Integer>();

		for (int categoryId : categoryConceptCounts.keySet()) {
			Counter<Integer> categoryCounts = categoryConceptCounts.getCounter(categoryId);
			for (int conceptId : categoryCounts.keySet()) {
				categoryFreqs.incrementCount(conceptId, 1);
			}
		}

		makeWordConceptWeights(conceptWordCounts, conceptFreqs);
		makeConceptCategoryWeights(categoryConceptCounts, categoryFreqs);

		SparseMatrix wordConceptWeights = VectorUtils.toSpasreMatrix(conceptWordCounts.invert());
		SparseMatrix conceptCategoryWeights = VectorUtils.toSpasreMatrix(categoryConceptCounts.invert());

		wordConceptWeights.setRowDim(wordIndexer.size());
		wordConceptWeights.setColDim(conceptIndexer.size());

		conceptCategoryWeights.setRowDim(conceptIndexer.size());
		conceptCategoryWeights.setColDim(categoryIndexer.size());

		wordConceptWeights.write(CDSPath.ICD10_WORD_CONCEPT_MAP_FILE);
		conceptCategoryWeights.write(CDSPath.ICD10_CONCEPT_CATEGORY_MAP_FILE);

		FileUtils.write(CDSPath.ICD10_CONCEPT_INDEXER_FILE, conceptIndexer);
		FileUtils.write(CDSPath.ICD10_CATEGORY_INDEXER_FILE, categoryIndexer);

		System.out.printf("concept size:\t%d\n", conceptIndexer.size());
		System.out.printf("category size:\t%d\n", categoryIndexer.size());

	}

	private void makeWordConceptWeights(CounterMap<Integer, Integer> conceptWordCounts, Counter<Integer> wordConceptFreqs) {
		for (int docId : conceptWordCounts.keySet()) {
			Counter<Integer> wordCounts = conceptWordCounts.getCounter(docId);
			double norm = 0;
			for (int w : wordCounts.keySet()) {
				double tf = wordCounts.getCount(w);
				tf = Math.log(tf) + 1;
				double conceptFreq = wordConceptFreqs.getCount(w);
				double numConcepts = indexReader.maxDoc();
				// double tf = 1 + (count == 0 ? 0 : Math.log(count));
				double idf = conceptFreq == 0 ? 0 : Math.log((numConcepts + 1) / conceptFreq);
				double tfidf = tf * idf;
				norm += tfidf * tfidf;
				wordCounts.setCount(w, tfidf);
			}
			norm = Math.sqrt(norm);
			wordCounts.scale(1f / norm);
		}
	}

	private void write(CounterMap<String, String> wordDocWeights, String outputFile) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(6);
		nf.setGroupingUsed(false);

		TextFileWriter writer = new TextFileWriter(outputFile);

		for (String word : wordDocWeights.keySet()) {
			Counter<String> docWeights = wordDocWeights.getCounter(word);
			if (docWeights.size() > 0) {
				StringBuffer sb = new StringBuffer(word);

				for (String doc : docWeights.getSortedKeys()) {
					double weight = docWeights.getCount(doc);
					sb.append(String.format("\t%s:%s", doc, nf.format(weight)));
				}

				writer.write(sb.toString() + "\n");
			}
		}
		writer.close();
	}

}
