package ohs.ml.svm.wrapper;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ui.Model;

import ohs.io.FileUtils;
import ohs.math.ArrayUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;

public class LibLinearWrapper implements Serializable {

	private static final long serialVersionUID = -3273222430839071709L;

	public static LibLinearWrapper read(String fileName) throws Exception {
		System.out.printf("read [%s]\n", fileName);

		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);

		Indexer<String> labelIndexer = FileUtils.readIndexer(ois);
		Indexer<String> featIndexer = FileUtils.readIndexer(ois);

		SolverType solver = SolverType.getById(ois.readInt());
		int nr_class = ois.readInt();
		int[] labels = new int[nr_class];

		for (int i = 0; i < labels.length; i++) {
			labels[i] = ois.readInt();
		}

		int nr_feature = ois.readInt();
		double bias = ois.readDouble();
		double[] w = FileUtils.readDoubleArray(ois);

		Model model = new Model();
		model.solverType = solver;
		model.nr_class = nr_class;
		model.label = labels;
		model.nr_feature = nr_feature;
		model.w = w;

		ois.close();

		return new LibLinearWrapper(model, labelIndexer, featIndexer);
	}

	private Model model;

	private Indexer<String> labelIndexer;

	private Indexer<String> featureIndexer;

	public LibLinearWrapper(Model model, Indexer<String> labelIndexer, Indexer<String> featureIndexer) {
		this.model = model;
		this.labelIndexer = labelIndexer;
		this.featureIndexer = featureIndexer;
	}

	public String evalute(List<SparseVector> testData) {
		SparseVector label_correct = new SparseVector(ArrayUtils.copy(model.getLabels()));

		SparseVector label_answer = label_correct.copy();
		SparseVector label_predict = label_correct.copy();

		for (int i = 0; i < testData.size(); i++) {
			SparseVector query = testData.get(i);
			SparseVector label_score = score(query);
			int predictId = label_score.argMax();
			int answerId = query.label();

			if (predictId == answerId) {
				label_correct.increment(answerId, 1);
			}

			label_answer.increment(answerId, 1);
			label_predict.increment(predictId, 1);
		}

		return TopicEval.evalute(null, label_answer, label_predict, label_correct);
	}

	public Indexer<String> featureIndexer() {
		return featureIndexer;
	}

	public Indexer<String> labelIndexer() {
		return labelIndexer;
	}

	public Counter<String> score(Counter<String> query) {
		return VectorUtils.toCounter(score(VectorUtils.toSparseVector(query, featureIndexer)), labelIndexer);
	}

	public List<SparseVector> score(List<SparseVector> queries) {
		List<SparseVector> ret = new ArrayList<SparseVector>();
		for (int i = 0; i < queries.size(); i++) {
			SparseVector query = queries.get(i);
			SparseVector label_score = score(query);
			label_score.setLabel(query.label());
			ret.add(label_score);
		}
		return ret;
	}

	public SparseVector score(SparseVector query) {
		Feature[] input = new Feature[query.size()];

		for (int i = 0; i < query.size(); i++) {
			int index = query.indexAtLoc(i) + 1;
			double value = query.valueAtLoc(i);
			assert index >= 0;
			input[i] = new FeatureNode(index + 1, value);
		}

		int[] labels = model.getLabels();
		double[] prob_estimates = new double[labels.length];

		// Linear.predictProbability(model, input, prob_estimates);

		Linear.predictValues(model, input, prob_estimates);

		SparseVector ret = new SparseVector(labels, prob_estimates, query.label());
		VectorMath.normalizeBySigmoid(ret);

		return ret;
	}

	public void write(String fileName) throws Exception {
		System.out.printf("write to [%s].\n", fileName);
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);

		FileUtils.write(oos, labelIndexer);
		FileUtils.write(oos, featureIndexer);

		oos.writeInt(model.solverType.getId());
		oos.writeInt(model.getNrClass());

		for (int i = 0; i < model.getNrClass(); i++) {
			oos.writeInt(model.getLabels()[i]);
		}

		oos.writeInt(model.getNrFeature());
		oos.writeDouble(model.getBias());

		FileUtils.writeStrings(oos, model.getFeatureWeights());
		oos.close();
	}
}
