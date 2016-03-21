package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class MultiToken extends Token {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4806700641614757112L;

	public static final String DELIM_MULTI_TOKEN = " + ";

	private Token[] toks = new Token[0];

	public MultiToken() {

	}

	public MultiToken(int start, String text) {
		super(start, text);
	}

	public Token getToken(int i) {
		return toks[i];
	}

	public Token[] getTokens() {
		return toks;
	}

	@Override
	public String[] getValues() {
		return getValues(0, toks.length, TokenAttr.values());
	}

	public String[] getValues(int start, int end, TokenAttr[] attrs) {
		String[] ret = new String[end - start];
		for (int i = start; i < end; i++) {
			ret[i] = toks[i].joinValues(DELIM_MULTI_TOKEN, attrs);
		}
		return ret;
	}

	@Override
	public String joinValues() {
		return joinValues(Token.DELIM_TOKEN, DELIM_MULTI_TOKEN, TokenAttr.values());
	}

	public String joinValues(String delimValue, String delimTok, TokenAttr[] attrs) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < toks.length; i++) {
			sb.append(String.join(delimValue, toks[i].getValues(attrs)));
			if (i != toks.length - 1) {
				sb.append(delimTok);
			}
		}
		return sb.toString();
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

	public void setTokens(Token[] toks) {
		this.toks = toks;
	}

	public void setValue(TokenAttr attr, String[] values) {
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

		sb.append(text);
		sb.append("\t");
		sb.append(joinValues());

		// if (printAttrNames) {
		// sb.append("Loc\tLoc");
		// for (int i = 0; i < TokenAttr.values().length; i++) {
		// TokenAttr ta = TokenAttr.values()[i];
		// sb.append("\t" + ta);
		// }
		// sb.append("\n");
		// }
		//
		// for (int i = 0; i < toks.length; i++) {
		// Token tok = toks[i];
		// sb.append(start + "\t" + tok.getStart());
		// for (int j = 0; j < TokenAttr.values().length; j++) {
		// TokenAttr attr = TokenAttr.values()[j];
		// sb.append("\t" + tok.getValue(attr));
		// }
		// if (i != toks.length - 1) {
		// sb.append("\n");
		// }
		// }

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
