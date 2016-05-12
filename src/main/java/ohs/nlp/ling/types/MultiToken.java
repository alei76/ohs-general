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

	public String[] getSub(int start, int end, TokenAttr attr) {
		String[] ret = new String[end - start];
		for (int i = start, j = 0; i < end; i++, j++) {
			ret[j] = toks[i].get(attr);
		}
		return ret;
	}

	public String[] getSub(TokenAttr attr) {
		return getSub(0, toks.length, attr);
	}

	public Token getToken(int i) {
		return toks[i];
	}

	public Token[] getTokens() {
		return toks;
	}

	@Override
	public void readObject(ObjectInputStream ois) throws Exception {
		super.readObject(ois);
		toks = new Token[ois.readInt()];
		for (int i = 0; i < toks.length; i++) {
			Token t = new Token();
			t.readObject(ois);
			toks[i] = t;
		}
	}

	public void setSubTokens(Token[] toks) {
		this.toks = toks;
	}

	public void setSub(TokenAttr attr, String[] values) {
		for (int i = 0; i < toks.length; i++) {
			toks[i].set(attr, values[i]);
		}
	}

	public int size() {
		return toks.length;
	}

	public String toString() {
		return toString(true);
	}

	public String toString(boolean print_attr_names, boolean print_sub_values_only) {
		StringBuffer sb = new StringBuffer();

		if (print_attr_names) {
			if (!print_sub_values_only) {
				sb.append(StrUtils.join("\t", TokenAttr.strValues()));
			}
			sb.append("SubValues");
			sb.append("\n");
		}

		if (!print_sub_values_only) {
			sb.append(super.toString());
		}

		String[] s = new String[toks.length];
		for (int i = 0; i < toks.length; i++) {
			s[i] = StrUtils.join(Token.DELIM_TOKEN, StrUtils.replace(toks[i].get(), "", "X"));
		}
		sb.append(StrUtils.join(DELIM_MULTI_TOKEN, StrUtils.wrap(s)));
		return sb.toString();
	}

	public void writeObject(ObjectOutputStream oos) throws Exception {
		super.writeObject(oos);
		oos.writeInt(toks.length);
		for (int i = 0; i < toks.length; i++) {
			toks[i].writeObject(oos);
		}
	}

}
