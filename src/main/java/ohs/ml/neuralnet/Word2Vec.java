package ohs.ml.neuralnet;

import java.util.List;

import ohs.io.FileUtils;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.NLPUtils;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.ml.neuralnet.Word2VecParam.Type;
import ohs.types.Indexer;
import ohs.utils.Generics;

public class Word2Vec {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String text = FileUtils.readText(MIRPath.WIKI_DIR + "wiki_cancer.txt");

		Indexer<String> wordIndexer = Generics.newIndexer();
		List<String> sents = NLPUtils.tokenize(text);

		SentenceCollection sc = new SentenceCollection();
		sc.create(sents);

		Word2VecParam param = new Word2VecParam(Type.SKIP_GRAM, 10, 5);

		Word2Vec word2Vec = new Word2Vec();
		word2Vec.train(param, sc);

		System.out.println("process ends.");
	}

	private Word2VecParam param;

	private double[][] inVecs;

	private double[][] outVecs;

	private double[][] grad;

	private double[] gradPred;

	private double[] predicted;

	private SentenceCollection sc;

	private double[][] gradIn;

	private double[][] gradOut;

	public Word2Vec() {
	}

	public void softmaxCostAndGradient(double[] predicted, int target, double[][] outVecs, SentenceCollection sc) {

	}

	public void negSamplingCostAndGradient(double[] predicted, int target, double[][] outVecs, SentenceCollection sc, int K) {

		int[] indexes = new int[K + 1];
		indexes[0] = target;

		for (int i = 0; i < K; i++) {
			int nw = sc.sampleWord();
			while (nw == target) {
				nw = sc.sampleWord();
			}
			indexes[i + 1] = nw;
		}

		double[] labels = new double[indexes.length];
		labels[0] = 1;

		for (int i = 1; i < labels.length; i++) {
			labels[i] = -1;
		}

		double[][] vecs = new double[indexes.length][];

		for (int i = 0; i < indexes.length; i++) {
			vecs[i] = outVecs[indexes[i]];
		}

		double[] t = new double[labels.length];

		ArrayMath.product(vecs, predicted, t);
		ArrayMath.multiply(t, labels, t);
		ArrayMath.sigmoid(t, t);

		double cost = -ArrayMath.sumAfterLog(t);

		double[] delta = new double[t.length];

		ArrayMath.add(t, -1, delta);
		ArrayMath.multiply(delta, labels, delta);

		ArrayMath.product(delta, vecs, gradPred);

		double[][] gradTemp = new double[labels.length][predicted.length];

		ArrayMath.outerProduct(delta, predicted, gradTemp);

		for (int i = 0; i < indexes.length; i++) {
			ArrayUtils.copy(gradTemp[i], grad[indexes[i]]);
		}

	}

	public void train() {
		int batch_size = 50;

		double cost = 0;
		int[] loc;
		int center_w = 0;
		int[] context = null;
		int cont_w = 0;
		int w = 0;

		for (int i = 0; i < batch_size; i++) {
			int C1 = ArrayMath.random(0, param.getContextSize());
			loc = sc.getRandomWordLoc();
			center_w = sc.getWord(loc);
			context = sc.getRandomContext(loc, C1);

			if (context.length == 0) {
				continue;
			}

			train(center_w, C1, context);

		}
	}

	public void train(int center_w, int C1, int[] context) {
		predicted = inVecs[center_w];

		for (int i = 0; i < context.length; i++) {
			int cont_w = context[i];

			negSamplingCostAndGradient(predicted, cont_w, outVecs, sc, 10);
		}
	}

	public void setup(Word2VecParam param, SentenceCollection sc) {
		this.param = param;
		this.sc = sc;

		sc.makeSampleTable(1000000);

		inVecs = new double[sc.getVocab().size()][param.getVectorSize()];
		outVecs = new double[sc.getVocab().size()][param.getVectorSize()];

		ArrayMath.random(0, 1, inVecs);

		ArrayMath.add(inVecs, -0.5, inVecs);

		predicted = new double[param.getVectorSize()];

		grad = new double[sc.getVocab().size()][param.getVectorSize()];

		gradPred = new double[predicted.length];

		gradIn = new double[sc.getVocab().size()][param.getVectorSize()];
		gradOut = new double[sc.getVocab().size()][param.getVectorSize()];
	}

	public void train(Word2VecParam param, SentenceCollection sc) {
		setup(param, sc);

		train();
	}

}
