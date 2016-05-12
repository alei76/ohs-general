package ohs.nlp.pos;

import java.util.List;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.CommonFuncs;
import ohs.ml.neuralnet.Word2Vec;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KDocumentCollection;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.Conditions;
import ohs.utils.Generics;
import ohs.utils.UnicodeUtils;

public class HMMTrainer {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// test01();
		test02();

		System.out.println("process ends.");
	}

	// public static void test01() {
	// int[] states = { 0, 1 };
	// int N = states.length;
	//
	// double[][] tr_prs = ArrayUtils.matrix(N);
	// tr_prs[0][0] = 0.7;
	// tr_prs[0][1] = 0.3;
	// tr_prs[1][0] = 0.4;
	// tr_prs[1][1] = 0.6;
	//
	// int V = 3;
	//
	// double[][] ems_prs = ArrayUtils.matrix(N, V, 0);
	//
	// /*
	// * 산책:0, 쇼핑:1, 청소:2
	// */
	//
	// ems_prs[0][0] = 0.1;
	// ems_prs[0][1] = 0.4;
	// ems_prs[0][2] = 0.5;
	// ems_prs[1][0] = 0.6;
	// ems_prs[1][1] = 0.3;
	// ems_prs[1][2] = 0.1;
	//
	// double[] start_prs = new double[] { 0.6, 0.4 };
	//
	// HMM hmm = new HMM(start_prs, tr_prs, ems_prs);
	//
	// int[] obs = new int[] { 0, 0, 2, 1 };
	//
	// // hmm.forward(obs);
	//
	// // hmm.viterbi(obs);
	//
	// // hmm.backward(obs);
	//
	// HMMTrainer t = new HMMTrainer();
	//
	// t.train(hmm, obs, 1);
	// }

	public static void test02() throws Exception {

		KDocumentCollection coll = new KDocumentCollection();

		SejongReader r = new SejongReader(NLPPath.POS_DATA_FILE);
		while (r.hasNext()) {
			if (coll.size() == 1000) {
				break;
			}
			KDocument doc = r.next();
			coll.add(doc);

		}
		r.close();

		for (KDocument doc : coll) {
			for (KSentence sent : doc.getSentences()) {
				for (MultiToken mt : sent.toMultiTokens()) {
					String text = mt.getValue(TokenAttr.WORD);
					String text2 = UnicodeUtils.decomposeToJamoStr(text);

					System.out.println(text + "\t" + text2 + "\t" + String.valueOf(text2.getBytes()));
					// char[][] phomenes = UnicodeUtils.decomposeKoreanWordToPhonemes(word);
				}
			}
		}

		Set<String> posSet = Generics.newHashSet();

		for (String line : FileUtils.readLines(NLPPath.POS_TAG_SET_FILE)) {
			String[] parts = line.split("\t");
			posSet.add(parts[0]);
		}

		Indexer<String> wordIndexer = Generics.newIndexer();
		Indexer<String> posIndexer = Generics.newIndexer();

		wordIndexer.add("UNK");

		KSentence[] sents = coll.getSentences();

		int[][] wss = new int[sents.length][];
		int[][] posss = new int[sents.length][];

		for (int i = 0; i < sents.length; i++) {
			KSentence sent = sents[i];

			MultiToken[] toks = sent.toMultiTokens();
			int[] ws = ArrayUtils.arrayInt(toks.length);
			int[] poss = ArrayUtils.arrayInt(toks.length);

			for (int j = 0; j < toks.length; j++) {
				Token t = toks[j];
				ws[j] = wordIndexer.getIndex(t.getValue(TokenAttr.WORD));
				poss[j] = posIndexer.getIndex(t.getValue(TokenAttr.POS));
			}

			wss[i] = ws;
			posss[i] = poss;
		}

		HMM hmm = new HMM(posIndexer, wordIndexer);

		HMMTrainer trainer = new HMMTrainer();
		// trainer.trainUnsupervised(hmm, obss, 1);

		trainer.trainSupervised(hmm, wss, posss);

		hmm.write(NLPPath.POS_HMM_MODEL_FILE);

		// for (int i = 0; i < col.size(); i++) {
		// KDocument doc = col.get(i);
		//
		// for (int j = 0; j < doc.size(); j++) {
		// for (int k = 0; k < doc.size(); k++) {
		// KSentence sent = doc.getSentence(k);
		// }
		// }
		// }

		for (int i = 0; i < wss.length; i++) {
			int[] obs = wss[i];
			int[] sts = posss[i];
			int[] path = hmm.viterbi(obs);

			List<String> words = Generics.newArrayList();
			List<String> anss = Generics.newArrayList();
			List<String> preds = Generics.newArrayList();

			for (int j = 0; j < obs.length; j++) {
				String word = wordIndexer.getObject(obs[j]);
				String pred = posIndexer.getObject(path[j]);
				String ans = posIndexer.getObject(sts[j]);

				words.add(word);
				preds.add(pred);
				anss.add(ans);
			}

			System.out.println(words);
			System.out.println(anss);
			System.out.println(preds);
			System.out.println();
		}
	}

	private HMM hmm;

	private int N;

	private int V;

	private double[] tmp_phi;

	private double[][] tmp_a;

	private double[][] tmp_b;

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

	public void train(int[] obs) {

		double[][] alpha = forward(obs);
		double[][] beta = backward(obs);

		ArrayUtils.print("alpha", alpha);
		ArrayUtils.print("beta", alpha);

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

		ArrayUtils.copy(tmp_phi, hmm.getPhi());
		ArrayUtils.copy(tmp_a, hmm.getA());
		ArrayUtils.copy(tmp_b, hmm.getB());

		hmm.print();
	}

	public void train(int[] obs, int iter) {
		for (int i = 0; i < iter; i++) {
			train(obs);
		}
	}

	public void trainSupervised(HMM hmm, int[][] wss, int[][] posss) {
		double[] phi = hmm.getPhi();
		double[][] a = hmm.getA();
		double[][] b = hmm.getB();

		double[] posPrs = ArrayUtils.array(hmm.getN());
		double[] wordPrs = ArrayUtils.array(hmm.getV());

		ArrayUtils.setAll(phi, 0);
		ArrayUtils.setAll(a, 0);
		ArrayUtils.setAll(b, 0);

		CounterMap<Integer, Integer> bigramCnts = Generics.newCounterMap();

		for (int i = 0; i < wss.length; i++) {
			int[] ws = wss[i];
			int[] poss = posss[i];

			for (int j = 0; j < ws.length; j++) {
				int w1 = ws[j];
				int pos1 = poss[j];

				posPrs[pos1]++;
				wordPrs[w1]++;

				if (i == 0) {
					phi[w1]++;
				}

				b[pos1][w1]++;

				if (j + 1 < ws.length) {
					int w2 = ws[j + 1];
					int pos2 = poss[j + 1];
					a[pos1][pos2]++;
					bigramCnts.incrementCount(w1, w2, 1);
				}
			}
		}

		ArrayMath.normalize(phi);

		ArrayMath.normalizeRows(a);
		ArrayMath.normalize(posPrs);
		ArrayMath.addAfterScale(a, 0.5, posPrs, 0.5, a);

		ArrayMath.normalizeRows(b);
		ArrayMath.normalize(wordPrs);
		ArrayMath.addAfterScale(b, 0.5, wordPrs, 0.5, b);
	}

	public void trainUnsupervised(HMM hmm, int[][] obss, int iter) {
		this.hmm = hmm;

		this.N = hmm.getN();
		this.V = hmm.getV();

		tmp_phi = ArrayUtils.array(N);
		tmp_a = ArrayUtils.matrix(N, N);
		tmp_b = ArrayUtils.matrix(N, V);

		for (int i = 0; i < iter; i++) {
			for (int j = 0; j < obss.length; j++) {
				train(obss[j]);
			}
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
