package ohs.ir.medical.general;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.TextFileWriter;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.query.BaseQuery;
import ohs.ir.medical.query.QueryReader;
import ohs.ir.medical.query.RelevanceReader;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.StrUtils;

public class RelevanceCollector {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		RelevanceCollector c = new RelevanceCollector();
		c.collect();
		// c.collect3();

		System.out.println("process ends.");
	}

	public void collect() throws Exception {
		String[] queryFileNames = MIRPath.QueryFileNames;

		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] relDataFileNames = MIRPath.RelevanceFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		String[] queryDocFileNames = MIRPath.QueryDocFileNames;

		String[] resDirNames = MIRPath.ResultDirNames;

		IndexSearcher[] iss = SearcherUtils.getIndexSearchers(indexDirNames);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = QueryReader.readQueries(queryFileNames[i]);
			IndexSearcher is = iss[i];

			String qldResFileName = resDirNames[i] + "qld.txt";
			String cbeemResFileName = resDirNames[i] + "cbeem.txt";

			CounterMap<String, String> qldResData = PerformanceEvaluator.readSearchResults(qldResFileName);
			CounterMap<String, String> cbeemResData = PerformanceEvaluator.readSearchResults(cbeemResFileName);

			BidMap<String, String> docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileNames[i]);
			CounterMap<String, String> queryRels = RelevanceReader.readRelevances(relDataFileNames[i]);
			queryRels = DocumentIdMapper.mapDocIdsToIndexIds(queryRels, docIdMap);

			String dataDirName = MIRPath.DataDirNames[i];
			String[] labels = { "QLD", "CBEEM", "REL" };

			for (BaseQuery bq : bqs) {
				String outputFileName = dataDirName + String.format("rel_analysis/%s.txt", bq.getId());

				String qId = bq.getId();
				Counter<String> docScores1 = qldResData.getCounter(qId);
				Counter<String> docScores2 = cbeemResData.getCounter(qId);
				Counter<String> docRels = queryRels.getCounter(qId);

				CounterMap<String, String> cm = new CounterMap<>();

				List<String> indexIds = docScores1.getSortedKeys();
				for (int j = 0; j < indexIds.size(); j++) {
					cm.setCount(indexIds.get(j), "QLD", j + 1);
				}

				indexIds = docScores2.getSortedKeys();
				for (int j = 0; j < indexIds.size(); j++) {
					cm.setCount(indexIds.get(j), "CBEEM", j + 1);
				}

				indexIds = docRels.getSortedKeys();

				for (int j = 0; j < indexIds.size(); j++) {
					cm.setCount(indexIds.get(j), "REL", docRels.getCount(indexIds.get(j)));
				}

				Counter<String> allDocRels = new Counter<String>();

				for (String indexId : cm.keySet()) {
					allDocRels.setCount(indexId, cm.getCount(indexId, "REL"));
				}

				indexIds = allDocRels.getSortedKeys();

				TextFileWriter writer = new TextFileWriter(outputFileName);
				writer.write("Rank\tIndexId\t" + StrUtils.join("\t", labels) + "\tContent");

				for (int j = 0; j < indexIds.size(); j++) {
					String indexId = indexIds.get(j);

					StringBuffer sb = new StringBuffer();
					sb.append(String.format("%d\t%s", j + 1, indexId));

					for (int k = 0; k < labels.length; k++) {
						int val = (int) cm.getCount(indexId, labels[k]);
						sb.append("\t");
						sb.append(val);
					}

					Document doc = is.doc(Integer.parseInt(indexId));
					String content = "null";

					if (doc != null) {
						content = doc.get(IndexFieldName.CONTENT);
					}

					sb.append(String.format("\t%s", content.replace("\n", "\\n")));

					writer.write("\n" + sb.toString());
				}
				writer.close();

				System.out.println();
			}

		}
	}

	public void collect2() throws Exception {
		String[] queryFileNames = MIRPath.QueryFileNames;

		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] relDataFileNames = MIRPath.RelevanceFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		String[] queryDocFileNames = MIRPath.QueryDocFileNames;

		IndexSearcher[] indexSearchers = SearcherUtils.getIndexSearchers(indexDirNames);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = new ArrayList<BaseQuery>();
			CounterMap<String, String> queryRels = new CounterMap<String, String>();

			File queryFile = new File(queryFileNames[i]);
			File relvFile = new File(relDataFileNames[i]);

			if (i == 0) {
				bqs = QueryReader.readTrecCdsQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readTrecCdsRelevances(relDataFileNames[i]);
			} else if (i == 1) {
				bqs = QueryReader.readClefEHealthQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readClefEHealthRelevances(relDataFileNames[i]);
			} else if (i == 2) {
				bqs = QueryReader.readOhsumedQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readOhsumedRelevances(relDataFileNames[i]);
			} else if (i == 3) {
				bqs = QueryReader.readTrecGenomicsQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readTrecGenomicsRelevances(relDataFileNames[i]);
			}

			List<Counter<String>> qs = new ArrayList<Counter<String>>();

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				qs.add(AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer));
			}

			BidMap<String, String> docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileNames[i]);

			// queryRelevances = RelevanceReader.filter(queryRelevances, docIdMap);

			// baseQueries = QueryReader.filter(baseQueries, queryRelevances);

			List<SparseVector> docRelData = DocumentIdMapper.mapDocIdsToIndexIds(bqs, queryRels, docIdMap);

			IndexReader indexReader = indexSearchers[i].getIndexReader();

			if (bqs.size() != docRelData.size()) {
				throw new Exception();
			}

			TextFileWriter writer = new TextFileWriter(queryDocFileNames[i]);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				SparseVector docRels = docRelData.get(j);

				Indexer<String> wordIndexer = new Indexer<String>();

				Counter<String> qwcs = qs.get(j);

				SparseVector q = VectorUtils.toSparseVector(qs.get(j), wordIndexer, true);

				{
					SparseVector docFreqs = VectorUtils.toSparseVector(
							WordCountBox.getDocFreqs(indexReader, IndexFieldName.CONTENT, qs.get(j).keySet()), wordIndexer, true);
					computeTFIDFs(q, docFreqs, indexReader.maxDoc());

				}

				WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docRels, wordIndexer);
				SparseMatrix sm = wcb.getDocWordCounts();
				SparseVector docFreqs = wcb.getCollDocFreqs();

				for (int k = 0; k < sm.rowSize(); k++) {
					int docId = sm.indexAtRowLoc(k);
					SparseVector sv = sm.vectorAtRowLoc(k);
					computeTFIDFs(sv, docFreqs, wcb.getNumDocsInCollection());
				}

				writer.write(String.format("#Query\t%d\t%s\n", j + 1, bq.toString()));
				writer.write(String.format("#Query Words\t%s\n", toString(VectorUtils.toCounter(q, wordIndexer))));

				docRels.sortByValue();

				for (int k = 0; k < docRels.size(); k++) {
					int docId = docRels.indexAtLoc(k);
					double rel = docRels.valueAtLoc(k);

					if (rel == 0) {
						continue;
					}

					Document doc = indexReader.document(docId);

					List<Integer> ws = wcb.getDocWords().get(docId);

					StringBuffer sb = new StringBuffer();

					for (int l = 0; l < ws.size(); l++) {
						int w = ws.get(l);
						boolean found = false;
						if (q.location(w) > -1) {
							found = true;
						}

						if (found) {
							sb.append(String.format("%d\t%s\t%s\n", l + 1, wordIndexer.getObject(w), found ? 1 + "" : ""));
						}
					}

					String content = doc.get(IndexFieldName.CONTENT);

					SparseVector sv = sm.rowAlways(docId);

					if (sv.size() == 0) {
						continue;
					}

					writer.write(String.format("DOC-ID\t%d\nRelevance\t%d\n", docId, (int) rel));
					writer.write(String.format("Loc\tWord\tMark\n%s\n", sb.toString()));

				}
				writer.write("\n");
			}
		}
	}

	public void collect3() throws Exception {
		String[] queryFileNames = MIRPath.QueryFileNames;

		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] relDataFileNames = MIRPath.RelevanceFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		String[] queryDocFileNames = MIRPath.QueryDocFileNames;

		IndexSearcher[] indexSearchers = SearcherUtils.getIndexSearchers(indexDirNames);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = new ArrayList<BaseQuery>();
			CounterMap<String, String> queryRels = new CounterMap<String, String>();

			File queryFile = new File(queryFileNames[i]);
			File relvFile = new File(relDataFileNames[i]);

			if (i == 0) {
				bqs = QueryReader.readTrecCdsQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readTrecCdsRelevances(relDataFileNames[i]);
			} else if (i == 1) {
				bqs = QueryReader.readClefEHealthQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readClefEHealthRelevances(relDataFileNames[i]);
			} else if (i == 2) {
				bqs = QueryReader.readOhsumedQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readOhsumedRelevances(relDataFileNames[i]);
			} else if (i == 3) {
				bqs = QueryReader.readTrecGenomicsQueries(queryFileNames[i]);
				queryRels = RelevanceReader.readTrecGenomicsRelevances(relDataFileNames[i]);
			}

			Counter<String> qWordCounts = new Counter<String>();

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				qWordCounts.incrementAll(AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer));
			}

			System.out.println(qWordCounts);

		}

	}

	private void computeTFIDFs(SparseVector wcs, SparseVector docFreqs, double num_docs) {
		double norm = 0;
		for (int i = 0; i < wcs.size(); i++) {
			int w = wcs.indexAtLoc(i);
			double cnt = wcs.valueAtLoc(i);
			double doc_freq = docFreqs.valueAlways(w);
			double tf = Math.log(cnt) + 1;
			double idf = Math.log((num_docs + 1) / doc_freq);
			double tfidf = tf * idf;
			wcs.setAtLoc(i, tfidf);

			norm += (tfidf * tfidf);
		}
		norm = Math.sqrt(norm);
		wcs.scale(1f / norm);
	}

	public String toString(Counter<String> c) {
		StringBuffer sb = new StringBuffer();
		List<String> keys = c.getSortedKeys();
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			double value = c.getCount(key);
			sb.append(String.format("%s:%f", key, value));
			if (i != keys.size() - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

}
