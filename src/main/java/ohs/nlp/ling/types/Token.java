package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import ohs.io.FileUtils;

public class Token implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4675926604151002510L;

	public static final String DELIM_TOKEN = " / ";

	protected String[] values = new String[TokenAttr.values().length];

	private int start = 0;

	public Token() {
		for (int i = 0; i < values.length; i++) {
			values[i] = "";
		}
	}

	public void setStart(int start) {
		this.start = start;
	}

	public Token(int start, String word) {
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

	public String joinValues() {
		return joinValues(DELIM_TOKEN, TokenAttr.values());
	}

	public String joinValues(String delim, TokenAttr[] attrs) {
		return String.join(delim, getValues(attrs));
	}

	public int length() {
		return getValue(TokenAttr.WORD).length();
	}

	public void read(ObjectInputStream ois) throws Exception {
		start = ois.readInt();
		values = FileUtils.readStrArray(ois);
	}

	public void setValue(TokenAttr attr, String value) {
		values[attr.ordinal()] = value;
	}

	@Override
	public String toString() {
		return toString(true);
	}

	public String toString(boolean printAttrNames) {
		StringBuffer sb = new StringBuffer();

		if (printAttrNames) {
			sb.append("Start");
			for (int i = 0; i < TokenAttr.values().length; i++) {
				TokenAttr ta = TokenAttr.values()[i];
				sb.append("\t" + ta);
			}
			sb.append("\n");
		}
		sb.append(start);
		for (int i = 0; i < TokenAttr.values().length; i++) {
			TokenAttr ta = TokenAttr.values()[i];
			String v = values[ta.ordinal()];
			sb.append("\t" + (v == null ? "" : v));
		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(start);
		FileUtils.writeStrArray(oos, values);
	}

}
