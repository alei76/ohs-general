package ohs.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Sentence {

	private Token[] toks;

	public Sentence() {

	}

	public Sentence(Token[] toks) {
		this.toks = toks;
	}

	public Token get(int i) {
		return toks[i];
	}

	public Token[] getTokens(int start, int end) {
		Token[] ret = new Token[end - start];
		for (int i = start, loc = 0; i < end; i++, loc++) {
			ret[loc] = toks[i];
		}
		return ret;
	}

	public String[] getValues(TokenAttr attr) {
		return getValues(attr, "+", 0, toks.length);
	}

	public String[] getValues(TokenAttr attr, String delim, int start, int end) {
		String[] ret = new String[end - start];
		for (int i = start, loc = 0; i < end; i++, loc++) {
			Token t = toks[i];
			if (t.lengthOfSubTokens() == 0) {
				ret[loc] = toks[i].getValue(attr);
			} else {
				ret[loc] = String.join(delim, toks[i].getSubTokenValues(attr));
			}
		}
		return ret;
	}

	public String joinValues() {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < toks.length; i++) {
			Token tok = toks[i];

			if (tok.lengthOfSubTokens() == 0) {
				sb.append(tok.joinValues("/"));
			} else {
				sb.append(tok.joinSubTokenValues("/", "+"));
			}

			if (i != toks.length - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	public String joinValues(TokenAttr attr, String delim, int start, int end) {
		return String.join(" ", getValues(attr, delim, start, end));
	}

	public void read(ObjectInputStream ois) throws Exception {
		toks = new Token[ois.readInt()];
		for (int i = 0; i < toks.length; i++) {
			toks[i].read(ois);
		}
	}

	public int size() {
		return toks.length;
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
				sb.append(String.format("\t%s", TokenAttr.values()[i]));
			}
			sb.append("\n");
		}

		for (int i = 0; i < toks.length; i++) {
			Token tok = toks[i];

			if (tok.getSubTokens().length == 0) {
				sb.append(i);
				for (int j = 0; j < TokenAttr.values().length; j++) {
					sb.append(String.format("\t%s", tok.getValue(TokenAttr.values()[j])));
				}
			} else {
				sb.append(i);

				Token[] subToks = tok.getSubTokens();
				for (int j = 0; j < subToks.length; j++) {
					Token subTok = subToks[j];

					for (int k = 0; k < TokenAttr.values().length; k++) {
						sb.append(String.format("\t%s", subTok.getValue(TokenAttr.values()[k])));
					}
					if (j != subToks.length - 1) {
						sb.append("\n");
					}
				}
			}

			if (i != toks.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(toks.length);
		for (int i = 0; i < toks.length; i++) {
			toks[i].write(oos);
		}
	}

}
