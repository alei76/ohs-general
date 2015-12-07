package ohs.ir.medical.clef.ehealth_2014;

public class CountPropagation {

	public static double gaussianKernel(int i, int j, double sigma) {
		return Math.exp(-Math.pow((i - j), 2) / (2 * Math.pow(sigma, 2)));
	}

	public static double hal(int i, int j, int windowSize) {
		return windowSize - (j - i) + 1;
	}
}
