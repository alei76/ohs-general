package ohs.ml.svm.wrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import ohs.io.FileUtils;
import ohs.math.ArrayUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;

public class LibLinearWrapper implements Serializable {

	private static final long serialVersionUID = -3273222430839071709L;

	public static Feature[] toFeatureNodes(SparseVector x, int num_feats, double bias) {
		Feature[] fs = new Feature[bias >= 0 ? x.size() + 1 : x.size()];

		for (int j = 0; j < x.size(); j++) {
			int index = x.indexAtLoc(j);
			double value = x.valueAtLoc(j);
			fs[j] = new FeatureNode(index + 1, value);
		}

		if (bias >= 0) {
			fs[fs.length - 1] = new FeatureNode(num_feats, bias);
		}
		return fs;
	}

	public Model getModel() {
		return model;
	}

	private Model model;

	private Indexer<String> labelIndexer;

	private Indexer<String> featIndexer;

	public LibLinearWrapper() {

	}

	public LibLinearWrapper(Model model, Indexer<String> labelIndexer, Indexer<String> featIndexer) {
		this.model = model;
		this.labelIndexer = labelIndexer;
		this.featIndexer = featIndexer;
	}

	public String evalute(List<SparseVector> testData, List<Integer> testLabels) {
		SparseVector correct = new SparseVector(ArrayUtils.copy(model.getLabels()));
		correct.sortByIndex();

		SparseVector anss = correct.copy();
		SparseVector preds = correct.copy();

		for (int i = 0; i < testData.size(); i++) {
			SparseVector x = testData.get(i);
			SparseVector scores = score(x);
			int pred = scores.argMax();
			int ans = testLabels.get(i);

			if (pred == ans) {
				correct.increment(ans, 1);
			}

			anss.increment(ans, 1);
			preds.increment(pred, 1);
		}

		return TopicEval.evalute(null, anss, preds, correct);
	}

	public Indexer<String> featureIndexer() {
		return featIndexer;
	}

	public Indexer<String> labelIndexer() {
		return labelIndexer;
	}

	public void read(String fileName) throws Exception {
		System.out.printf("read [%s]\n", fileName);

		BufferedReader br = FileUtils.openBufferedReader(fileName);

		labelIndexer = FileUtils.readStrIndexer(br);
		featIndexer = FileUtils.readStrIndexer(br);
		model = Linear.loadModel(br);

		br.close();

	}

	public Counter<String> score(Counter<String> x) {
		SparseVector sv = VectorUtils.toSparseVector(x, featIndexer, false);
		VectorMath.unitVector(sv);

		return VectorUtils.toCounter(score(sv), labelIndexer);
	}

	public List<SparseVector> score(List<SparseVector> xs) {
		List<SparseVector> ret = new ArrayList<SparseVector>();
		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);
			SparseVector scores = score(x);
			ret.add(scores);
		}
		return ret;
	}

	public SparseVector score(SparseVector x) {

		int[] labels = model.getLabels();
		double[] prob_estimates = new double[labels.length];
		Feature[] fs = toFeatureNodes(x, model.getNrFeature(), model.getBias());

		// Linear.predictProbability(model, input, prob_estimates);

		Linear.predictValues(model, fs, prob_estimates);

		SparseVector ret = new SparseVector(labels, prob_estimates);
		VectorMath.softmax(ret);
		return ret;
	}

	public void write(String fileName) throws Exception {
		System.out.printf("write to [%s].\n", fileName);

		BufferedWriter bw = FileUtils.openBufferedWriter(fileName);

		FileUtils.writeStrIndexer(bw, labelIndexer);
		FileUtils.writeStrIndexer(bw, featIndexer);

		model.save(bw);

		bw.close();
	}
}
