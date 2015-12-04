package ohs.matrix;

import java.io.ObjectOutputStream;
import java.io.Serializable;

public interface Matrix extends Serializable {

	public int colDim();

	public Vector column(int colId);

	public Vector columnSums();

	public int indexAtRowLoc(int rowLoc);

	public int label();

	public void normalizeColumns();

	public void normalizeRows();

	public Vector row(int rowId);

	public int rowDim();

	public int[] rowIndexes();

	public Vector[] rows();

	public int rowSize();

	public void rowSummation();

	public Vector rowSums();

	public void set(int rowId, int colId, double value);

	public void setColDim(int colDim);

	public void setLabel(int label);

	public void setRow(int rowId, Vector x);

	public void setRowDim(int rowDim);

	public void setVectorAtRowLoc(int loc, Vector x);

	public Vector vectorAtRowLoc(int rowLoc);

	public void write(ObjectOutputStream oos) throws Exception;

	public void write(String fileName) throws Exception;

}
