package ohs.ir.medical.trec.cds_2014;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.math.ArrayMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.CounterMap;

public class ResultsAnalyzer {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ResultsAnalyzer ra = new ResultsAnalyzer();
		ra.analyze();

		System.out.println("process ends.");
	}

	public void analyze() {

		List<SparseMatrix> resultsList = new ArrayList<SparseMatrix>();

		List<String> fileNames = new ArrayList<String>();
		fileNames.add("search_results-init.txt");
		fileNames.add("search_results-init+prf.txt");
		fileNames.add("search_results-init+prf+esa.txt");

		for (int i = 0; i < fileNames.size(); i++) {
			File file = new File(CDSPath.OUTPUT_RERANKING_DIR, fileNames.get(i));
			CounterMap<Integer, Integer> queryDocScore = new CounterMap<Integer, Integer>();

			TextFileReader reader = new TextFileReader(file);
			while (reader.hasNext()) {
				if (reader.getNumLines() == 1) {
					continue;
				}
				String line = reader.next();
				String[] parts = line.split("\t");
				int queryId = Integer.parseInt(parts[0]);
				int docId = Integer.parseInt(parts[1]);
				double score = Double.parseDouble(parts[2]);
				queryDocScore.setCount(queryId, docId, score);
			}
			reader.close();

			queryDocScore.normalize();

			resultsList.add(VectorUtils.toSpasreMatrix(queryDocScore));
		}

		SparseMatrix baseline = resultsList.get(0);

		double[][] correlationsMatrix = new double[resultsList.size() - 1][];

		for (int i = 1; i < resultsList.size(); i++) {
			SparseMatrix results = resultsList.get(i);

			double[] correlations = new double[results.rowSize()];

			for (int j = 0; j < results.rowSize(); j++) {
				int qId = results.indexAtRowLoc(j);
				SparseVector docScores1 = baseline.vectorAtRowLoc(j);
				SparseVector docScores2 = results.vectorAtRowLoc(j);

				double[] ranking1 = docScores1.ranking().values();
				double[] ranking2 = docScores2.ranking().values();
				double spearmanCorrelation = ArrayMath.correlationSpearman(ranking1, ranking2);
				// double pearsonCorrelation = ArrayMath.correlationPearson(docScores1.values(), docScores2.values());

				correlations[j] = spearmanCorrelation;
			}
			correlationsMatrix[i - 1] = correlations;
		}

		TextFileWriter writer = new TextFileWriter(CDSPath.OUTPUT_CORRELATION_FILE);
		writer.write("QueryId\tCor-1\tCor-2\tCor-3\n");

		for (int j = 0; j < correlationsMatrix[0].length; j++) {
			StringBuffer sb = new StringBuffer();
			sb.append(j + 1);
			for (int i = 0; i < correlationsMatrix.length; i++) {
				sb.append(String.format("\t%text", correlationsMatrix[i][j]));
			}
			writer.write(sb.toString() + "\n");
		}

		StringBuffer sb = new StringBuffer();
		sb.append("AVG");

		for (int i = 0; i < correlationsMatrix.length; i++) {
			double avgCor = ArrayMath.mean(correlationsMatrix[i]);
			sb.append(String.format("\t%text", avgCor));
		}
		writer.write(sb.toString() + "\n");
		writer.close();
	}

}
