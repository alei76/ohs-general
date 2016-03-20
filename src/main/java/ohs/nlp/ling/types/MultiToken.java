package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class MultiToken {

	public static final String DELIM_TOKEN = " + ";

	public static MultiToken parse(String s) {
		String[] two = s.split("\t");

		String suface = two[0];
		String[] parts = two[1].split(DELIM_TOKEN.replace(" + ", " \\+ "));
		Token[] toks = new Token[parts.length];
		for (int i = 0; i < parts.length; i++) {
			toks[i] = Token.parse(parts[i]);
		}
		MultiToken ret = new MultiToken(0, suface, toks);
		return ret;
	}

	private Token[] toks = new Token[0];

	private int start;

	private String text = "";

	public MultiToken() {

	}

	public MultiToken(int start, List<Token> toks) {
		this.start = start;
		this.toks = toks.toArray(new Token[toks.size()]);
	}

	public MultiToken(int start, String text, Token[] toks) {
		this.start = start;
		this.text = text;
		this.toks = toks;
	}

	public MultiToken(int start, Token tok) {
		this(start, new Token[] { tok });
	}

	public MultiToken(int start, Token[] toks) {
		this.start = start;
		this.toks = toks;
	}

	public int getStart() {
		return start;
	}

	public String getText() {
		return text;
	}

	public Token getToken(int i) {
		return toks[i];
	}

	public Token[] getTokens() {
		return toks;
	}

	public String[] getValue(int ordinal) {
		String[] ret = new String[toks.length];
		for (int i = 0; i < toks.length; i++) {
			ret[i] = toks[i].getValue(ordinal);
		}
		return ret;
	}

	public String[] getValue(TokenAttr attr) {
		return getValue(attr.ordinal());
	}

	public String[][] getValues() {
		return getValues(TokenAttr.values());
	}

	public String[][] getValues(TokenAttr[] attrs) {
		String[][] ret = null;
		ret = new String[attrs.length][];
		for (int i = 0; i < attrs.length; i++) {
			ret[i] = getValue(attrs[i].ordinal());
		}
		return ret;
	}

	public String joinValues() {
		return joinValues(Token.DELIM_VALUE, DELIM_TOKEN, TokenAttr.values());
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

	public int length() {
		int ret = 0;
		for (int i = 0; i < toks.length; i++) {
			ret += toks[i].length();
		}
		return ret;
	}

	public void read(ObjectInputStream ois) throws Exception {
		start = ois.readInt();
		toks = new Token[ois.readInt()];
		for (int i = 0; i < toks.length; i++) {
			Token t = new Token();
			t.read(ois);
			toks[i] = t;
		}
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setValue(TokenAttr attr, String[] values) {
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

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(start);
		oos.writeInt(toks.length);
		for (int i = 0; i < toks.length; i++) {
			toks[i].write(oos);
		}
	}

}
