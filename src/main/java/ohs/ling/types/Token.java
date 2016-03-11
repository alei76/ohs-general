package ohs.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import ohs.io.FileUtils;

public class Token {

	public static final String DELIM_VALUE = "/";

	public static final String DELIM_SUBTOKEN = "+";

	private String[] values = new String[TokenAttr.values().length];

	private int start;

	private Token[] subToks;

	public Token() {

	}

	public Token(int start, String word) {
		this.start = start;
		for (int i = 0; i < values.length; i++) {
			values[i] = "";
		}
		values[TokenAttr.WORD.ordinal()] = word;
		subToks = new Token[0];
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
		if (!Arrays.equals(subToks, other.subToks))
			return false;
		if (!Arrays.equals(values, other.values))
			return false;
		return true;
	}

	public int getStart() {
		return start;
	}

	public Token[] getSubTokens() {
		return subToks;
	}

	public String[] getSubTokenValues(String delimValue, TokenAttr[] attrs) {
		String[] ret = new String[subToks.length];
		for (int i = 0; i < subToks.length; i++) {
			ret[i] = subToks[i].joinValues(delimValue, attrs);
		}
		return ret;
	}

	public String[] getSubTokenValues(TokenAttr attr) {
		String[] ret = new String[subToks.length];
		for (int i = 0; i < subToks.length; i++) {
			ret[i] = subToks[i].getValue(attr);
		}
		return ret;
	}

	public String[] getSubTokenValues(TokenAttr[] attrs) {
		return getSubTokenValues(DELIM_VALUE, attrs);
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
		String[] ret = null;
		if (attrs.length == values.length) {
			ret = values;
		} else {
			ret = new String[attrs.length];
			for (int i = 0; i < attrs.length; i++) {
				ret[i] = values[attrs[i].ordinal()];
			}
		}
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + start;
		result = prime * result + Arrays.hashCode(subToks);
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	public String joinSubTokenValues() {
		return joinSubTokenValues(DELIM_VALUE, DELIM_SUBTOKEN, TokenAttr.values());
	}

	/**
	 * 외/NNG+하/XSV+ㄴ/ETM 각종/NNG
	 * 
	 * delimValue: / delimSubtok: +
	 * 
	 * @param delimValue
	 * @param delimSubtok
	 * @param attrs
	 * @return
	 */
	public String joinSubTokenValues(String delimValue, String delimSubtok, TokenAttr[] attrs) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < subToks.length; i++) {
			sb.append(String.join(delimValue, subToks[i].getValues(attrs)));
			if (i != subToks.length - 1) {
				sb.append(delimSubtok);
			}
		}
		return sb.toString();
	}

	public String joinValues() {
		return joinValues(DELIM_VALUE, TokenAttr.values());
	}

	public String joinValues(String delim, TokenAttr[] attrs) {
		return String.join(delim, getValues(attrs));
	}

	public int length() {
		int ret = values[TokenAttr.WORD.ordinal()].length();
		if (subToks.length > 0) {
			for (Token t : subToks) {
				ret += t.length();
			}
		}
		return ret;
	}

	public void read(ObjectInputStream ois) throws Exception {
		start = ois.readInt();
		values = FileUtils.readStrArray(ois);
		subToks = new Token[ois.readInt()];
		for (int i = 0; i < subToks.length; i++) {
			subToks[i].read(ois);
		}
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setSubTokens(Token[] subToks) {
		this.subToks = subToks;
	}

	public void setValue(TokenAttr attr, String value) {
		values[attr.ordinal()] = value;
	}

	public int sizeOfSubTokens() {
		return subToks.length;
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

		if (subToks.length == 0) {
			sb.append(0);
			for (int i = 0; i < TokenAttr.values().length; i++) {
				TokenAttr ta = TokenAttr.values()[i];
				sb.append("\t" + values[ta.ordinal()]);
			}
		} else {
			for (int i = 0; i < subToks.length; i++) {
				sb.append(i);
				Token sub = subToks[i];
				for (int j = 0; j < TokenAttr.values().length; j++) {
					TokenAttr ta = TokenAttr.values()[j];
					sb.append("\t" + sub.getValue(ta));
				}

				if (i != subToks.length - 1) {
					sb.append("\n");
				}
			}
		}

		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(start);
		FileUtils.writeStrArray(oos, values);
		oos.writeInt(subToks.length);
		for (int i = 0; i < subToks.length; i++) {
			subToks[i].write(oos);
		}

	}

}
