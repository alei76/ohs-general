package ohs.ir.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.ir.medical.clef.ehealth_2014.EHPath;
import ohs.ir.medical.clef.ehealth_2014.RelevanceJudgementReader;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.Generics;

public class PerformanceEvaluator {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		CounterMap<String, String> relevanceData = RelevanceJudgementReader.read(new File(EHPath.QUERY_2013_TEST_RELEVANCE_FILE));

		for (File file : new File(EHPath.OUTPUT_BASIC_DIR).listFiles()) {
			String retModel = FileUtils.removeExtension(file.getName());

			CounterMap<String, String> resultData = new CounterMap<String, String>();

			TextFileReader reader = new TextFileReader(file.getPath());
			while (reader.hasNext()) {
				String[] parts = reader.next().split("\t");
				String qId = parts[0];
				String docId = parts[1];
				int indexId = Integer.parseInt(parts[2]);
				double score = Double.parseDouble(parts[3]);
				resultData.incrementCount(qId, docId, score);
			}
			reader.close();

			PerformanceEvaluator eval = new PerformanceEvaluator();
			eval.evalute(resultData, relevanceData);

			System.out.printf("[%s]\n", retModel);
			System.out.printf("%s\n\n", eval.toString(true));
		}

		System.out.println("process ends.");
	}

	public static CounterMap<String, String> readSearchResults(String fileName) {
		CounterMap<String, String> ret = Generics.newCounterMap();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");
			String qid = parts[0];
			String docid = parts[1];
			double score = Double.parseDouble(parts[2]);
			ret.incrementCount(qid, docid, score);
		}
		reader.close();
		return ret;
	}

	// public static final int[] top_n_for_each_eval = { 5, 10, 20 };

	private int[] top_n_for_each_eval = { 10 };

	public List<Performance> evalute(CounterMap<String, String> resultData, CounterMap<String, String> relevData) {
		List<Performance> ret = new ArrayList<Performance>();

		for (int top_n : top_n_for_each_eval) {
			CounterMap<MetricType, String> cm = new CounterMap<MetricType, String>();

			for (String qId : resultData.keySet()) {
				Counter<String> docScores = resultData.getCounter(qId);
				Counter<String> docRels = relevData.getCounter(qId);

				List<String> docIds = docScores.getSortedKeys();

				double num_relevant_in_result = Metrics.relevantAtN(docIds, docScores.size(), docRels);
				double num_relevant_in_judgements = Metrics.relevant(docRels);
				double num_relevant_at_n = Metrics.relevantAtN(docIds, top_n, docRels);
				double num_retrieved = docScores.size();

				cm.setCount(MetricType.RETRIEVED, qId, num_retrieved);
				cm.setCount(MetricType.RELEVANT, qId, num_relevant_in_judgements);
				cm.setCount(MetricType.RELEVANT_IN_RET, qId, num_relevant_in_result);
				cm.setCount(MetricType.RELEVANT_AT, qId, num_relevant_at_n);

				double precision = Metrics.precisionAtN(docIds, top_n, docRels);
				double ap = Metrics.averagePrecisionAtN(docIds, top_n, docRels);
				double ndcg = Metrics.normalizedDiscountedCumulativeGainAtN(docIds, top_n, docRels);

				cm.setCount(MetricType.P, qId, precision);
				cm.setCount(MetricType.AP, qId, ap);
				cm.setCount(MetricType.NDCG, qId, ndcg);
			}

			Performance eval = new Performance(top_n, cm);
			ret.add(eval);
		}

		return ret;
	}

	public void setTopNs(int[] topNs) {
		this.top_n_for_each_eval = topNs;
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean showIndividuals) {
		StringBuffer ret = new StringBuffer();

		return ret.toString().trim();
	}
}
