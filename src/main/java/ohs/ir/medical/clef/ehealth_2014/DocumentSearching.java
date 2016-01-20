package ohs.ir.medical.clef.ehealth_2014;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.AfterEffectL;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelBE;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.NormalizationH1;
import org.apache.lucene.search.similarities.Similarity;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.SearcherUtils;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.StrUtils;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class DocumentSearching {

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

	public static void doSearching() throws Exception {
		List<EHealthQuery> clefQueries = EHealthQuery.read(EHPath.QUERY_2014_TEST_FILE, EHPath.DISCHARGE_DIR);

		EHealthQuery.tokenize(clefQueries);

		CounterMap<String, String> relevanceData = RelevanceJudgementReader.read(new File(EHPath.QUERY_2013_TEST_RELEVANCE_FILE));

		FileUtils.deleteFilesUnder(new File(EHPath.OUTPUT_DIR));

		MedicalEnglishAnalyzer analyzer = new MedicalEnglishAnalyzer();

		List<IndexSearcher> indexSearchers = new ArrayList<IndexSearcher>();
		indexSearchers.add(SearcherUtils.getIndexSearcher(EHPath.INDEX_DIR));
		// indexSearchers.add(new
		// IndexSearcher(DirectoryReader.open(FSDirectory.open(new
		// File(WikiPath.INDEX_FULL_DIR)))));

		List<IndexReader> indexReaders = new ArrayList<IndexReader>();

		for (int i = 0; i < indexSearchers.size(); i++) {
			indexReaders.add(indexSearchers.get(i).getIndexReader());
		}

		List<String> medicalWords = new ArrayList<String>();
		medicalWords.add("medical");
		medicalWords.add("clinical");
		medicalWords.add("doctor");
		medicalWords.add("hospital");
		medicalWords.add("symptoms");
		medicalWords.add("disease");
		medicalWords.add("infection");
		medicalWords.add("disorder");
		medicalWords.add("diagnosis");

		List<TermQuery> medicalTQs = convert(analyze(StrUtils.join(" ", medicalWords), analyzer).get(0));

		List<Integer> topKs = new ArrayList<Integer>();
		topKs.add(1000);
		topKs.add(10);

		PerformanceEvaluator eval = new PerformanceEvaluator();

		DocumentScorer docScorer = new DocumentScorer(indexReaders, analyzer);

		for (int r = 0; r < 7; r++) {
			int runId = r + 1;

			double abbr_portion = 0;
			boolean useDocCentralities = false;
			boolean useClustering = false;
			boolean useDischargeSummary = false;

			switch (runId) {
			case 1:
				// abbr_portion = 0.15;
				// useDocCentralities = true;
				// useClustering = false;
				// useDischargeSummary = true;
				break;
			case 2:
				abbr_portion = 0.15;
				useDischargeSummary = true;
				break;
			case 3:
				abbr_portion = 0.15;
				useClustering = true;
				useDischargeSummary = true;
				break;
			case 4:
				abbr_portion = 0.15;
				useDocCentralities = true;
				useClustering = true;
				useDischargeSummary = true;
				break;
			case 5:
				abbr_portion = 0.15;
				break;
			case 6:
				abbr_portion = 0;
				useClustering = true;
				break;
			case 7:
				abbr_portion = 0.15;
				useDocCentralities = true;
				useClustering = true;
				break;
			}

			docScorer.setAbbrPortion(abbr_portion);
			docScorer.setUseDischargeSummary(useDischargeSummary);
			docScorer.setUseClustering(useClustering);
			docScorer.setUseDocCentralities(useDocCentralities);

			CounterMap<String, String> resultData = new CounterMap<String, String>();
			CounterMap<String, String> newResultData = new CounterMap<String, String>();

			for (int i = 0; i < clefQueries.size(); i++) {
				EHealthQuery clefQuery = clefQueries.get(i);
				String qId = clefQuery.getId();

				System.out.printf("%dth query: %s\n", i + 1, clefQuery.getTitle());

				StringBuffer qBuff = new StringBuffer();
				qBuff.append(clefQuery.getTitle());
				qBuff.append("\n" + clefQuery.getDescription());

				List<String> queryWords = analyze(qBuff.toString(), analyzer).get(0);

				// StringBuffer dsBuff = new StringBuffer();
				//
				// for (String line : clefQuery.getDischarge().split("\n")) {
				// line = line.trim();
				// if (line.equals("") || line.contains(":")) {
				// continue;
				// }
				// dsBuff.append(line + "\n");
				// }

				List<List<String>> dischargeWords = analyze(clefQuery.getDischarge(), analyzer);

				List<SparseVector> indexScoreData = new ArrayList<SparseVector>();
				Map<Integer, String> docIndexMap = new HashMap<Integer, String>();
				Counter<String> docScores = new Counter<String>();

				for (int j = 0; j < indexSearchers.size(); j++) {
					IndexSearcher indexSearcher = indexSearchers.get(j);
					indexSearcher.setSimilarity(new LMDirichletSimilarity());
					int top_k = topKs.get(j);

					BooleanQuery searchQuery = new BooleanQuery();

					for (TermQuery tq : convert(queryWords)) {
						searchQuery.add(new BooleanClause(tq, Occur.SHOULD));
					}

					TopDocs topDocs = indexSearcher.search(searchQuery, top_k);

					Counter<Integer> indexScores = new Counter<Integer>();

					for (int k = 0; k < topDocs.scoreDocs.length; k++) {
						ScoreDoc scoreDoc = topDocs.scoreDocs[k];
						int indexId = scoreDoc.doc;
						double score = scoreDoc.score;
						Document doc = indexSearcher.getIndexReader().document(indexId);

						if (j == 0) {
							String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
							String date = doc.getField(CommonFieldNames.DATE).stringValue();
							String url = doc.getField(CommonFieldNames.URL).stringValue();
							String content = doc.getField(CommonFieldNames.CONTENT).stringValue();

							StringBuffer sb = new StringBuffer();
							sb.append(String.format("docId:\t%s\n", docId));
							sb.append(String.format("date:\t%s\n", date));
							sb.append(String.format("url:\t%s\n", url));
							sb.append(String.format("content:\t%s\n", content));

							// System.out.println(String.format("%d, %s, url:\t%s",
							// u + 1, docId, url));

							docScores.setCount(docId, score);
							indexScores.setCount(indexId, score);
							docIndexMap.put(indexId, docId);
						} else {
							String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
							String title = doc.getField(CommonFieldNames.TITLE).stringValue();
							String content = doc.getField(CommonFieldNames.CONTENT).stringValue();

							if (content.contains("may refer to")) {
								continue;
							}

							System.out.println(title + "\n" + content);
							System.out.println();
							indexScores.setCount(indexId, score);
						}
					}

					SparseVector sv = VectorUtils.toSparseVector(indexScores);
					sv.normalizeAfterSummation();

					indexScoreData.add(sv);
				}

				List<SparseVector> subIndexScoreData = new ArrayList<SparseVector>();

				for (int j = 0; j < indexScoreData.size(); j++) {
					SparseVector indexScores = indexScoreData.get(j);
					SparseVector subIndexScores = indexScores.copy();
					subIndexScores.keepTopN(100);
					subIndexScoreData.add(subIndexScores);
				}

				Counter<Integer> newIndexScores = docScorer.score(queryWords, subIndexScoreData, dischargeWords);
				SparseVector indexScores = indexScoreData.get(0);

				Counter<String> newDocScores = new Counter<String>();

				int num_matches = 0;

				for (int j = 0; j < indexScores.size(); j++) {
					int indexId = indexScores.indexAtLoc(j);
					double score = indexScores.valueAtLoc(j);

					double newScore = newIndexScores.getCount(indexId);

					if (newScore > 0) {
						score = 1 + newScore;
						num_matches++;
					}

					String docId = docIndexMap.get(indexId);
					newDocScores.setCount(docId, score);
				}

				resultData.setCounter(qId, docScores);
				newResultData.setCounter(qId, newDocScores);
			}

			// eval.evalute(resultData, relevanceData);
			// System.out.println(eval.toString(false) + "\n");
			//
			// eval.evalute(newResultData, relevanceData);
			// System.out.println(eval.toString(false) + "\n");

			String outputFileName = String.format("KISTI_EN_Run%d.dat", runId);

			File outputFile = new File(EHPath.OUTPUT_RERANKING_DIR, outputFileName);

			writeResults(outputFile, newResultData);
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		// selectBaseline();
		// doSearching();
		System.out.println("process ends.");
	}

	public static void selectBaseline() throws Exception {
		List<EHealthQuery> clefQueries = EHealthQuery.read(EHPath.QUERY_2013_TEST_FILE, EHPath.DISCHARGE_DIR);

		EHealthQuery.tokenize(clefQueries);

		CounterMap<String, String> relevanceData = RelevanceJudgementReader.read(new File(EHPath.QUERY_2013_TEST_RELEVANCE_FILE));

		List<Similarity> simList = new ArrayList<Similarity>();
		simList.add(new DefaultSimilarity());
		simList.add(new BM25Similarity());
		simList.add(new LMDirichletSimilarity());
		simList.add(new DFRSimilarity(new BasicModelBE(), new AfterEffectL(), new NormalizationH1()));

		FileUtils.deleteFilesUnder(new File(EHPath.OUTPUT_DIR));

		MedicalEnglishAnalyzer analyzer = new MedicalEnglishAnalyzer();

		List<IndexSearcher> indexSearchers = new ArrayList<IndexSearcher>();
		indexSearchers.add(SearcherUtils.getIndexSearcher(EHPath.INDEX_DIR));
		// indexSearchers.add(new
		// IndexSearcher(DirectoryReader.open(FSDirectory.open(new
		// File(WikiPath.INDEX_FULL_DIR)))));

		List<IndexReader> indexReaders = new ArrayList<IndexReader>();

		for (int i = 0; i < indexSearchers.size(); i++) {
			indexReaders.add(indexSearchers.get(i).getIndexReader());
		}

		List<Integer> topKs = new ArrayList<Integer>();
		topKs.add(100);

		PerformanceEvaluator eval = new PerformanceEvaluator();

		for (int i = 0; i < simList.size(); i++) {
			Similarity sim = simList.get(i);
			String retModel = sim.getClass().getSimpleName();

			CounterMap<String, String> resultData = new CounterMap();

			for (int j = 0; j < clefQueries.size(); j++) {
				EHealthQuery clefQuery = clefQueries.get(j);
				String qId = clefQuery.getId();

				// System.out.printf("%dth query: %s\n", j + 1,
				// clefQuery.getTitle());

				StringBuffer qBuff = new StringBuffer();
				qBuff.append(clefQuery.getTitle());
				qBuff.append("\n" + clefQuery.getDescription());

				List<String> queryWords = analyze(qBuff.toString(), analyzer).get(0);

				Counter<String> docScores = new Counter<String>();

				for (int k = 0; k < indexSearchers.size(); k++) {
					IndexSearcher indexSearcher = indexSearchers.get(k);
					indexSearcher.setSimilarity(sim);
					int top_k = topKs.get(k);

					BooleanQuery searchQuery = new BooleanQuery();

					for (TermQuery tq : convert(queryWords)) {
						searchQuery.add(new BooleanClause(tq, Occur.SHOULD));
					}

					TopDocs topDocs = indexSearcher.search(searchQuery, top_k);

					Counter<Integer> indexScores = new Counter<Integer>();

					for (int u = 0; u < topDocs.scoreDocs.length; u++) {
						ScoreDoc scoreDoc = topDocs.scoreDocs[u];
						int indexId = scoreDoc.doc;
						double score = scoreDoc.score;
						Document doc = indexSearcher.getIndexReader().document(indexId);

						String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
						String date = doc.getField(CommonFieldNames.DATE).stringValue();
						String url = doc.getField(CommonFieldNames.URL).stringValue();
						String content = doc.getField(CommonFieldNames.CONTENT).stringValue();

						StringBuffer sb = new StringBuffer();
						sb.append(String.format("docId:\t%s\n", docId));
						sb.append(String.format("date:\t%s\n", date));
						sb.append(String.format("url:\t%s\n", url));
						sb.append(String.format("content:\t%s\n", content));

						// System.out.println(String.format("%d, %s, url:\t%s",
						// u + 1, docId, url));

						docScores.setCount(docId, score);
					}

					resultData.setCounter(qId, docScores);
				}

				resultData.setCounter(qId, docScores);
			}

			eval.evalute(resultData, relevanceData);
			System.out.println(sim.getClass().getSimpleName());
			System.out.println(eval.toString(false) + "\n");
		}
	}

	private static void writeResults(File outputFile, CounterMap<String, String> resultData) {
		System.out.printf("write to [%s]\n", outputFile.getPath());
		TextFileWriter writer = new TextFileWriter(outputFile);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(20);
		nf.setGroupingUsed(false);

		CounterMap<String, String> temp = new CounterMap<String, String>();

		for (String qId : resultData.keySet()) {
			Counter<String> docScores = resultData.getCounter(qId);

			if (qId.startsWith("qtest2014.")) {
				String numStr = qId.substring("qtest2014.".length());
				qId = new DecimalFormat("000").format(Integer.parseInt(numStr));
			} else if (qId.startsWith("qtest")) {
				String numStr = qId.substring("qtest".length());
				qId = new DecimalFormat("000").format(Integer.parseInt(numStr));
			}

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
				String output = String.format("%s\tQ%d\t%s\t%d\t%s\t%d", qId, iter, docId, rank, nf.format(score), runId);
				writer.write(output + "\n");
			}
		}
		writer.close();
	}

	private static void writeResults(File outputFile, CounterMap<String, String> resultData, CounterMap<String, String> relevanceData) {
		System.out.printf("write to [%s]\n", outputFile.getPath());
		TextFileWriter writer = new TextFileWriter(outputFile);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(7);
		nf.setGroupingUsed(false);

		for (String qId : new TreeSet<String>(resultData.keySet())) {
			Counter<String> docScores = resultData.getCounter(qId);
			Counter<String> docRelevances = relevanceData.getCounter(qId);

			for (String docId : docScores.getSortedKeys()) {
				double score = docScores.getCount(docId);
				double relevance = docRelevances.getCount(docId);
				String output = String.format("%s\t%s\t%s\t%d", qId, docId, nf.format(score), (int) relevance);
				writer.write(output + "\n");
			}
		}
		writer.close();
	}
}
