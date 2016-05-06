package ohs.ml.neuralnet;

import ohs.math.ArrayMath;

public class Word2Vec {

	public static void main(String[] args) {
		Word2Vec nn = new Word2Vec();

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

	private double[][] W1;

	private double[][] W2;

	private double[] b1;

	private double[] b2;

	private NeuralNetParams param = new NeuralNetParams();

	public Word2Vec() {
		W1 = new double[param.getNumInputNeurons()][param.getNumHiddenNeurons()];
		W2 = new double[param.getNumHiddenNeurons()][param.getNumOutputNeurons()];
		b1 = new double[param.getNumHiddenNeurons()];
		b2 = new double[param.getNumOutputNeurons()];

		ArrayMath.random(0, 1, W1);
		ArrayMath.random(0, 1, W2);
		ArrayMath.random(0, 1, b1);
		ArrayMath.random(0, 1, b2);

	}

	public void train(double[][] xs, double[][] ys) {
		double[] h = new double[param.getNumHiddenNeurons()];
		double[] z1 = new double[h.length];
		double[] z2 = new double[param.getNumOutputNeurons()];

		double[] yh = new double[param.getNumOutputNeurons()];
		double cost = 0;
		

		for (int i = 0; i < xs.length; i++) {
			ArrayMath.product(W1, xs[i], z1);
			ArrayMath.add(z1, b1, z1);
			ArrayMath.sigmoid(z1, h);

			ArrayMath.product(W2, h, z2);
			ArrayMath.add(z2, b2, z2);
			ArrayMath.softmax(z2, yh);
			cost -= ArrayMath.crossEntropy(ys[i], yh);
		}
		
		
	}
	
	public void train2(double[] x, double[] y) {
		
	}


}
