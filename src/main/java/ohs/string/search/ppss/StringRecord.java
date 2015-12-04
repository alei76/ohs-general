package ohs.string.search.ppss;

import java.io.Serializable;

/**
 * A container class for a string.
 * 
 * @author Heung-Seon Oh
 */
public class StringRecord implements Serializable {

	private int id;

	private String s;

	public StringRecord(int id, String s) {
		super();
		this.id = id;
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
		StringRecord other = (StringRecord) obj;
		if (id != other.id)
			return false;
		if (s == null) {
			if (other.s != null)
				return false;
		} else if (!s.equals(other.s))
			return false;
		return true;
	}

	public int getId() {
		return id;
	}

	public String getString() {
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((s == null) ? 0 : s.hashCode());
		return result;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setString(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return String.format("(%d, %s)", id, s);
	}

}
