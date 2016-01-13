package ohs.string.sim;

import java.util.List;

import ohs.utils.StrUtils;

public class StringSequence implements Sequence {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7775832719092857726L;

	private List<String> s;

	public StringSequence(List<String> s) {
		this.s = s;
	}

	public StringSequence(String s) {
		this(StrUtils.split(s));
	}

	@Override
	public String get(int i) {
		return s.get(i);
	}

	@Override
	public int length() {
		return s.size();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.size(); i++) {
			sb.append(s.get(i));
			if (i != s.size() - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

}
