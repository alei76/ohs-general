package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

public class KSentence {

	private Token[] toks;

	public KSentence() {

	}

	public KSentence(List<Token> toks) {
		this(toks.toArray(new Token[toks.size()]));
	}

	public KSentence(Token[] toks) {
		this.toks = toks;
	}

	public Token getFirst() {
		return toks[0];
	}

	public Token getLast() {
		return toks[toks.length - 1];
	}

	public KSentence getSentence(int start, int end) {
		return new KSentence(getTokens(start, end));
	}

	public Token getToken(int i) {
		return toks[i];
	}

	public Token[] getTokens() {
		return toks;
	}

	public Token[] getTokens(int start, int end) {
		Token[] ret = new Token[end - start];
		for (int i = start, loc = 0; i < end; i++, loc++) {
			ret[loc] = toks[i];
		}
		return ret;
	}

	public String[] getValues() {
		return getValues(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, TokenAttr.values(), 0, toks.length);
	}

	public String[] getValues(String delimTok, String delimMultiTok, TokenAttr[] attrs, int start, int end) {
		String[] ret = new String[end - start];
		for (int i = start, loc = 0; i < end; i++, loc++) {
			Token tok = toks[i];

			if (tok instanceof MultiToken) {
				MultiToken mt = (MultiToken) tok;
				ret[loc] = mt.joinValues(delimTok, delimMultiTok, attrs);
			} else {
				ret[loc] = tok.joinValues(delimMultiTok, attrs);
			}
		}
		return ret;
	}

	public String[] getValues(TokenAttr attr) {
		return getValues(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, new TokenAttr[] { attr }, 0, toks.length);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(toks);
		return result;
	}

	public String joinValues() {
		return joinValues(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, TokenAttr.values(), 0, toks.length);
	}

	public String joinValues(String delimValue, String delimTok, TokenAttr[] attrs, int start, int end) {
		return String.join(" ", getValues(delimValue, delimTok, attrs, start, end));
	}

	public int length() {
		int ret = 0;
		for (Token t : toks) {
			ret += t.length();
		}
		return ret;
	}

	public void read(ObjectInputStream ois) throws Exception {
		boolean isMultiToken = ois.readBoolean();
		toks = new Token[ois.readInt()];
		for (int i = 0; i < toks.length; i++) {
			if (isMultiToken) {
				MultiToken mt = new MultiToken();
				mt.read(ois);
				toks[i] = mt;
			} else {
				Token t = new Token();
				t.read(ois);
				toks[i] = t;
			}
		}
	}

	public int size() {
		return toks.length;
	}

	public KDocument toDocument() {
		return new KDocument(new KSentence[] { this });
	}

	@Override
	public String toString() {
		return toString(true);
	}

	public String toString(boolean printAttrNames) {
		StringBuffer sb = new StringBuffer();

		// if (printAttrNames) {
		// sb.append("Loc");
		// for (int i = 0; i < TokenAttr.values().length; i++) {
		// sb.append(String.format("\t%text", TokenAttr.values()[i]));
		// }
		// sb.append("\n");
		// }
		//
		// for (int i = 0; i < toks.length; i++) {
		// MultiToken tok = toks[i];
		// sb.append(String.format("%d\t%text", i, tok.joinValues()));
		// if (i != toks.length - 1) {
		// sb.append("\n");
		// }
		// }

		for (int i = 0; i < toks.length; i++) {
			sb.append(String.format("%d\t%s\t%s", i, toks[i].getText(), toks[i].joinValues()));
			if (i != toks.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		boolean isMultiToken = false;
		if (toks[0] instanceof MultiToken) {
			isMultiToken = true;
		}
		oos.writeBoolean(isMultiToken);
		oos.writeInt(toks.length);
		for (int i = 0; i < toks.length; i++) {
			toks[i].write(oos);
		}
	}

}
