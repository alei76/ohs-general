package ohs.ml.neuralnet;

import ohs.math.ArrayMath;

public class NeuralNet {

	public static void main(String[] args) {
		NeuralNetParams param = new NeuralNetParams();

		NeuralNet nn = new NeuralNet(param);

		int data_size = 100;
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

		double[][] D = new double[X.length][param.getNumOutputNeurons()];
		double[][] GW2 = new double[param.getNumHiddenNeurons()][param.getNumOutputNeurons()];
		double[][] HT = ArrayMath.transpose(H);

		ArrayMath.substract(Yh, Y, D);
		ArrayMath.product(ArrayMath.transpose(H), D, GW2);

		D = ArrayMath.product(D, ArrayMath.transpose(W2));

		ArrayMath.multiply(D, ArrayMath.sigmoidGradient(H), D);

		// for (int i = 0; i < X.length; i++) {
		// ArrayMath.product(W1, X[i], Z1[i]);
		// ArrayMath.add(Z1[i], b1, Z1[i]);
		// ArrayMath.sigmoid(Z1[i], H[i]);
		//
		// ArrayMath.product(W1, H[i], Z2[i]);
		// ArrayMath.add(Z2[i], b2, Z2[i]);
		// ArrayMath.softmax(Z2[i], Yh[i]);
		// ArrayMath.log(Yh[i], tmp);
		//
		// cost -= ArrayMath.dotProduct(tmp, Y[i]);
		// }

		// double[][] delta1 = new double[X.length][param.getNumOutputNeurons()];
		// double[][] gradW2 = new double[param.getNumHiddenNeurons()][param.getNumOutputNeurons()];
		//
		// ArrayMath.addAfterScale(Yh, 1, Y, -1, delta1);

	}

	public void train2(double[] x, double[] y) {

	}

}
