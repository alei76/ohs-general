package ohs.neuralnet;

import ohs.math.ArrayMath;

public class NeuralNet {

	private double[][] W1;

	private double[][] W2;

	private double[] b1;

	private double[] b2;

	private NeuralNetParams param = new NeuralNetParams();

	public static void main(String[] args) {
		NeuralNet nn = new NeuralNet();

		int num_data = 100;
		int num_labels = 5;

		double[][] xs = ArrayMath.random(0, 1, num_data, num_labels);
		double[][] ys = new double[num_data][num_labels];

		int[] labels = ArrayMath.random(0, 4, num_data);

		for (int i = 0; i < ys.length; i++) {
			int label = labels[i];
			ys[i][label] = 1;
		}

		nn.train(xs, ys);

	}

	public NeuralNet() {
		W1 = new double[param.getNumHiddenNeurons()][param.getNumInputNeurons()];
		W2 = new double[param.getNumOutputNeurons()][param.getNumHiddenNeurons()];
		b1 = new double[param.getNumHiddenNeurons()];
		b2 = new double[param.getNumOutputNeurons()];

		ArrayMath.random(0, 1, W1);
		ArrayMath.random(0, 1, W2);
		ArrayMath.random(0, 1, b1);
		ArrayMath.random(0, 1, b2);

	}

	public void train(double[][] xs, double[][] ys) {
		double[][] h = new double[xs.length][param.getNumHiddenNeurons()];
		double[][] z1 = new double[xs.length][param.getNumHiddenNeurons()];
		double[][] z2 = new double[xs.length][param.getNumOutputNeurons()];

		double[][] yh = new double[xs.length][param.getNumOutputNeurons()];
		double cost = 0;

		for (int i = 0; i < xs.length; i++) {
			ArrayMath.product(W1, xs[i], z1[i]);
			ArrayMath.add(z1[i], b1, z1[i]);
			ArrayMath.sigmoid(z1[i], h[i]);

			ArrayMath.product(W2, h[i], z2[i]);
			ArrayMath.add(z2[i], b2, z2[i]);
			ArrayMath.softmax(z2[i], yh[i]);
			cost += ArrayMath.crossEntropy(ys[i], yh[i]);
		}

	}

}
