package ohs.eden.keyphrase;

import ohs.ling.types.Document;
import ohs.ling.types.Sentence;
import ohs.ling.types.Token;
import ohs.ling.types.TokenAttr;

public class TaggedTextParser {

	public static final String DELIM_TAG = "#P#";

	public static final String DELIM_SUBTOKEN = "#S#";

	public static Document parse(String s) {
		String[] lines = s.split("\n");
		Sentence[] sents = new Sentence[lines.length];
		int loc_in_doc = 0;

		for (int i = 0; i < lines.length; i++) {
			String[] parts = lines[i].split(" ");
			Token[] toks = new Token[parts.length];

			int loc_in_sub_tok = 0;

			for (int j = 0; j < parts.length; j++) {
				String part = parts[j];
				String[] subParts = part.split(DELIM_TAG);

				Token[] subToks = new Token[subParts.length];

				for (int k = 0; k < subParts.length; k++) {
					String subPart = subParts[k];
					String[] two = subPart.split(DELIM_SUBTOKEN);

					Token t = new Token(loc_in_sub_tok++, two[0]);
					t.setValue(TokenAttr.POS, two[1]);

					subToks[k] = t;
				}

				Token t = new Token();
				t.setSubTokens(subToks);
				t.setStart(loc_in_doc++);

				toks[j] = t;
			}
			sents[i] = new Sentence(toks);
		}

		Document doc = new Document(sents);
		return doc;
	}

}
