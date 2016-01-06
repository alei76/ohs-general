package ohs.string.sim;

public class CharacterSequence implements Sequence {

	/**
	 * 
	 */
	private static final long serialVersionUID = -392245256643096856L;

	private String s;

	public CharacterSequence(String s) {
		this.s = s;
	}

	public String get(int i) {
		return s.charAt(i) + "";
	}

	@Override
	public int length() {
		return s.length();
	}

	public String toString() {
		return s;
	}

}
