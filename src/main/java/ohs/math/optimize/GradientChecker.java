package ohs.math.optimize;

import ohs.math.ArrayMath;

public class GradientChecker {

	private Function f;

	private double h = 1e-4;

	private double cutoff = 1e-5;

	public GradientChecker(Function f) {
		this(f, 1e-4);
	}

	public GradientChecker(Function f, double h) {
		this.f = f;
		this.h = h;
	}

	public boolean check(double[] a) {
		double grad = 0;
		double fxh1 = 0;
		double fxh2 = 0;
		double num_grad = 0;
		double grad_diff = 0;
		double[] tmp = new double[3];
		tmp[0] = 1;

		for (int i = 0; i < a.length; i++) {
			grad = f.gradient(a[i]);
			fxh1 = f.value(a[i] + h);
			fxh2 = f.value(a[i] - h);
			num_grad = (fxh1 - fxh2) / 2 / h;

			tmp[1] = Math.abs(grad);
			tmp[2] = Math.abs(num_grad);

			grad_diff = Math.abs(num_grad - grad) / ArrayMath.max(tmp);

			if (grad_diff > cutoff) {
				StringBuffer sb = new StringBuffer();
				sb.append(String.format("First gradient error found at index %s\n", i));
				sb.append(String.format("Your gradient: %f \t Numerical gradient: %f", grad, num_grad));
				System.out.println(sb.toString());

				return false;
			}
		}

		return true;
	}

}
