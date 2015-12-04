package ohs.ling.struct;

import java.io.Serializable;

public class Span implements Serializable {

	protected int start;

	protected String s;

	public Span(int start, String s) {
		super();
		this.start = start;
		this.s = s;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Span other = (Span) obj;
		if (s == null) {
			if (other.s != null)
				return false;
		} else if (!s.equals(other.s))
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	public int getEnd() {
		return start + s.length();
	}

	public int getStart() {
		return start;
	}

	public String getString() {
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((s == null) ? 0 : s.hashCode());
		result = prime * result + start;
		return result;
	}

	public int length() {
		return s.length();
	}

	public void set(int start, String s) {
		this.start = start;
		this.s = s;
	}

	public String toString() {
		return String.format("%d-%d\t%s", getStart(), getEnd(), s);
	}

}
