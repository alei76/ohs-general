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

		for (int i = 0; i < x.length; i++) {
			v = x[i];
			grad = 0;
			cost = 0;
			step = this.step;

			for (int j = 1; j <= num_iters; j++) {
				cost = f.value(v);
				grad = f.gradient(v);
				v -= step * grad;

				if (j % anneal_step == 0) {
					step *= 0.5;
				}
			}
			y[i] = v;
		}
	}

}
