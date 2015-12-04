package ohs.ml.svm.wrapper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ohs.math.ArrayUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.Pair;
import ohs.utils.StrUtils;

public class TopicEval {

	public static String evalute(Indexer<String> labelIndexer, List<SparseVector> results) {
		Set<Integer> labelSet = new TreeSet<Integer>();
		for (SparseVector x : results) {
			labelSet.add(x.label());
		}

		int[] indexes = new int[labelSet.size()];
		ArrayUtils.copy(labelSet, indexes);

		SparseVector label_answer = new SparseVector(indexes);
		SparseVector label_predict = label_answer.copy();
		SparseVector label_correct = label_answer.copy();

		for (int j = 0; j < results.size(); j++) {
			SparseVector global_score = results.get(j);
			int answerId = global_score.label();
			int predictId = global_score.argMax();

			label_answer.increment(answerId, 1);
			label_predict.increment(predictId, 1);
			if (answerId == predictId) {
				label_correct.increment(answerId, 1);
			}
		}

		return evalute(labelIndexer, label_answer, label_predict, label_correct);
	}

	public static String evalute(Indexer<String> labelIndexer, SparseVector label_answer, SparseVector label_predict,
			SparseVector label_correct) {
		StringBuffer sb = new StringBuffer();
		sb.append("Label\tAnswer\tPredict\tCorrect\tPrec\tRec\tF1\n");

		double macroPrecision = 0;
		double macroRecall = 0;
		double macroF1 = 0;

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(4);

		for (int i = 0; i < label_answer.size(); i++) {
			int labelId = label_answer.indexAtLoc(i);
			String label = null;

			if (labelIndexer != null) {
				label = labelIndexer.getObject(labelId);
			}

			double numAnswer = label_answer.valueAtLoc(i);
			double numPredict = label_predict.valueAtLoc(i);
			double numCorrect = label_correct.valueAtLoc(i);
			double[] prf = getPrecisionRecallF1(numAnswer, numPredict, numCorrect);

			sb.append(String.format("%s\t%d\t%d\t%d\t%s\t%s\t%s\n",

			label == null ? labelId : label, (int) numAnswer, (int) numPredict, (int) numCorrect, nf.format(prf[0]), nf.format(prf[1]),
					nf.format(prf[2])));

			macroPrecision += prf[0];
			macroRecall += prf[1];
			macroF1 += prf[2];
		}

		macroPrecision /= label_answer.size();
		macroRecall /= label_answer.size();
		macroF1 /= label_answer.size();

		double totalAnswer = label_answer.sum();
		double totalPredict = label_predict.sum();
		double totalCorrect = label_correct.sum();

		double[] prf = getPrecisionRecallF1(totalAnswer, totalPredict, totalCorrect);

		sb.append(String.format("%s\t%d\t%d\t%d\t%s\t%s\t%s\n",

		"Macro", (int) totalAnswer, (int) totalPredict, (int) totalCorrect, nf.format(macroPrecision), nf.format(macroRecall),
				nf.format(macroF1)));

		sb.append(String.format("%s\t%d\t%d\t%d\t%s\t%s\t%s\n",

		"Micro", (int) totalAnswer, (int) totalPredict, (int) totalCorrect, nf.format(prf[0]), nf.format(prf[1]), nf.format(prf[2])));

		return sb.toString().trim();
	}

	public static double[] getPrecisionRecallF1(double numAnswer, double numPredict, double numCorrect) {
		double[] ret = new double[3];

		double precision = 0;
		double recall = 0;
		double f1 = 0;

		if (numPredict > 0) {
			precision = numCorrect / numPredict;
			ret[0] = precision;
		}

		if (numAnswer > 0) {
			recall = numCorrect / numAnswer;
			ret[1] = recall;
		}

		if (precision > 0 && recall > 0) {
			f1 = 2 * (precision * recall) / (precision + recall);
			ret[2] = f1;
		}

		return ret;
	}

	private List<String> answers;

	private List<Counter<String>> allPredicts;

	private Counter<String> topic_predictCount;

	private Counter<String> topic_answerCount;

	private Counter<String> topic_correctCount;

	private String output;

	private double macroF1;

	private double microF1;

	private int topK;

	boolean evaluteGlobal;

	public TopicEval(List<String> answers, List<Counter<String>> allPredicts) {
		this(answers, allPredicts, 1, false);
	}

	public TopicEval(List<String> answers, List<Counter<String>> allPredicts, int topK, boolean evaluteTopTopics) {
		this.answers = answers;

		this.allPredicts = allPredicts;

		this.topK = topK;

		this.evaluteGlobal = evaluteTopTopics;
	}

	public TopicEval(Pair<List<String>, List<Counter<String>>> pair) {
		this(pair.getFirst(), pair.getSecond(), 1, false);
	}

	public TopicEval(Pair<List<String>, List<Counter<String>>> pair, int topK) {
		this(pair.getFirst(), pair.getSecond(), topK, false);
	}

	public TopicEval(Pair<List<String>, List<Counter<String>>> pair, int topK, boolean evaluteTopTopics) {
		this(pair.getFirst(), pair.getSecond(), topK, evaluteTopTopics);
	}

	public Counter<String> allF1() {
		Counter<String> ret = new Counter<String>();
		for (String topic : new TreeSet<String>(topic_answerCount.keySet())) {
			double numPredict = topic_predictCount.getCount(topic);
			double numAnswer = topic_answerCount.getCount(topic);
			double numCorrect = topic_correctCount.getCount(topic);

			double[] scores = getPrecisionRecallF1(numAnswer, numPredict, numCorrect);
			double precision = scores[0];
			double recall = scores[1];
			double f1 = scores[2];
			ret.setCount(topic, f1);
		}
		return ret;
	}

	public void evaluate() {
		topic_predictCount = new Counter<String>();
		topic_answerCount = new Counter<String>();
		topic_correctCount = new Counter<String>();

		for (int i = 0; i < answers.size(); i++) {
			String answer = answers.get(i);
			Counter<String> allPredict = allPredicts.get(i);
			List<String> predicts = allPredict.getSortedKeys();

			if (evaluteGlobal) {
				answer = answer.split("-")[0];

				for (int j = 0; j < predicts.size(); j++) {
					String predict = predicts.get(j);
					predict = predict.split("-")[0];
					predicts.set(j, predict);
				}
			}

			int index = -1;

			for (int j = 0; j < topK; j++) {
				String pred = predicts.get(j);
				if (answer.equals(pred)) {
					index = j;
					break;
				}
			}

			String predict = index == -1 ? predicts.get(0) : predicts.get(index);
			topic_answerCount.incrementCount(answer, 1);
			topic_predictCount.incrementCount(predict, 1);

			if (answer.equals(predict)) {
				topic_correctCount.incrementCount(answer, 1);
			}
		}

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(4);

		double f1Sum = 0;
		double precisionSum = 0;
		double recallSum = 0;
		double numTopics = topic_answerCount.size();

		List<String> list = new ArrayList<String>();
		// list.add(String.format("[Top-%d]", topK));
		list.add(String.format("Topic\tAnswer\tPredict\tCorrect\tPrecision\tRecall\tF1"));

		for (String topic : new TreeSet<String>(topic_answerCount.keySet())) {
			double numPredict = topic_predictCount.getCount(topic);
			double numAnswer = topic_answerCount.getCount(topic);
			double numCorrect = topic_correctCount.getCount(topic);

			double[] scores = getPrecisionRecallF1(numAnswer, numPredict, numCorrect);
			double precision = scores[0];
			double recall = scores[1];
			double f1 = scores[2];

			f1Sum += f1;
			precisionSum += precision;
			recallSum += recall;

			list.add(String.format("%s\t%d\t%d\t%d\t%s\t%s\t%s",

			topic, (int) numAnswer, (int) numPredict, (int) numCorrect,

			nf.format(precision), nf.format(recall), nf.format(f1)));
		}

		int numTotalAnswers = (int) topic_answerCount.totalCount();
		int numTotalPredict = (int) topic_predictCount.totalCount();
		int numTotalCorrect = (int) topic_correctCount.totalCount();

		double macroPrecision = precisionSum / numTopics;
		double macroRecall = recallSum / numTopics;
		double macroF1 = f1Sum / numTopics;

		list.add(String.format("%s\t%d\t%d\t%d\t%s\t%s\t%s", "macro",

		numTotalAnswers, numTotalPredict, numTotalCorrect,

		nf.format(macroPrecision), nf.format(macroRecall), nf.format(macroF1)));

		double[] scores = getPrecisionRecallF1(numTotalAnswers, numTotalPredict, numTotalCorrect);
		double microPrecision = scores[0];
		double microRecall = scores[1];
		double microF1 = scores[2];

		list.add(String.format("%s\t%d\t%d\t%d\t%s\t%s\t%s", "micro",

		numTotalAnswers, numTotalPredict, numTotalCorrect,

		nf.format(microPrecision), nf.format(microRecall), nf.format(microF1)));
		output = StrUtils.join("\n", list);

		this.macroF1 = macroF1;

		this.microF1 = microF1;

	}

	public double macroF1() {
		return macroF1;
	}

	public double microF1() {
		return microF1;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(boolean summarize) {
		if (summarize) {
			String[] lines = output.split("\n");
			output = lines[lines.length - 2] + "\n" + lines[lines.length - 1];
		}
		return output;
	}
}