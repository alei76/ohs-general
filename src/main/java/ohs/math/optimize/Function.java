package ohs.math.optimize;

public interface Function {

	public double gradient(double a);

	public double gradient(double[] a, double[] b);

	public double value(double a);

	public double value(double[] a, double[] b);
}
