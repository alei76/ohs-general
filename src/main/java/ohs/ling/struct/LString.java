package ohs.ling.struct;

public class LString {

	private String s;

	private String[] labels;

	public LString(String s) {
		this(s, new String[s.length()]);
	}

	public LString(String s, String[] labels) {
		this.s = s;
		this.labels = labels;
	}

	public String[] getLabels() {
		return labels;
	}

	public String getString() {
		return s;
	}

	public void setLabels(String[] labels) {
		this.labels = labels;
	}

	public void setString(String s) {
		this.s = s;
	}

}
