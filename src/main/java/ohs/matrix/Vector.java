package ohs.matrix;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;

/**
 * @author Heung-Seon Oh
 * 
 */

public interface Vector extends Serializable {

	public int argMax();

	public int argMin();

	public Vector copy();

	public int dim();

	public void increment(int i, double v);

	public void incrementAll(double v);

	public void incrementAtLoc(int loc, double v);

	public void incrementAtLoc(int loc, int i, double v);

	public int indexAtLoc(int loc);

	public int[] indexes();

	public void keepAbove(double cutoff);

	public void keepTopN(int topN);

	public int label();

	public int location(int index);

	public double max();

	public double min();

	public void normalize();

	public void normalizeAfterSummation();

	public void normalizeByL2Norm();

	public double prob(int index);

	public double probAlways(int index);

	public double probAtLoc(int loc);

	public void prune(final Set<Integer> toRemove);

	public void pruneExcept(final Set<Integer> toKeep);

	public Vector ranking();

	public void scale(double factor);

	public void scale(int i, double factor);

	public void scaleAtLoc(int loc, double factor);

	public void scaleAtLoc(int loc, int i, double factor);

	public void set(int i, double v);

	public void setAll(double v);

	public void setAtLoc(int loc, double v);

	public void setAtLoc(int loc, int i, double v);

	public void setDim(int dim);

	public void setIndexes(int[] indexes);

	public void setLabel(int label);

	public void setSum(double sum);

	public void setValues(double[] values);

	public int size();

	public int sizeOfNonzero();

	public double sum();

	public void summation();

	public double value(int index);

	public double valueAlways(int index);

	public double valueAtLoc(int loc);

	public double[] values();

	public void write(ObjectOutputStream oos) throws Exception;

	public void write(String fileName) throws Exception;

}
