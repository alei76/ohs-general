package ohs.ir.medical.clef.ehealth_2015;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import edu.stanford.nlp.stats.IntCounter;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.SearcherUtils;
import ohs.math.ArrayMath;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.utils.CounterUtils;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

public class ESASearcher {

	public static void doReranking(ESASearcher esaSearcher, StrCounterMap queryModels, StrCounterMap searchResult, File outputFile)
			throws Exception {
		System.out.printf("process for [%text]\n", outputFile.getName());

		List<String> queryIds = new ArrayList<String>(new TreeSet<String>(queryModels.keySet()));
		TextFileWriter writer = new TextFileWriter(outputFile.getPath());

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < queryIds.size(); i++) {
			if ((i + 1) % 10 == 0) {
				System.out.printf("\r[%d/%text, %text]", i + 1, queryIds.size(), stopWatch.stop());
			}
			String qId = queryIds.get(i);
			Counter<String> queryModel = queryModels.getCounter(qId);
			Counter<String> docScores = searchResult.getCounter(qId);
			SparseVector newDocScores = esaSearcher.score(queryModel, VectorUtils.toSparseVector(CounterUtils.toIntegerKeys(docScores)));
			newDocScores.sortByValue();

			for (int j = 0; j < newDocScores.size(); j++) {
				int docId = newDocScores.indexAtLoc(j);
				double score = newDocScores.valueAtLoc(j);
				writer.write(qId + "\t" + docId + "\t" + score + "\n");
			}
		}
		writer.close();

		System.out.printf("\r[%d/%text, %text]\n", queryIds.size(), queryIds.size(), stopWatch.stop());
	}

	public static void main(String[] args) throws Exception {

		File queryFile = new File(MIRPath.CLEF_EHEALTH_QUERY_2015_FILE);
		File esaFile = new File(MIRPath.ICD10_ESA_FILE);
		File indexDir = new File(MIRPath.CLEF_EHEALTH_INDEX_DIR);
		File searchResultDir = new File(MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_RERANK_DIR);
		File searchResultLogFile = new File(MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_LOG_DIR,
				"cbeem_1000_10_5_25_2000.0_0.5_false_false_false_0.5_false_false.txt");

		StrCounterMap queryModels = readQueryModels(searchResultLogFile, false);
		StrCounterMap expQueryModels = readQueryModels(searchResultLogFile, true);

		ESASearcher searcher = new ESASearcher(esaFile, indexDir);

		File[] files = searchResultDir.listFiles();

		boolean[] useRandomWalks = { false, true };

		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			StrCounterMap searchResult = PerformanceEvaluator.readSearchResults(file.getPath());

			for (int j = 0; j < useRandomWalks.length; j++) {
				boolean useRandomWalk = useRandomWalks[j];
				searcher.setUseRandomWalk(useRandomWalk);

				if (file.getName().contains("lm_dirichlet")) {
					doReranking(searcher, queryModels, searchResult,
							new File(searchResultDir, String.format("lmd_esa_%s_%text.txt", false, useRandomWalk)));
				} else {
					doReranking(searcher, queryModels, searchResult,
							new File(searchResultDir, String.format("cbeem_esa_%s_%text.txt", false, useRandomWalk)));
					doReranking(searcher, expQueryModels, searchResult,
							new File(searchResultDir, String.format("cbeem_esa_%s_%text.txt", true, useRandomWalk)));
				}
			}
		}
	}

	public static StrCounterMap readQueryModels(File searchResultLogFile, boolean readExpQueryModel) {

		StrCounterMap ret = new StrCounterMap();

		String qId = null;
		TextFileReader reader = new TextFileReader(searchResultLogFile);
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			if (lines.get(0).startsWith("QID")) {
				String[] parts = lines.get(0).split("\t");
				qId = parts[1];
			} else {
				String[] parts = readExpQueryModel ? lines.get(1).split("\t") : lines.get(0).split("\t");
				String s = parts[1];
				s = s.substring(1, s.length() - 1);
				for (String split : s.split(" ")) {
					String[] two = StrUtils.split2Two(":", split);
					ret.setCount(qId, two[0], Double.parseDouble(two[1]));
				}
			}
		}

		return ret;
	}

	private IndexSearcher indexSearcher;

	private SparseVector collWordCounts;

	private SparseVector docScores;

	private Counter<String> queryModel;

	private StrIndexer wordIndexer;

	private SparseMatrix wordConceptWeights;

	private StrIndexer icdWordIndexer;

	private double cnt_sum_in_coll;

	private double num_docs_in_coll;

	private SparseMatrix docWordWeightData;

	private SparseVector wordDocFreqs;

	private SparseVector qm;

	private StrIndexer conceptIndexer;

	private boolean useRandomWalk = false;

	public ESASearcher(File esaFile, File indexDir) throws Exception {
		readESAModel(esaFile);

		indexSearcher = SearcherUtils.getIndexSearcher(indexDir.getPath());
		cnt_sum_in_coll = indexSearcher.getIndexReader().getSumTotalTermFreq(CommonFieldNames.CONTENT);
		num_docs_in_coll = indexSearcher.getIndexReader().maxDoc();
	}

	private void computeDocumentWordWeights() throws Exception {
		IndexReader indexReader = indexSearcher.getIndexReader();
		IntCounterMap c1 = new IntCounterMap();
		IntCounter c2 = new IntCounter();
		IntCounter c3 = new IntCounter();

		for (int j = 0; j < docScores.size(); j++) {
			int docId = docScores.indexAtLoc(j);
			double score = docScores.valueAtLoc(j);
			Document doc = indexReader.document(docId);

			Terms termVector = indexReader.getTermVector(docId, CommonFieldNames.CONTENT);

			if (termVector == null) {
				continue;
			}

			TermsEnum reuse = null;
			TermsEnum iterator = termVector.iterator();
			BytesRef ref = null;
			DocsAndPositionsEnum docsAndPositions = null;
			IntCounter wordCounts = new IntCounter();
			List<Integer> words = new ArrayList<Integer>();

			while ((ref = iterator.next()) != null) {
				docsAndPositions = iterator.docsAndPositions(null, docsAndPositions);
				if (docsAndPositions.nextDoc() != 0) {
					throw new AssertionError();
				}

				String word = ref.utf8ToString();
				int w = wordIndexer.getIndex(word);
				int freq = docsAndPositions.freq();
				wordCounts.incrementCount(w, freq);

				for (int k = 0; k < freq; k++) {
					final int position = docsAndPositions.nextPosition();
					words.add(w);
				}
			}
			c1.setCounter(docId, wordCounts);
		}

		for (int w = 0; w < wordIndexer.size(); w++) {
			String word = wordIndexer.getObject(w);
			Term termInstance = new Term(CommonFieldNames.CONTENT, word);
			double cnt = indexReader.totalTermFreq(termInstance);
			double df = indexReader.docFreq(termInstance);
			c2.setCount(w, cnt);
			c3.setCount(w, df);
		}

		docWordWeightData = VectorUtils.toSpasreMatrix(c1);
		collWordCounts = VectorUtils.toSparseVector(c2);
		wordDocFreqs = VectorUtils.toSparseVector(c3);

		for (int i = 0; i < docWordWeightData.rowSize(); i++) {
			SparseVector wordCounts = docWordWeightData.vectorAtRowLoc(i);
			for (int j = 0; j < wordCounts.size(); j++) {
				int w = wordCounts.indexAtLoc(j);
				double cnt = wordCounts.valueAtLoc(j);
				double tf = 1 + Math.log(cnt);
				double df = wordDocFreqs.valueAlways(w);
				double idf = Math.log((num_docs_in_coll + 1) / df);
				double tfidf = tf * idf;
				wordCounts.setAtLoc(j, tfidf);
			}
			wordCounts.normalizeByL2Norm();
		}

	}

	private SparseVector getQueryWordWeights() {
		SparseVector ret = qm.copy();

		for (int i = 0; i < ret.size(); i++) {
			int w = ret.indexAtLoc(i);
			double tf = ret.valueAtLoc(i);
			double df = wordDocFreqs.valueAlways(w);
			double idf = Math.log((num_docs_in_coll + 1) / df);
			double tfidf = tf * idf;
			ret.setAtLoc(i, tfidf);
		}
		ret.normalizeByL2Norm();
		return ret;
	}

	public void readESAModel(File esaFile) {
		icdWordIndexer = new StrIndexer();
		conceptIndexer = new StrIndexer();

		IntCounterMap map = new IntCounterMap();

		TextFileReader reader = new TextFileReader(esaFile);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");

			int num_concepts = parts.length - 1;

			if (num_concepts < 5) {
				continue;
			}

			IntCounter c = new IntCounter();
			String word = null;
			int w = 0;

			for (int i = 0; i < parts.length; i++) {
				String[] two = StrUtils.split2Two(":", parts[i]);
				if (i == 0) {
					word = two[0];
					w = icdWordIndexer.getIndex(word);
				} else {
					String concept = two[0];
					int cpt = conceptIndexer.getIndex(concept);
					double weight = Double.parseDouble(two[1]);
					c.setCount(cpt, weight);
				}
			}
			map.setCounter(w, c);

		}
		reader.close();

		wordConceptWeights = VectorUtils.toSpasreMatrix(map);

	}

	public SparseVector score(Counter<String> queryModel, SparseVector docScores) throws Exception {
		this.queryModel = queryModel;
		this.docScores = docScores;

		setupQuery();

		computeDocumentWordWeights();

		SparseVector queryWordWeights = getQueryWordWeights();
		SparseVector queryConceptWeights = null;

		{
			IntCounter c = new IntCounter();

			for (int i = 0; i < queryWordWeights.size(); i++) {
				int q = queryWordWeights.indexAtLoc(i);
				double q_weight = queryWordWeights.valueAtLoc(i);
				SparseVector conceptWeights = wordConceptWeights.rowAlways(q);

				for (int j = 0; j < conceptWeights.size(); j++) {
					int cId = conceptWeights.indexAtLoc(j);
					double c_weight = conceptWeights.valueAtLoc(j);
					c.incrementCount(cId, q_weight * c_weight);
				}
			}
			queryConceptWeights = VectorUtils.toSparseVector(c);
			queryConceptWeights.scale(1f / queryWordWeights.size());
		}

		Map<Integer, SparseVector> map = new HashMap<Integer, SparseVector>();

		for (int i = 0; i < docScores.size(); i++) {
			int docId = docScores.indexAtLoc(i);
			SparseVector docWordWeights = docWordWeightData.rowAlways(docId);
			IntCounter cc = new IntCounter();

			for (int j = 0; j < docWordWeights.size(); j++) {
				int w = docWordWeights.indexAtLoc(j);
				double w_weight = docWordWeights.valueAlways(w);

				SparseVector conceptWeights = wordConceptWeights.rowAlways(w);

				for (int k = 0; k < conceptWeights.size(); k++) {
					int cId = conceptWeights.indexAtLoc(k);
					double c_weight = conceptWeights.valueAtLoc(k);
					cc.incrementCount(cId, w_weight * c_weight);
				}
			}

			SparseVector docConceptWeights = VectorUtils.toSparseVector(cc);
			docConceptWeights.scale(1f / docWordWeights.size());
			map.put(docId, docConceptWeights);
		}

		SparseMatrix docConceptWeightData = new SparseMatrix(map);
		SparseVector ret = docScores.copy();

		if (useRandomWalk) {
			CentralityEstimator estimator = new CentralityEstimator();
			SparseVector docCents = estimator.estimate(queryConceptWeights, docConceptWeightData);
			ArrayMath.multiply(ret.values(), docCents.values(), ret.values());
		} else {
			SparseVector docCosines = new SparseVector(docScores.size());
			for (int i = 0; i < docConceptWeightData.rowSize(); i++) {
				int docId = docConceptWeightData.indexAtRowLoc(i);
				SparseVector docConceptWeights = docConceptWeightData.vectorAtRowLoc(i);
				double cosine = VectorMath.cosine(queryConceptWeights, docConceptWeights, false);
				docCosines.incrementAtLoc(i, docId, cosine);
			}

			ArrayMath.multiply(ret.values(), docCosines.values(), ret.values());
		}
		ret.normalizeAfterSummation();
		return ret;
	}

	private void setupQuery() {
		wordIndexer = new StrIndexer();

		for (int i = 0; i < icdWordIndexer.size(); i++) {
			wordIndexer.add(icdWordIndexer.getObject(i));
		}

		IntCounter cc = new IntCounter();

		for (String word : queryModel.keySet()) {
			double prob = queryModel.getCount(word);
			int w = wordIndexer.getIndex(word);
			cc.incrementCount(w, prob);
		}

		qm = VectorUtils.toSparseVector(cc);

	}

	public void setUseRandomWalk(boolean useRandomWalk) {
		this.useRandomWalk = useRandomWalk;
	}
}
