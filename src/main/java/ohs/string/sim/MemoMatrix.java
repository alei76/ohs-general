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

	protected Sequence s;

	protected Sequence t;

	protected Pair<Integer, Integer> indexAtMax;

	protected Pair<Integer, Integer> indexAtMin;

	protected double max;

	protected double min;

	protected MemoMatrix(Sequence s, Sequence t) {
		this.s = s;
		this.t = t;
		values = new double[s.length() + 1][t.length() + 1];
		computed = new boolean[s.length() + 1][t.length() + 1];
		indexAtMax = new Pair<Integer, Integer>(-1, -1);
		indexAtMin = new Pair<Integer, Integer>(-1, -1);
		max = -Double.MAX_VALUE;
		min = Double.MAX_VALUE;
	}

	abstract protected double compute(int i, int j);

	public double get(int i, int j) {
		if (!computed[i][j]) {
			values[i][j] = compute(i, j);
			computed[i][j] = true;
		}
		return values[i][j];
	}

	public Pair<Integer, Integer> getIndexAtMax() {
		return indexAtMax;
	}

	public double getMaxScore() {
		return get(indexAtMax.getFirst(), indexAtMax.getSecond());
	}
	
	public double getMinScore(){
		return get(indexAtMin.getFirst(), indexAtMin.getSecond());
	}

	public Sequence getSource() {
		return s;
	}

	public Sequence getTarget() {
		return t;
	}

	public double[][] getValues() {
		return values;
	}

	public void setIndexAtBest(Pair<Integer, Integer> indexAtMax) {
		this.indexAtMax = indexAtMax;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("<S/T>");
		for (int i = 0; i < t.length(); i++)
			sb.append("\t" + t.get(i));
		sb.append("\n");

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setGroupingUsed(false);

		for (int i = 1; i <= s.length(); i++) {
			sb.append(s.get(i - 1));
			for (int j = 1; j <= t.length(); j++) {
				double v = get(i, j);
				sb.append("\t" + nf.format(v));
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
