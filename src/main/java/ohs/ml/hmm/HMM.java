package ohs.ml.hmm;

import java.util.List;

import org.apache.commons.math.stat.descriptive.SynchronizedMultivariateSummaryStatistics;
import org.apache.lucene.index.NoMergePolicy;

import kr.co.shineware.nlp.komoran.b.b;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;

public class HMM {

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

		HMM hmm = new HMM(start_probs, trans_probs, emission_probs);

		int[] obs = new int[] { 0, 0, 2, 1 };

		hmm.forward(obs);

		hmm.viterbi(obs);

		hmm.backward(obs);

		System.out.println("process ends.");
	}

	/**
	 * start probabilities
	 */
	private double[] phi;

	/**
	 * transition probabilities
	 */
	private double[][] A;

	/**
	 * emission probabilities
	 */
	private double[][] B;

	private int num_states;

	private int voc_size;

	public HMM(double[] start_prs, double[][] trans_prs, double[][] emission_prs) {
		this.num_states = start_prs.length;
		this.voc_size = emission_prs[0].length;

		this.phi = start_prs;
		this.A = trans_prs;
		this.B = emission_prs;
	}

	public HMM(int num_states, int voc_size) {
		this.num_states = num_states;
		this.voc_size = voc_size;

		A = ArrayUtils.matrix(num_states, num_states);
		B = ArrayUtils.matrix(num_states, voc_size);
		phi = ArrayUtils.array(voc_size);

		ArrayMath.random(0, 1, A);
		ArrayMath.random(0, 1, B);
		ArrayMath.random(0, 1, phi);

		ArrayMath.normalizeRows(A);
		ArrayMath.normalizeRows(B);
	}

	public double[][] forward(int[] obs) {
		int T = obs.length;
		double[][] fwd = ArrayUtils.matrix(num_states, T);
		for (int i = 0; i < num_states; i++) {
			int o = obs[i];
			fwd[i][0] = phi[i] * B[i][o];
		}
		double sum = 0;
		for (int t = 1; t < T; t++) {
			for (int j = 0; j < num_states; j++) {
				sum = 0;
				for (int i = 0; i < num_states; i++) {
					sum += fwd[i][t - 1] * A[i][j];
				}
				sum *= B[j][obs[t]];
				fwd[j][t] = sum;
			}
		}

		System.out.println(ArrayUtils.toString(fwd));
		System.out.println();

		return fwd;
	}

	public double[][] backward(int[] obs) {
		int T = obs.length;
		double[][] bwd = ArrayUtils.matrix(num_states, T);
		for (int i = 0; i < num_states; i++) {
			bwd[i][T - 1] = 1;
		}

		// System.out.println(ArrayUtils.toString(bwd));
		// System.out.println();
		//
		// System.out.println(ArrayUtils.toString(A));
		// System.out.println();
		//
		// System.out.println(ArrayUtils.toString(B));
		// System.out.println();

		double sum = 0;
		for (int t = T - 2; t >= 0; t--) {
			for (int j = 0; j < num_states; j++) {
				sum = 0;
				for (int i = 0; i < num_states; i++) {
					sum += bwd[i][t + 1] * A[j][i] * B[i][obs[t + 1]];
				}
				bwd[j][t] = sum;
			}
		}

		System.out.println(ArrayUtils.toString(bwd));
		System.out.println();

		return bwd;

	}

	public void train(int[] obs) {
		double[][] alpha = forward(obs);
		double[][] beta = backward(obs);

	}

	private double[][] tmp_fwd;

	private double[][] tmp_bwd;

	private int[][] tmp_backtrace;

	public double gamma(int i, int t, double[][] alpha, double[][] beta) {
		double ret = (alpha[i][t] * beta[i][t]);
		double norm = 0;
		for (int j = 0; j < num_states; j++) {
			norm += alpha[j][t] * beta[j][t];
		}
		if (ret != 0) {
			ret /= norm;
		}
		return ret;
	}

	public int[] viterbi(int[] obs) {
		int T = obs.length;
		double[][] fwd = ArrayUtils.matrix(num_states, T);
		int[][] backtrace = ArrayUtils.matrixInt(num_states, T);

		for (int i = 0; i < num_states; i++) {
			fwd[i][0] = phi[i] * B[i][obs[i]];
		}

		double[] tmp = ArrayUtils.array(num_states);

		for (int t = 1; t < T; t++) {
			for (int j = 0; j < num_states; j++) {
				ArrayUtils.setAll(tmp, 0);
				for (int i = 0; i < num_states; i++) {
					tmp[i] = fwd[i][t - 1] * A[i][j];
				}
				int k = ArrayMath.argMax(tmp);
				fwd[j][t] = tmp[k] * B[j][obs[t]];
				backtrace[j][t] = k;
			}
		}

		// System.out.println(ArrayUtils.toString(fwd));
		// System.out.println();
		//
		// System.out.println(ArrayUtils.toString(backtrace));
		// System.out.println();

		int[] path = ArrayUtils.arrayInt(T);
		ArrayUtils.copyColumn(fwd, T - 1, tmp);
		int q = ArrayMath.argMax(tmp);

		for (int t = T - 1; t >= 0; t--) {
			path[t] = q;
			q = backtrace[q][t];
		}
		return path;
	}

}
