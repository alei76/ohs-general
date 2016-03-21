package ohs.nlp.ling.types;

public class TypeCaster {

	public static MultiToken[] toMultiTokens(Token[] toks) {
		MultiToken[] ret = new MultiToken[toks.length];
		for (int i = 0; i < ret.length; i++) {
			if (toks[i] instanceof MultiToken) {
				ret[i] = (MultiToken) toks[i];
			}
		}
		return ret;
	}

}
