package ohs.ml.neuralnet;

import ohs.math.ArrayMath;

public class NeuralNetParams {

	private int input_neuron_size = 10;

	private int hidden_neuron_size = 5;

	private int output_neuron_size = 10;

	private double learning_rate = 0.05;

	private double regularize_mixture = 0.1;

	public NeuralNetParams() {

	}

	public NeuralNetParams(int num_input_neurons, int num_hidden_neurons, int num_output_neurons, double learning_rate) {
		super();
		this.input_neuron_size = num_input_neurons;
		this.hidden_neuron_size = num_hidden_neurons;
		this.output_neuron_size = num_output_neurons;
		this.learning_rate = learning_rate;
	}



	public double getRegularizeMixture() {
		return regularize_mixture;
	}

	public void setRegularizeMixture(double regularize_mixture) {
		this.regularize_mixture = regularize_mixture;
	}

	public double getLearningRate() {
		return learning_rate;
	}

	public int getNumHiddenNeurons() {
		return hidden_neuron_size;
	}

	public int getNumInputNeurons() {
		return input_neuron_size;
	}

	public int getNumOutputNeurons() {
		return output_neuron_size;
	}

	public void setLearningRate(double learning_rate) {
		this.learning_rate = learning_rate;
	}

	public void setNumHiddenNeurons(int num_hidden_neurons) {
		this.hidden_neuron_size = num_hidden_neurons;
	}

	public void setNumInputNeurons(int num_input_neurons) {
		this.input_neuron_size = num_input_neurons;
	}

	public void setNumOutputNeurons(int num_output_neurons) {
		this.output_neuron_size = num_output_neurons;
	}

}
