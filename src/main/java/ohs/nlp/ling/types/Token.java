package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import ohs.io.FileUtils;

public class Token extends TextSpan {

	/**
	 * 
	 */
	private static final long serialVersionUID = 353783289264943298L;

	public static final String DELIM_TOKEN = " / ";

	protected String[] values = new String[TokenAttr.values().length];

	public Token() {
		for (int i = 0; i < values.length; i++) {
			values[i] = "";
		}
	}

	public Token(int start, String text) {
		super(start, text);
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

	public void read(ObjectInputStream ois) throws Exception {
		super.read(ois);
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
		super.write(oos);
		FileUtils.writeStrArray(oos, values);
	}

}
