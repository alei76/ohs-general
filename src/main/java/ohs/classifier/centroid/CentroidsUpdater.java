package ohs.classifier.centroid;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohs.matrix.SparseVector;
import ohs.utils.StopWatch;

public class CentroidsUpdater {

	public static void main(String args[]) throws Exception {
		System.out.println("process begins");
		boolean useOdpWeb = true;
		boolean updateFromTest = false;
		boolean useBatchMode = false;

		// int max_iter = 20;
		// double update_weight = 0.2;
		// double learning_rate = 0.5;
		// double min_margin = 0.9;
		// System.out.printf("train size:\t%d\n", trainData.size());
		//
		// CentroidsUpdater updater = new CentroidsUpdater(classifier, trainData, null, true, useBatchMode, max_iter, min_margin,
		// learning_rate,
		// update_weight);
		// updater.update();

		System.out.println("probess ends.");
	}

	private boolean useBatchMode;

	private int max_iter;

	private double update_weight;

	private double learning_rate;

	private double min_margin;

	private CentroidClassifier classifier;

	private boolean printLog;

	private List<SparseVector> trainData;

	private List<SparseVector> testData;

	private List<Integer> docLocs;

	private NumberFormat nf;

	public CentroidsUpdater(CentroidClassifier classifier, List<SparseVector> trainData, List<SparseVector> testData,

	boolean printLog, boolean useBatchMode, int maxIter, double weight, double learningRate, double minMargin) {
		this.classifier = classifier;
		this.trainData = trainData;
		this.testData = testData;
		this.printLog = printLog;
		this.useBatchMode = useBatchMode;
		this.max_iter = maxIter;
		this.update_weight = weight;
		this.learning_rate = learningRate;
		this.min_margin = minMargin;

		docLocs = new ArrayList<Integer>();
		for (int i = 0; i < trainData.size(); i++) {
			docLocs.add(i);
		}

		nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(4);
		nf.setGroupingUsed(false);
	}

	public void batchMode() {
	}

	private void evaluateTestData() {
		// if (testData != null) {
		// TopicEval eval = classifier.evaluate(testData);
		// System.out.println(eval.toString());
		// System.out.println();
		// }
	}

	public void singleMode() {
		evaluateTestData();

		for (int iter = 0; iter < max_iter; iter++) {
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();

			Collections.shuffle(docLocs);
			double num_correct = 0;

			for (int i = 0; i < docLocs.size(); i++) {
				int docLoc = docLocs.get(i);

				if (printLog && (i + 1) % 10 == 0) {
					System.out.printf("\r[%dth, %d/%d, %s]", iter + 1, i + 1, trainData.size(), stopWatch.stop());
				}

				SparseVector query = trainData.get(docLoc);
				int answer = query.label();

				SparseVector predScores = classifier.score(query);
				double answer_score = predScores.value(answer);

				predScores.sortByValue();
				if (predScores.indexAtLoc(0) == answer) {
					num_correct++;
				}

				int[] updateTypes = new int[predScores.size()];

				for (int j = 0; j < predScores.size(); j++) {
					int pred = predScores.indexAtLoc(j);

					if (j == 0) {
						if (pred == answer) {
							double diff = Math.abs(answer_score - predScores.valueAtLoc(1));
							if (diff < min_margin) {
								updateTypes[j] = 2;
								updateTypes[++j] = -2;
								break;
							}
						} else {
							updateTypes[j] = -1;
						}
					} else {
						if (pred == answer) {
							updateTypes[j] = 1;
						}
					}
				}

				for (int j = 0; j < updateTypes.length; j++) {
					int updateType = updateTypes[j];

					if (updateType == 0) {
						continue;
					}

					int pred = predScores.indexAtLoc(j);
					SparseVector centroid = classifier.getCentroid(pred);

					for (int k = 0; k < query.size(); k++) {
						int f = query.indexAtLoc(k);
						double weight_at_query = query.valueAtLoc(k);
						int loc = centroid.location(f);
						if (loc < 0) {
							continue;
						}

						double weight_to_update = 0;

						if (updateType > 0) {
							if (updateType == 1) {
								weight_to_update = learning_rate * weight_at_query;
							} else {
								weight_to_update = learning_rate * update_weight * weight_at_query;
							}
						} else {
							double weight_at_centroid = centroid.valueAtLoc(loc);
							if (updateType == -1) {
								weight_to_update = -learning_rate * weight_at_query;
							} else {
								weight_to_update = -learning_rate * update_weight * weight_at_query;
							}
							if (weight_at_centroid + weight_to_update <= 0) {
								weight_to_update = 0;
							}
						}
						centroid.incrementAtLoc(loc, weight_to_update);
					}
				}
			}

			double accuracy = num_correct / trainData.size();

			if (printLog) {
				System.out.printf("\r[%dth, %d/%d, %s]\n", iter + 1, trainData.size(), trainData.size(), stopWatch.stop());
				System.out.printf("[accuracy: %s (%d/%d)]\n", nf.format(accuracy), (int) num_correct, trainData.size());

				evaluateTestData();
			}

			if (accuracy == 1) {
				break;
			}
		}

		evaluateTestData();
	}

	public void update() {
		if (useBatchMode) {
			batchMode();
		} else {
			singleMode();
		}
	}

}
