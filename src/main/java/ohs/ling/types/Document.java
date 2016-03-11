package ohs.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class Document {

	private Sentence[] sents;

	public Document() {

	}

	public Document(Sentence[] sents) {
		this.sents = sents;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Document other = (Document) obj;
		if (!Arrays.equals(sents, other.sents))
			return false;
		return true;
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

	public String[][] getValues(TokenAttr attr) {
		return getValues(Token.DELIM_VALUE, Token.DELIM_SUBTOKEN, new TokenAttr[] { attr }, 0, sents.length);
	}

	public String[][] getValues(TokenAttr[] attrs) {
		return getValues(Token.DELIM_VALUE, Token.DELIM_SUBTOKEN, attrs, 0, sents.length);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(sents);
		return result;
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

	public int length() {
		int ret = 0;
		for (Sentence sent : sents) {
			ret += sent.length();
		}
		return ret;
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

	public int sizeOfTokens() {
		int ret = 0;
		for (Sentence s : sents) {
			ret += s.size();
		}
		return ret;
	}

	public Sentence toSentence() {
		Token[] toks = new Token[sizeOfTokens()];
		for (int i = 0, loc = 0; i < sents.length; i++) {
			Sentence sent = sents[i];
			for (int j = 0; j < sent.size(); j++) {
				toks[loc++] = sent.get(j);
			}
		}
		Sentence ret = new Sentence(toks);
		return ret;
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
