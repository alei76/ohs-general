package ohs.math.optimize;

import ohs.math.CommonFuncs;

public class Sigmoid implements Function {

	@Override
	public double gradient(double a) {
		return CommonFuncs.sigmoidGradient(a);
	}

	@Override
	public double gradient(double[] a, double[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			b[i] = CommonFuncs.sigmoidGradient(a[i]);
			sum += b[i];
		}
		return sum;
	}

	@Override
	public double value(double a) {
		return CommonFuncs.sigmoid(a);
	}

	@Override
	public double value(double[] a, double[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			b[i] = CommonFuncs.sigmoid(a[i]);
			sum += b[i];
		}
		return sum;
	}

}
