package ohs.ir.lucene.common;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharacterUtils;

public final class NumberFilter extends TokenFilter {
	private static final Pattern p = Pattern.compile("\\d+[\\d,\\.]*");
	private final CharacterUtils charUtils = CharacterUtils.getInstance();
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	public NumberFilter(TokenStream in) {
		super(in);
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (!input.incrementToken()) {
			return false;
		}

		String term = termAtt.toString();
		String temp = term;
		Matcher m = p.matcher(term);

		if (m.find()) {
			StringBuffer sb = new StringBuffer();
			do {
				String g = m.group();
				g = g.replace(",", "");

				StringBuffer sb2 = new StringBuffer("<N");

				String[] toks = g.split("\\.");
				for (int i = 0; i < toks.length; i++) {
					String tok = toks[i];
					sb2.append(tok.length());

					if (i != toks.length - 1) {
						sb2.append("_");
					}
				}
				sb2.append(">");
				String r = sb2.toString();
				m.appendReplacement(sb, r);
			} while (m.find());
			m.appendTail(sb);

			term = sb.toString();
			termAtt.copyBuffer(term.toCharArray(), 0, term.length());
			// System.out.printf("%text -> %text\n", temp, term);
		}

		return true;
	}
}