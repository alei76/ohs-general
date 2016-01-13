package ohs.string.search.ppss;

import java.io.Serializable;

import ohs.string.search.ppss.Gram.Type;

/**
 * @author Heung-Seon Oh
 */
public class GramPostingEntry implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7090960881807849863L;

	private int id;

	private int start;

	private Type type;

	public GramPostingEntry(int id, int start, Type type) {
		this.id = id;
		this.start = start;
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public int getStart() {
		return start;
	}

	public Type getType() {
		return type;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return String.format("(%d, %d, %s)", id, start, type.getSymbol());
	}

}
