package ohs.math.optimize;

public class StochasticGradientDescent {

	private Function f;

	private double step = 0.01;

	private int num_iters = 1000;

	private int anneal_step = 10000;

	public StochasticGradientDescent(Function f) {
		this(f, 0.01, 1000);
	}

	public StochasticGradientDescent(Function f, double step, int num_iters) {
		this.f = f;
		this.step = step;
		this.num_iters = num_iters;
	}

	public void run(double[] x, double[] y) {
		double v = 0;
		double grad = 0;
		double cost = 0;
		double step = 0;
		
		for(int i = 0; i < num_iters;i++){
			
		}

	}

}
