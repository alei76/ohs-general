package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class KDocument {

	public static KDocument newDocument(String[] lines) {
		KSentence[] sents = new KSentence[lines.length];
		for (int i = 0; i < lines.length; i++) {
			sents[i] = KSentence.newSentence(lines[i].split(" "));
		}
		KDocument ret = new KDocument(sents);

		MultiToken[] mts = ret.toMultiTokens();
		for (int i = 0, loc = 0; i < mts.length; i++) {
			MultiToken mt = mts[i];
			mt.setStart(loc);
			loc += mt.length();
			loc++;
		}
		return ret;
	}

	private KSentence[] sents;

	public KDocument() {

	}

	public KDocument(KSentence[] sents) {
		this.sents = sents;
	}

	public KDocument(Token[] toks) {
		this(new KSentence[] { new KSentence(toks) });
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

	public Token[] getSubTokens() {
		List<Token> ret = Generics.newArrayList();
		for (MultiToken mt : toMultiTokens()) {
			for (Token t : mt.getTokens()) {
				ret.add(t);
			}
		}
		return ret.toArray(new Token[ret.size()]);
	}

	public String[][][] getSubValues(int start, int end, TokenAttr attr) {
		String[][][] ret = new String[end - start][][];

		for (int i = start; i < end; i++) {
			ret[i] = sents[i].getSub(attr);
		}
		return ret;
	}

	public String[][][] getSubValues(TokenAttr attr) {
		return getSubValues(0, sents.length, attr);
	}

	public Token[] getTokens() {
		List<Token> ret = Generics.newArrayList();
		for (KSentence sent : sents) {
			for (Token t : sent.getTokens()) {
				ret.add(t);
			}
		}
		return ret.toArray(new MultiToken[ret.size()]);
	}

	public String[] getValues(int start, int end, TokenAttr attr) {
		List<String> ret = Generics.newArrayList();
		for (int i = start; i < end; i++) {
			KSentence sent = sents[i];
			for (Token t : sent.getTokens()) {
				ret.add(t.get(attr));
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	public String[] getValues(TokenAttr attr) {
		return getValues(0, sents.length, attr);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(sents);
		return result;
	}

	public String joinSubValues(int start, int end, TokenAttr attr, String delim1, String delim2, String delim3) {
		return StrUtils.join(delim1, delim2, delim3, getSubValues(start, end, attr));
	}

	public String joinSubValues(TokenAttr attr) {
		return StrUtils.join(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, "\n", getSubValues(0, sents.length, attr));
	}

	public String joinSubValues(TokenAttr attr, String delim1, String delim2, String delim3) {
		return StrUtils.join(delim1, delim2, delim3, getSubValues(0, sents.length, attr));
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
			sent.readObject(ois);
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

	public MultiToken[] toMultiTokens() {
		List<MultiToken> ret = Generics.newArrayList();
		for (KSentence sent : sents) {
			for (MultiToken mt : sent.toMultiTokens()) {
				ret.add(mt);
			}
		}
		return ret.toArray(new MultiToken[ret.size()]);
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
			KSentence sent = sents[i];

			sb.append(sent.toString());

			if (i != sents.length - 1) {
				sb.append("\n\n");
			}

		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(sents.length);
		for (int i = 0; i < sents.length; i++) {
			sents[i].writeObject(oos);
		}
	}
}
