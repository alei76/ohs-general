package ohs.eden.keyphrase;

import java.util.List;

import ohs.ling.types.KDocument;
import ohs.ling.types.MultiToken;
import ohs.ling.types.Sentence;
import ohs.ling.types.Token;
import ohs.ling.types.TokenAttr;
import ohs.utils.Generics;

public class TaggedTextParser {

	public static final String DELIM_TAG = "#P#";

	public static final String DELIM_SUBTOKEN = "#S#";

	public static KDocument parse(String s) {
		String[] lines = s.split("\n");
		KSentence[] sents = new KSentence[lines.length];

		for (int i = 0; i < lines.length; i++) {
			String[] parts = lines[i].split(" ");
			MultiToken[] toks = new MultiToken[parts.length];

			for (int j = 0; j < parts.length; j++) {
				String part = parts[j];
				String[] subParts = part.split(DELIM_TAG);
				Token[] subToks = new Token[subParts.length];

				for (int k = 0; k < subParts.length; k++) {
					String subPart = subParts[k];
					String[] two = subPart.split(DELIM_SUBTOKEN);
					String word = two[0];
					String pos = two[1];

					Token tok = new Token(0, word);
					tok.setValue(TokenAttr.POS, pos);
					subToks[k] = tok;

				}

				MultiToken mt = new MultiToken(0, subToks);
				toks[j] = mt;

			}
			sents[i] = new KSentence(toks);
		}

		KDocument doc = new KDocument(sents);
		List<MultiToken> mts = Generics.newArrayList();

		for (KSentence sent : doc.getSentences()) {
			for (MultiToken mt : sent.getTokens()) {
				mts.add(mt);
			}
		}

		for (int i = 0, loc = 0; i < mts.size(); i++) {
			MultiToken mt = mts.get(i);
			mt.setStart(i);
			for (int j = 0; j < mt.size(); j++) {
				Token t = mt.getToken(j);
				t.setStart(loc);
				loc += t.length();
			}
			loc++;
		}

		return doc;
	}

}
