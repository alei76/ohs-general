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

	public String[][] getSubValues(int start, int end, TokenAttr[] attrs) {
		String[][] ret = new String[end - start][];
		for (int i = start; i < end; i++) {
			ret[i] = toks[i].getValues(attrs);
		}
		return ret;
	}

	public String[][] getSubValues() {
		return getSubValues(0, toks.length, TokenAttr.values());
	}

	public Token getToken(int i) {
		return toks[i];
	}

	public Token[] getTokens() {
		return toks;
	}

	public String joinSubValues(String delimValue, String delimTok, int start, int end, TokenAttr[] attrs) {
		return StrUtils.join(delimValue, delimTok, getSubValues(start, end, attrs));
	}

	public String joinSubValues() {
		return joinSubValues(Token.DELIM_TOKEN, DELIM_MULTI_TOKEN, 0, toks.length, TokenAttr.values());
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

	@Override
	public String toString() {
		return toString(true);
	}

	@Override
	public String toString(boolean printAttrNames) {
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString(false));
		sb.append(joinSubValues());

		return sb.toString();
	}

	@Override
	public void write(ObjectOutputStream oos) throws Exception {
		super.write(oos);
		oos.writeInt(toks.length);
		for (int i = 0; i < toks.length; i++) {
			toks[i].write(oos);
		}
	}

}
