package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TextSpan implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3963874236917138978L;

	protected int start;

	protected String s;

	public TextSpan(int start, String s) {
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
		TextSpan other = (TextSpan) obj;
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

	public void read(ObjectInputStream ois) throws Exception {
		start = ois.readInt();
		s = ois.readUTF();
	}

	public void set(int start, String s) {
		this.start = start;
		this.s = s;
	}

	@Override
	public String toString() {
		return String.format("<%d-%d:\t%s>", getStart(), getEnd(), s);
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(start);
		oos.writeUTF(s);
	}

}
