package ohs.ir.medical.clef.ehealth_2015;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.ir.medical.general.DocumentIdMapper;
import ohs.ir.medical.general.MIRPath;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.Generics;

public class ResultFormatter {

	public static void main(String[] args) {
		System.out.println("process begins.");
		List<File> files = FileUtils.getFilesUnder(new File(MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_RERANK_DIR));

		//

		/*
		 * run1: unigram language model with dirichlet prior
		 * 
		 * run2: unigram language model with dirichlet prior + ESAModel based on
		 * ICD-10
		 * 
		 * run3: unigram language model with dirichlet prior + ESAModel with document
		 * centrality based on ICD-10
		 * 
		 * run4: unigram language model with dirichlet prior + CBEEM
		 * 
		 * run5: unigram language model with dirichlet prior + CBEEM + ESAModel based
		 * on ICD-10
		 * 
		 * run6: unigram language model with dirichlet prior + CBEEM + ESAModel with
		 * document centrality based on ICD-10
		 * 
		 * run7: unigram language model with dirichlet prior + CBEEM + expanded
		 * query by CBEEM + ESAModel based on ICD-10
		 * 
		 * run8: unigram language model with dirichlet prior + CBEEM + +
		 * expanded query by CBEEM + ESAModel with document centrality based on
		 * ICD-10
		 */

		Map<String, String> nameMap = new HashMap<String, String>();
		nameMap.put("lm_dirichlet.txt", "KISTI_EN_RUN1.dat");
		nameMap.put("lmd_esa_false_false.txt", "KISTI_EN_RUN2.dat"); // unigram
																		// langauge
																		// model
																		// with
																		// dirichlet
																		// prior
																		// + ESAModel
																		// based
																		// on
																		// ICD-10
		nameMap.put("lmd_esa_false_true.txt", "KISTI_EN_RUN3.dat"); // unigram
																	// langauge
																	// model
																	// with
																	// dirichlet
																	// prior +
																	// ESAModel with
																	// document
																	// centrality
																	// based on
																	// ICD-10
		nameMap.put("cbeem_1000_10_5_25_2000.0_0.5_false_false_false_0.5_false_false.txt", "KISTI_EN_RUN4.dat");
		nameMap.put("cbeem_esa_false_false.txt", "KISTI_EN_RUN5.dat");
		nameMap.put("cbeem_esa_false_true.txt", "KISTI_EN_RUN6.dat");
		nameMap.put("cbeem_esa_true_false.txt", "KISTI_EN_RUN7.dat");
		nameMap.put("cbeem_esa_true_true.txt", "KISTI_EN_RUN8.dat");

		BidMap<String, String> docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.CLEF_EHEALTH_DOC_ID_MAP_FIE);

		File outputDir = new File(MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2015_DIR, "submit");

		for (int i = 0; i < files.size(); i++) {
			File inputFile = files.get(i);
			String outputFileName = nameMap.get(inputFile.getName());
			File outputFile = new File(outputDir, outputFileName);

			if (outputFileName.equals("KISTI_EN_RUN8.dat")) {
				System.out.println();
			}

			System.out.printf("%s -> %s\n", inputFile.getName(), outputFileName);
			CounterMap<String, String> searchResults = PerformanceEvaluator.readSearchResults(inputFile.getPath());
			searchResults = DocumentIdMapper.mapIndexIdsToDocIds(searchResults, docIdMap);

			writeResults(outputFile, searchResults);
		}

		System.out.println("process ends.");
	}

	private static void writeResults(File outputFile, CounterMap<String, String> resultData) {
		System.out.printf("write to [%s]\n", outputFile.getPath());
		TextFileWriter writer = new TextFileWriter(outputFile);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(20);
		nf.setGroupingUsed(false);

		CounterMap<String, String> temp = Generics.newCounterMap();

		for (String qId : resultData.keySet()) {
			Counter<String> docScores = resultData.getCounter(qId);

			if (qId.startsWith("qtest2014.")) {
				String numStr = qId.substring("qtest2014.".length());
				qId = new DecimalFormat("000").format(Integer.parseInt(numStr));
			} else if (qId.startsWith("clef2015.test.")) {
				String numStr = qId.substring("clef2015.test.".length());
				qId = new DecimalFormat("000").format(Integer.parseInt(numStr));
			} else if (qId.startsWith("qtest")) {
				String numStr = qId.substring("qtest".length());
				qId = new DecimalFormat("000").format(Integer.parseInt(numStr));
			}

			for (String docId : docScores.keySet()) {
				double score = docScores.getCount(docId);
				temp.setCount(qId, docId, score);
			}
		}

		for (String qId : new TreeSet<String>(temp.keySet())) {
			Counter<String> docScores = temp.getCounter(qId);
			for (String docId : docScores.getSortedKeys()) {
				double score = docScores.getCount(docId);
				int iter = 0;
				int rank = 0;
				int runId = 0;
				String output = String.format("%s\tQ%d\t%s\t%d\t%s\t%d", qId, iter, docId, rank, Double.toString(score),
						runId);
				writer.write(output + "\n");
			}
		}
		writer.close();
	}

}
