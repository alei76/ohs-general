package ohs.ir.medical.general;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;

import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.query.BaseQuery;
import ohs.ir.medical.query.QueryReader;
import ohs.ir.medical.query.RelevanceReader;
import ohs.types.CounterMap;

public class DataStats {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String[] queryFileNames = MIRPath.QueryFileNames;
		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] relFileNames = MIRPath.RelevanceFileNames;

		QueryParser queryParser = SearcherUtils.getQueryParser();

		String[] collNames = MIRPath.CollNames;
		String[] labels = { "#Docs", "Voc. Size", "Avg. Doc. Len", "#Queries", "Avg. Query Len.", "Query-Doc Pairs" };

		double[][] values = new double[labels.length][collNames.length];

		Analyzer analyzer = MedicalEnglishAnalyzer.newAnalyzer();

		for (int i = 0; i < indexDirNames.length; i++) {
			int num_queries = 0;
			int num_query_doc_pairs = 0;
			int num_docs = 0;
			long word_cnt_sum = 0;
			long num_words = 0;
			double avg_doc_len = 0;
			double avg_query_len = 0;

			IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(indexDirNames[i]);
			IndexReader indexReader = indexSearcher.getIndexReader();

			if (i < indexDirNames.length - 1) {
				File queryFile = new File(queryFileNames[i]);
				File relvFile = new File(relFileNames[i]);

				List<BaseQuery> baseQueries = QueryReader.readQueries(queryFileNames[i]);
				CounterMap<String, String> relData = RelevanceReader.readRelevances(relFileNames[i]);

				baseQueries = QueryReader.filter(baseQueries, relData);

				num_queries = baseQueries.size();
				num_query_doc_pairs = relData.totalSize();

				for (int j = 0; j < baseQueries.size(); j++) {
					BaseQuery bq = baseQueries.get(j);
					List<String> words = AnalyzerUtils.getWords(bq.getSearchText(), analyzer);
					avg_query_len += words.size();
				}

				avg_query_len /= baseQueries.size();
			}

			num_docs = indexReader.maxDoc();
			word_cnt_sum = indexReader.getSumTotalTermFreq(CommonFieldNames.CONTENT);
			avg_doc_len = word_cnt_sum / num_docs;

			Fields fields = MultiFields.getFields(indexReader);
			Terms terms = fields.terms(CommonFieldNames.CONTENT);
			num_words = terms.size();

			values[0][i] = num_docs;
			values[1][i] = num_words;
			values[2][i] = avg_doc_len;
			values[3][i] = num_queries;
			values[4][i] = avg_query_len;
			values[5][i] = num_query_doc_pairs;

			// sb.append("KDocumentCollection\t#Docs\tVoc\tAvg Doc Len\t#Queries\t#Query-Doc Pairs");
		}

		StringBuffer sb = new StringBuffer();
		// sb.append("KDocumentCollection\t#Docs\tVoc\tAvg Doc Len\t#Queries\t#Avg Query Len\t#Query-Doc Pairs");

		for (int i = 0; i < collNames.length; i++) {
			sb.append(String.format("\t%text", collNames[i]));
		}

		for (int i = 0; i < labels.length; i++) {
			sb.append("\n");
			sb.append(labels[i]);
			for (int j = 0; j < collNames.length; j++) {
				sb.append(String.format("\t%f", values[i][j]));
			}
		}

		// sb.append(String.format("\n%text\t%d\t%d\t%f\t%d\t%f\t%d", collNames[i],
		// num_docs, num_words, avg_doc_len, num_queries,
		// avg_query_len,
		// num_query_doc_pairs));

		System.out.println(sb.toString());

		System.out.println("process ends.");
	}

	public void computeStats() throws Exception {
		String[] queryFileNames = MIRPath.QueryFileNames;
		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] relFileNames = MIRPath.RelevanceFileNames;

		QueryParser queryParser = SearcherUtils.getQueryParser();

		String[] collNames = MIRPath.CollNames;
		String[] labels = { "#Docs", "Voc. Size", "Avg. Doc. Len", "#Queries", "Avg. Query Len.", "Query-Doc Pairs" };

		double[][] values = new double[labels.length][collNames.length];

		Analyzer analyzer = MedicalEnglishAnalyzer.newAnalyzer();

		for (int i = 0; i < indexDirNames.length; i++) {
			int num_queries = 0;
			int num_query_doc_pairs = 0;
			int num_docs = 0;
			long word_cnt_sum = 0;
			long num_words = 0;
			double avg_doc_len = 0;
			double avg_query_len = 0;

			IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(indexDirNames[i]);
			IndexReader indexReader = indexSearcher.getIndexReader();

			if (i < indexDirNames.length - 1) {
				File queryFile = new File(queryFileNames[i]);
				File relvFile = new File(relFileNames[i]);

				List<BaseQuery> baseQueries = new ArrayList<BaseQuery>();
				CounterMap<String, String> relevanceData = new CounterMap<String, String>();

				if (i == 0) {
					baseQueries = QueryReader.readTrecCdsQueries(queryFileNames[i]);
					relevanceData = RelevanceReader.readTrecCdsRelevances(relFileNames[i]);
				} else if (i == 1) {
					baseQueries = QueryReader.readClefEHealthQueries(queryFileNames[i]);
					relevanceData = RelevanceReader.readClefEHealthRelevances(relFileNames[i]);
				} else if (i == 2) {
					baseQueries = QueryReader.readOhsumedQueries(queryFileNames[i]);
					relevanceData = RelevanceReader.readOhsumedRelevances(relFileNames[i]);
				} else if (i == 2) {
					baseQueries = QueryReader.readTrecGenomicsQueries(queryFileNames[i]);
					relevanceData = RelevanceReader.readTrecGenomicsRelevances(relFileNames[i]);
				}

				baseQueries = QueryReader.filter(baseQueries, relevanceData);

				num_queries = baseQueries.size();
				num_query_doc_pairs = relevanceData.totalSize();

				for (int j = 0; j < baseQueries.size(); j++) {
					BaseQuery bq = baseQueries.get(j);
					List<String> words = AnalyzerUtils.getWords(bq.getSearchText(), analyzer);
					avg_query_len += words.size();
				}

				avg_query_len /= baseQueries.size();
			}

			num_docs = indexReader.maxDoc();
			word_cnt_sum = indexReader.getSumTotalTermFreq(CommonFieldNames.CONTENT);
			avg_doc_len = word_cnt_sum / num_docs;

			Fields fields = MultiFields.getFields(indexReader);
			Terms terms = fields.terms(CommonFieldNames.CONTENT);
			num_words = terms.size();

			values[0][i] = num_docs;
			values[1][i] = num_words;
			values[2][i] = avg_doc_len;
			values[3][i] = num_queries;
			values[4][i] = avg_query_len;
			values[5][i] = num_query_doc_pairs;

			// sb.append("KDocumentCollection\t#Docs\tVoc\tAvg Doc Len\t#Queries\t#Query-Doc Pairs");
		}

		StringBuffer sb = new StringBuffer();
		// sb.append("KDocumentCollection\t#Docs\tVoc\tAvg Doc Len\t#Queries\t#Avg Query Len\t#Query-Doc Pairs");

		for (int i = 0; i < collNames.length; i++) {
			sb.append(String.format("\t%text", collNames[i]));
		}

		for (int i = 0; i < labels.length; i++) {
			sb.append("\n");
			sb.append(labels[i]);
			for (int j = 0; j < collNames.length; j++) {
				sb.append(String.format("\t%f", values[i][j]));
			}
		}

		System.out.println(sb.toString());
	}
}
