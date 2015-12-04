package ohs.string.sim;

public enum MatchType {
	MATCH("="), UNMATCH("!="), SIMILAR(":=");

	private String symbol;

	private MatchType(String symbol) {
		this.symbol = symbol;
	}

	String getSymbol() {
		return symbol;
	}
}