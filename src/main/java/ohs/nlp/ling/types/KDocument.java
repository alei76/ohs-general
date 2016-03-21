package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import ohs.utils.Generics;

public class KDocument {

	private KSentence[] sents;

	public KDocument() {

	}

	public KDocument(KSentence[] sents) {
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
		KDocument other = (KDocument) obj;
		if (!Arrays.equals(sents, other.sents))
			return false;
		return true;
	}

	public KSentence getSentence(int i) {
		return sents[i];
	}

	public KSentence[] getSentences() {
		return sents;
	}

	public Token[] getTokens() {
		List<Token> ret = Generics.newArrayList();
		for (KSentence sent : sents) {
			for (Token mt : sent.getTokens()) {
				ret.add(mt);
			}
		}
		return ret.toArray(new MultiToken[ret.size()]);
	}

	public String[][] getValues() {
		return getValues(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, TokenAttr.values(), 0, sents.length);
	}

	public String[][] getValues(String delimValue, String delimSubtok, TokenAttr[] attrs, int start, int end) {
		String[][] ret = new String[end - start][];
		for (int i = start; i < end; i++) {
			KSentence sent = sents[i];
			ret[i] = sent.getValues(delimValue, delimSubtok, attrs, 0, sent.size());
		}
		return ret;
	}

	public String[][] getValues(TokenAttr attr) {
		return getValues(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, new TokenAttr[] { attr }, 0, sents.length);
	}

	public String[][] getValues(TokenAttr[] attrs) {
		return getValues(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, attrs, 0, sents.length);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(sents);
		return result;
	}

	public String joinValues() {
		return joinValues(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, TokenAttr.values(), 0, sents.length);
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

	public String joinValues(TokenAttr attr) {
		return joinValues(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, new TokenAttr[] { attr }, 0, sents.length);
	}

	public int length() {
		int ret = 0;
		for (KSentence sent : sents) {
			ret += sent.length();
		}
		return ret;
	}

	public void read(ObjectInputStream ois) throws Exception {
		sents = new KSentence[ois.readInt()];
		for (int i = 0; i < sents.length; i++) {
			KSentence sent = new KSentence();
			sent.read(ois);
			sents[i] = sent;
		}
	}

	public void setSentences(KSentence[] sents) {
		this.sents = sents;
	}

	public int size() {
		return sents.length;
	}

	public int sizeOfTokens() {
		int ret = 0;
		for (KSentence s : sents) {
			ret += s.size();
		}
		return ret;
	}

	public KSentence toSentence() {
		Token[] toks = new Token[sizeOfTokens()];
		for (int i = 0, loc = 0; i < sents.length; i++) {
			KSentence sent = sents[i];
			for (int j = 0; j < sent.size(); j++) {
				toks[loc++] = sent.getToken(j);
			}
		}
		KSentence ret = new KSentence(toks);
		return ret;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sents.length; i++) {
			sb.append(sents[i].toString());
			if (i != sents.length - 1) {
				sb.append("\n\n");
			}
		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(sents.length);
		for (int i = 0; i < sents.length; i++) {
			sents[i].write(oos);
		}
	}
}
