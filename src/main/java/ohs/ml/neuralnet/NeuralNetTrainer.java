package ohs.ml.neuralnet;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;

public class NeuralNetTrainer {

	public static void main(String[] args) {
		System.out.println("process begins.");
		NeuralNetParams param = new NeuralNetParams();
		// param.setNumHiddenNeurons(50);

		NeuralNetTrainer nn = new NeuralNetTrainer(param);

		int data_size = 50;
		int label_size = param.getNumOutputNeurons();
		int feat_size = param.getNumInputNeurons();

		double[][] X = ArrayMath.random(0, 1, data_size, feat_size);
		double[][] Y = new double[data_size][label_size];

		int[] labels = ArrayMath.random(0, label_size, data_size);

		for (int i = 0; i < Y.length; i++) {
			int label = labels[i];
			Y[i][label] = 1;
		}

		// nn.trainFullBatch(X, Y, 100);
		nn.trainMiniBatch(X, Y, 100);

		System.out.println("process ends.");
	}

	private double[][] W1;

	private double[][] W2;

	private double[] b1;

	private double[] b2;

	private NeuralNetModel model;

	private NeuralNetParams param;

	public NeuralNetTrainer() {
		this(new NeuralNetParams());
	}

	public NeuralNetTrainer(NeuralNetParams param) {
		this.param = param;

		model = new NeuralNetModel(param.getNumInputNeurons(), param.getNumHiddenNeurons(), param.getNumOutputNeurons());
		model.init();

		W1 = model.getW1();
		W2 = model.getW2();
		b1 = model.getB1();
		b2 = model.getB2();
	}

	public NeuralNetModel trainFullBatch(double[][] X, double[][] Y, int num_iters) {
		double[][] H = new double[X.length][param.getNumHiddenNeurons()];
		double[][] Yh = new double[X.length][param.getNumOutputNeurons()];

		double[][] D1 = new double[X.length][param.getNumHiddenNeurons()];
		double[][] D2 = new double[X.length][param.getNumOutputNeurons()];

		double[][] GW1 = new double[param.getNumOutputNeurons()][param.getNumHiddenNeurons()];
		double[][] GW2 = new double[param.getNumHiddenNeurons()][param.getNumOutputNeurons()];

		double[] gb1 = new double[param.getNumHiddenNeurons()];
		double[] gb2 = new double[param.getNumOutputNeurons()];

		double weight_decay = (1 - param.getLearningRate() * param.getRegularizeMixture() / X.length);
		double scale_factor = -param.getLearningRate() / X.length;

		double cost = 0;
		int num_correct = 0;

		for (int i = 0; i < num_iters; i++) {
			ArrayMath.product(X, W1, H);
			ArrayMath.add(H, b1, H);
			ArrayMath.sigmoid(H, H);

			ArrayMath.product(H, W2, Yh);
			ArrayMath.add(Yh, b2, Yh);
			ArrayMath.softmax(Yh, Yh);

			cost = 0;
			num_correct = 0;

			for (int j = 0; j < Y.length; j++) {
				int ans = ArrayMath.argMax(Y[j]);
				int pred = ArrayMath.argMax(Yh[j]);

				if (ans == pred) {
					num_correct++;
				}

				for (int k = 0; k < Y[j].length; k++) {
					if (Y[j][k] != 0) {
						cost -= Math.log(Yh[j][k]);
					}
				}
			}

			double accuracy = 1f * num_correct / X.length;

			System.out.printf("cost:\t%f, accuracy:\t%f (%d/%d)\n", cost, accuracy, num_correct, X.length);

			// double[][] D2 = new double[X.length][param.getNumOutputNeurons()];

			ArrayMath.subtract(Yh, Y, D2);
			// ArrayMath.product(ArrayMath.transpose(H), D2, GW2);
			ArrayMath.productNonsyncA(H, D2, GW2);
			ArrayMath.sumColumns(GW2, gb2);

			/*
			 * D (20 x 10) x (10 x 5) = (20 x 5)
			 * 
			 */

			// ArrayMath.product(D2, ArrayMath.transpose(W2), D1);
			ArrayMath.productNonsyncB(D2, W2, D1);

			// ArrayMath.multiply(D1, ArrayMath.sigmoidGradient(H), D1);
			ArrayMath.sigmoidGradient(H, H);
			ArrayMath.multiply(D1, H, D1);

			// ArrayMath.product(ArrayMath.transpose(X), D1, GW1);
			ArrayMath.productNonsyncA(X, D1, GW1);
			ArrayMath.sumColumns(GW1, gb1);

			ArrayMath.addAfterScale(W1, weight_decay, GW1, scale_factor, W1);
			ArrayMath.addAfterScale(W2, weight_decay, GW2, scale_factor, W2);

			ArrayMath.addAfterScale(b1, 1, gb1, scale_factor, b1);
			ArrayMath.addAfterScale(b2, 1, gb2, scale_factor, b2);
		}

		return model;
	}

	public NeuralNetModel trainMiniBatch(double[][] X, double[][] Y, int num_iters) {
		double[][] GW1 = new double[param.getNumInputNeurons()][param.getNumHiddenNeurons()];
		double[][] GW2 = new double[param.getNumHiddenNeurons()][param.getNumOutputNeurons()];

		double[] gb1 = new double[param.getNumHiddenNeurons()];
		double[] gb2 = new double[param.getNumOutputNeurons()];

		double[][] tmpGW1 = new double[param.getNumInputNeurons()][param.getNumHiddenNeurons()];
		double[][] tmpGW2 = new double[param.getNumHiddenNeurons()][param.getNumOutputNeurons()];

		double[] x;
		double[] y;
		double[] h = new double[param.getNumHiddenNeurons()];
		double[] yh = new double[param.getNumOutputNeurons()];
		double[] d1 = new double[param.getNumHiddenNeurons()];
		double[] d2 = new double[param.getNumOutputNeurons()];

		double cost = 0;
		double batch_cost = 0;
		int num_correct = 0;

		int mini_batch_size = 10;
		int current_mini_batch_size = 0;
		double scale_factor = 0;

		double weight_decay = (1 - param.getLearningRate() * param.getRegularizeMixture() / X.length);

		for (int iter = 0; iter < num_iters; iter++) {
			batch_cost = 0;
			num_correct = 0;
			cost = 0;

			for (int i = 0; i < X.length; i++) {
				x = X[i];
				y = Y[i];

				ArrayMath.product(x, W1, h);
				ArrayMath.add(h, b1, h);
				ArrayMath.sigmoid(h, h);

				ArrayMath.product(h, W2, yh);
				ArrayMath.add(yh, b2, yh);
				ArrayMath.softmax(yh, yh);

				if (ArrayMath.argMax(y) == ArrayMath.argMax(yh)) {
					num_correct++;
				}

				for (int k = 0; k < y.length; k++) {
					if (y[k] != 0) {
						batch_cost -= Math.log(yh[k]);
					}
				}

				ArrayMath.subtract(yh, y, d2);

				ArrayMath.outerProduct(h, d2, tmpGW2);

				ArrayMath.product(W2, d2, d1);

				ArrayMath.sigmoidGradient(h, h);

				ArrayMath.multiply(d1, h, d1);

				ArrayMath.outerProduct(x, d1, tmpGW1);

				ArrayMath.add(tmpGW1, GW1, GW1);
				ArrayMath.add(tmpGW2, GW2, GW2);

				int mod = (i + 1) % mini_batch_size;

				current_mini_batch_size = mod == 0 ? mini_batch_size : mod;

				if (mod == 0 || i == X.length - 1) {
					scale_factor = -param.getLearningRate() / current_mini_batch_size;

					ArrayMath.sumColumns(GW1, gb1);
					ArrayMath.sumColumns(GW2, gb2);

					ArrayMath.addAfterScale(W1, weight_decay, GW1, scale_factor, W1);
					ArrayMath.addAfterScale(W2, weight_decay, GW2, scale_factor, W2);

					ArrayMath.addAfterScale(b1, 1, gb1, scale_factor, b1);
					ArrayMath.addAfterScale(b2, 1, gb2, scale_factor, b2);

					cost += (batch_cost / current_mini_batch_size);

					batch_cost = 0;

					ArrayUtils.setAll(GW1, 0);
					ArrayUtils.setAll(GW2, 0);
					ArrayUtils.setAll(gb1, 0);
					ArrayUtils.setAll(gb2, 0);
				}
			}

			double accuracy = 1f * num_correct / X.length;

			System.out.printf("cost:\t%f, accuracy:\t%f (%d/%d)\n", cost, accuracy, num_correct, X.length);

		}

		return model;
	}

}
