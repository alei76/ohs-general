package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class KSentence {

	public static final String DELIM_SENT = "\n";

	public static KSentence newSentence(String[] words) {
		MultiToken[] mts = new MultiToken[words.length];
		for (int j = 0, loc = 0; j < words.length; j++) {
			MultiToken mt = new MultiToken(loc++, words[j]);
			mts[j] = mt;
		}
		return new KSentence(mts);
	}

	private Token[] toks;

	private boolean isMultiToken = false;

	public KSentence() {

	}

	public KSentence(List<Token> toks) {
		this(toks.toArray(new Token[toks.size()]));
	}

	public KSentence(Token[] toks) {
		this.toks = toks;

		if (toks[0] instanceof MultiToken) {
			isMultiToken = true;
		}
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

	public Token[] getSubTokens() {
		List<Token> ret = Generics.newArrayList();
		for (MultiToken mt : toMultiTokens()) {
			for (Token t : mt.getTokens()) {
				ret.add(t);
			}
		}
		return ret.toArray(new Token[ret.size()]);
	}

	public String[][] getSubValues(int start, int end, TokenAttr attr) {
		String[][] ret = new String[end - start][];
		for (int i = start; i < end; i++) {
			MultiToken mt = toks[i].toMultiToken();
			ret[i] = mt.getSubValues(attr);
		}
		return ret;
	}

	public String[][] getSubValues(TokenAttr attr) {
		return getSubValues(0, toks.length, attr);
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

	public String[] getValues(int start, int end, TokenAttr attr) {
		String[] ret = new String[end - start];
		for (int i = start; i < end; i++) {
			ret[i] = toks[i].getValue(attr);
		}
		return ret;
	}

	public String[] getValues(TokenAttr attr) {
		return getValues(0, toks.length, attr);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(toks);
		return result;
	}

	// public String joinSubValues(int start, int end, TokenAttr attr, String delim1, String delim2) {
	// return StrUtils.join(delim1, delim2, getSubValues(start, end, attr));
	// }
	//
	// public String joinSubValues(TokenAttr attr) {
	// return StrUtils.join(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, getSubValues(0, toks.length, attr));
	// }
	//
	// public String joinSubValues(TokenAttr attr, String delim1, String delim2) {
	// return StrUtils.join(delim1, delim2, getSubValues(0, toks.length, attr));
	// }

	public int length() {
		int ret = 0;
		for (Token t : toks) {
			if (isMultiToken) {
				ret += t.toMultiToken().length();
			} else {
				ret += t.length();
			}
		}
		return ret;
	}

	public void read(ObjectInputStream ois) throws Exception {
		isMultiToken = ois.readBoolean();
		toks = new MultiToken[ois.readInt()];
		for (int i = 0; i < toks.length; i++) {
			if (isMultiToken) {
				MultiToken t = new MultiToken();
				t.read(ois);
				toks[i] = t;
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

	public MultiToken[] toMultiTokens() {
		return (MultiToken[]) toks;
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
			if (isMultiToken) {
				sb.append("\tSubValues");
			}
			sb.append("\n");
		}

		for (int i = 0; i < toks.length; i++) {
			Token t = toks[i];
			if (isMultiToken) {
				MultiToken mt = t.toMultiToken();
				sb.append(String.format("%d\t%s", i, mt.toString(false, print_sub_values_only)));
			} else {
				sb.append(String.format("%d\t%s", i, t.toString(false)));
			}

			if (i != toks.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeBoolean(isMultiToken);
		oos.writeInt(toks.length);
		for (int i = 0; i < toks.length; i++) {
			if (isMultiToken) {
				toks[i].toMultiToken().write(oos);
			} else {
				toks[i].write(oos);
			}
		}
	}

}
