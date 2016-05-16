package ohs.math.optimize;

public interface Function {

	public double cost = 0;

	public default double cost() {
		return cost;
	}

	public double gradient(double a);

	public double gradient(double[] a, double[] b);

	public double gradient(double[][] a, double[][] b);

	public double value(double a);

	public double value(double[] a, double[] b);

	public double value(double[][] a, double[][] b);
}
