package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import ohs.io.FileUtils;

public class Token {

	public static final String DELIM_VALUE = " / ";

	private String[] values = new String[TokenAttr.values().length];

	private int start;

	public Token() {

	}

	public static Token parse(String s) {
		String[] values = s.split(DELIM_VALUE);
		Token ret = new Token();
		for (TokenAttr attr : TokenAttr.values()) {
			ret.setValue(attr, values[attr.ordinal()]);
		}
		return ret;
	}

	public Token(int start, String word) {
		this.start = start;
		for (int i = 0; i < values.length; i++) {
			values[i] = "";
		}
		values[TokenAttr.WORD.ordinal()] = word;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Token other = (Token) obj;
		if (start != other.start)
			return false;
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
		int result = 1;
		result = prime * result + start;
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	public String joinValues() {
		return joinValues(DELIM_VALUE, TokenAttr.values());
	}

	public String joinValues(String delim, TokenAttr[] attrs) {
		return String.join(delim, getValues(attrs));
	}

	public int length() {
		int ret = values[TokenAttr.WORD.ordinal()].length();
		return ret;
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

	public String toString() {
		return toString(true);
	}

	public String toString(boolean printAttrNames) {
		StringBuffer sb = new StringBuffer();

		if (printAttrNames) {
			sb.append("Loc");
			for (int i = 0; i < TokenAttr.values().length; i++) {
				TokenAttr ta = TokenAttr.values()[i];
				sb.append("\t" + ta);
			}
			sb.append("\n");
		}

		sb.append(start);
		for (int i = 0; i < TokenAttr.values().length; i++) {
			TokenAttr ta = TokenAttr.values()[i];
			sb.append("\t" + values[ta.ordinal()]);
		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(start);
		FileUtils.writeStrArray(oos, values);
	}

}
