package ohs.ir.medical.trec.cds_2015;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.eval.Performance;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.DocumentIdMapper;
import ohs.ir.medical.general.HyperParameter;
import ohs.ir.medical.general.KLDivergenceScorer;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.RelevanceModelBuilder;
import ohs.ir.medical.general.SearcherUtils;
import ohs.ir.medical.general.WordCountBox;
import ohs.ir.medical.query.BaseQuery;
import ohs.ir.medical.query.QueryReader;
import ohs.ir.medical.query.RelevanceReader;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseVector;
import ohs.matrix.Vector;
import ohs.types.Indexer;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class TrecSearcher {

	public static void analyze() {
		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_PERFORMANCE_FILE);
		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_PERFORMANCE_COMPACT_FILE);

		writer.write("ModelName\tFbIters\tFbMix\tTitleMix\tAbsMix\tContentMix\tFbDocs\tFbWords\tP\tMap\tNDCG\n");

		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();

			File file = new File(lines.get(0));
			String fileName = file.getName();
			fileName = FileUtils.removeExtension(fileName);
			fileName = fileName.replace("kld_fb", "kld-fb");

			int num_fb_iters = 0;
			double mixture_for_fb_model = 0;
			double[] mixtures_for_field_rms = new double[] { 0, 0, 0 };
			int num_fb_docs = 0;
			int num_fb_words = 0;

			String modelName = fileName;

			if (fileName.equals("qld") || fileName.equals("kld") || fileName.equals("cbeem")) {

			} else {
				String[] toks = fileName.split("_");
				modelName = toks[0];
				num_fb_iters = Integer.parseInt(toks[1]);
				mixture_for_fb_model = Double.parseDouble(toks[2]);
				mixtures_for_field_rms[0] = Double.parseDouble(toks[3]);
				mixtures_for_field_rms[1] = Double.parseDouble(toks[4]);
				mixtures_for_field_rms[2] = Double.parseDouble(toks[5]);
				num_fb_docs = Integer.parseInt(toks[6]);
				num_fb_words = Integer.parseInt(toks[7]);
			}

			int num_relevant_all = Integer.parseInt(lines.get(3).split("\t")[1]);
			int num_retrieved_all = Integer.parseInt(lines.get(4).split("\t")[1]);
			int num_relevant_all_in_retrieved_all = Integer.parseInt(lines.get(5).split("\t")[1]);
			int num_relevant_at = Integer.parseInt(lines.get(6).split("\t")[1]);
			double p = Double.parseDouble(lines.get(7).split("\t")[1]);
			double map = Double.parseDouble(lines.get(8).split("\t")[1]);
			double ndcg = Double.parseDouble(lines.get(9).split("\t")[1]);

			String output = String.format("%text\t%d\t%f\t%f\t%f\t%f\t%d\t%d\t%f\t%f\t%f",

					modelName, num_fb_iters, mixture_for_fb_model, mixtures_for_field_rms[0], mixtures_for_field_rms[1],
					mixtures_for_field_rms[2],

					num_fb_docs, num_fb_words,

					p, map, ndcg);

			writer.write(output + "\n");

		}
		reader.close();
		writer.close();
	}

	public static void evalute() throws Exception {
		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOC_ID_MAP_FILE);
		StrCounterMap relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);

		List<File> files = FileUtils.getFilesUnder(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR);

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);

			if (file.getName().contains("_log")) {
				continue;
			}

			StrCounterMap res = PerformanceEvaluator.readSearchResults(file.getPath());
			StrCounterMap resultData = DocumentIdMapper.mapIndexIdsToDocIds(res, docIdMap);

			PerformanceEvaluator eval = new PerformanceEvaluator();
			eval.setTopNs(new int[] { 10 });
			List<Performance> perfs = eval.evalute(resultData, relevanceData);

			System.out.println(file.getPath());
			sb.append(file.getPath());
			for (int j = 0; j < perfs.size(); j++) {
				sb.append("\n" + perfs.get(j).toString());
			}
			sb.append("\n\n");
		}

		FileUtils.write(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_PERFORMANCE_FILE, sb.toString().trim());
	}

	public static void format() throws Exception {
		List<File> files = FileUtils.getFilesUnder(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR);
		StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOC_ID_MAP_FILE);

		for (File file : files) {
			String fileName = file.getName();
			fileName = FileUtils.removeExtension(fileName);

			if (fileName.equals("qld") || fileName.equals("cbeem") || fileName.equals("kld_fb_1_0.5_50_30_20_15_10")) {
				if (fileName.startsWith("kld_fb")) {
					fileName = "kld_fb";
				} else {

				}
			} else {
				continue;
			}

			String runId = "";

			if (fileName.equals("qld")) {
				runId = "KISTI001B";
			} else if (fileName.equals("cbeem")) {
				runId = "KISTI002B";
			} else if (fileName.equals("kld_fb")) {
				runId = "KISTI003B";
			}

			StrCounterMap cm = PerformanceEvaluator.readSearchResults(file.getPath());

			cm = DocumentIdMapper.mapIndexIdsToDocIds(cm, docIdMap);

			List<Integer> qIds = new ArrayList<Integer>();

			for (String qId : cm.keySet()) {
				qIds.add(Integer.parseInt(qId));
			}

			Collections.sort(qIds);

			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < qIds.size(); i++) {
				String qId = qIds.get(i) + "";

				StrCounter docScores = (StrCounter) cm.getCounter(qId);
				List<String> docIds = docScores.getSortedKeys();

				for (int j = 0; j < docIds.size(); j++) {
					String docId = docIds.get(j);
					double score = docScores.getCount(docId);
					int rank = j + 1;

					sb.append(String.format("%text\t%text\t%text\t%d\t%text\t%text\n", qId, "Q0", docId, rank, score, runId));
				}

			}

			String outputFileName = String.format("%text.txt", runId);
			File outputFile = new File(MIRPath.TREC_CDS_OUTPUT_DIR + "/result-2015-submit", outputFileName);
			FileUtils.write(outputFile.getPath(), sb.toString().trim());

		}

	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		TrecSearcher tc = new TrecSearcher();
		// tc.searchByQLD();
		// tc.searchByKLD();
		// tc.searchByKLDFB();
		tc.searchByCBEEM();
		// tc.searchByKLDPLM();
		// tc.searchByKLDPassage();
		// tc.searchByKLDProximityFB();
		// tc.searchByKLDMultiFieldFB();
		// tc.searchByKLDMultiFieldsProximityFB();
		// tc.searchByCBEEM();
		// evalute();
		// analyze();
		// format();

		System.out.println("process ends.");
	}

	private List<BaseQuery> bqs;

	private IndexSearcher indexSearcher;

	private IndexReader indexReader;

	private Analyzer analyzer = MedicalEnglishAnalyzer.newAnalyzer();

	public TrecSearcher() throws Exception {
		bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		indexSearcher = SearcherUtils.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);

		indexReader = indexSearcher.getIndexReader();
	}

	public void run(int num_fb_iters, double mixture_for_fb_model, double[] mixtures_for_field_rms, int num_fb_docs, int num_fb_words)
			throws Exception {
		IndexSearcher wikiIndexSearcher = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
		String outputFileName = null;

		{
			StringBuffer sb = new StringBuffer("kld_fb");
			sb.append(String.format("_%d", num_fb_iters));
			sb.append(String.format("_%text", mixture_for_fb_model + ""));

			for (int i = 0; i < mixtures_for_field_rms.length; i++) {
				sb.append(String.format("_%d", (int) mixtures_for_field_rms[i]));
			}

			sb.append(String.format("_%d", num_fb_docs));
			sb.append(String.format("_%d", num_fb_words));
			sb.append(".txt");
			outputFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + sb.toString();
		}

		System.out.println(outputFileName);

		mixtures_for_field_rms = ArrayUtils.copy(mixtures_for_field_rms);
		ArrayMath.normalize(mixtures_for_field_rms);

		TextFileWriter writer = new TextFileWriter(outputFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			Indexer<String> wordIndexer = new Indexer<String>();
			StringBuffer qBuf = new StringBuffer(bq.getSearchText());
			StrCounter qWordCounts = AnalyzerUtils.getWordCounts(qBuf.toString(), analyzer);

			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			SparseVector expQueryModel = queryModel.copy();
			SparseVector docScores = null;

			for (int j = 0; j < 1; j++) {
				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
				docScores = SearcherUtils.search(lbq, indexSearcher, 1000);

				// SparseVector wikiScores = SearcherUtils.search(lbq, wikiIndexSearcher, 50);

				WordCountBox wcb1 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.TITLE);
				WordCountBox wcb2 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.ABSTRACT);
				WordCountBox wcb3 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.CONTENT);
				// WordCountBox wcb4 = WordCountBox.getWordCountBox(wikiIndexSearcher.getIndexReader(), wikiScores, wordIndexer,
				// CommonFieldNames.CONTENT);

				// KLDivergenceScorer kldScorer = new KLDivergenceScorer();
				// docScores = kldScorer.scoreDocuments(wcb3, expQueryModel);

				RelevanceModelBuilder rmb = new RelevanceModelBuilder(num_fb_docs, num_fb_words, 2000);
				SparseVector rm1 = rmb.getRelevanceModel(wcb1, docScores);
				SparseVector rm2 = rmb.getRelevanceModel(wcb2, docScores);
				SparseVector rm3 = rmb.getRelevanceModel(wcb3, docScores);
				// SparseVector rm4 = rmb.getRelevanceModel(wcb4, wikiScores);

				SparseVector rm = VectorMath.addAfterScale(new Vector[] { rm1, rm2, rm3 }, mixtures_for_field_rms);
				rm.removeZeros();
				rm.normalize();

				expQueryModel = VectorMath.addAfterScale(queryModel, rm, 1 - mixture_for_fb_model, mixture_for_fb_model);
			}

			BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
			docScores = SearcherUtils.search(lbq, indexSearcher, 1000);

			WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.CONTENT);

			KLDivergenceScorer kldScorer = new KLDivergenceScorer();
			docScores = kldScorer.score(wcb, expQueryModel);

			// System.out.println(bq);
			// System.out.printf("QM1:\t%text\n", VectorUtils.toCounter(queryModel, wordIndexer));
			// System.out.printf("QM2:\t%text\n", VectorUtils.toCounter(expQueryModel, wordIndexer));
			// System.out.printf("RM1:\t%text\n", VectorUtils.toCounter(rm1, wordIndexer));
			// System.out.printf("RM2:\t%text\n", VectorUtils.toCounter(rm2, wordIndexer));
			// System.out.printf("RM3:\t%text\n", VectorUtils.toCounter(rm3, wordIndexer));
			// System.out.printf("RM4:\t%text\n", VectorUtils.toCounter(rm4, wordIndexer));
			// System.out.printf("RM:\t%text\n", VectorUtils.toCounter(rm, wordIndexer));
			// System.out.println();

			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByCBEEM() throws Exception {
		System.out.println("search by CBEEM.");

		// String[] indexDirNames = { MIRPath.TREC_CDS_INDEX_DIR, MIRPath.CLEF_EHEALTH_INDEX_DIR, MIRPath.OHSUMED_INDEX_DIR };

		String[] indexDirNames = { MIRPath.TREC_CDS_INDEX_DIR, MIRPath.CLEF_EHEALTH_INDEX_DIR, MIRPath.OHSUMED_INDEX_DIR,
				MIRPath.TREC_GENOMICS_INDEX_DIR };

		String[] docPriorFileNames = MIRPath.DocPriorFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		IndexSearcher[] indexSearchers = SearcherUtils.getIndexSearchers(indexDirNames);

		DenseVector[] docPriorData = new DenseVector[indexSearchers.length];

		for (int i = 0; i < indexDirNames.length; i++) {
			File inputFile = new File(docPriorFileNames[i]);
			DenseVector docPriors = null;
			if (inputFile.exists()) {
				docPriors = DenseVector.read(inputFile.getPath());
				double uniform_prior = 1f / docPriors.size();
				for (int j = 0; j < docPriors.size(); j++) {
					if (docPriors.value(j) == 0) {
						docPriors.set(j, uniform_prior);
					}
				}
			} else {
				docPriors = new DenseVector(indexSearchers[i].getIndexReader().maxDoc());
				double uniform_prior = 1f / docPriors.size();
				docPriors.setAll(uniform_prior);
			}
			docPriorData[i] = docPriors;
		}

		HyperParameter hyperParameter = new HyperParameter();
		hyperParameter.setTopK(1000);
		hyperParameter.setMixtureForAllCollections(0.5);
		// hyperParameter.setNumFBDocs(10);
		// hyperParameter.setNumFBWords(10);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + String.format("cbeem.txt");
		// String logFileName = logDirName
		// + String.format("cbeem_%text.txt", hyperParameter.toString(true));

		System.out.printf("process for [%text].\n", resultFileName);

		TrecCbeemDocumentSearcher ds = new TrecCbeemDocumentSearcher(indexSearchers, docPriorData, hyperParameter, analyzer, false);
		ds.search(0, bqs, null, resultFileName, null);

	}

	public void searchByKLD() throws Exception {
		System.out.println("search by KLD.");

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			System.out.println(bq);

			StrCounter qWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);

			SparseVector docScores = SearcherUtils.search(lbq, indexSearcher, 1000);
			docScores.normalizeAfterSummation();

			Indexer<String> wordIndexer = new Indexer<String>();
			SparseVector qLM = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			qLM.normalize();

			WordCountBox wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.score(wcb, qLM);

			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDFB() throws Exception {
		System.out.println("search by KLD FB.");

		IndexSearcher wikiIndexSearcher = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_fb.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			Indexer<String> wordIndexer = new Indexer<String>();
			StringBuffer qBuf = new StringBuffer(bq.getSearchText());
			StrCounter qWordCounts = AnalyzerUtils.getWordCounts(qBuf.toString(), analyzer);

			SparseVector qLM = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			qLM.normalize();

			SparseVector expQLM = qLM.copy();
			SparseVector docScores = null;

			BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQLM, wordIndexer));
			docScores = SearcherUtils.search(lbq, indexSearcher, 1000);

			WordCountBox wcb3 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.CONTENT);

			RelevanceModelBuilder rmb = new RelevanceModelBuilder(10, 15, 20);
			SparseVector rm = rmb.getRelevanceModel(wcb3, docScores);
			// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3, docScores);

			double mixture = 0.5;

			expQLM = VectorMath.addAfterScale(qLM, rm, 1 - mixture, mixture);

			lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQLM, wordIndexer));
			docScores = SearcherUtils.search(lbq, indexSearcher, 1000);

			WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.CONTENT);

			KLDivergenceScorer kldScorer = new KLDivergenceScorer();
			docScores = kldScorer.score(wcb, expQLM);

			System.out.println(bq);
			System.out.printf("QM1:\t%text\n", VectorUtils.toCounter(qLM, wordIndexer));
			System.out.printf("QM2:\t%text\n", VectorUtils.toCounter(expQLM, wordIndexer));

			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDMultiFieldFB() throws Exception {
		System.out.println("search by KLD Multi-fields FB.");

		// double[][] mixture_for_field_rms = { { 0, 0, 100 }, { 0, 100, 0 }, { 100, 0, 0 }, { 50, 50, 50 }, { 50, 30, 20 }, { 20, 30, 50 }
		// };
		// int[] num_fb_iters = { 1, 2 };
		// double[] mixture_for_fb_model = { 0.5 };
		// int[] num_fb_docs = { 5, 10, 15 };
		// int[] num_fb_words = { 10, 15, 20 };

		double[][] mixture_for_field_rms = { { 50, 30, 20 } };
		int[] num_fb_iters = { 1 };
		double[] mixture_for_fb_model = { 0.5 };
		int[] num_fb_docs = { 15 };
		int[] num_fb_words = { 10 };

		for (int l1 = 0; l1 < num_fb_iters.length; l1++) {
			for (int l2 = 0; l2 < mixture_for_fb_model.length; l2++) {
				for (int l3 = 0; l3 < mixture_for_field_rms.length; l3++) {
					for (int l4 = 0; l4 < num_fb_docs.length; l4++) {
						for (int l5 = 0; l5 < num_fb_docs.length; l5++) {
							run(num_fb_iters[l1], mixture_for_fb_model[l2], mixture_for_field_rms[l3], num_fb_docs[l4], num_fb_words[l5]);
						}
					}
				}
			}
		}
	}

	public void searchByKLDMultiFieldsProximityFB() throws Exception {
		System.out.println("search by KLD Multi-Fields Proximity FB.");

		IndexSearcher wikiIndexSearcher = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_fb_proximity.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			Indexer<String> wordIndexer = new Indexer<String>();
			StringBuffer qBuf = new StringBuffer(bq.getSearchText());
			StrCounter qWordCounts = AnalyzerUtils.getWordCounts(qBuf.toString(), analyzer);

			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			SparseVector expQueryModel = queryModel.copy();
			SparseVector docScores = null;

			for (int j = 0; j < 1; j++) {
				BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
				docScores = SearcherUtils.search(lbq, indexSearcher, 1000);

				// SparseVector wikiScores = SearcherUtils.search(lbq, wikiIndexSearcher, 50);

				WordCountBox wcb1 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.TITLE);
				WordCountBox wcb2 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.ABSTRACT);
				WordCountBox wcb3 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.CONTENT);

				// WordCountBox wcb4 = WordCountBox.getWordCountBox(wikiIndexSearcher.getIndexReader(), wikiScores, wordIndexer,
				// CommonFieldNames.CONTENT);

				// KLDivergenceScorer kldScorer = new KLDivergenceScorer();
				// docScores = kldScorer.scoreDocuments(wcb3, expQueryModel);

				ProximityRelevanceModelBuilder rmb = new ProximityRelevanceModelBuilder(wordIndexer, 10, 15, 2000, 3, false);
				rmb.computeWordProximities(expQueryModel, docScores, wcb1);
				SparseVector rm1 = rmb.getRelevanceModel(wcb1, docScores);

				rmb.computeWordProximities(expQueryModel, docScores, wcb2);
				SparseVector rm2 = rmb.getRelevanceModel(wcb2, docScores);

				rmb.computeWordProximities(expQueryModel, docScores, wcb3);
				SparseVector rm3 = rmb.getRelevanceModel(wcb3, docScores);

				// SparseVector rm4 = rmb.getRelevanceModel(wcb4, wikiScores);

				double mixture = 0.5;

				double[] mixtures = { 50, 50, 50 };

				ArrayMath.normalize(mixtures);

				SparseVector rm = VectorMath.addAfterScale(new Vector[] { rm1, rm2, rm3 }, mixtures);
				rm.removeZeros();
				rm.normalize();

				expQueryModel = VectorMath.addAfterScale(queryModel, rm, 1 - mixture, mixture);
			}

			BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
			docScores = SearcherUtils.search(lbq, indexSearcher, 1000);

			WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.CONTENT);

			KLDivergenceScorer kldScorer = new KLDivergenceScorer();
			docScores = kldScorer.score(wcb, expQueryModel);

			System.out.println(bq);
			System.out.printf("QM1:\t%text\n", VectorUtils.toCounter(queryModel, wordIndexer));
			System.out.printf("QM2:\t%text\n", VectorUtils.toCounter(expQueryModel, wordIndexer));
			// System.out.printf("RM1:\t%text\n", VectorUtils.toCounter(rm1, wordIndexer));
			// System.out.printf("RM2:\t%text\n", VectorUtils.toCounter(rm2, wordIndexer));
			// System.out.printf("RM3:\t%text\n", VectorUtils.toCounter(rm3, wordIndexer));
			// System.out.printf("RM4:\t%text\n", VectorUtils.toCounter(rm4, wordIndexer));
			// System.out.printf("RM:\t%text\n", VectorUtils.toCounter(rm, wordIndexer));
			// System.out.println();

			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDPassage() throws Exception {
		System.out.println("search by KLD.");

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_passage.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			System.out.println(bq);

			StrCounter wordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);

			SparseVector docScores = SearcherUtils.search(lbq, indexSearcher, 1000);
			docScores.normalizeAfterSummation();

			Indexer<String> wordIndexer = new Indexer<String>();
			SparseVector queryModel = VectorUtils.toSparseVector(wordCounts, wordIndexer, true);
			queryModel.normalize();

			WordCountBox wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.scoreByPassages(wcb, queryModel);
			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDPLM() throws Exception {
		System.out.println("search by KLD PLM.");

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_plm.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			System.out.println(bq);

			StrCounter qWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);

			SparseVector docScores = SearcherUtils.search(lbq, indexSearcher, 1000);
			docScores.normalizeAfterSummation();

			Indexer<String> wordIndexer = new Indexer<String>();
			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			WordCountBox wcb = WordCountBox.getWordCountBox(indexSearcher.getIndexReader(), docScores, wordIndexer);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.scoreByPLMs(wcb, queryModel);
			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByKLDProximityFB() throws Exception {
		System.out.println("search by KLD Proximity FB.");

		IndexSearcher wikiIndexSearcher = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "kld_fb_proximity.txt";

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			Indexer<String> wordIndexer = new Indexer<String>();
			StringBuffer qBuf = new StringBuffer(bq.getSearchText());
			StrCounter qWordCounts = AnalyzerUtils.getWordCounts(qBuf.toString(), analyzer);

			SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
			queryModel.normalize();

			SparseVector expQueryModel = queryModel.copy();
			SparseVector docScores = null;

			BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
			docScores = SearcherUtils.search(lbq, indexSearcher, 1000);

			// SparseVector wikiScores = SearcherUtils.search(lbq, wikiIndexSearcher, 50);

			// WordCountBox wcb1 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.TITLE);
			// WordCountBox wcb2 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.ABSTRACT);
			WordCountBox wcb3 = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.CONTENT);

			ProximityRelevanceModelBuilder rmb = new ProximityRelevanceModelBuilder(wordIndexer, 10, 15, 2000, 1, false);
			rmb.computeWordProximities(expQueryModel, docScores, wcb3);
			SparseVector rm = rmb.getRelevanceModel(wcb3, docScores);

			// SparseVector rm4 = rmb.getRelevanceModel(wcb4, wikiScores);

			double mixture = 0.5;
			expQueryModel = VectorMath.addAfterScale(queryModel, rm, 1 - mixture, mixture);

			lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQueryModel, wordIndexer));
			docScores = SearcherUtils.search(lbq, indexSearcher, 1000);

			WordCountBox wcb = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer, CommonFieldNames.CONTENT);

			KLDivergenceScorer kldScorer = new KLDivergenceScorer();
			docScores = kldScorer.score(wcb, expQueryModel);

			System.out.println(bq);
			System.out.printf("QM1:\t%text\n", VectorUtils.toCounter(queryModel, wordIndexer));
			System.out.printf("QM2:\t%text\n", VectorUtils.toCounter(expQueryModel, wordIndexer));
			// System.out.println(rmb.getLogBuffer().toString());
			// System.out.printf("RM1:\t%text\n", VectorUtils.toCounter(rm1, wordIndexer));
			// System.out.printf("RM2:\t%text\n", VectorUtils.toCounter(rm2, wordIndexer));
			// System.out.printf("RM3:\t%text\n", VectorUtils.toCounter(rm3, wordIndexer));
			// System.out.printf("RM4:\t%text\n", VectorUtils.toCounter(rm4, wordIndexer));
			// System.out.printf("RM:\t%text\n", VectorUtils.toCounter(rm, wordIndexer));
			// System.out.println();

			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	public void searchByQLD() throws Exception {
		System.out.println("search by QLD.");

		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "qld.txt");

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			BooleanQuery lbq = AnalyzerUtils.getQuery(bq.getSearchText(), analyzer);
			SparseVector docScores = SearcherUtils.search(lbq, indexSearcher, 1000);
			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

}
