package ohs.ml.neuralnet;

import ohs.math.ArrayMath;

public class NeuralNet {

	public static void main(String[] args) {
		System.out.println("process begins.");
		NeuralNetParams param = new NeuralNetParams();

		NeuralNet nn = new NeuralNet(param);

		int data_size = 20;
		int label_size = param.getNumOutputNeurons();
		int feat_size = param.getNumInputNeurons();

		double[][] xs = ArrayMath.random(0, 1, data_size, feat_size);
		double[][] ys = new double[data_size][label_size];

		int[] labels = ArrayMath.random(0, label_size - 1, data_size);

		for (int i = 0; i < ys.length; i++) {
			int label = labels[i];
			ys[i][label] = 1;
		}

		nn.train(xs, ys);

		System.out.println("process ends.");
	}

	private double[][] W1;

	private double[][] W2;

	private double[] b1;

	private double[] b2;

	private NeuralNetParams param;

	public NeuralNet() {
		this(new NeuralNetParams());
	}

	public NeuralNet(NeuralNetParams param) {
		this.param = param;

		W1 = new double[param.getNumInputNeurons()][param.getNumHiddenNeurons()];
		W2 = new double[param.getNumHiddenNeurons()][param.getNumOutputNeurons()];
		b1 = new double[param.getNumHiddenNeurons()];
		b2 = new double[param.getNumOutputNeurons()];

		ArrayMath.random(0, 1, W1);
		ArrayMath.random(0, 1, W2);
		ArrayMath.random(0, 1, b1);
		ArrayMath.random(0, 1, b2);
	}

	public void train(double[][] X, double[][] Y) {
		double[][] H = new double[X.length][param.getNumHiddenNeurons()];
		double[][] Yh = new double[X.length][param.getNumOutputNeurons()];
		double cost = 0;

		ArrayMath.product(X, W1, H);
		ArrayMath.add(H, b1, H);
		ArrayMath.sigmoid(H, H);

		ArrayMath.product(H, W2, Yh);
		ArrayMath.add(Yh, b2, Yh);
		ArrayMath.softmax(Yh, Yh);

		for (int i = 0; i < Y.length; i++) {
			for (int j = 0; j < Y[i].length; j++) {
				if (Y[i][j] != 0) {
					cost -= Math.log(Yh[i][j]);
				}
			}
		}

		/*
		 * D (20 x 10)
		 * 
		 * X (20 x 10)
		 * 
		 * Y (20 x 10)
		 * 
		 * Yh(20 x 10)
		 * 
		 * W1(10 x 5)
		 * 
		 * W2(5 x 10)
		 * 
		 * GW2(5 x 10)
		 * 
		 * gb2 (10)
		 * 
		 * GW1 (10 x 20) x (20 x 5) = (10 x 5)
		 * 
		 * gb1 (5)
		 */

		double[][] D = new double[X.length][param.getNumOutputNeurons()];
		double[][] GW2 = new double[param.getNumHiddenNeurons()][param.getNumOutputNeurons()];
		double[][] GW1 = new double[param.getNumOutputNeurons()][param.getNumHiddenNeurons()];
		double[] gb1 = new double[param.getNumHiddenNeurons()];
		double[] gb2 = new double[param.getNumOutputNeurons()];

		ArrayMath.substract(Yh, Y, D);
		ArrayMath.product(ArrayMath.transpose(H), D, GW2);
		ArrayMath.sumColumns(GW2, gb2);

		/*
		 * D (20 x 10) x (10 x 5) = (20 x 5)
		 * 
		 */

		D = ArrayMath.product(D, ArrayMath.transpose(W2));
		ArrayMath.multiply(D, ArrayMath.sigmoidGradient(H), D);
		ArrayMath.product(ArrayMath.transpose(X), D, GW1);
		ArrayMath.sumColumns(GW1, gb1);
	}

	public void train2(double[] x, double[] y) {

	}

}
