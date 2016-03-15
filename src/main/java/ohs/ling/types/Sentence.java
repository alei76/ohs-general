package ohs.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class Sentence {

	private MultiToken[] toks;

	public Sentence() {

	}

	public Sentence(MultiToken[] toks) {
		this.toks = toks;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sentence other = (Sentence) obj;
		if (!Arrays.equals(toks, other.toks))
			return false;
		return true;
	}

	public MultiToken getFirst() {
		return toks[0];
	}

	public MultiToken getLast() {
		return toks[toks.length - 1];
	}

	public Sentence getSentence(int start, int end) {
		return new Sentence(getTokens(start, end));
	}

	public MultiToken getToken(int i) {
		return toks[i];
	}

	public MultiToken[] getTokens() {
		return toks;
	}

	public MultiToken[] getTokens(int start, int end) {
		MultiToken[] ret = new MultiToken[end - start];
		for (int i = start, loc = 0; i < end; i++, loc++) {
			ret[loc] = toks[i];
		}
		return ret;
	}

	public String[] getValues() {
		return getValues(Token.DELIM_VALUE, MultiToken.DELIM_TOKEN, TokenAttr.values(), 0, toks.length);
	}

	public String[] getValues(String delimValue, String delimTok, TokenAttr[] attrs, int start, int end) {
		String[] ret = new String[end - start];
		for (int i = start, loc = 0; i < end; i++, loc++) {
			MultiToken tok = toks[i];
			ret[loc] = tok.joinValues(delimValue, delimTok, attrs);
		}
		return ret;
	}

	public String[] getValues(TokenAttr attr) {
		return getValues(Token.DELIM_VALUE, MultiToken.DELIM_TOKEN, new TokenAttr[] { attr }, 0, toks.length);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(toks);
		return result;
	}

	public String joinValues() {
		return joinValues(Token.DELIM_VALUE, MultiToken.DELIM_TOKEN, TokenAttr.values(), 0, toks.length);
	}

	public String joinValues(String delimValue, String delimTok, TokenAttr[] attrs, int start, int end) {
		return String.join(" ", getValues(delimValue, delimTok, attrs, start, end));
	}

	public int length() {
		int ret = 0;
		for (MultiToken t : toks) {
			ret += t.length();
		}
		return ret;
	}

	public void read(ObjectInputStream ois) throws Exception {
		toks = new MultiToken[ois.readInt()];
		for (int i = 0; i < toks.length; i++) {
			toks[i].read(ois);
		}
	}

	public int size() {
		return toks.length;
	}

	public Document toDocument() {
		return new Document(new Sentence[] { this });
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
			MultiToken tok = toks[i];
			sb.append(String.format("%d\t%s", i, tok.joinValues()));
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
