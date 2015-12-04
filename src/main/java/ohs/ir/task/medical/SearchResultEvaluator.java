package ohs.ir.task.medical;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math.stat.inference.TTestImpl;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.eval.MetricType;
import ohs.ir.eval.Performance;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.ir.task.medical.query.RelevanceReader;
import ohs.types.BidMap;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.DeepMap;
import ohs.types.ListMap;
import ohs.utils.StrUtils;

public class SearchResultEvaluator {

	public static void analyze() {
		TextFileReader reader = new TextFileReader(MIRPath.PERFORMANCE_DETAIL_FILE);

		ListMap<String, String> collFileNames = new ListMap<String, String>();
		ListMap<String, CounterMap<String, MetricType>> collCms = new ListMap<String, CounterMap<String, MetricType>>();

		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String collName = lines.get(0).split("\t")[1];
			String fileName = lines.get(1).split("\t")[1].replace(".txt", "");

			collFileNames.put(collName, fileName);

			CounterMap<String, MetricType> cm = new CounterMap<String, MetricType>();

			for (int i = 4; i < lines.size(); i++) {
				String[] parts = lines.get(i).split("\t");
				String qId = parts[0];

				if (qId.equals("Overall")) {
					continue;
				}

				double num_retrieved = Double.parseDouble(parts[1]);
				double num_relevant_all = Double.parseDouble(parts[2]);
				double num_relevant_in_ret = Double.parseDouble(parts[3]);
				double num_relevant_at = Double.parseDouble(parts[4]);
				double precision = Double.parseDouble(parts[5]);
				double ap = Double.parseDouble(parts[6]);
				double ndcg = Double.parseDouble(parts[7]);

				Counter<MetricType> c = new Counter<MetricType>();
				c.setCount(MetricType.RETRIEVED, num_retrieved);
				c.setCount(MetricType.RELEVANT, num_relevant_all);
				c.setCount(MetricType.RELEVANT_IN_RET, num_relevant_in_ret);
				c.setCount(MetricType.RELEVANT_AT, num_relevant_at);
				c.setCount(MetricType.P, precision);
				c.setCount(MetricType.AP, ap);
				c.setCount(MetricType.NDCG, ndcg);
				cm.setCounter(qId, c);
			}
			collCms.put(collName, cm);
		}
		reader.close();

		for (String collName : collFileNames.keySet()) {
			List<String> fileNames = collFileNames.get(collName);
			List<CounterMap<String, MetricType>> cms = collCms.get(collName);

			int ql_index = 0;
			int eem_idx = 0;
			int best_cbeem_idx = 0;

			Set<String> targetQueryIds = new TreeSet<String>();
			Counter<Integer> temp = new Counter<Integer>();

			for (int i = 0; i < fileNames.size(); i++) {
				String fileName = fileNames.get(i);
				CounterMap<String, MetricType> cm = cms.get(i);

				String[] parts = fileName.split("_");

				if (fileName.equals("init")) {
					ql_index = i;
				} else {
					HyperParameter hp = HyperParameter.parse(StrUtils.subTokens(parts, 1, parts.length));

					if (hp.getTopK() == 100 && hp.getNumFBDocs() == 5 && hp.getNumFBWords() == 25 && hp.getMixtureForAllCollections() == 0
							&& !hp.isUseDocPrior() && !hp.isUseDoubleScoring() && !hp.isUseWiki() && hp.getMixtureForFeedbackModel() == 0.5
							&& !hp.isSmoothCollectionMixtures() && !hp.isAdjustNumbers()) {
						eem_idx = i;
					}
					{
						double ndcg = cm.invert().getCounter(MetricType.NDCG).totalCount();
						temp.setCount(i, ndcg);
					}
				}
			}

			CounterMap<String, MetricType> qlPerformances = cms.get(ql_index);
			CounterMap<String, MetricType> eemPerformances = cms.get(eem_idx);
			CounterMap<String, MetricType> cbeemPerformances = cms.get(best_cbeem_idx);

			for (String qId : qlPerformances.keySet()) {
				Counter<MetricType> qlPerformance = qlPerformances.getCounter(qId);
				Counter<MetricType> eemPerformance = eemPerformances.getCounter(qId);
				Counter<MetricType> cbeemPerformance = cbeemPerformances.getCounter(qId);

				Counter<MetricType>[] ps = new Counter[] { qlPerformance, eemPerformance, cbeemPerformance };

				double[] aps = new double[ps.length];
				double[] ndcgs = new double[ps.length];

				StringBuffer sb = new StringBuffer(collName + "\t" + qId);
				NumberFormat nf = NumberFormat.getInstance();
				nf.setMinimumFractionDigits(4);

				for (int i = 0; i < ps.length; i++) {
					double ap = ps[i].getCount(MetricType.AP);
					double ndcg = ps[i].getCount(MetricType.NDCG);
					aps[i] = ap;
					ndcgs[i] = ndcg;

					sb.append(String.format("\t%s\t%s", nf.format(ap), nf.format(ndcg)));
				}

				if (aps[0] == 0) {
					System.out.println(sb.toString());
				}
			}
		}
	}

	public static void evaluateForJBICBEEM() throws Exception {
		String[] resultDirNames = MIRPath.ResultDirNames;
		String[] relDataFileNames = MIRPath.RelevanceFileNames;
		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		TextFileWriter writer1 = new TextFileWriter(MIRPath.PERFORMANCE_FILE);
		TextFileWriter writer2 = new TextFileWriter(MIRPath.PERFORMANCE_DETAIL_FILE);

		StringBuffer sb = new StringBuffer();
		sb.append(
				"Collection\tQuery\tModel\tTopN\tTopK\tWikiTopK\tFBDocs\tFBWords\tDIR\tAllCollMix\tUseDocPrior\tUseDoubleScoring\tUseWiki\tFBMix\tUseSmoothCollMix\tAdjustNumbers");
		sb.append("\tRelevant_ALL");
		sb.append("\tRetrieved All");
		sb.append("\tRelevant_All_In_Retreived_All");
		sb.append("\tRelevant_At");
		sb.append("\tPrecision");
		sb.append("\tMAP");
		sb.append("\tNDCG");
		sb.append("\tP-0.05");
		sb.append("\tP-0.01");
		sb.append("\tM-0.05");
		sb.append("\tM-0.01");
		sb.append("\tN-0.05");
		sb.append("\tN-0.01");

		sb.append("\tP-Gain");
		sb.append("\tP-Reward");
		sb.append("\tP-Risk");
		sb.append("\tM-Gain");
		sb.append("\tM-Reward");
		sb.append("\tM-Risk");
		sb.append("\tN-Gain");
		sb.append("\tN-Reward");
		sb.append("\tN-Risk");

		writer1.write(sb.toString());

		DeepMap<String, String, List<Performance>> performanceMap = new DeepMap<String, String, List<Performance>>();

		for (int i = 0; i < resultDirNames.length; i++) {
			String resultDirName = resultDirNames[i];
			String relFileName = relDataFileNames[i];
			String docMapFileName = docMapFileNames[i];
			String collName = "";

			{
				File f = new File(resultDirName);
				collName = f.getParentFile().getParentFile().getName();
			}

			BidMap<String, String> docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileName);

			CounterMap<String, String> relData = RelevanceReader.readRelevances(relFileName);
			relData = RelevanceReader.filter(relData, docIdMap);

			PerformanceEvaluator retEvaluator = new PerformanceEvaluator();

			File baselineFile = null;

			TreeSet<File> resultFileSet = new TreeSet<File>();

			for (File resultFile : new File(resultDirName).listFiles()) {
				String paramStr = resultFile.getName().replace(".txt", "");
				String[] parts = paramStr.split("_");
				String modelName = parts[0];

				if (modelName.equals("init")) {
					baselineFile = resultFile;
				} else {
					resultFileSet.add(resultFile);
				}
			}

			List<File> resultFiles = new ArrayList<File>();
			resultFiles.add(baselineFile);
			resultFiles.addAll(resultFileSet);

			List<Performance> baselines = new ArrayList<Performance>();

			for (int j = 0; j < resultFiles.size(); j++) {
				File resultFile = resultFiles.get(j);
				CounterMap<String, String> resultData = DocumentIdMapper
						.mapIndexIdsToDocIds(PerformanceEvaluator.readSearchResults(resultFile.getPath()), docIdMap);

				String paramStr = resultFile.getName().replace(".txt", "");
				String[] parts = paramStr.split("_");
				String modelName = parts[0];

				List<Performance> targets = retEvaluator.evalute(resultData, relData);

				if (j == 0) {
					baselines = targets;
				}

				HyperParameter hp = new HyperParameter();

				if (j > 0) {
					hp = HyperParameter.parse(StrUtils.subTokens(parts, 1, parts.length));
				}

				// if (hyperParameter.getMixtureForExpQueryModel() == 0 ||
				// hyperParameter.getMixtureForExpQueryModel() == 1) {
				// continue;
				// }

				writer2.write(String.format("Collection:\t%s\n", collName));
				writer2.write(String.format("FileName:\t%s\n", resultFile.getName()));

				NumberFormat nf = NumberFormat.getInstance();
				nf.setMinimumFractionDigits(4);

				for (int k = 0; k < targets.size(); k++) {
					Performance baseline = baselines.get(k);
					Performance target = targets.get(k);

					{
						StringBuffer sb2 = new StringBuffer();
						sb2.append(String.format("%s\t%d\t%s\t%d", collName, resultData.keySet().size(), modelName, target.getTopN()));

						sb2.append(String.format("\t%s", hp.getTopK()));
						sb2.append(String.format("\t%s", hp.getTopKInWiki()));
						sb2.append(String.format("\t%s", hp.getNumFBDocs()));
						sb2.append(String.format("\t%s", hp.getNumFBWords()));
						sb2.append(String.format("\t%s", hp.getDirichletPrior()));
						sb2.append(String.format("\t%s", hp.getMixtureForAllCollections()));
						sb2.append(String.format("\t%s", hp.isUseDocPrior()));
						sb2.append(String.format("\t%s", hp.isUseDoubleScoring()));
						sb2.append(String.format("\t%s", hp.isUseWiki()));
						sb2.append(String.format("\t%s", hp.getMixtureForFeedbackModel()));
						sb2.append(String.format("\t%s", hp.isSmoothCollectionMixtures()));
						sb2.append(String.format("\t%s", hp.isAdjustNumbers()));

						sb2.append(String.format("\t%d", (int) target.getTotalRelevant()));
						sb2.append(String.format("\t%d", (int) target.getTotalRetrieved()));
						sb2.append(String.format("\t%d", (int) target.getTotalCorrect()));
						sb2.append(String.format("\t%d", (int) target.getTotalCorrectAtN()));
						sb2.append(String.format("\t%s", nf.format(target.getPrecisionAtN())));
						sb2.append(String.format("\t%s", nf.format(target.getMAP())));
						sb2.append(String.format("\t%s", nf.format((target.getNDCG()))));

						{
							MetricType[] metricTypes = { MetricType.P, MetricType.AP, MetricType.NDCG };

							for (int l = 0; l < metricTypes.length; l++) {
								MetricType metricType = metricTypes[l];
								Counter<String> c1 = baseline.getMetricQueryScores().getCounter(metricType);
								Counter<String> c2 = target.getMetricQueryScores().getCounter(metricType);
								int size1 = c1.size();
								int size2 = c2.size();

								double[] scores1 = new double[c1.size()];
								double[] scores2 = new double[c2.size()];
								int loc = 0;

								for (String qId : c1.keySet()) {
									scores1[loc] = c1.getCount(qId);
									scores2[loc] = c2.getCount(qId);
									loc++;
								}

								TTestImpl tt = new TTestImpl();
								boolean isSignificantlyImproved1 = tt.pairedTTest(scores1, scores2, 0.05);
								boolean isSignificantlyImproved2 = tt.pairedTTest(scores1, scores2, 0.01);
								sb2.append(String.format("\t%s\t%s", isSignificantlyImproved1, isSignificantlyImproved2));
							}
						}

						{
							MetricType[] metricTypes = { MetricType.P, MetricType.AP, MetricType.NDCG };

							NumberFormat nf2 = NumberFormat.getInstance();
							nf2.setMinimumFractionDigits(6);

							for (int l = 0; l < metricTypes.length; l++) {
								MetricType metricType = metricTypes[l];
								Counter<String> c1 = baseline.getMetricQueryScores().getCounter(metricType);
								Counter<String> c2 = target.getMetricQueryScores().getCounter(metricType);

								double risk = 0;
								double reward = 0;
								double num_pos = 0;
								double num_neg = 0;

								for (String qId : c1.keySet()) {
									double score1 = c1.getCount(qId);
									double score2 = c2.getCount(qId);
									risk += Math.max(0, score1 - score2);
									reward += Math.max(0, score2 - score1);

									if (score2 > score1) {
										num_pos++;
									} else {
										num_neg++;
									}
								}
								risk /= c1.size();
								reward /= c1.size();

								double gain = reward - risk;
								double ri = (num_pos - num_neg) / c1.size();

								sb2.append(String.format("\t%s\t%s\t%s", nf2.format(gain), nf2.format(reward), nf2.format(risk)));
							}
						}

						writer1.write("\n" + sb2.toString());
					}

					{
						MetricType[] types = new MetricType[] { MetricType.RETRIEVED, MetricType.RELEVANT, MetricType.RELEVANT_IN_RET,
								MetricType.RELEVANT_AT, MetricType.P, MetricType.AP, MetricType.NDCG };

						StringBuffer sb3 = new StringBuffer();
						sb3.append(String.format("Top-%d", target.getTopN()));
						sb3.append("\nQueryId");

						for (MetricType type : types) {
							sb3.append("\t" + type);
						}
						sb3.append("\n");

						CounterMap<String, MetricType> queryMetricValues = target.getMetricQueryScores().invert();

						List<String> queryIds = new ArrayList<String>(new TreeSet<String>(queryMetricValues.keySet()));

						BidMap<String, Integer> map = new BidMap<String, Integer>();

						for (int l = 0; l < queryIds.size(); l++) {
							String qid = queryIds.get(l);
							int qid2 = 0;
							if (collName.equals("clef_ehealth")) {
								int idx = qid.lastIndexOf(".") + 1;
								qid2 = Integer.parseInt(qid.substring(idx));
							} else {
								qid2 = Integer.parseInt(qid);
							}
							map.put(qid, qid2);
						}

						List<Integer> qIds = new ArrayList<Integer>(map.getValues());
						Collections.sort(qIds);

						Counter<MetricType> overallValues = new Counter<MetricType>();

						for (int l = 0; l < qIds.size(); l++) {
							int qId = qIds.get(l);
							String qid = map.getKey(qId);
							sb3.append(qid);

							Counter<MetricType> metricValues = queryMetricValues.getCounter(qid);

							for (int m = 0; m < types.length; m++) {
								MetricType type = types[m];
								double score = metricValues.getCount(type);
								if (type == MetricType.P || type == MetricType.AP || type == MetricType.NDCG) {
									sb3.append(String.format("\t%s", nf.format(score)));
								} else {
									sb3.append(String.format("\t%d", (int) score));
								}
								overallValues.incrementCount(types[m], score);
							}
							sb3.append("\n");
						}

						sb3.append("Overall");

						for (int l = 0; l < types.length; l++) {
							MetricType type = types[l];
							double score = overallValues.getCount(types[l]);

							if (type == MetricType.P || type == MetricType.AP || type == MetricType.NDCG) {
								score /= queryIds.size();
								sb3.append(String.format("\t%s", nf.format(score)));
							} else {
								sb3.append(String.format("\t%d", (int) score));
							}
						}
						sb3.append("\n");

						writer2.write(sb3.toString() + "\n");
					}
				}
			}
		}

		writer1.close();
	}

	public static void evaluate(String[] resDirNames, String[] relDataFileNames, String[] docIdMapFileNames, String perfFileName,
			String perfDetailFileName) throws Exception {

		TextFileWriter writer1 = new TextFileWriter(perfFileName);
		TextFileWriter writer2 = new TextFileWriter(perfDetailFileName);

		StringBuffer sb = new StringBuffer();
		sb.append("Collection\tQueries\tModel\tTop-K");

		sb.append(String.format("\t%s", MetricType.RELEVANT));
		sb.append(String.format("\t%s", MetricType.RETRIEVED));
		sb.append(String.format("\t%s", MetricType.RELEVANT_IN_RET));
		sb.append(String.format("\t%s", MetricType.RELEVANT_AT));
		sb.append(String.format("\t%s", MetricType.P));
		sb.append(String.format("\t%s", MetricType.MAP));
		sb.append(String.format("\t%s", MetricType.NDCG));

		sb.append(String.format("\t%s-0.05", MetricType.P));
		sb.append(String.format("\t%s-0.01", MetricType.P));
		sb.append(String.format("\t%s-0.05", MetricType.MAP));
		sb.append(String.format("\t%s-0.01", MetricType.MAP));
		sb.append(String.format("\t%s-0.05", MetricType.NDCG));
		sb.append(String.format("\t%s-0.01", MetricType.NDCG));

		sb.append(String.format("\t%s-Gain", MetricType.P));
		sb.append(String.format("\t%s-Reward", MetricType.P));
		sb.append(String.format("\t%s-Risk", MetricType.P));

		sb.append(String.format("\t%s-Gain", MetricType.MAP));
		sb.append(String.format("\t%s-Reward", MetricType.MAP));
		sb.append(String.format("\t%s-Risk", MetricType.MAP));

		sb.append(String.format("\t%s-Gain", MetricType.NDCG));
		sb.append(String.format("\t%s-Reward", MetricType.NDCG));
		sb.append(String.format("\t%s-Risk", MetricType.NDCG));

		writer1.write(sb.toString());

		DeepMap<String, String, List<Performance>> perfMap = new DeepMap<String, String, List<Performance>>();

		for (int i = 0; i < resDirNames.length; i++) {
			String resDirName = resDirNames[i];
			String relFileName = relDataFileNames[i];
			String docMapFileName = docIdMapFileNames[i];
			String collName = "";

			{
				File f = new File(resDirName);
				collName = f.getParentFile().getParentFile().getName();
			}

			BidMap<String, String> docIdMap = new BidMap<String, String>();
			if (docMapFileName != null && docMapFileName.length() > 0) {
				docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileName);
			}

			CounterMap<String, String> relData = RelevanceReader.readRelevances(relFileName);

			if (docIdMap.size() > 0) {
				relData = RelevanceReader.filter(relData, docIdMap);
			}

			PerformanceEvaluator pe = new PerformanceEvaluator();

			File blFile = null;

			TreeSet<File> resFileSet = new TreeSet<File>();

			for (File resFile : new File(resDirName).listFiles()) {
				String paramStr = resFile.getName().replace(".txt", "");
				String[] parts = paramStr.split("_");
				String modelName = parts[0];

				if (modelName.equals("qld")) {
					blFile = resFile;
				} else {
					resFileSet.add(resFile);
				}
			}

			List<File> resFiles = new ArrayList<File>();
			resFiles.add(blFile);
			resFiles.addAll(resFileSet);

			List<Performance> baselines = new ArrayList<Performance>();

			for (int j = 0; j < resFiles.size(); j++) {
				File resFile = resFiles.get(j);

				CounterMap<String, String> resData = PerformanceEvaluator.readSearchResults(resFile.getPath());

				if (docIdMap.size() > 0) {
					resData = DocumentIdMapper.mapIndexIdsToDocIds(resData, docIdMap);
				}

				String modelName = resFile.getName();

				List<Performance> targets = pe.evalute(resData, relData);

				if (j == 0) {
					baselines = targets;
				}

				writer2.write(String.format("Collection:\t%s\n", collName));
				writer2.write(String.format("FileName:\t%s\n", resFile.getName()));

				NumberFormat nf = NumberFormat.getInstance();
				nf.setMinimumFractionDigits(4);

				for (int k = 0; k < targets.size(); k++) {
					Performance baseline = baselines.get(k);
					Performance target = targets.get(k);

					{
						StringBuffer sb2 = new StringBuffer();
						sb2.append(String.format("%s\t%d\t%s\t%d", collName, resData.keySet().size(), modelName, target.getTopN()));

						sb2.append(String.format("\t%d", (int) target.getTotalRelevant()));
						sb2.append(String.format("\t%d", (int) target.getTotalRetrieved()));
						sb2.append(String.format("\t%d", (int) target.getTotalCorrect()));
						sb2.append(String.format("\t%d", (int) target.getTotalCorrectAtN()));
						sb2.append(String.format("\t%s", nf.format(target.getPrecisionAtN())));
						sb2.append(String.format("\t%s", nf.format(target.getMAP())));
						sb2.append(String.format("\t%s", nf.format((target.getNDCG()))));

						{
							MetricType[] mts = { MetricType.P, MetricType.AP, MetricType.NDCG };

							for (int l = 0; l < mts.length; l++) {
								MetricType mt = mts[l];
								Counter<String> c1 = baseline.getMetricQueryScores().getCounter(mt);
								Counter<String> c2 = target.getMetricQueryScores().getCounter(mt);
								int size1 = c1.size();
								int size2 = c2.size();

								double[] scores1 = new double[c1.size()];
								double[] scores2 = new double[c2.size()];
								int loc = 0;

								for (String qId : c1.keySet()) {
									scores1[loc] = c1.getCount(qId);
									scores2[loc] = c2.getCount(qId);
									loc++;
								}

								TTestImpl tt = new TTestImpl();
								boolean improved1 = tt.pairedTTest(scores1, scores2, 0.05);
								boolean improved2 = tt.pairedTTest(scores1, scores2, 0.01);
								sb2.append(String.format("\t%s\t%s", improved1, improved2));
							}
						}

						{
							MetricType[] mts = { MetricType.P, MetricType.AP, MetricType.NDCG };

							NumberFormat nf2 = NumberFormat.getInstance();
							nf2.setMinimumFractionDigits(6);

							for (int l = 0; l < mts.length; l++) {
								MetricType mt = mts[l];
								Counter<String> c1 = baseline.getMetricQueryScores().getCounter(mt);
								Counter<String> c2 = target.getMetricQueryScores().getCounter(mt);

								double risk = 0;
								double reward = 0;
								double num_pos = 0;
								double num_neg = 0;

								for (String qId : c1.keySet()) {
									double score1 = c1.getCount(qId);
									double score2 = c2.getCount(qId);
									risk += Math.max(0, score1 - score2);
									reward += Math.max(0, score2 - score1);

									if (score2 > score1) {
										num_pos++;
									} else {
										num_neg++;
									}
								}
								risk /= c1.size();
								reward /= c1.size();

								double gain = reward - risk;
								double ri = (num_pos - num_neg) / c1.size();

								sb2.append(String.format("\t%s\t%s\t%s", nf2.format(gain), nf2.format(reward), nf2.format(risk)));
							}
						}

						writer1.write("\n" + sb2.toString());
					}

					{
						MetricType[] mts = new MetricType[] { MetricType.RETRIEVED, MetricType.RELEVANT, MetricType.RELEVANT_IN_RET,
								MetricType.RELEVANT_AT, MetricType.P, MetricType.AP, MetricType.NDCG };

						StringBuffer sb3 = new StringBuffer();
						sb3.append(String.format("Top-%d", target.getTopN()));
						sb3.append("\nQID");

						for (MetricType mt : mts) {
							sb3.append("\t" + mt);
						}
						sb3.append("\n");

						CounterMap<String, MetricType> qmvs = target.getMetricQueryScores().invert();

						List<String> qids = new ArrayList<String>(new TreeSet<String>(qmvs.keySet()));

						BidMap<String, Integer> map = new BidMap<String, Integer>();

						for (int l = 0; l < qids.size(); l++) {
							String qid = qids.get(l);
							int qid2 = 0;
							if (collName.equals("clef_ehealth")) {
								int idx = qid.lastIndexOf(".") + 1;
								qid2 = Integer.parseInt(qid.substring(idx));
							} else {
								qid2 = Integer.parseInt(qid);
							}
							map.put(qid, qid2);
						}

						List<Integer> qIds = new ArrayList<Integer>(map.getValues());
						Collections.sort(qIds);

						Counter<MetricType> overallValues = new Counter<MetricType>();

						for (int l = 0; l < qIds.size(); l++) {
							int qId = qIds.get(l);
							String qid = map.getKey(qId);
							sb3.append(qid);

							Counter<MetricType> metricValues = qmvs.getCounter(qid);

							for (int m = 0; m < mts.length; m++) {
								MetricType type = mts[m];
								double score = metricValues.getCount(type);
								if (type == MetricType.P || type == MetricType.AP || type == MetricType.NDCG) {
									sb3.append(String.format("\t%s", nf.format(score)));
								} else {
									sb3.append(String.format("\t%d", (int) score));
								}
								overallValues.incrementCount(mts[m], score);
							}
							sb3.append("\n");
						}

						sb3.append("Overall");

						for (int l = 0; l < mts.length; l++) {
							MetricType type = mts[l];
							double score = overallValues.getCount(mts[l]);

							if (type == MetricType.P || type == MetricType.AP || type == MetricType.NDCG) {
								score /= qids.size();
								sb3.append(String.format("\t%s", nf.format(score)));
							} else {
								sb3.append(String.format("\t%d", (int) score));
							}
						}
						sb3.append("\n");

						writer2.write(sb3.toString() + "\n");
					}
				}
			}
		}

		writer1.close();
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		int evalType = 0;

		if (evalType == 0) {

			String[] resDirNames = MIRPath.ResultDirNames;
			String[] relDataFileNames = MIRPath.RelevanceFileNames;
			String[] docIdMapFileNames = MIRPath.DocIdMapFileNames;

			String perfFileName = MIRPath.PERFORMANCE_FILE;
			String perfDetailFileName = MIRPath.PERFORMANCE_DETAIL_FILE;

			evaluate(resDirNames, relDataFileNames, docIdMapFileNames, perfFileName, perfDetailFileName);
		} else if (evalType == 1) {
			String[] resDirNames = MIRPath.ResultDirNames;

			for (int i = 0; i < resDirNames.length; i++) {
				resDirNames[i] = resDirNames[i].replace("result", "result_sent");
			}

			String[] relDataFileNames = MIRPath.RelevanceFileNames;
			String[] docIdMapFileNames = MIRPath.DocIdMapFileNames;

			String perfFileName = MIRPath.PERFORMANCE_FILE.replace("performance", "performance_sent");
			String perfDetailFileName = MIRPath.PERFORMANCE_DETAIL_FILE.replace("performance_detail", "performance_detail_sent");

			evaluate(resDirNames, relDataFileNames, docIdMapFileNames, perfFileName, perfDetailFileName);
		}

		System.out.println("process ends.");
	}
}
