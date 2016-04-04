package ohs.ir.medical.general;

import java.util.ArrayList;
import java.util.List;

import ohs.io.TextFileReader;
import ohs.ir.medical.query.BaseQuery;
import ohs.matrix.SparseVector;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.CounterMap;

public class DocumentIdMapper {

	public static CounterMap<String, String> mapDocIdsToIndexIds(CounterMap<String, String> resultData, BidMap<String, String> docIdMap) {
		CounterMap<String, String> ret = new CounterMap<String, String>();
		for (String queryId : resultData.keySet()) {
			Counter<String> indexScores = resultData.getCounter(queryId);
			for (String docId : indexScores.keySet()) {
				double score = indexScores.getCount(docId);
				String indexId = docIdMap.getKey(docId);
				if (indexId != null) {
					ret.setCount(queryId, indexId, score);
				} else {
					System.out.printf("index id for doc id [%s] is not found.\n", docId);
				}
			}
		}
		return ret;
	}

	public static List<SparseVector> mapDocIdsToIndexIds(List<BaseQuery> baseQueries, CounterMap<String, String> relevanceData,
			BidMap<String, String> docIdMap) {
		List<SparseVector> ret = new ArrayList<SparseVector>();
		for (BaseQuery bq : baseQueries) {
			String queryId = bq.getId();
			Counter<String> docScores = relevanceData.getCounter(queryId);
			Counter<Integer> counter = new Counter<Integer>();

			for (String docId : docScores.keySet()) {
				double score = docScores.getCount(docId);
				String indexId = docIdMap.getKey(docId);

				if (indexId == null) {
					System.out.printf("index-id is not found for doc-id [%s]\n", docId);
					continue;
				}
				counter.setCount(Integer.parseInt(indexId), score);
			}
			SparseVector sv = new SparseVector(counter, -1, -1);
			ret.add(sv);
		}

		return ret;
	}

	public static CounterMap<String, String> mapIndexIdsToDocIds(CounterMap<String, String> resultData, BidMap<String, String> docIdMap) {
		CounterMap<String, String> ret = new CounterMap<String, String>();
		for (String queryId : resultData.keySet()) {
			Counter<String> indexScores = resultData.getCounter(queryId);
			for (String indexId : indexScores.keySet()) {
				double score = indexScores.getCount(indexId);
				String docId = docIdMap.getValue(indexId);
				if (docId != null) {
					ret.setCount(queryId, docId, score);
				} else {
					System.out.printf("doc id for index id [%s] is not found.\n", indexId);
				}
			}
		}
		return ret;
	}

	public static BidMap<String, String> readDocumentIdMap(String fileName) {
		BidMap<String, String> ret = new BidMap<String, String>();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");
			ret.put(parts[0], parts[1]);
		}
		reader.close();
		System.out.printf("read [%d] doc-id pairs at [%s].\n", ret.size(), fileName);
		return ret;
	}
}
