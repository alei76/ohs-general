package ohs.ling.struct;

public class Token {

	private String[] attrs = new String[TokenAttr.values().length];

	private int start;

	public Token(int start, String word) {
		this.start = start;
		attrs[TokenAttr.WORD.ordinal()] = word;
	}

	public String getValue(TokenAttr attr) {
		return attrs[attr.ordinal()];
	}

	public void setValue(TokenAttr attr, String value) {
		attrs[attr.ordinal()] = value;
	}

}
