package ohs.string.sim;

import java.text.NumberFormat;

import ohs.types.Pair;

/**
 * 
 * A modified version of ScoreMatrix in SecondString
 * 
 * 
 * @author ohs
 */
public abstract class MemoMatrix {

	protected double[][] values;

	protected boolean[][] computed;

	protected String s;

	protected String t;

	protected Pair indexAtBest;

	protected double best;

	protected MemoMatrix(String s, String t) {
		this.s = s;
		this.t = t;
		values = new double[s.length() + 1][t.length() + 1];
		computed = new boolean[s.length() + 1][t.length() + 1];
		indexAtBest = new Pair(-1, -1);
		best = Double.NEGATIVE_INFINITY;
	}

	abstract protected double compute(int i, int j);

	public double get(int i, int j) {
		if (!computed[i][j]) {
			values[i][j] = compute(i, j);
			computed[i][j] = true;
		}
		return values[i][j];
	}

	public double getBestScore() {
		return get((int) indexAtBest.getFirst(), (int) indexAtBest.getSecond());
	}

	public Pair getIndexAtBest() {
		return indexAtBest;
	}

	public String getSource() {
		return s;
	}

	public String getTarget() {
		return t;
	}

	public double[][] getValues() {
		return values;
	}

	public void setIndexAtBest(Pair indexAtBest) {
		this.indexAtBest = indexAtBest;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("<S/T>");
		for (int i = 0; i < t.length(); i++)
			sb.append("\t" + t.charAt(i));
		sb.append("\n");

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setGroupingUsed(false);

		for (int i = 1; i <= s.length(); i++) {
			sb.append(s.charAt(i - 1));
			for (int j = 1; j <= t.length(); j++) {
				double v = get(i, j);
				sb.append("\t" + nf.format(v));
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
