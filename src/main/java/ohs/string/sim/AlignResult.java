package ohs.string.sim;

import java.util.List;

public class AlignResult {

	private MemoMatrix scoreMatrix;
	private String s;
	private String t;
	private List<MatchType> matches;

	public AlignResult(MemoMatrix scoreMatrix, String s, String t, List<MatchType> matches) {
		super();
		this.scoreMatrix = scoreMatrix;
		this.s = s;
		this.t = t;
		this.matches = matches;
	}

	public List<MatchType> getMarkups() {
		return matches;
	}

	public MemoMatrix getScoreMatrix() {
		return scoreMatrix;
	}

	public String getSource() {
		return s;
	}

	public String getTarget() {
		return t;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < matches.size(); i++) {
			char si = s.charAt(i);
			char ti = t.charAt(i);
			String s = String.format("%d:\t%s\t%s\t%s", i + 1, si, matches.get(i).getSymbol(), ti);
			sb.append(s + "\n");
		}

		return sb.toString().trim();
	}

}
