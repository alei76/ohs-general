package ohs.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ohs.io.FileUtils;

public class Token {

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

	public int getStart() {
		return start;
	}

	public Token[] getSubTokens() {
		return subToks;
	}

	public int lengthOfSubTokens() {
		return subToks.length;
	}

	public String[] getSubTokenValues(TokenAttr attr) {
		String[] ret = new String[subToks.length];
		for (int i = 0; i < subToks.length; i++) {
			ret[i] = subToks[i].getValue(attr);
		}
		return ret;
	}

	public String getValue(int ordinal) {
		return values[ordinal];
	}

	public String joinValues(String delim) {
		return String.join(delim, values);
	}

	public String joinSubTokenValues(String delimInToken, String delimAmongTokens) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < subToks.length; i++) {
			sb.append(String.join(delimInToken, subToks[i].getValues()));
			if (i != subToks.length - 1) {
				sb.append(delimAmongTokens);
			}
		}
		return sb.toString();
	}

	public String getValue(TokenAttr attr) {
		return getValue(attr.ordinal());
	}

	public String[] getValues() {
		return values;
	}

	public int length() {
		return values[TokenAttr.WORD.ordinal()].length();
	}

	public void read(ObjectInputStream ois) throws Exception {
		start = ois.readInt();
		values = FileUtils.readStrArray(ois);
		subToks = new Token[ois.readInt()];
		for (int i = 0; i < subToks.length; i++) {
			subToks[i].read(ois);
		}
	}

	public void setSubTokens(Token[] subToks) {
		this.subToks = subToks;
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
