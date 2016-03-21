package ohs.ir.medical.trec.cds_2014;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.SearcherUtils;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.CounterMap;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class SearchDocuments {

	public static List<List<String>> analyze(String text, Analyzer analyzer) throws Exception {
		List<List<String>> ret = new ArrayList<List<String>>();

		String[] lines = text.split("[\n]+");

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			TokenStream ts = analyzer.tokenStream(CommonFieldNames.CONTENT, line);
			CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
			ts.reset();

			List<String> words = new ArrayList<String>();
			while (ts.incrementToken()) {
				String word = attr.toString();
				words.add(word);
			}
			ts.end();
			ts.close();
			ret.add(words);
		}
		return ret;
	}

	public static List<TermQuery> convert(List<String> words) throws Exception {
		List<TermQuery> ret = new ArrayList<TermQuery>();
		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			TermQuery tq = new TermQuery(new Term(CommonFieldNames.CONTENT, word));
			ret.add(tq);
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		SearchDocuments sd = new SearchDocuments();
		// sd.doInitialSearch();
		// sd.rankByPRF();
		// sd.rankByESA();
		// sd.rankByQC();
		sd.changeFormats();
		System.out.println("process ends.");
	}

	private static void writeResults(String fileName, CounterMap<String, String> resultData) {
		System.out.printf("write to [%text]\n", fileName);
		TextFileWriter writer = new TextFileWriter(fileName);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(20);
		nf.setGroupingUsed(false);

		CounterMap<String, String> temp = new CounterMap<String, String>();

		for (String qId : resultData.keySet()) {
			Counter<String> docScores = resultData.getCounter(qId);
			for (String docId : docScores.keySet()) {
				double score = docScores.getCount(docId);
				temp.setCount(qId, docId, score);
			}
		}

		for (String qId : new TreeSet<String>(temp.keySet())) {
			Counter<String> docScores = temp.getCounter(qId);
			for (String docId : docScores.getSortedKeys()) {
				double score = docScores.getCount(docId);
				int iter = 0;
				int rank = 0;
				int runId = 0;
				// String output = String.format("%text\tQ%d\t%text\t%d\t%text\t%d", qId,
				// iter, docId, rank, nf.format(score), runId);
				double logScore = Math.log(score);
				String output = String.format("%text\t%text\t%text", qId, docId, logScore);
				writer.write(output + "\n");
			}
		}
		writer.close();
	}

	private static void writeResults(String fileName, CounterMap<String, String> resultData, CounterMap<String, String> relevanceData) {
		System.out.printf("write to [%text]\n", fileName);
		TextFileWriter writer = new TextFileWriter(fileName);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(7);
		nf.setGroupingUsed(false);

		for (String qId : new TreeSet<String>(resultData.keySet())) {
			Counter<String> docScores = resultData.getCounter(qId);
			Counter<String> docRelevances = relevanceData.getCounter(qId);

			for (String docId : docScores.getSortedKeys()) {
				double score = docScores.getCount(docId);
				double relevance = docRelevances.getCount(docId);
				String output = String.format("%text\t%text\t%text\t%d", qId, docId, nf.format(score), (int) relevance);
				writer.write(output + "\n");
			}
		}
		writer.close();
	}

	private List<CDSQuery> cdsQueries;

	private MedicalEnglishAnalyzer analyzer;

	private IndexSearcher indexSearcher;

	public SearchDocuments() throws Exception {
		cdsQueries = CDSQuery.read(CDSPath.TEST_QUERY_FILE);

		analyzer = MedicalEnglishAnalyzer.newAnalyzer();

		indexSearcher = SearcherUtils.getIndexSearcher(CDSPath.INDEX_DIR);
		indexSearcher.setSimilarity(new LMDirichletSimilarity());
	}

	public void changeFormats() {
		List<File> files = FileUtils.getFilesUnder(new File(CDSPath.OUTPUT_RERANKING_DIR));

		BidMap<String, Integer> documentIdMap = readDocumentIdMap();

		for (int i = 0; i < files.size(); i++) {
			File inputFile = files.get(i);
			if (!inputFile.getName().startsWith("temp_results")) {
				continue;
			}

			String outputFileName = inputFile.getName().replace("temp_", "search_");
			File outputFile = new File(inputFile.getParent(), outputFileName);

			TextFileReader reader = new TextFileReader(inputFile);
			TextFileWriter writer = new TextFileWriter(new File(inputFile.getParent(), outputFileName).getPath());

			while (reader.hasNext()) {
				String line = reader.next();

				if (reader.getNumLines() == 1) {
					writer.write("Query ID\tDocument ID\tScore\n");
				} else {
					String[] parts = line.split("\t");
					String queryId = parts[0];
					int indexId = Integer.parseInt(parts[1]);
					String score = parts[2];
					String docId = documentIdMap.getKey(indexId);
					String output = String.format("%text\t%text\t%text", queryId, docId, score);
					writer.write(output + "\n");
				}
			}
			reader.close();
			writer.close();
		}
	}

	public void doInitialSearch() throws Exception {
		System.out.println("do initial search.");

		// FileUtils.deleteFilesUnder(new File(CDSPath.OUTPUT_DIR));

		int top_k = 1000;

		Map<String, Integer> documentIdMap = new TreeMap<String, Integer>();

		TextFileWriter writer = new TextFileWriter(CDSPath.OUTPUT_INITIAL_SEARCH_RESULT_FILE);
		writer.write("Query ID\tIndex ID\tScore\n");

		for (int i = 0; i < cdsQueries.size(); i++) {
			CDSQuery cdsQuery = cdsQueries.get(i);
			String qId = cdsQuery.getId();

			System.out.printf("%dth query: %text\n", i + 1, cdsQuery.getDescription());

			StringBuffer qBuff = new StringBuffer();
			qBuff.append(cdsQuery.getDescription());

			List<String> queryWords = analyze(qBuff.toString(), analyzer).get(0);

			Counter<String> docScores = new Counter<String>();

			BooleanQuery searchQuery = new BooleanQuery();

			for (TermQuery tq : convert(queryWords)) {
				searchQuery.add(new BooleanClause(tq, Occur.SHOULD));
			}

			TopDocs topDocs = indexSearcher.search(searchQuery, top_k);

			Counter<Integer> indexScores = new Counter<Integer>();

			StringBuffer sb = new StringBuffer();

			for (int k = 0; k < topDocs.scoreDocs.length; k++) {
				ScoreDoc scoreDoc = topDocs.scoreDocs[k];
				int indexId = scoreDoc.doc;
				double score = scoreDoc.score;
				Document doc = indexSearcher.getIndexReader().document(indexId);

				String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
				String content = doc.getField(CommonFieldNames.CONTENT).stringValue();

				sb.append(String.format("%text\t%d\t%text\n",

				new DecimalFormat("00").format(Integer.parseInt(qId)), indexId, score));

				documentIdMap.put(docId, indexId);
			}
			writer.write(sb.toString().trim() + "\n");
		}
		writer.close();

		writer = new TextFileWriter(CDSPath.OUTPUT_DOCUMENT_ID_MAP_FILE);
		writer.write("KDocument ID\tIndex ID");

		for (String docId : documentIdMap.keySet()) {
			Integer indexId = documentIdMap.get(docId);
			writer.write(String.format("\n%text\t%d", docId, indexId));
		}
		writer.close();
	}

	public void rankByESA() throws Exception {
		System.out.println("rank documents using ESA.");

		BidMap<String, Integer> documentIdMap = readDocumentIdMap();

		File vocDir = new File(CDSPath.VOCABULARY_DIR);

		if (!vocDir.exists()) {
			VocabularyData.make(indexSearcher.getIndexReader());
		}

		VocabularyData vocData = VocabularyData.read(vocDir);

		ExplicitSemanticModel esa = ExplicitSemanticModel.read(

		vocData.getWordIndexer(), vocData.getDocumentFrequencies(), indexSearcher.getIndexReader().maxDoc());

		ESADocumentScorer docScorer = new ESADocumentScorer(indexSearcher.getIndexReader(), analyzer, vocData, esa);

		CounterMap<Integer, Integer> queryDocumentScores = readSearchResults(new File(CDSPath.OUTPUT_PRF_RERANKED_SEARCH_RESULT_FILE));

		CounterMap<Integer, Integer> queryDocumentNewScores = new CounterMap<Integer, Integer>();

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(10);

		TextFileWriter writer = new TextFileWriter(CDSPath.OUTPUT_ESA_RERANKED_SEARCH_RESULT_FILE);
		writer.write("Query ID\tIndex ID\tScore\n");

		for (int i = 0; i < cdsQueries.size(); i++) {
			CDSQuery cdsQuery = cdsQueries.get(i);
			String qId = cdsQuery.getId();

			Counter<Integer> docScores = queryDocumentScores.getCounter(i + 1);

			System.out.printf("%dth query: %text\n", i + 1, cdsQuery.getDescription());

			StringBuffer qBuff = new StringBuffer();
			qBuff.append(cdsQuery.getDescription());

			List<String> queryWords = analyze(qBuff.toString(), analyzer).get(0);

			Counter<Integer> newDocScores = docScorer.score(queryWords, VectorUtils.toSparseVector(docScores));

			queryDocumentNewScores.setCounter(i + 1, newDocScores);

			List<Integer> docIds = newDocScores.getSortedKeys();

			StringBuffer sb = new StringBuffer();

			for (int j = 0; j < docIds.size(); j++) {
				int docId = docIds.get(j);
				double score = newDocScores.getCount(docId);
				sb.append(String.format("%text\t%d\t%text\n", new DecimalFormat("00").format(Integer.parseInt(qId)), docId, nf.format(score)));
			}
			writer.write(sb.toString().trim() + "\n");
		}
		writer.close();
	}

	public void rankByPRF() throws Exception {
		System.out.println("rank documents using PRF.");

		BidMap<String, Integer> documentIdMap = readDocumentIdMap();

		File vocDir = new File(CDSPath.VOCABULARY_DIR);

		if (!vocDir.exists()) {
			VocabularyData.make(indexSearcher.getIndexReader());
		}

		VocabularyData vocData = VocabularyData.read(vocDir);

		SparseMatrix abbrTransModel = SparseMatrix.read(CDSPath.ABBREVIATION_MODEL_FILE);

		PRFDocumentScorer docScorer = new PRFDocumentScorer(indexSearcher.getIndexReader(), analyzer, vocData, abbrTransModel);

		CounterMap<Integer, Integer> queryDocumentScores = readSearchResults(new File(CDSPath.OUTPUT_INITIAL_SEARCH_RESULT_FILE));

		CounterMap<Integer, Integer> queryDocumentNewScores = new CounterMap<Integer, Integer>();

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(10);

		TextFileWriter writer = new TextFileWriter(CDSPath.OUTPUT_PRF_RERANKED_SEARCH_RESULT_FILE);
		writer.write("Query ID\tIndex ID\tScore\n");

		for (int i = 0; i < cdsQueries.size(); i++) {
			CDSQuery cdsQuery = cdsQueries.get(i);
			String qId = cdsQuery.getId();

			Counter<Integer> docScores = queryDocumentScores.getCounter(i + 1);

			System.out.printf("%dth query: %text\n", i + 1, cdsQuery.getDescription());

			StringBuffer qBuff = new StringBuffer();
			qBuff.append(cdsQuery.getDescription());

			List<String> queryWords = analyze(qBuff.toString(), analyzer).get(0);

			Counter<Integer> newDocScores = docScorer.score(queryWords, VectorUtils.toSparseVector(docScores));

			queryDocumentNewScores.setCounter(i + 1, newDocScores);

			List<Integer> docIds = newDocScores.getSortedKeys();

			StringBuffer sb = new StringBuffer();

			for (int j = 0; j < docIds.size(); j++) {
				int docId = docIds.get(j);
				double score = newDocScores.getCount(docId);

				sb.append(String.format("%text\t%d\t%text\n",

				new DecimalFormat("00").format(Integer.parseInt(qId)), docId, nf.format(score)));
			}

			writer.write(sb.toString().trim() + "\n");
		}

		writer.close();
	}

	public void rankByQC() throws Exception {
		System.out.println("rank documents using query classifier.");

		BidMap<String, Integer> documentIdMap = readDocumentIdMap();

		File vocDir = new File(CDSPath.VOCABULARY_DIR);

		if (!vocDir.exists()) {
			VocabularyData.make(indexSearcher.getIndexReader());
		}

		VocabularyData vocData = VocabularyData.read(vocDir);

		QueryClassifier queryClassifier = QueryClassifier.read(vocData.getWordIndexer());

		QCDocumentScorer docScorer = new QCDocumentScorer(indexSearcher.getIndexReader(), analyzer, vocData, queryClassifier);

		CounterMap<Integer, Integer> queryDocumentScores = readSearchResults(new File(CDSPath.OUTPUT_PRF_RERANKED_SEARCH_RESULT_FILE));

		CounterMap<Integer, Integer> queryDocumentNewScores = new CounterMap<Integer, Integer>();

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(10);

		TextFileWriter writer = new TextFileWriter(CDSPath.OUTPUT_QC_RERANKED_SEARCH_RESULT_FILE);
		writer.write("Query ID\tIndex ID\tScore\n");

		for (int i = 0; i < cdsQueries.size(); i++) {
			CDSQuery cdsQuery = cdsQueries.get(i);
			String qId = cdsQuery.getId();

			Counter<Integer> docScores = queryDocumentScores.getCounter(i + 1);

			System.out.printf("%dth query: %text\n", i + 1, cdsQuery.getDescription());

			StringBuffer qBuff = new StringBuffer();
			qBuff.append(cdsQuery.getDescription());

			List<String> queryWords = analyze(qBuff.toString(), analyzer).get(0);

			Counter<Integer> newDocScores = docScorer.score(queryWords, VectorUtils.toSparseVector(docScores));

			queryDocumentNewScores.setCounter(i + 1, newDocScores);

			List<Integer> docIds = newDocScores.getSortedKeys();

			StringBuffer sb = new StringBuffer();

			for (int j = 0; j < docIds.size(); j++) {
				int docId = docIds.get(j);
				double score = newDocScores.getCount(docId);

				sb.append(String.format("%text\t%d\t%text\n",

				new DecimalFormat("00").format(Integer.parseInt(qId)), docId, nf.format(score)));
			}

			writer.write(sb.toString().trim() + "\n");
		}

		writer.close();
	}

	private BidMap<String, Integer> readDocumentIdMap() {
		BidMap<String, Integer> ret = new BidMap<String, Integer>();

		TextFileReader reader = new TextFileReader(CDSPath.OUTPUT_DOCUMENT_ID_MAP_FILE);
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}
			String[] parts = reader.next().split("\t");
			ret.put(parts[0], Integer.parseInt(parts[1]));
		}

		return ret;
	}

	private CounterMap<Integer, Integer> readSearchResults(File inputFile) {
		CounterMap<Integer, Integer> ret = new CounterMap<Integer, Integer>();

		TextFileReader reader = new TextFileReader(inputFile);
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = reader.next().split("\t");
			int queryId = Integer.parseInt(parts[0]);
			int indexId = Integer.parseInt(parts[1]);
			double score = Double.parseDouble(parts[2]);
			ret.setCount(queryId, indexId, score);
		}
		reader.close();
		return ret;
	}

}
