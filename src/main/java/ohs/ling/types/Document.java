package ohs.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Document {

	private Sentence[] sents;

	public Document() {

	}

	public Document(Sentence[] sents) {
		this.sents = sents;
	}

	public Sentence get(int i) {
		return sents[i];
	}

	public String[][] getValues() {
		return getValues(Token.DELIM_VALUE, Token.DELIM_SUBTOKEN, TokenAttr.values(), 0, sents.length);
	}

	public String[][] getValues(String delimValue, String delimSubtok, TokenAttr[] attrs, int start, int end) {
		String[][] ret = new String[end - start][];
		for (int i = start; i < end; i++) {
			Sentence sent = sents[i];
			ret[i] = sent.getValues(delimValue, delimSubtok, attrs, 0, sent.size());
		}
		return ret;
	}

	public String[][] getValues(TokenAttr[] attrs) {
		return getValues(Token.DELIM_VALUE, Token.DELIM_SUBTOKEN, attrs, 0, sents.length);
	}

	public String[][] getValues(TokenAttr attr) {
		return getValues(Token.DELIM_VALUE, Token.DELIM_SUBTOKEN, new TokenAttr[] { attr }, 0, sents.length);
	}

	public String joinValues() {
		return joinValues(Token.DELIM_VALUE, Token.DELIM_SUBTOKEN, TokenAttr.values(), 0, sents.length);
	}

	public String joinValues(String delimValue, String delimSubtok, TokenAttr[] attrs, int start, int end) {
		String[][] vals = getValues(delimValue, delimSubtok, attrs, start, end);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < vals.length; i++) {
			sb.append(String.join(" ", vals[i]));
			if (i != vals.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public void read(ObjectInputStream ois) throws Exception {
		sents = new Sentence[ois.readInt()];
		for (int i = 0; i < sents.length; i++) {
			sents[i].read(ois);
		}
	}

	public int size() {
		return sents.length;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(sents.length);
		for (int i = 0; i < sents.length; i++) {
			sents[i].write(oos);
		}
	}
}
