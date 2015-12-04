package ohs.ir.task.medical;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.task.medical.query.BaseQuery;
import ohs.ir.task.medical.query.QueryReader;
import ohs.ir.task.medical.query.RelevanceReader;
import ohs.math.ArrayMath;
import ohs.matrix.SparseVector;
import ohs.types.BidMap;
import ohs.types.CounterMap;
import ohs.utils.StrUtils;

public class SearchLogAnalyzer {

	public static void main(String[] args) throws Exception {
		SearchLogAnalyzer ala = new SearchLogAnalyzer();
		ala.analyze();
		// ala.analyzeCollectionPriors();
		// ala.makeQueryDocPairs();
	}

	public void analyze() {
		String[] logDirNames = MIRPath.LogDirNames;
		String[] collNames = MIRPath.CollNames;

		String targetRunName = "cbeem_100_10_5_25_2000.0_0.5_false_false_false_0.5_false_false.txt";

		CounterMap<String, String> map1 = new CounterMap<String, String>();
		CounterMap<String, String> map2 = new CounterMap<String, String>();
		CounterMap<String, String> map3 = new CounterMap<String, String>();

		for (int i = 0; i < logDirNames.length; i++) {
			String logDir = logDirNames[i];
			System.out.println(logDir);

			File[] files = new File(logDir).listFiles();

			for (int j = 0; j < files.length; j++) {
				File file = files[j];

				if (!file.getName().equals(targetRunName)) {
					continue;
				}

				String paramStr = file.getName().replace(".txt", "");
				String[] parts = paramStr.split("_");
				String modelName = parts[0];

				HyperParameter hyperParameter = new HyperParameter();
				hyperParameter = HyperParameter.parse(StrUtils.subTokens(parts, 1, parts.length));
				List<String> lines = new ArrayList<String>();

				TextFileReader reader = new TextFileReader(file.getPath());
				while (reader.hasNext()) {
					String line = reader.next();
					if (line.startsWith("id:")) {
						if (lines.size() > 0) {
							List<Double> values = new ArrayList<Double>();

							for (int k = 0; k < lines.size(); k++) {
								String str = lines.get(k);
								String regex = "(RM[\\d]) \\((0.[\\d]+)\\)";
								Pattern p = Pattern.compile(regex);
								Matcher m = p.matcher(str);
								if (m.find()) {
									String g1 = m.group(1);
									String g2 = m.group(2);
									values.add(Double.parseDouble(g2));
								}
							}

							lines = new ArrayList<String>();
						}

						lines.add(line);
					} else {
						lines.add(line);
					}
				}
				reader.close();

			}
		}

		// System.out.println(map1.toString());
		// System.out.println();
		// System.out.println(map2.toString());

		int row = 3;
		int col = 4;

		double[][] mat1 = new double[row][col];
		double[][] mat2 = new double[row][col];
		double[][] mat3 = new double[row][col];

		for (int j = 0; j < row; j++) {
			for (int k = 0; k < col; k++) {
				mat1[j][k] = map1.getCount(collNames[j], collNames[k]);
				mat2[j][k] = map2.getCount(collNames[j], collNames[k]);
				mat3[j][k] = map3.getCount(collNames[j], collNames[k]);

				if (mat3[j][k] > 0) {
					mat3[j][k] = 1f / mat3[j][k];
				}
			}
		}

		ArrayMath.multiply(mat2, mat3, mat2);

		System.out.println(getResultMatrix(collNames, mat1));
		System.out.println();
		System.out.println(getResultMatrix(collNames, mat2));

	}

	public void analyzeCollectionPriors() {
		String[] logDirNames = MIRPath.LogDirNames;
		String[] collNames = MIRPath.CollNames;

		CounterMap<String, String> map1 = new CounterMap<String, String>();
		CounterMap<String, String> map2 = new CounterMap<String, String>();
		CounterMap<String, String> map3 = new CounterMap<String, String>();

		for (int i = 0; i < logDirNames.length; i++) {
			String logDir = logDirNames[i];
			System.out.println(logDir);

			File[] files = new File(logDir).listFiles();

			for (int j = 0; j < files.length; j++) {
				File file = files[j];

				String paramStr = file.getName().replace(".txt", "");
				String[] parts = paramStr.split("_");
				String modelName = parts[0];

				HyperParameter hyperParameter = new HyperParameter();
				hyperParameter = HyperParameter.parse(StrUtils.subTokens(parts, 1, parts.length));
				List<String> lines = new ArrayList<String>();

				if (!hyperParameter.isUseWiki()) {
					continue;
				}

				List<List<Double>> valuesList = new ArrayList<List<Double>>();

				TextFileReader reader = new TextFileReader(file.getPath());
				while (reader.hasNext()) {
					String line = reader.next();
					if (line.startsWith("id:")) {
						if (lines.size() > 0) {
							List<Double> values = new ArrayList<Double>();

							for (int k = 0; k < lines.size(); k++) {
								String str = lines.get(k);
								String regex = "(RM[\\d]) \\((0.[\\d]+)\\)";
								Pattern p = Pattern.compile(regex);
								Matcher m = p.matcher(str);
								if (m.find()) {
									String g1 = m.group(1);
									String g2 = m.group(2);
									values.add(Double.parseDouble(g2));
								}
							}
							valuesList.add(values);

							lines = new ArrayList<String>();
						}

						lines.add(line);
					} else {
						lines.add(line);
					}
				}
				reader.close();

				{
					List<Double> values = new ArrayList<Double>();
					for (int k = 0; k < lines.size(); k++) {
						String str = lines.get(k);
						String regex = "(RM[\\d]) \\((0.[\\d]+)\\)";
						Pattern p = Pattern.compile(regex);
						Matcher m = p.matcher(str);
						if (m.find()) {
							String g1 = m.group(1);
							String g2 = m.group(2);
							values.add(Double.parseDouble(g2));
						}
					}
					valuesList.add(values);
				}

				for (int k = 0; k < valuesList.size(); k++) {
					List<Double> values = valuesList.get(k);
					double[] vs = new double[values.size()];

					for (int l = 0; l < values.size(); l++) {
						vs[l] = values.get(l);
						map2.incrementCount(collNames[i], collNames[l], vs[l]);
						map3.incrementCount(collNames[i], collNames[l], 1);
					}
					int maxId = ArrayMath.argMax(vs);
					map1.incrementCount(collNames[i], collNames[maxId], 1);
				}
			}
		}

		// System.out.println(map1.toString());
		// System.out.println();
		// System.out.println(map2.toString());

		int row = 3;
		int col = 4;

		double[][] mat1 = new double[row][col];
		double[][] mat2 = new double[row][col];
		double[][] mat3 = new double[row][col];

		for (int j = 0; j < row; j++) {
			for (int k = 0; k < col; k++) {
				mat1[j][k] = map1.getCount(collNames[j], collNames[k]);
				mat2[j][k] = map2.getCount(collNames[j], collNames[k]);
				mat3[j][k] = map3.getCount(collNames[j], collNames[k]);

				if (mat3[j][k] > 0) {
					mat3[j][k] = 1f / mat3[j][k];
				}
			}
		}

		ArrayMath.multiply(mat2, mat3, mat2);

		System.out.println(getResultMatrix(collNames, mat1));
		System.out.println();
		System.out.println(getResultMatrix(collNames, mat2));

	}

	private String getResultMatrix(String[] collNames, double[][] mat) {
		int row = mat.length;
		int col = mat[0].length;

		StringBuffer sb = new StringBuffer("Col");
		for (int i = 0; i < col; i++) {
			sb.append("\t" + collNames[i]);
		}
		sb.append("\n");

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(4);

		for (int i = 0; i < row; i++) {
			sb.append(collNames[i]);
			for (int j = 0; j < col; j++) {
				sb.append(String.format("\t%s", nf.format(mat[i][j])));
			}
			sb.append("\n");
		}
		return sb.toString().trim();
	}

	public void makeQueryDocPairs() throws Exception {
		String[] queryFileNames = MIRPath.QueryFileNames;

		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] resultDirNames = MIRPath.ResultDirNames;

		String[] logDirNames = MIRPath.LogDirNames;

		String[] docPriorFileNames = MIRPath.DocPriorFileNames;

		String[] relevanceDataFileNames = MIRPath.RelevanceFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		IndexSearcher[] indexSearchers = new IndexSearcher[indexDirNames.length];

		for (int i = 0; i < indexDirNames.length; i++) {
			indexSearchers[i] = SearcherUtils.getIndexSearcher(indexDirNames[i]);
		}

		QueryParser queryParser = SearcherUtils.getQueryParser();
		List<List<BaseQuery>> queryData = new ArrayList<List<BaseQuery>>();
		List<List<SparseVector>> relevanceData = new ArrayList<List<SparseVector>>();

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> baseQueries = new ArrayList<BaseQuery>();
			CounterMap<String, String> queryRelevances = new CounterMap<String, String>();

			File queryFile = new File(queryFileNames[i]);
			File relvFile = new File(relevanceDataFileNames[i]);

			if (i == 0) {
				baseQueries = QueryReader.readTrecCdsQueries(queryFileNames[i]);
				queryRelevances = RelevanceReader.readTrecCdsRelevances(relevanceDataFileNames[i]);
			} else if (i == 1) {
				baseQueries = QueryReader.readClefEHealthQueries(queryFileNames[i]);
				queryRelevances = RelevanceReader.readClefEHealthRelevances(relevanceDataFileNames[i]);
			} else if (i == 2) {
				baseQueries = QueryReader.readOhsumedQueries(queryFileNames[i]);
				queryRelevances = RelevanceReader.readOhsumedRelevances(relevanceDataFileNames[i]);
			}

			for (int j = 0; j < baseQueries.size(); j++) {
				BaseQuery bq = baseQueries.get(j);
				Query luceneQuery = queryParser.parse(bq.getSearchText());
				bq.setLuceneQuery(luceneQuery);
			}

			BidMap<String, String> docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileNames[i]);

			queryRelevances = RelevanceReader.filter(queryRelevances, docIdMap);

			baseQueries = QueryReader.filter(baseQueries, queryRelevances);

			queryData.add(baseQueries);

			List<SparseVector> docRelevances = DocumentIdMapper.mapDocIdsToIndexIds(baseQueries, queryRelevances, docIdMap);

			relevanceData.add(docRelevances);
		}

		for (int i = 0; i < queryData.size(); i++) {
			List<BaseQuery> baseQueries = queryData.get(i);
			List<SparseVector> queryRelevances = relevanceData.get(i);
			String collName = MIRPath.CollNames[i];

			IndexSearcher indexSearcher = indexSearchers[i];

			String outputFileName = String.format("query_doc_%s.txt", collName);

			TextFileWriter writer = new TextFileWriter(MIRPath.DATA_DIR + outputFileName);

			for (int j = 0; j < baseQueries.size(); j++) {
				BaseQuery baseQuery = baseQueries.get(j);

				writer.write(baseQuery.toString() + "\n\n");

				SparseVector docRelevances = queryRelevances.get(j);
				docRelevances.sortByValue();

				for (int k = 0; k < docRelevances.size(); k++) {
					int docId = docRelevances.indexAtLoc(k);
					double relevance = docRelevances.valueAtLoc(k);
					Document doc = indexSearcher.doc(docId);
					String content = doc.get(IndexFieldName.CONTENT);

					StringBuffer sb = new StringBuffer();

					sb.append(String.format("DocId:\t%d\n", docId));
					sb.append(String.format("Relevance:\t%d\n", (int) relevance));
					sb.append(String.format("Content:\n%s\n", content));

					writer.write(sb.toString() + "\n");
				}

				writer.write("<-----------Query End------------>\n");
			}
			writer.close();
		}

	}

}
