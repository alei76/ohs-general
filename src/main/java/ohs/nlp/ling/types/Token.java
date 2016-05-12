package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import ohs.io.FileUtils;
import ohs.utils.StrUtils;

public class Token implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4675926604151002510L;

	public static final String DELIM_TOKEN = " / ";

	protected String[] values = new String[0];

	protected int start = 0;

	public Token() {

	}

	public Token(int start, String word) {
		this();
		this.start = start;
		set(TokenAttr.WORD, word);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Token other = (Token) obj;
		if (!Arrays.equals(values, other.values))
			return false;
		return true;
	}

	public String[] get() {
		return get(TokenAttr.values());
	}

	public String get(int ordinal) {
		return values[ordinal];
	}

	public String get(TokenAttr attr) {
		return get(attr.ordinal());
	}

	public String[] get(TokenAttr[] attrs) {
		String[] ret = new String[attrs.length];
		for (int i = 0; i < attrs.length; i++) {
			ret[i] = values[attrs[i].ordinal()];
		}
		return ret;
	}

	public int getStart() {
		return start;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	public int length() {
		return get(TokenAttr.WORD).length();
	}

	public void readObject(ObjectInputStream ois) throws Exception {
		start = ois.readInt();
		values = FileUtils.readStrArray(ois);
	}

	public void set(int i, String value) {
		if (i >= values.length) {
			values = Arrays.copyOf(values, i + 1);
		}
		values[i] = value;
	}

	public void set(TokenAttr attr, String value) {
		set(attr.ordinal(), value);
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int size() {
		return values.length;
	}

	public MultiToken toMultiToken() {
		MultiToken ret = null;
		if (this instanceof MultiToken) {
			ret = (MultiToken) this;
		}
		return ret;
	}

	@Override
	public String toString() {
		return toString(true);
	}

	public String toString(boolean print_attr_names) {
		StringBuffer sb = new StringBuffer();
		if (print_attr_names) {
			for (int i = 0; i < values.length; i++) {
				sb.append(TokenAttr.values()[i]);
				if (i != values.length) {
					sb.append("\t");
				}
			}
			sb.append("\n");
		}
		sb.append(StrUtils.join("\t", StrUtils.wrap(values)));
		return sb.toString();
	}

	public void writeObject(ObjectOutputStream oos) throws Exception {
		oos.writeInt(start);
		FileUtils.writeStrArray(oos, values);
	}

}
