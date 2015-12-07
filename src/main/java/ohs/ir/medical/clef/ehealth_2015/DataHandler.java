package ohs.ir.medical.clef.ehealth_2015;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.medical.general.DocumentIdMapper;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.NLPUtils;
import ohs.ir.medical.general.SearcherUtils;
import ohs.ir.medical.query.BaseQuery;
import ohs.ir.medical.query.QueryReader;
import ohs.ir.medical.query.RelevanceReader;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		pairQueryRelevantDocuments();
		System.out.println("process ends.");
	}

	public static void pairQueryDocuments() throws Exception {
		String queryFileName = MIRPath.CLEF_EHEALTH_QUERY_2015_FILE;
		String[] indexDirNames = MIRPath.IndexDirNames;
		String[] collNames = MIRPath.CollNames;

		IndexSearcher[] indexSearchers = SearcherUtils.getIndexSearchers(indexDirNames);

		Map map = new HashMap();

		List<BaseQuery> baseQueries = QueryReader.readClefEHealthQueries(queryFileName);

		File outputDir = new File(MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_INIT_DIR);

		File[] searchResultFiles = outputDir.listFiles();

		for (int i = 0; i < indexSearchers.length; i++) {
			IndexSearcher indexSearcher = indexSearchers[i];
			String collName = collNames[i];
			String dataFileName = MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_INIT_DIR + String.format("%s.txt", collName);
			String outputFileName = MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_QUERY_DOC_DIR + String.format("%s.txt", collName);

			StrCounterMap queryDocScores = new StrCounterMap();

			TextFileReader reader = new TextFileReader(dataFileName);
			while (reader.hasNext()) {
				String line = reader.next();
				String[] parts = line.split("\t");
				String qId = parts[0];
				String docId = parts[1];
				double score = Double.parseDouble(parts[2]);
				queryDocScores.setCount(qId, docId, score);
			}
			reader.close();

			TextFileWriter writer = new TextFileWriter(outputFileName);

			for (int j = 0; j < baseQueries.size(); j++) {
				BaseQuery bq = baseQueries.get(j);
				Counter<String> docScores = queryDocScores.getCounter(bq.getId());
				List<String> docIds = docScores.getSortedKeys();

				StringBuffer sb = new StringBuffer();
				sb.append(String.format("Query-%d\n%s\n", j + 1, bq.toString()));

				for (int k = 0; k < docIds.size() && k < 20; k++) {
					String docId = docIds.get(k);
					Document doc = indexSearcher.doc(Integer.parseInt(docId));
					String title = doc.get(IndexFieldName.TITLE);
					String content = doc.get(IndexFieldName.CONTENT);
					content = StrUtils.join("\n", NLPUtils.tokenize(content));

					if (dataFileName.contains("WIKI")) {
						content = "";
					}

					sb.append(String.format("Document-%d: %f\n%s\n%s\n", k + 1, docScores.getCount(docId), title, content));
				}

				writer.write(sb.toString().trim() + "\n\n");
			}

			writer.close();
		}
	}

	public static void pairQueryRelevantDocuments() throws Exception {
		String queryFileName = MIRPath.CLEF_EHEALTH_QUERY_2015_FILE;
		String revFileName = MIRPath.CLEF_EHEALTH_RELEVANCE_JUDGE_2015_FILE;
		String docMapFileName = MIRPath.CLEF_EHEALTH_DOC_ID_MAP_FIE;

		List<BaseQuery> baseQueries = QueryReader.readClefEHealthQueries(queryFileName);
		CounterMap<String, String>  relvData = RelevanceReader.readClefEHealthRelevances(revFileName);
		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileName);

		IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(MIRPath.CLEF_EHEALTH_INDEX_DIR);

		String outputFileName = MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_QUERY_DOC_DIR + "CLEF_2015_TRAIN.txt";

		TextFileWriter writer = new TextFileWriter(outputFileName);

		for (int j = 0; j < baseQueries.size(); j++) {
			BaseQuery bq = baseQueries.get(j);
			Counter<String> docRelevances = relvData.getCounter(bq.getId());
			List<String> docIds = docRelevances.getSortedKeys();

			StringBuffer sb = new StringBuffer();
			sb.append(String.format("Query-%d\n%s\n", j + 1, bq.toString()));

			for (int k = 0; k < docIds.size() && k < 20; k++) {
				String docId = docIds.get(k);
				String newDocId = docIdMap.getKey(docId);
				Document doc = indexSearcher.doc(Integer.parseInt(newDocId));
				String title = doc.get(IndexFieldName.TITLE);
				String content = doc.get(IndexFieldName.CONTENT);
				content = StrUtils.join("\n", NLPUtils.tokenize(content));
				sb.append(String.format("Document-%d: %f\n%s\n%s\n", k + 1, docRelevances.getCount(docId), title, content));
			}

			writer.write(sb.toString().trim() + "\n\n");
		}

		writer.close();
	}

}
