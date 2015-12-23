package ohs.string.search.ppss;

import java.io.Serializable;

/**
 * @author ohs
 * 
 *         A container class for q-gram
 *
 */
public class Gram implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7094138314372356349L;

	public static enum Type {
		NONE, PREFIX, SUFFIX, PIVOT;

		public static Type parse(String symbol) {
			Type ret = NONE;

			if (symbol.equals("PR")) {
				ret = PREFIX;
			} else if (symbol.equals("PI")) {
				ret = PIVOT;
			} else if (symbol.equals("SU")) {
				ret = SUFFIX;
			}

			return ret;
		}

		public String getSymbol() {
			String ret = "";

			if (this == PREFIX) {
				ret = "PR";
			} else if (this == PIVOT) {
				ret = "PI";
			} else if (this == SUFFIX) {
				ret = "SU";
			} else if (this == NONE) {
				ret = "NO";
			}

			return ret;
		}
	}

	/**
	 * a string for gram
	 */
	private String g;

	/**
	 * Start position of the gram in a string
	 */
	private int start;

	/**
	 * Type should be a one of PREFIX, SUFFIX, and PIVOT
	 */
	private Type type;

	public Gram(String g, int start, Type type) {
		super();
		this.g = g;
		this.start = start;
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Gram other = (Gram) obj;
		if (g == null) {
			if (other.g != null)
				return false;
		} else if (!g.equals(other.g))
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	public int getStart() {
		return start;
	}

	public String getString() {
		return g;
	}

	public Type getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((g == null) ? 0 : g.hashCode());
		result = prime * result + start;
		return result;
	}

	/**
	 * Start position of the gram in a string
	 * 
	 * @param start
	 */
	public void setStart(int start) {
		this.start = start;
	}

	public void setString(String s) {
		this.g = s;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return String.format("(%s, %d, %s)", g, start, type);
	}

}
