package ohs.ml.hmm;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.types.Indexer;

public class HMM {

	/**
	 * start probabilities
	 */
	private double[] phi;

	/**
	 * transition probabilities
	 */
	private double[][] a;

	/**
	 * emission probabilities
	 */
	private double[][] b;

	/**
	 * number of states
	 */
	private int N;

	/**
	 * number of unique observations in vocabulary
	 */
	private int V;

	private Indexer<String> stateIndexer;

	private Indexer<String> wordIndexer;

	public HMM(double[] start_prs, double[][] trans_prs, double[][] emission_prs) {
		this.N = start_prs.length;
		this.V = emission_prs[0].length;

		this.phi = start_prs;
		this.a = trans_prs;
		this.b = emission_prs;
	}

	public HMM(Indexer<String> stateIndexer, Indexer<String> wordIndexer) {
		this.stateIndexer = stateIndexer;
		this.wordIndexer = wordIndexer;
	}

	public HMM(int N, int V) {
		this.N = N;
		this.V = V;

		phi = ArrayUtils.array(N);
		a = ArrayUtils.matrix(N, N);
		b = ArrayUtils.matrix(N, V);

		ArrayMath.random(0, 1, phi);
		ArrayMath.random(0, 1, a);
		ArrayMath.random(0, 1, b);

		ArrayMath.normalize(phi);
		ArrayMath.normalizeRows(a);
		ArrayMath.normalizeRows(b);
	}

	public double[][] getA() {
		return a;
	}

	public double getA(int i, int j) {
		return a[i][j];
	}

	public double[][] getB() {
		return b;
	}

	public double getB(int i, int t) {
		return b[i][t];
	}

	public int getN() {
		return N;
	}

	public double[] getPhi() {
		return phi;
	}

	public double getPhi(int i) {
		return phi[i];
	}

	public int getV() {
		return V;
	}

	public void print() {
		System.out.println(ArrayUtils.toString("phi", phi));
		System.out.println();

		System.out.println(ArrayUtils.toString("a", a));
		System.out.println();

		System.out.println(ArrayUtils.toString("b", b));
		System.out.println();
	}

	public void setA(double[][] a) {
		this.a = a;
	}

	public void setA(int i, int j, double value) {
		a[i][j] = value;
	}

	public void setB(double[][] b) {
		this.b = b;
	}

	public void setB(int i, int t, double value) {
		b[i][t] = value;
	}

	public void setN(int n) {
		N = n;
	}

	public void setPhi(double[] phi) {
		this.phi = phi;
	}

	public void setPhi(int i, double value) {
		phi[i] = value;
	}

	public void setV(int v) {
		V = v;
	}

	public int[] viterbi(int[] obs) {
		int T = obs.length;
		double[][] fwd = ArrayUtils.matrix(N, T);
		int[][] backPointers = ArrayUtils.matrixInt(N, T);

		for (int i = 0; i < N; i++) {
			fwd[i][0] = phi[i] * b[i][obs[i]];
		}

		double[] tmp = ArrayUtils.array(N);

		for (int t = 1; t < T; t++) {
			for (int j = 0; j < N; j++) {
				ArrayUtils.setAll(tmp, 0);
				for (int i = 0; i < N; i++) {
					tmp[i] = fwd[i][t - 1] * a[i][j];
				}
				int k = ArrayMath.argMax(tmp);
				fwd[j][t] = tmp[k] * b[j][obs[t]];
				backPointers[j][t] = k;
			}
		}

		int[] path = ArrayUtils.arrayInt(T);
		ArrayUtils.copyColumn(fwd, T - 1, tmp);
		int q = ArrayMath.argMax(tmp);

		for (int t = T - 1; t >= 0; t--) {
			path[t] = q;
			q = backPointers[q][t];
		}
		return path;
	}

}
