package ohs.ml.hmm;

import edu.stanford.nlp.ling.Word;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.CommonFuncs;
import ohs.nlp.ling.types.KCollection;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.nlp.pos.NLPPath;
import ohs.types.Indexer;
import ohs.utils.Generics;

public class HMMTrainer {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// test01();
		test02();

		System.out.println("process ends.");
	}

	public static void test01() {
		int[] states = { 0, 1 };
		int N = states.length;

		double[][] tr_prs = ArrayUtils.matrix(N);
		tr_prs[0][0] = 0.7;
		tr_prs[0][1] = 0.3;
		tr_prs[1][0] = 0.4;
		tr_prs[1][1] = 0.6;

		int V = 3;

		double[][] ems_prs = ArrayUtils.matrix(N, V, 0);

		/*
		 * 산책:0, 쇼핑:1, 청소:2
		 */

		ems_prs[0][0] = 0.1;
		ems_prs[0][1] = 0.4;
		ems_prs[0][2] = 0.5;
		ems_prs[1][0] = 0.6;
		ems_prs[1][1] = 0.3;
		ems_prs[1][2] = 0.1;

		double[] start_prs = new double[] { 0.6, 0.4 };

		HMM hmm = new HMM(start_prs, tr_prs, ems_prs);

		int[] obs = new int[] { 0, 0, 2, 1 };

		// hmm.forward(obs);

		// hmm.viterbi(obs);

		// hmm.backward(obs);

		HMMTrainer t = new HMMTrainer();

		t.train(hmm, obs, 2);
	}

	public static void test02() throws Exception {
		KCollection col = new KCollection();
		col.read(NLPPath.POS_SENT_COL_FILE);

		Indexer<String> wordIndexer = Generics.newIndexer();
		Indexer<String> posIndexer = Generics.newIndexer();

		int[][] obss = new int[col.size()][];
		int[][] stss = new int[col.size()][];

		for (int i = 0; i < col.size(); i++) {
			KSentence sent = col.get(i).toSentence();

			Token[] toks = sent.toTokens();
			int[] obs = ArrayUtils.arrayInt(toks.length);
			int[] sts = ArrayUtils.arrayInt(toks.length);

			for (int j = 0; j < toks.length; j++) {
				Token t = toks[j];
				obs[j] = wordIndexer.getIndex(t.getValue(TokenAttr.WORD));
				sts[j] = posIndexer.getIndex(t.getValue(TokenAttr.POS));
			}

			obss[i] = obs;
			stss[i] = sts;
		}

		int N = posIndexer.size();
		int V = wordIndexer.size();

		HMM hmm = new HMM(N, V);

		HMMTrainer trainer = new HMMTrainer();
		trainer.train(hmm, obss, 1);

	}

	private HMM hmm;

	private int N;

	private int V;

	private HMM tmp;

	public HMMTrainer() {

	}

	public double[][] backward(int[] obs) {
		int T = obs.length;
		double[][] a = hmm.getA();
		double[][] b = hmm.getB();
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
		double[] phi = hmm.getPhi();
		double[][] a = hmm.getA();
		double[][] b = hmm.getB();

		double[][] alpha = ArrayUtils.matrix(N, T);
		for (int i = 0; i < N; i++) {
			double v = b[i][obs[0]];
			alpha[i][0] = phi[i] * b[i][obs[0]];
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
		double norm = ArrayMath.dotProductColumns(alpha, t, beta, t);
		ret = CommonFuncs.divide(ret, norm);
		return ret;
	}

	public double likelihood(double[][] alpha) {
		return ArrayMath.sumColumn(alpha, alpha[0].length - 1);
	}

	private void train(HMM hmm, HMM tmp, int[][] obss, int iter) {
		this.hmm = hmm;
		this.tmp = tmp;

		this.N = hmm.getN();
		this.V = hmm.getV();

		for (int i = 0; i < iter; i++) {
			for (int j = 0; j < obss.length; j++) {
				train(obss[j]);
			}
		}
	}

	public void train(HMM hmm, int[] obs, int iter) {
		tmp = new HMM(hmm.getN(), hmm.getV());
		int[][] d = new int[1][];
		d[0] = obs;
		train(hmm, tmp, d, iter);
	}

	public void train(HMM hmm, int[][] obss, int iter) {
		tmp = new HMM(hmm.getN(), hmm.getV());
		train(hmm, tmp, obss, iter);
	}

	public void train(int N, int V, int[][] obss, int iter) {
		hmm = new HMM(N, V);
		tmp = new HMM(N, V);

		train(hmm, tmp, obss, iter);
	}

	public void train(int[] obs) {
		double[] tmp_phi = tmp.getPhi();
		double[][] tmp_a = tmp.getA();
		double[][] tmp_b = tmp.getB();

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
					value += g * CommonFuncs.value(k == obs[t], 1, 0);
					norm += g;
				}
				tmp_b[i][k] = CommonFuncs.divide(value, norm);
			}
		}

		ArrayUtils.copy(tmp_phi, hmm.getPhi());
		ArrayUtils.copy(tmp_a, hmm.getA());
		ArrayUtils.copy(tmp_b, hmm.getB());

		// hmm.print();
	}

	public void train(int[] obs, int iter) {
		for (int i = 0; i < iter; i++) {
			train(obs);
		}
	}

	public double xi(int t, int i, int j, int[] obs, double[][] alpha, double[][] beta) {
		double ret = 0;
		int T = obs.length;
		double[][] a = hmm.getA();
		double[][] b = hmm.getB();
		if (t == T - 1) {
			ret = alpha[i][t] * a[i][j];
		} else {
			ret = alpha[i][t] * a[i][j] * b[j][obs[t + 1]] * beta[j][t + 1];
		}
		double norm = ArrayMath.dotProductColumns(alpha, t, beta, t);
		ret = CommonFuncs.divide(ret, norm);
		return ret;
	}

}
