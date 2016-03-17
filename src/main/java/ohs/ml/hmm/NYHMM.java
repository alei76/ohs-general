package ohs.ml.hmm;

import java.text.*;

import org.apache.commons.math.stat.descriptive.SynchronizedMultivariateSummaryStatistics;

import ohs.math.ArrayUtils;

/**
 * This class implements a Hidden Markov Model, as well as the Baum-Welch Algorithm for training HMMs.
 * 
 * @author Holger Wunsch (wunsch@sfs.nphil.uni-tuebingen.de)
 */
public class NYHMM {

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

		NYHMM hmm = new NYHMM(start_probs, trans_probs, emission_probs);

		int[] obs = new int[] { 0, 0, 2, 1 };

		hmm.forwardProc(obs);
		hmm.backwardProc(obs);
		hmm.train(obs, 2);

		System.out.println("process ends.");
	}

	/** number of states */
	public int numStates;

	/** size of output vocabulary */
	public int sigmaSize;

	/** initial state probabilities */
	public double pi[];

	/** transition probabilities */
	public double a[][];

	/** emission probabilities */
	public double b[][];

	public NYHMM(double[] start_prs, double[][] trans_prs, double[][] emission_prs) {
		this.numStates = start_prs.length;
		this.sigmaSize = emission_prs[0].length;

		this.pi = start_prs;
		this.a = trans_prs;
		this.b = emission_prs;
	}

	/**
	 * initializes an NYHMM.
	 * 
	 * @param numStates
	 *            number of states
	 * @param sigmaSize
	 *            size of output vocabulary
	 */
	public NYHMM(int numStates, int sigmaSize) {
		this.numStates = numStates;
		this.sigmaSize = sigmaSize;

		pi = new double[numStates];
		a = new double[numStates][numStates];
		b = new double[numStates][sigmaSize];
	}

	/**
	 * implementation of the Baum-Welch Algorithm for HMMs.
	 * 
	 * @param o
	 *            the training set
	 * @param steps
	 *            the number of steps
	 */
	public void train(int[] o, int steps) {
		int T = o.length;
		double[][] fwd;
		double[][] bwd;

		double pi1[] = new double[numStates];
		double a1[][] = new double[numStates][numStates];
		double b1[][] = new double[numStates][sigmaSize];

		for (int s = 0; s < steps; s++) {
			/*
			 * calculation of Forward- und Backward Variables from the current model
			 */
			fwd = forwardProc(o);
			bwd = backwardProc(o);

			/* re-estimation of initial state probabilities */
			for (int i = 0; i < numStates; i++)
				pi1[i] = gamma(i, 0, o, fwd, bwd);

			/* re-estimation of transition probabilities */
			for (int i = 0; i < numStates; i++) {
				for (int j = 0; j < numStates; j++) {
					double num = 0;
					double denom = 0;
					for (int t = 0; t <= T - 1; t++) {
						num += p(t, i, j, o, fwd, bwd);
						denom += gamma(i, t, o, fwd, bwd);
					}
					a1[i][j] = divide(num, denom);
				}
			}

			/* re-estimation of emission probabilities */
			for (int i = 0; i < numStates; i++) {
				for (int k = 0; k < sigmaSize; k++) {
					double num = 0;
					double denom = 0;

					for (int t = 0; t <= T - 1; t++) {
						double g = gamma(i, t, o, fwd, bwd);
						num += g * (k == o[t] ? 1 : 0);
						denom += g;
					}
					b1[i][k] = divide(num, denom);
				}
			}

			pi = pi1;
			a = a1;
			b = b1;
		}
		
		System.out.println("pi:");
		System.out.println(ArrayUtils.toString(pi));
		System.out.println();

		System.out.println("a:");
		System.out.println(ArrayUtils.toString(a));
		System.out.println();

		System.out.println("b:");
		System.out.println(ArrayUtils.toString(b));
		System.out.println();
	}

	/**
	 * calculation of Forward-Variables f(i,t) for state i at time t for output sequence O with the current NYHMM parameters
	 * 
	 * @param o
	 *            the output sequence O
	 * @return an array f(i,t) over states and times, containing the Forward-variables.
	 */
	public double[][] forwardProc(int[] o) {
		int T = o.length;
		double[][] fwd = new double[numStates][T];

		/* initialization (time 0) */
		for (int i = 0; i < numStates; i++)
			fwd[i][0] = pi[i] * b[i][o[0]];

		/* induction */
		for (int t = 0; t <= T - 2; t++) {
			for (int j = 0; j < numStates; j++) {
				fwd[j][t + 1] = 0;
				for (int i = 0; i < numStates; i++)
					fwd[j][t + 1] += (fwd[i][t] * a[i][j]);
				fwd[j][t + 1] *= b[j][o[t + 1]];
			}
		}

//		System.out.println(ArrayUtils.toString(fwd));
//		System.out.println();

		return fwd;
	}

	/**
	 * calculation of Backward-Variables b(i,t) for state i at time t for output sequence O with the current NYHMM parameters
	 * 
	 * @param o
	 *            the output sequence O
	 * @return an array b(i,t) over states and times, containing the Backward-Variables.
	 */
	public double[][] backwardProc(int[] o) {
		int T = o.length;
		double[][] bwd = new double[numStates][T];

		/* initialization (time 0) */
		for (int i = 0; i < numStates; i++)
			bwd[i][T - 1] = 1;

		/* induction */
		for (int t = T - 2; t >= 0; t--) {
			for (int i = 0; i < numStates; i++) {
				bwd[i][t] = 0;
				for (int j = 0; j < numStates; j++)
					bwd[i][t] += (bwd[j][t + 1] * a[i][j] * b[j][o[t + 1]]);
			}
		}

//		System.out.println(ArrayUtils.toString(bwd));
//		System.out.println();

		return bwd;
	}

	/**
	 * calculation of probability P(X_t = s_i, X_t+1 = s_j | O, m).
	 * 
	 * @param t
	 *            time t
	 * @param i
	 *            the number of state s_i
	 * @param j
	 *            the number of state s_j
	 * @param o
	 *            an output sequence o
	 * @param fwd
	 *            the Forward-Variables for o
	 * @param bwd
	 *            the Backward-Variables for o
	 * @return P
	 */
	public double p(int t, int i, int j, int[] o, double[][] fwd, double[][] bwd) {
		double num;
		if (t == o.length - 1)
			num = fwd[i][t] * a[i][j];
		else
			num = fwd[i][t] * a[i][j] * b[j][o[t + 1]] * bwd[j][t + 1];

		double denom = 0;

		for (int k = 0; k < numStates; k++)
			denom += (fwd[k][t] * bwd[k][t]);

		return divide(num, denom);
	}

	/** computes gamma(i, t) */
	public double gamma(int i, int t, int[] o, double[][] fwd, double[][] bwd) {
		double num = fwd[i][t] * bwd[i][t];
		double denom = 0;

		for (int j = 0; j < numStates; j++)
			denom += fwd[j][t] * bwd[j][t];

		return divide(num, denom);
	}

	/** prints all the parameters of an NYHMM */
	public void print() {
		DecimalFormat fmt = new DecimalFormat();
		fmt.setMinimumFractionDigits(5);
		fmt.setMaximumFractionDigits(5);

		for (int i = 0; i < numStates; i++)
			System.out.println("pi(" + i + ") = " + fmt.format(pi[i]));
		System.out.println();

		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++)
				System.out.print("a(" + i + "," + j + ") = " + fmt.format(a[i][j]) + "  ");
			System.out.println();
		}

		System.out.println();
		for (int i = 0; i < numStates; i++) {
			for (int k = 0; k < sigmaSize; k++)
				System.out.print("b(" + i + "," + k + ") = " + fmt.format(b[i][k]) + "  ");
			System.out.println();
		}
	}

	/** divides two doubles. 0 / 0 = 0! */
	public double divide(double n, double d) {
		if (n == 0)
			return 0;
		else
			return n / d;
	}
}