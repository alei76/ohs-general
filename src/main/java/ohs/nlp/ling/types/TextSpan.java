package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TextSpan implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3963874236917138978L;

	protected int start = 0;

	protected String text = "";

	public TextSpan() {

	}

	public TextSpan(int start, String text) {
		super();
		this.start = start;
		this.text = text;
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
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	public int getEnd() {
		return start + text.length();
	}

	public int getStart() {
		return start;
	}

	public String getText() {
		return text;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + start;
		return result;
	}

	public int length() {
		return text.length();
	}

	public void read(ObjectInputStream ois) throws Exception {
		start = ois.readInt();
		text = ois.readUTF();
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setText(String text) {
		this.setText(text);
	}

	@Override
	public String toString() {
		return String.format("<%d-%d:\t%s>", getStart(), getEnd(), text);
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(start);
		oos.writeUTF(text);
	}

}
