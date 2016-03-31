package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import ohs.utils.Generics;

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

	public MultiToken[] toMultiTokens() {
		List<Token> ret = Generics.newArrayList();
		for (KSentence sent : sents) {
			for (MultiToken mt : sent.toMultiTokens()) {
				ret.add(mt);
			}
		}
		return ret.toArray(new MultiToken[ret.size()]);
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

	public Token[] getSubTokens() {
		List<Token> ret = Generics.newArrayList();
		for (MultiToken mt : toMultiTokens()) {
			for (Token t : mt.getTokens()) {
				ret.add(t);
			}
		}
		return ret.toArray(new MultiToken[ret.size()]);
	}

	public KSentence getSentence(int i) {
		return sents[i];
	}

	public KSentence[] getSentences() {
		return sents;
	}

	public String[] getSubValues(int start, int end, TokenAttr attr) {
		List<String> ret = Generics.newArrayList();
		for (int i = start; i < end; i++) {
			KSentence sent = sents[i];
			for (Token t : sent.getTokens()) {
				MultiToken mt = t.toMultiToken();
				for (Token st : mt.getTokens()) {
					ret.add(st.getValue(attr));
				}
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	public String[] getSubValues(TokenAttr attr) {
		return getSubValues(0, sents.length, attr);
	}

	public String[] getValue(int start, int end, TokenAttr attr) {
		List<String> ret = Generics.newArrayList();
		for (int i = start; i < end; i++) {
			KSentence sent = sents[i];
			for (Token t : sent.getTokens()) {
				ret.add(t.getValue(attr));
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	public String[] getValue(TokenAttr attr) {
		return getValue(0, sents.length, attr);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(sents);
		return result;
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
			sents[i].write(oos);
		}
	}
}
