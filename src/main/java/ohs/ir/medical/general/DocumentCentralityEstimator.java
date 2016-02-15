package ohs.ir.medical.general;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.LA;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;

/**
 * This class implements centralities of categories.
 * 
 * The standard centralites are computed by PageRank algorithms where a graph over categories are constructed.
 * 
 * 
 * 
 * 
 * 1. Kurland, O. and Lee, L. 2005. PageRank without hyperlinks: structural re-ranking using links induced by language models. Proceedings
 * of the 28th annual international ACM SIGIR conference on Research and development in information retrieval, 306–313.
 * 
 * 
 * 2. Strube, M. and Ponzetto, S.P. 2006. WikiRelate! computing semantic relatedness using wikipedia. proceedings of the 21st national
 * conference on Artificial intelligence - Volume 2, AAAI Press, 1419–1424.
 * 
 * 
 * @author Heung-Seon Oh
 * 
 * 
 */
public class DocumentCentralityEstimator {

	private StringBuffer logBuff;

	private double dirichlet_prior = 1500;

	private double mixture_for_col = 0;

	private int num_top_docs = 10;

	private WordCountBox wcb;

	public DocumentCentralityEstimator(WordCountBox wcb) {
		this.wcb = wcb;
	}

	public SparseVector estimate() {
		double[][] trans_probs = getTransProbMatrix();

		SparseVector ret = new SparseVector(ArrayUtils.copy(wcb.getDocWordCounts().rowIndexes()));
		double[] cents = ret.values();
		ArrayUtils.setAll(cents, 1f / cents.length);
		ArrayMath.randomWalk(trans_probs, cents, 10, 0.0000001, 0.85);
		ret.summation();
		return ret;

	}

	private double getKLD(SparseVector d, SparseVector q) {
		double ret = 0;
		for (int i = 0; i < q.size(); i++) {
			int w = q.indexAtLoc(i);
			double pr_w_in_q = q.probAlways(w);
			double pr_w_in_col = wcb.getCollWordCounts().probAlways(w);

			double cnt_w_in_d = d.valueAlways(w);
			double cnt_sum_in_d = d.sum();
			double pr_w_in_d = (cnt_w_in_d + dirichlet_prior * pr_w_in_col) / (cnt_sum_in_d + dirichlet_prior);
			pr_w_in_d = (1 - mixture_for_col) * pr_w_in_d + mixture_for_col * pr_w_in_col;

			if (pr_w_in_d > 0) {
				double div = pr_w_in_q * Math.log(pr_w_in_q / pr_w_in_d);
				ret += div;
			}
		}
		return ret;
	}

	public String getLog() {
		return logBuff.toString();
	}

	private double[][] getTransProbMatrix() {
		SparseMatrix docWordCountData = wcb.getDocWordCounts();
		int num_docs = docWordCountData.rowSize();

		double[][] trans_probs = ArrayUtils.matrix(num_docs);

		for (int i = 0; i < num_docs; i++) {
			SparseVector dwcs1 = docWordCountData.vectorAtRowLoc(i);

			for (int j = i + 1; j < num_docs; j++) {
				SparseVector dcwc2 = docWordCountData.vectorAtRowLoc(j);
				double forward_div_sum = getKLD(dwcs1, dcwc2);
				double backward_div_sum = getKLD(dcwc2, dwcs1);

				double pr_forward = Math.exp(-forward_div_sum);
				double pr_backward = Math.exp(-backward_div_sum);

				trans_probs[i][j] = pr_forward;
				trans_probs[j][i] = pr_backward;
			}
		}

		for (int i = 0; i < num_docs; i++) {
			double[] probs = trans_probs[i];
			int[] indexes = ArrayUtils.rankedIndexes(probs);
			for (int j = num_top_docs; j < num_docs; j++) {
				probs[indexes[j]] = 0;
			}
		}
		LA.transpose(trans_probs);
		ArrayMath.normalizeColumns(trans_probs);
		return trans_probs;
	}
}
