package ohs.ir.lucene.common;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

public final class LengthFilter extends FilteringTokenFilter {

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private int max_token_len;
	private int min_token_len;

	public LengthFilter(TokenStream in) {
		this(in, 30, 1);
	}

	public LengthFilter(TokenStream in, int max_token_len, int min_token_len) {
		super(in);
		this.max_token_len = max_token_len;
		this.min_token_len = min_token_len;
	}

	@Override
	protected boolean accept() {
		String term = new String(termAtt.buffer(), 0, termAtt.length());

		if (min_token_len < term.length() && max_token_len > term.length()) {
			return true;
		} else {
			return false;
		}

	}

}
