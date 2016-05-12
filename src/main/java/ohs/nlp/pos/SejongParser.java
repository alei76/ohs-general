package ohs.nlp.pos;

import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.utils.StrUtils;

public class SejongParser {

	public static MultiToken parseMultiToken(String s) {
		String[] two = s.split("\t");

		String surface = two[0];
		String[] parts = two[1].split(MultiToken.DELIM_MULTI_TOKEN.replace("+", "\\+"));
		Token[] toks = new Token[parts.length];
		for (int i = 0; i < parts.length; i++) {
			toks[i] = parseToken(parts[i]);
		}
		MultiToken ret = new MultiToken(0, surface);
		ret.setSubTokens(toks);

		return ret;
	}

	public static KDocument parseDocument(String s) {
		String[] lines = s.split("\n\n");
		KSentence[] sents = new KSentence[lines.length];
		for (int i = 0; i < sents.length; i++) {
			sents[i] = parseSentence(lines[i]);
		}

		KDocument doc = new KDocument(sents);

		enumerateStarts(doc);

		return doc;
	}

	public static void enumerateStarts(KDocument doc) {
		Token[] mts = doc.toMultiTokens();
		for (int i = 0, loc = 0; i < mts.length; i++) {
			MultiToken mt = (MultiToken) mts[i];
			mt.setStart(loc);

			for (int j = 0; j < mt.size(); j++) {
				mt.getToken(j).setStart(j);
			}

			loc += mt.length();
			loc++;
		}
	}

	public static KSentence parseSentence(String s) {
		String[] lines = s.split("\n");
		MultiToken[] mts = new MultiToken[lines.length];
		for (int i = 0; i < lines.length; i++) {
			mts[i] = parseMultiToken(lines[i]);
		}
		return new KSentence(mts);
	}

	public static Token parseToken(String s) {
		String[] values = new String[TokenAttr.size()];

		StrUtils.copy(s.split(Token.DELIM_TOKEN), values);

		Token ret = new Token();
		for (TokenAttr attr : TokenAttr.values()) {
			ret.set(attr, values[attr.ordinal()]);
		}
		return ret;
	}

}
