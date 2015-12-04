package ohs.neuralnet;

public class NeuralNetParams {

	private int num_hidden_layers = 1;

	private int num_input_neurons = 5;

	private int num_hidden_neurons = 3;

	private int num_output_neurons = 5;

	private double learning_rate = 0.05;
	
	public NeuralNetParams(){
		
	}

	public NeuralNetParams(int num_hidden_layers, int num_input_neurons, int num_hidden_neurons, int num_output_neurons,
			double learning_rate) {
		super();
		this.num_hidden_layers = num_hidden_layers;
		this.num_input_neurons = num_input_neurons;
		this.num_hidden_neurons = num_hidden_neurons;
		this.num_output_neurons = num_output_neurons;
		this.learning_rate = learning_rate;
	}

	public double getLearningRate() {
		return learning_rate;
	}

	public int getNumHiddenLayers() {
		return num_hidden_layers;
	}

	public int getNumHiddenNeurons() {
		return num_hidden_neurons;
	}

	public int getNumInputNeurons() {
		return num_input_neurons;
	}

	public int getNumOutputNeurons() {
		return num_output_neurons;
	}

	public void setLearningRate(double learning_rate) {
		this.learning_rate = learning_rate;
	}

	public void setNumHiddenLayers(int num_hidden_layers) {
		this.num_hidden_layers = num_hidden_layers;
	}

	public void setNumHiddenNeurons(int num_hidden_neurons) {
		this.num_hidden_neurons = num_hidden_neurons;
	}

	public void setNumInputNeurons(int num_input_neurons) {
		this.num_input_neurons = num_input_neurons;
	}

	public void setNumOutputNeurons(int num_output_neurons) {
		this.num_output_neurons = num_output_neurons;
	}

}
