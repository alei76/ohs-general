package ohs.ir.medical.trec.cds_2014;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;
import org.springframework.ui.Model;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.medical.general.SearcherUtils;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

public class QueryClassifierTrainer {

	private static List<SparseVector> getDocumentWordCounts(

	IndexReader indexReader, Indexer<String> wordIndexer, List<Integer> indexIds, List<Integer> typeIds) throws Exception {

		CounterMap<Integer, Integer> docWordCounts = new CounterMap<Integer, Integer>();

		List<SparseVector> svs = new ArrayList<SparseVector>();

		for (int j = 0; j < indexIds.size(); j++) {
			int indexId = indexIds.get(j);
			int typeId = typeIds.get(j);

			Terms termVector = indexReader.getTermVector(indexId, IndexFieldName.CONTENT);

			if (termVector == null) {
				continue;
			}

			TermsEnum reuse = null;
			TermsEnum iterator = termVector.iterator();
			BytesRef ref = null;
			DocsAndPositionsEnum docsAndPositions = null;
			Counter<Integer> counter = new Counter<Integer>();

			while ((ref = iterator.next()) != null) {
				docsAndPositions = iterator.docsAndPositions(null, docsAndPositions);
				if (docsAndPositions.nextDoc() != 0) {
					throw new AssertionError();
				}
				String word = ref.utf8ToString();
				int w = wordIndexer.indexOf(word);

				if (w < 0) {
					continue;
				}

				int freq = docsAndPositions.freq();
				counter.incrementCount(w, freq);
			}

			SparseVector sv = VectorUtils.toSparseVector(counter);
			sv.setLabel(typeId);
			sv.setDim(wordIndexer.size());
			svs.add(sv);
		}

		return svs;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		QueryClassifierTrainer trainer = new QueryClassifierTrainer();
		trainer.train();

		System.out.println("process ends.");
	}

	private Indexer<String> labelIndexer;

	private Indexer<String> featureIndexer;

	private List<SparseVector> trainData;

	private Model model;

	private void chooseDocumentTypes() throws Exception {
		System.out.println("choose document types.");
		CounterMap<String, String> queryDocMap = new CounterMap<String, String>();

		TextFileReader reader = new TextFileReader(new File(CDSPath.OUTPUT_INITIAL_SEARCH_RESULT_FILE));
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}
			String[] parts = reader.next().split("\t");
			String qId = parts[0];
			String docId = parts[1];
			double score = Double.parseDouble(parts[2]);
			queryDocMap.setCount(qId, docId, score);
		}
		reader.close();

		Set<String> docSet = new TreeSet<String>();

		for (String queryId : queryDocMap.keySet()) {
			Counter<String> docScores = queryDocMap.getCounter(queryId);
			docScores.keepTopNKeys(200);

			for (String docId : docScores.keySet()) {
				docSet.add(docId);
			}
		}

		Map<String, CDSQuery> queryMap = new TreeMap<String, CDSQuery>();

		for (CDSQuery cdsQuery : CDSQuery.read(CDSPath.TEST_QUERY_FILE)) {
			queryMap.put(cdsQuery.getId(), cdsQuery);
		}

		CounterMap<String, String> docTypeMap = new CounterMap<String, String>();

		for (String queryId : queryDocMap.keySet()) {
			String type = queryMap.get(queryId).getType();
			Counter<String> docScores = queryDocMap.getCounter(queryId);
			for (String docId : docScores.keySet()) {
				double score = docScores.getCount(docId);
				docTypeMap.setCount(docId, type, 1);
			}
		}

		Counter<String> docTypeCounts = new Counter<String>();
		Map<String, String> docTypes = new TreeMap<String, String>();

		for (String docId : docTypeMap.keySet()) {
			Counter<String> typeCounts = docTypeMap.getCounter(docId);
			if (typeCounts.size() > 1) {
				Iterator<String> iter = typeCounts.keySet().iterator();
				while (iter.hasNext()) {
					iter.next();
					iter.remove();
				}
				typeCounts.setCount("other", 1);
				docTypes.put(docId, "other");
			} else {
				docTypes.put(docId, typeCounts.getSortedKeys().get(0));
			}
		}

		for (String docId : docTypeMap.keySet()) {
			List<String> types = docTypeMap.getCounter(docId).getSortedKeys();
			docTypeCounts.incrementCount(types.get(0), 1);
		}

		System.out.printf("doc size:\t%d\n", docSet.size());
		System.out.println(docTypeCounts.toString());

		{
			StringBuffer sb = new StringBuffer();
			sb.append("IndexId\tType");

			for (String docId : docTypes.keySet()) {
				String type = docTypes.get(docId);
				sb.append(String.format("\n%s\t%s", docId, type));
			}

			FileUtils.write(CDSPath.OUTPUT_DOC_TYPE_FILE, sb.toString());
		}
	}

	private void generateTrainData() throws Exception {
		System.out.println("generate training data.");

		IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(CDSPath.INDEX_DIR);

		File vocDir = new File(CDSPath.VOCABULARY_DIR);

		if (!vocDir.exists()) {
			VocabularyData.make(indexSearcher.getIndexReader());
		}

		VocabularyData vocData = VocabularyData.read(vocDir);

		Indexer<String> wordIndexer = vocData.getWordIndexer();
		Indexer<String> typeIndexer = new Indexer<String>();

		List<Integer> indexIds = new ArrayList<Integer>();
		List<Integer> typeIds = new ArrayList<Integer>();

		Counter<String> typeCounts = new Counter<String>();

		TextFileReader reader = new TextFileReader(new File(CDSPath.OUTPUT_DOC_TYPE_FILE));
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = reader.next().split("\t");
			String type = parts[1];

			// if (type.equals("other")) {
			// continue;
			// }

			int docId = Integer.parseInt(parts[0]);
			int typeId = typeIndexer.getIndex(type);

			indexIds.add(docId);
			typeIds.add(typeId);

			typeCounts.incrementCount(type, 1);
		}
		reader.close();

		System.out.println(typeCounts.toString());

		List<SparseVector> docs = getDocumentWordCounts(indexSearcher.getIndexReader(), wordIndexer, indexIds, typeIds);
		SparseVector termDocFreqs = vocData.getDocumentFrequencies();

		for (int i = 0; i < docs.size(); i++) {
			SparseVector doc = docs.get(i);
			double norm = 0;
			for (int j = 0; j < doc.size(); j++) {
				int termId = doc.indexAtLoc(j);
				double tf = doc.valueAtLoc(j);

				if (tf > 0) {
					tf = Math.log(tf) + 1;
				}

				double docFreq = termDocFreqs.valueAlways(termId);
				double numDocs = indexSearcher.getIndexReader().maxDoc();
				// double tf = 1 + (count == 0 ? 0 : Math.log(count));
				double idf = docFreq == 0 ? 0 : Math.log((numDocs + 1) / docFreq);
				double tfidf = tf * idf;
				doc.setAtLoc(j, tfidf);
				norm += tfidf * tfidf;
			}
			doc.scale(1f / norm);
		}

		for (int i = 0; i < 5; i++) {
			Collections.shuffle(docs);
		}

		this.labelIndexer = typeIndexer;
		this.featureIndexer = wordIndexer;
		this.trainData = docs;
	}

	private Parameter getSVMParamter() {
		Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC_DUAL, 1, Double.POSITIVE_INFINITY, 0.1);

		if (param.getEps() == Double.POSITIVE_INFINITY) {
			switch (param.getSolverType()) {
			case L2R_LR:
			case L2R_L2LOSS_SVC:
				param.setEps(0.01);
				break;
			case L2R_L2LOSS_SVR:
				param.setEps(0.001);
				break;
			case L2R_L2LOSS_SVC_DUAL:
			case L2R_L1LOSS_SVC_DUAL:
			case MCSVM_CS:
			case L2R_LR_DUAL:
				param.setEps(0.1);
				break;
			case L1R_L2LOSS_SVC:
			case L1R_LR:
				param.setEps(0.01);
				break;
			case L2R_L1LOSS_SVR_DUAL:
			case L2R_L2LOSS_SVR_DUAL:
				param.setEps(0.1);
				break;
			default:
				throw new IllegalStateException("unknown solver type: " + param.getSolverType());
			}
		}

		return param;
	}

	public void train() throws Exception {
		System.out.println("train.");

		chooseDocumentTypes();
		generateTrainData();
		trainSVMs();
		write();
	}

	private void trainSVMs() {
		System.out.println("train SVMs.");
		Problem prob = new Problem();
		prob.l = trainData.size();
		prob.n = featureIndexer.size() + 1;
		prob.y = new double[prob.l];
		prob.x = new Feature[prob.l][];
		prob.bias = -1;

		if (prob.bias >= 0) {
			prob.n++;
		}

		for (int i = 0; i < trainData.size(); i++) {
			SparseVector x = trainData.get(i);

			Feature[] input = new Feature[prob.bias > 0 ? x.size() + 1 : x.size()];

			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j) + 1;
				double value = x.valueAtLoc(j);

				assert index >= 0;

				input[j] = new FeatureNode(index + 1, value);
			}

			if (prob.bias >= 0) {
				input[input.length - 1] = new FeatureNode(prob.n, prob.bias);
			}

			prob.x[i] = input;
			prob.y[i] = x.label();
		}

		model = Linear.train(prob, getSVMParamter());
	}

	private void write() throws Exception {
		System.out.println("write model.");
		FileUtils.write(CDSPath.QUERY_CLASSIFIER_TYPE_INDEXER_FILE, labelIndexer);
		model.save(new File(CDSPath.QUERY_CLASSIFIER_MODEL_FILE));
	}
}
