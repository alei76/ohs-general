package ohs.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ohs.io.FileUtils;

public class Token {

	private String[] values = new String[TokenAttr.values().length];

	private int start;

	public Token(int start, String word) {
		this.start = start;
		for (int i = 0; i < values.length; i++) {
			values[i] = "";
		}
		values[TokenAttr.WORD.ordinal()] = word;
	}

	public int getStart() {
		return start;
	}

	public String getValue(TokenAttr attr) {
		return values[attr.ordinal()];
	}

	public int length() {
		return values[TokenAttr.WORD.ordinal()].length();
	}

	public void read(ObjectInputStream ois) throws Exception {
		values = FileUtils.readStrArray(ois);
	}

	public void setValue(TokenAttr attr, String value) {
		values[attr.ordinal()] = value;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < TokenAttr.values().length; i++) {
			TokenAttr att = TokenAttr.values()[i];
			sb.append(String.format("%d:\t%s\t%s", i + 1, att, values[att.ordinal()]));
			if (i != TokenAttr.values().length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		FileUtils.writeStrArray(oos, values);
	}

}
