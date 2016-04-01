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

	protected String[] values = new String[TokenAttr.values().length];

	protected int start = 0;

	public Token() {
		for (int i = 0; i < values.length; i++) {
			values[i] = "";
		}
	}

	public Token(int start, String word) {
		this();
		this.start = start;
		setValue(TokenAttr.WORD, word);
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

	public int getStart() {
		return start;
	}

	public String getValue(int ordinal) {
		return values[ordinal];
	}

	public String getValue(TokenAttr attr) {
		return getValue(attr.ordinal());
	}

	public String[] getValues() {
		return getValues(TokenAttr.values());
	}

	public String[] getValues(TokenAttr[] attrs) {
		String[] ret = new String[attrs.length];
		for (int i = 0; i < attrs.length; i++) {
			ret[i] = values[attrs[i].ordinal()];
		}
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	public int length() {
		return getValue(TokenAttr.WORD).length();
	}

	public void read(ObjectInputStream ois) throws Exception {
		start = ois.readInt();
		values = FileUtils.readStrArray(ois);
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setValue(TokenAttr attr, String value) {
		values[attr.ordinal()] = value;
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
			sb.append(StrUtils.join("\t", TokenAttr.strValues()));
			sb.append("\n");
		}
		sb.append(StrUtils.join("\t", StrUtils.enclose(values)));
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(start);
		FileUtils.writeStrArray(oos, values);
	}

}
