package ohs.nlp.pos;

import java.util.List;

import ohs.nlp.ling.types.KDocument;
import ohs.utils.Generics;

public class TextTokenizer {

	public TextTokenizer() {

	}

	public KDocument tokenize(String text) {
		List<String> lines = Generics.newArrayList();

		for (String line : text.split("[\n]+")) {
			lines.add(line);
		}

		return KDocument.newDocument(lines.toArray(new String[lines.size()]));

	}

}
