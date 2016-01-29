package ohs.ir.medical.trec.cds_2015;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ui.Model;

import edu.stanford.nlp.math.ArrayMath;
import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.query.BaseQuery;
import ohs.ir.medical.query.QueryReader;
import ohs.ir.medical.query.TrecCdsQuery;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.ml.svm.wrapper.LibSvmTrainer;
import ohs.ml.svm.wrapper.LibSvmWrapper;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.utils.StrUtils;

public class QueryClassifierTrainer {

	public static final String WORD_INDEXER_FILE = MIRPath.TREC_CDS_DIR + "rel_word_indexer.txt";

	public static final String DATA_FILE = MIRPath.TREC_CDS_DIR + "rel_data.ser";

	public static final String TRAIN_DATA_FILE = MIRPath.TREC_CDS_DIR + "rel_train.ser";

	public static final String TEST_DATA_FILE = MIRPath.TREC_CDS_DIR + "rel_test.ser";

	public static final String MODEL_FILE = MIRPath.TREC_CDS_DIR + "rel_model.txt";

	public static void generateData1() throws Exception {
		System.out.println("generate data.");

		Indexer<String> wordIndexer = new Indexer<String>();
		TextFileWriter writer = new TextFileWriter(DATA_FILE);

		List<SparseVector> data = new ArrayList<SparseVector>();

		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_QUERY_DOC_FILE);
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			List<SparseVector> svs = new ArrayList<SparseVector>();
			int qid = -1;
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				String[] parts = line.split("\t");

				double relevance = -1;

				if (i == 0) {
					qid = Integer.parseInt(parts[1]);
				} else {
					relevance = Double.parseDouble(parts[1]);
				}

				StrCounter c = new StrCounter();
				String[] toks = parts[2].split(" ");
				for (int j = 0; j < toks.length; j++) {
					String[] two = StrUtils.split2Two(":", toks[j]);
					c.incrementCount(two[0], Double.parseDouble(two[1]));
				}

				SparseVector sv = VectorUtils.toSparseVector(c, wordIndexer, true);

				if (i > 0) {
					sv.setLabel((int) relevance);
				}

				svs.add(sv);
			}

			SparseVector q = svs.get(0);

			for (int i = 1; i < svs.size(); i++) {
				SparseVector d = svs.get(i);
				SparseVector qd = VectorMath.add(q, d);
				qd.setLabel(d.label());

				data.add(qd);
			}

		}
		reader.close();
		writer.close();

		SparseVector.write(DATA_FILE, data);
		FileUtils.writeStrIndexer(WORD_INDEXER_FILE, wordIndexer);
	}

	public static void generateData2() throws Exception {
		System.out.println("generate data.");

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);

		Map<Integer, BaseQuery> queryMap = new HashMap<Integer, BaseQuery>();

		for (BaseQuery bq : bqs) {
			int qid = Integer.parseInt(bq.getId());
			queryMap.put(qid, bq);
		}

		Indexer<String> wordIndexer = new Indexer<String>();
		Indexer<String> typeIndexer = new Indexer<String>();

		TextFileWriter writer = new TextFileWriter(DATA_FILE);

		List<SparseVector> data = new ArrayList<SparseVector>();

		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_QUERY_DOC_FILE);
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			List<SparseVector> svs = new ArrayList<SparseVector>();
			int qid = -1;

			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				String[] parts = line.split("\t");

				double relevance = -1;

				if (i == 0) {
					qid = Integer.parseInt(parts[1]);
				} else {
					relevance = Double.parseDouble(parts[1]);
				}

				StrCounter c = new StrCounter();
				String[] toks = parts[2].split(" ");
				for (int j = 0; j < toks.length; j++) {
					String[] two = StrUtils.split2Two(":", toks[j]);
					c.incrementCount(two[0], Double.parseDouble(two[1]));
				}

				SparseVector sv = VectorUtils.toSparseVector(c, wordIndexer, true);

				if (i > 0) {
					sv.setLabel((int) relevance);
				}

				svs.add(sv);
			}

			TrecCdsQuery tcq = (TrecCdsQuery) queryMap.get(qid);
			String type = tcq.getType();
			int typeId = typeIndexer.getIndex(type);

			// SparseVector q = svs.get(0);

			for (int i = 1; i < svs.size(); i++) {
				SparseVector d = svs.get(i);
				double relevance = d.label();

				if (relevance > 0) {
					d.setLabel(typeId);
				} else {
					d.setLabel(3);
				}

				// SparseVector qd = VectorMath.add(q, d);
				// qd.setLabel(d.label());

				data.add(d);
			}

		}
		reader.close();
		writer.close();

		SparseVector.write(DATA_FILE, data);
		FileUtils.writeStrIndexer(WORD_INDEXER_FILE, wordIndexer);
	}

	public static Parameter getSVMParamter() {
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

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// generateData1();
		// generateData2();

		// splitData();

		trainLibSVMs();
		// trainLibLinear();

		System.out.println("process ends.");
	}

	public static void splitData() throws Exception {
		ListMap<Integer, SparseVector> data = SparseVector.readMap(DATA_FILE);

		List<SparseVector> trainData = new ArrayList<SparseVector>();
		List<SparseVector> testData = new ArrayList<SparseVector>();

		double portion_for_train = 0.7;

		for (int label : data.keySet()) {
			List<SparseVector> svs = data.get(label);
			int num_train_docs = (int) (svs.size() * portion_for_train);

			for (int j = 0; j < svs.size(); j++) {
				SparseVector sv = svs.get(j);
				if (j < num_train_docs) {
					trainData.add(sv);
				} else {
					testData.add(sv);
				}
			}
		}

		SparseVector.write(TRAIN_DATA_FILE, trainData);
		SparseVector.write(TEST_DATA_FILE, testData);
	}

	public static void trainLibLinear() throws Exception {
		System.out.println("train SVMs.");

		Indexer<String> featureIndexer = FileUtils.readStrIndexer(WORD_INDEXER_FILE);

		List<SparseVector> trainData = SparseVector.readList(TRAIN_DATA_FILE);
		List<SparseVector> testData = SparseVector.readList(TEST_DATA_FILE);

		Collections.shuffle(trainData);
		Collections.shuffle(testData);

		// List[] lists = new List[] { trainData, testData };
		//
		// for (int i = 0; i < lists.length; i++) {
		// List<SparseVector> list = lists[i];
		// for (int j = 0; j < list.size(); j++) {
		// SparseVector sv = list.get(j);
		// if (sv.label() > 0) {
		// sv.setLabel(1);
		// }
		// }
		// }

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

		Model model = Linear.train(prob, getSVMParamter());

		CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();

		for (int i = 0; i < testData.size(); i++) {
			SparseVector sv = testData.get(i);
			Feature[] input = new Feature[sv.size()];
			for (int j = 0; j < sv.size(); j++) {
				int index = sv.indexAtLoc(j) + 1;
				double value = sv.valueAtLoc(j);
				input[j] = new FeatureNode(index + 1, value);
			}

			double[] dec_values = new double[model.getNrClass()];
			Linear.predictValues(model, input, dec_values);
			int max_id = ArrayMath.argmax(dec_values);
			int pred = model.getLabels()[max_id];
			int answer = sv.label();

			cm.incrementCount(answer, pred, 1);
		}

		System.out.println(cm);

		model.save(new File(MODEL_FILE));
	}

	public static void trainLibSVMs() throws Exception {
		System.out.println("train LibSVMs.");

		Indexer<String> featureIndexer = FileUtils.readStrIndexer(WORD_INDEXER_FILE);
		Indexer<String> labelIndexer = new Indexer<String>();
		labelIndexer.add("Non-Relevant");
		labelIndexer.add("Relevant");

		List<SparseVector> trainData = SparseVector.readList(TRAIN_DATA_FILE);
		List<SparseVector> testData = SparseVector.readList(TEST_DATA_FILE);

		Collections.shuffle(trainData);
		Collections.shuffle(testData);

		LibSvmTrainer trainer = new LibSvmTrainer();
		LibSvmWrapper wrapper = trainer.train(labelIndexer, featureIndexer, trainData);
		wrapper.evalute(testData);

	}

	private Indexer<String> labelIndexer;

	private Indexer<String> featureIndexer;

	private List<SparseVector> data;

	private Model model;

}
