package ohs.ml.neuralnet;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ohs.io.FileUtils;
import ohs.math.ArrayMath;

public class NeuralNetModel {

	/**
	 * input to hidden
	 */
	private double[][] W1;

	/**
	 * hiddent to output
	 */
	private double[][] W2;

	/**
	 * hidden bias
	 */
	private double[] b1;

	/**
	 * output bias
	 */
	private double[] b2;

	private double[] h;

	public NeuralNetModel() {

	}

	public NeuralNetModel(double[][] W1, double[][] W2, double[] b1, double[] b2) {
		this.W1 = W1;
		this.W2 = W2;
		this.b1 = b1;
		this.b2 = b2;
	}

	public NeuralNetModel(int num_input_neurons, int num_hidden_neurons, int num_output_neurons) {
		W1 = new double[num_input_neurons][num_hidden_neurons];
		W2 = new double[num_hidden_neurons][num_output_neurons];
		b1 = new double[num_hidden_neurons];
		b2 = new double[num_output_neurons];
	}

	public double[] getB1() {
		return b1;
	}

	public double[] getB2() {
		return b2;
	}

	public double[][] getW1() {
		return W1;
	}

	public double[][] getW2() {
		return W2;
	}

	public void init() {
		ArrayMath.random(0, 1, W1);
		ArrayMath.random(0, 1, W2);
		ArrayMath.random(0, 1, b1);
		ArrayMath.random(0, 1, b2);

		ArrayMath.add(W1, -0.5, W1);
		ArrayMath.add(W2, -0.5, W2);
		ArrayMath.add(b1, -0.5, b1);
		ArrayMath.add(b2, -0.5, b2);
	}

	public double[] predict(double[] x) {
		if (h == null) {
			h = new double[b1.length];
		}

		double[] yh = new double[b2.length];

		ArrayMath.product(x, W1, h);
		ArrayMath.add(h, b1, h);
		ArrayMath.sigmoid(h, h);

		ArrayMath.product(h, W2, yh);
		ArrayMath.add(yh, b2, yh);
		ArrayMath.softmax(yh, yh);
		return yh;
	}

	public double[][] predict(double[][] X) {
		if (h == null) {
			h = new double[b1.length];
		}
		double[][] Yh = new double[X.length][];

		for (int i = 0; i < X.length; i++) {
			double[] yh = new double[b2.length];
			ArrayMath.product(X[i], W1, h);
			ArrayMath.add(h, b1, h);
			ArrayMath.sigmoid(h, h);

			ArrayMath.product(h, W2, yh);
			ArrayMath.add(yh, b2, yh);
			ArrayMath.softmax(yh, yh);
			Yh[i] = yh;
		}
		return Yh;
	}

	public void readObject(ObjectInputStream ois) throws Exception {
		W1 = FileUtils.readDoubleMatrix(ois);
		W2 = FileUtils.readDoubleMatrix(ois);
		b1 = FileUtils.readDoubleArray(ois);
		b2 = FileUtils.readDoubleArray(ois);
	}

	public void readObject(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		readObject(ois);
		ois.close();
	}

	public void writeObject(ObjectOutputStream oos) throws Exception {
		FileUtils.writeDoubleMatrix(oos, W1);
		FileUtils.writeDoubleMatrix(oos, W2);
		FileUtils.writeDoubleArray(oos, b1);
		FileUtils.writeDoubleArray(oos, b2);
	}

	public void writeObject(String fileName) throws Exception {
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		writeObject(oos);
		oos.close();
	}
}
