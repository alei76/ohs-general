package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import ohs.utils.StrUtils;

public class KSentence {

	public static KSentence newSentence(String[] words) {
		MultiToken[] mts = new MultiToken[words.length];
		for (int j = 0, loc = 0; j < words.length; j++) {
			MultiToken mt = new MultiToken(loc++, words[j]);
			mts[j] = mt;
		}
		return new KSentence(mts);
	}

	private MultiToken[] toks;

	public KSentence() {

	}

	public KSentence(List<MultiToken> toks) {
		this(toks.toArray(new MultiToken[toks.size()]));
	}

	public KSentence(MultiToken[] toks) {
		this.toks = toks;
	}

	public MultiToken getFirst() {
		return toks[0];
	}

	public MultiToken getLast() {
		return toks[toks.length - 1];
	}

	public KSentence getSentence(int start, int end) {
		return new KSentence(getTokens(start, end));
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

	public String[][] getValues(int start, int end, TokenAttr[] attrs) {
		String[][] ret = new String[end - start][];
		for (int i = start, loc = 0; i < end; i++, loc++) {
			ret[loc] = toks[i].getValues(attrs);
		}
		return ret;
	}

	public String[][][] getSubValues(int start, int end, TokenAttr[] attrs) {
		String[][][] ret = new String[end - start][][];
		for (int i = start; i < end; i++) {
			ret[i] = toks[i].getSubValues(0, toks[i].size(), attrs);
		}
		return ret;
	}

	public String joinSubValues() {
		String[][][] vals = getSubValues(0, toks.length, TokenAttr.values());
		return StrUtils.join(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, "/n", vals);
	}

	public String joinSubValues(String delimValue, String delimTok, int start, int end, TokenAttr[] attrs) {
		String[][][] vals = getSubValues(start, end, attrs);
		return StrUtils.join(delimValue, delimTok, "/n", vals);
	}

	public String joinValues(String delimValue, int start, int end, TokenAttr[] attrs) {
		String[][] vals = getValues(start, end, attrs);
		return StrUtils.join(delimValue, "/n", vals);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(toks);
		return result;
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
			MultiToken t = new MultiToken();
			t.read(ois);
			toks[i] = t;
		}
	}

	public int size() {
		return toks.length;
	}

	public KDocument toDocument() {
		return new KDocument(new KSentence[] { this });
	}

	public MultiToken[] toMultiTokens() {
		MultiToken[] ret = new MultiToken[toks.length];
		for (int i = 0; i < toks.length; i++) {
			ret[i] = (MultiToken) toks[i];
		}
		return ret;
	}

	@Override
	public String toString() {
		return toString(true);
	}

	public String toString(boolean printAttrNames) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < toks.length; i++) {
			sb.append(String.format("%d\t%s", i, toks[i].toString()));
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
