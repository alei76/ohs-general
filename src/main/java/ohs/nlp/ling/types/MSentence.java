package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class MSentence {

	public static final String DELIM_SENT = "\n";

	public static MSentence newSentence(String[] words) {
		MultiToken[] mts = new MultiToken[words.length];
		for (int j = 0, loc = 0; j < words.length; j++) {
			MultiToken mt = new MultiToken(loc++, words[j]);
			mts[j] = mt;
		}
		return new MSentence(mts);
	}

	private MultiToken[] toks;

	public MSentence() {
		
	}

	public MSentence(List<MultiToken> toks) {
		this(toks.toArray(new MultiToken[toks.size()]));
	}

	public MSentence(MultiToken[] toks) {
		this.toks = toks;
	}

	public String[] get(int start, int end, TokenAttr attr) {
		String[] ret = new String[end - start];
		for (int i = start; i < end; i++) {
			ret[i] = toks[i].get(attr);
		}
		return ret;
	}

	public String[] get(TokenAttr attr) {
		return get(0, toks.length, attr);
	}

	public MSentence getSentence(int start, int end) {
		return new MSentence(getTokens(start, end));
	}

	public String[][] getSub(int start, int end, TokenAttr attr) {
		String[][] ret = new String[end - start][];
		for (int i = start; i < end; i++) {
			MultiToken mt = toks[i].toMultiToken();
			ret[i] = mt.getSub(attr);
		}
		return ret;
	}

	public String[][] getSub(TokenAttr attr) {
		return getSub(0, toks.length, attr);
	}

	public Token[] getSubTokens() {
		List<Token> ret = Generics.newArrayList();
		for (MultiToken mt : toks) {
			for (Token t : mt.getTokens()) {
				ret.add(t);
			}
		}
		return ret.toArray(new Token[ret.size()]);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(toks);
		return result;
	}

	public int length() {
		int ret = 0;
		for (Token t : toks) {
			ret += t.toMultiToken().length();
		}
		return ret;
	}

	public void readObject(ObjectInputStream ois) throws Exception {
		toks = new MultiToken[ois.readInt()];
		for (int i = 0; i < toks.length; i++) {
			MultiToken t = new MultiToken();
			t.readObject(ois);
			toks[i] = t;
		}
	}

	public int size() {
		return toks.length;
	}

	@Override
	public String toString() {
		return toString(true, true);
	}

	public String toString(boolean print_attr_names, boolean print_sub_values_only) {
		StringBuffer sb = new StringBuffer();

		if (print_attr_names) {
			sb.append("Loc\t");
			if (!print_sub_values_only) {
				sb.append(StrUtils.join("\t", TokenAttr.strValues()));
			}
			sb.append("\tSubValues");
			sb.append("\n");
		}

		for (int i = 0; i < toks.length; i++) {
			Token t = toks[i];
			MultiToken mt = t.toMultiToken();
			sb.append(String.format("%d\t%s", i, mt.toString(false, print_sub_values_only)));

			if (i != toks.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public void writeObject(ObjectOutputStream oos) throws Exception {
		oos.writeInt(toks.length);
		for (int i = 0; i < toks.length; i++) {
			toks[i].toMultiToken().writeObject(oos);
		}
	}

}
