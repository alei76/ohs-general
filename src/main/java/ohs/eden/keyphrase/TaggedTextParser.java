package ohs.eden.keyphrase;

import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;

public class TaggedTextParser {

	public static KDocument parse(String s) {
		String[] lines = s.split("\\\\n");
		KSentence[] sents = new KSentence[lines.length];

		int num_mts = 0;
		int num_ts = 0;

		for (int i = 0; i < lines.length; i++) {
			String[] parts = lines[i].split("\\\\t");
			MultiToken[] toks = new MultiToken[parts.length];

			for (int j = 0; j < parts.length; j++) {
				String part = parts[j];
				String[] subParts = part.split(MultiToken.DELIM_MULTI_TOKEN.replace("+", "\\+"));
				Token[] subToks = new Token[subParts.length];

				for (int k = 0; k < subParts.length; k++) {
					String subPart = subParts[k];
					String[] two = subPart.split(Token.DELIM_TOKEN);
					Token tok = new Token();

					if (two.length == 2) {
						String word = two[0];
						String pos = two[1];

						tok.setValue(TokenAttr.WORD, word);
						tok.setValue(TokenAttr.POS, pos);
					} else {
						tok.setValue(TokenAttr.WORD, "");
						tok.setValue(TokenAttr.POS, "");
					}

					tok.setStart(num_ts++);
					subToks[k] = tok;
				}

				MultiToken mt = new MultiToken();
				mt.setSubTokens(subToks);
				mt.setStart(num_mts++);
				toks[j] = mt;

			}
			sents[i] = new KSentence(toks);
		}

		KDocument doc = new KDocument(sents);
		return doc;
	}

}
