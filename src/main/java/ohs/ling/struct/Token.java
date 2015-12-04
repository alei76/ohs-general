package ohs.ling.struct;

import java.util.HashMap;
import java.util.Map;

public class Token extends Span {

	private Map attrs;

	public Token(int start, String s) {
		super(start, s);
	}

	public Map getAttrs() {
		if (attrs == null) {
			attrs = new HashMap();
		}
		return attrs;
	}

}
