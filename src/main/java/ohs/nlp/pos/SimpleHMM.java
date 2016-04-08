package ohs.nlp.pos;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.CommonFuncs;
import ohs.utils.Conditions;

public class SimpleHMM {

	public static void main(String[] args) {
		System.out.println("process begins.");

		int[] states = { 0, 1 };
		int num_states = states.length;

		double[][] trans_probs = ArrayUtils.matrix(num_states);
		trans_probs[0][0] = 0.7;
		trans_probs[0][1] = 0.3;
		trans_probs[1][0] = 0.4;
		trans_probs[1][1] = 0.6;

		int voc_size = 3;

		double[][] emission_probs = ArrayUtils.matrix(num_states, voc_size, 0);

		/*
		 * 산책:0, 쇼핑:1, 청소:2
		 */

		emission_probs[0][0] = 0.1;
		emission_probs[0][1] = 0.4;
		emission_probs[0][2] = 0.5;
		emission_probs[1][0] = 0.6;
		emission_probs[1][1] = 0.3;
		emission_probs[1][2] = 0.1;

		double[] start_probs = new double[] { 0.6, 0.4 };

		SimpleHMM hmm = new SimpleHMM(start_probs, trans_probs, emission_probs);

		int[] obs = new int[] { 0, 0, 2, 1 };

		// hmm.forward(obs);

		// hmm.viterbi(obs);

		// hmm.backward(obs);

		hmm.train(obs, 2);

		System.out.println("process ends.");
	}

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

	private double[][] tmp_a;

	private double[][] tmp_b;

	private double[] tmp_phi;

	public SimpleHMM(double[] start_prs, double[][] trans_prs, double[][] emission_prs) {
		this.N = start_prs.length;
		this.V = emission_prs[0].length;

		this.phi = start_prs;
		this.a = trans_prs;
		this.b = emission_prs;

		tmp_phi = ArrayUtils.copy(phi);
		tmp_a = ArrayUtils.copy(a);
		tmp_b = ArrayUtils.copy(b);
	}

	public SimpleHMM(int N, int V) {
		this.N = N;
		this.V = V;

		phi = ArrayUtils.array(V);
		a = ArrayUtils.matrix(N, N);
		b = ArrayUtils.matrix(N, V);

		ArrayMath.random(0, 1, phi);
		ArrayMath.random(0, 1, a);
		ArrayMath.random(0, 1, b);

		ArrayMath.normalize(phi);
		ArrayMath.normalizeRows(a);
		ArrayMath.normalizeRows(b);
	}

	public double[][] backward(int[] obs) {
		int T = obs.length;
		double[][] beta = ArrayUtils.matrix(N, T);
		for (int i = 0; i < N; i++) {
			beta[i][T - 1] = 1;
		}

		double sum = 0;
		for (int t = T - 2; t >= 0; t--) {
			for (int j = 0; j < N; j++) {
				sum = 0;
				for (int i = 0; i < N; i++) {
					sum += beta[i][t + 1] * a[j][i] * b[i][obs[t + 1]];
				}
				beta[j][t] = sum;
			}
		}
		return beta;
	}

	/**
	 * 
	 * 
	 * @param obs
	 * @return
	 */
	public double[][] forward(int[] obs) {
		int T = obs.length;
		double[][] alpha = ArrayUtils.matrix(N, T);
		for (int i = 0; i < N; i++) {
			int o = obs[i];
			alpha[i][0] = phi[i] * b[i][o];
		}
		double sum = 0;
		for (int t = 1; t < T; t++) {
			for (int j = 0; j < N; j++) {
				sum = 0;
				for (int i = 0; i < N; i++) {
					sum += alpha[i][t - 1] * a[i][j];
				}
				sum *= b[j][obs[t]];
				alpha[j][t] = sum;
			}
		}
		return alpha;
	}

	public double gamma(int t, int i, double[][] alpha, double[][] beta) {
		double ret = (alpha[i][t] * beta[i][t]);
		double norm = 0;
		for (int j = 0; j < N; j++) {
			norm += alpha[j][t] * beta[j][t];
		}
		ret = CommonFuncs.divide(ret, norm);
		return ret;
	}

	public double likelihood(double[][] alpha) {
		return ArrayMath.sumColumn(alpha, alpha[0].length - 1);
	}

	public void print() {
		System.out.println(ArrayUtils.toString("phi", phi));
		System.out.println();

		System.out.println(ArrayUtils.toString("a", a));
		System.out.println();

		System.out.println(ArrayUtils.toString("b", b));
		System.out.println();
	}

	public void train(int[] obs) {
		double[][] alpha = forward(obs);
		double[][] beta = backward(obs);

		for (int i = 0; i < N; i++) {
			tmp_phi[i] = gamma(0, i, alpha, beta);
		}

		int T = obs.length;

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				double value = 0;
				double norm = 0;
				for (int t = 0; t < T; t++) {
					value += xi(t, i, j, obs, alpha, beta);
					norm += gamma(t, i, alpha, beta);
				}
				tmp_a[i][j] = CommonFuncs.divide(value, norm);
			}
		}

		for (int i = 0; i < N; i++) {
			for (int k = 0; k < V; k++) {
				double value = 0;
				double norm = 0;
				for (int t = 0; t < T; t++) {
					double g = gamma(t, i, alpha, beta);
					value += g * Conditions.value(k == obs[t], 1, 0);
					norm += g;
				}
				tmp_b[i][k] = CommonFuncs.divide(value, norm);
			}
		}

		ArrayUtils.copy(tmp_phi, phi);
		ArrayUtils.copy(tmp_a, a);
		ArrayUtils.copy(tmp_b, b);

		print();
	}

	public void train(int[] obs, int iter) {
		for (int i = 0; i < iter; i++) {
			train(obs);
		}
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

	public double xi(int t, int i, int j, int[] obs, double[][] alpha, double[][] beta) {
		double ret = 0;
		int T = obs.length;

		if (t == T - 1) {
			ret = alpha[i][t] * a[i][j];
		} else {
			ret = alpha[i][t] * a[i][j] * b[j][obs[t + 1]] * beta[j][t + 1];
		}

		double norm = 0;
		for (int k = 0; k < N; k++) {
			norm += alpha[k][t] * beta[k][t];
		}
		ret = CommonFuncs.divide(ret, norm);
		return ret;
	}

}
