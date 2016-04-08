package ohs.string.sim;

public class CharSequence implements Sequence<Character> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -392245256643096856L;

	private String s;

	public CharSequence(String s) {
		this.s = s;
	}

	@Override
	public Character get(int i) {
		return s.charAt(i);
	}

	@Override
	public int length() {
		return s.length();
	}

	@Override
	public String toString() {
		return s;
	}

}
