package ohs.matrix;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public interface Matrix extends Serializable {

	public int colDim();

	public Vector column(int j);

	public Vector columnSums();

	public int indexAtLoc(int loc);

	public String info();

	public void normalizeColumns();

	public void normalizeRows();

	public Vector row(int i);

	public Vector rowAtLoc(int loc);

	public int rowDim();

	public int[] rowIndexes();

	public Vector[] rows();

	public int rowSize();

	public void rowSummation();

	public Vector rowSums();

	public void set(int i, int j, double value);

	public void setColDim(int dim);

	public void setRow(int i, Vector x);

	public void setRowAtLoc(int loc, Vector x);

	public void setRowDim(int dim);

	public double[][] values();

	public void writeObject(ObjectOutputStream oos) throws Exception;

	public void writeObject(String fileName) throws Exception;

	public void readObject(ObjectInputStream ois) throws Exception;

	public void readObject(String fileName) throws Exception;

}
