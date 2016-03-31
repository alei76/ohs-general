package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ohs.utils.StrUtils;

public class MultiToken extends Token {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1548339270826089537L;

	public static final String DELIM_MULTI_TOKEN = " + ";

	public static MultiToken[] toMultiTokens(Token[] ts) {
		MultiToken[] ret = new MultiToken[ts.length];
		for (int i = 0; i < ts.length; i++) {
			ret[i] = (MultiToken) ts[i];
		}
		return ret;
	}

	private Token[] toks = new Token[0];

	public MultiToken() {

	}

	public MultiToken(int start, String word) {
		super(start, word);
	}

	public String[] getSubValues(int start, int end, TokenAttr attr) {
		String[] ret = new String[end - start];
		for (int i = start; i < end; i++) {
			ret[i] = toks[i].getValue(attr);
		}
		return ret;
	}

	public String[] getSubValues(TokenAttr attr) {
		return getSubValues(0, toks.length, attr);
	}

	public Token getToken(int i) {
		return toks[i];
	}

	public Token[] getTokens() {
		return toks;
	}

	@Override
	public void read(ObjectInputStream ois) throws Exception {
		super.read(ois);
		toks = new Token[ois.readInt()];
		for (int i = 0; i < toks.length; i++) {
			Token t = new Token();
			t.read(ois);
			toks[i] = t;
		}
	}

	public void setSubTokens(Token[] toks) {
		this.toks = toks;
	}

	public void setSubValue(TokenAttr attr, String[] values) {
		for (int i = 0; i < toks.length; i++) {
			toks[i].setValue(attr, values[i]);
		}
	}

	public int size() {
		return toks.length;
	}

	public String toString() {
		return toString(true);
	}

	public String toString(boolean print_attr_names) {
		StringBuffer sb = new StringBuffer();

		if (print_attr_names) {
			sb.append("Loc\t");
			for (TokenAttr attr : TokenAttr.values()) {
				sb.append("\t" + attr);
			}
			sb.append("\n");
		}

		sb.append(0);
		for (TokenAttr attr : TokenAttr.values()) {
			sb.append(String.format("\t%s", values[attr.ordinal()]));
		}

		for (int i = 0; i < toks.length; i++) {
			sb.append(String.format("\n%d\t%s", i, toks[i].toString(false)));
		}
		return sb.toString();

	}

	public void write(ObjectOutputStream oos) throws Exception {
		super.write(oos);
		oos.writeInt(toks.length);
		for (int i = 0; i < toks.length; i++) {
			toks[i].write(oos);
		}
	}

}
