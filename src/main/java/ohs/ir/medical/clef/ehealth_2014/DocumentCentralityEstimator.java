package ohs.ir.medical.clef.ehealth_2014;

import java.text.NumberFormat;
import java.util.List;
import java.util.TreeSet;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;

/**
 * This class implements centralities of categories.
 * 
 * The standard centralites are computed by PageRank algorithms where a graph
 * over categories are constructed.
 * 
 * 
 * 
 * 
 * 1. Kurland, O. and Lee, L. 2005. PageRank without hyperlinks: structural
 * re-ranking using links induced by language models. Proceedings of the 28th
 * annual international ACM SIGIR conference on Research and development in
 * information retrieval, 306–313.
 * 
 * 
 * 2. Strube, M. and Ponzetto, S.P. 2006. WikiRelate! computing semantic
 * relatedness using wikipedia. proceedings of the 21st national conference on
 * Artificial intelligence - Volume 2, AAAI Press, 1419–1424.
 * 
 * 
 * @author Heung-Seon Oh
 * 
 * 
 */
public class DocumentCentralityEstimator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	private StringBuffer logBuff;

	private double dirichlet_prior = 1500;

	private double mixture_for_col = 0;

	private SparseVector collWordCounts;

	private int num_top_docs = 10;

	public DocumentCentralityEstimator(SparseVector collWordCounts) {
		this.collWordCounts = collWordCounts;
	}

	private double computeKLDivergence(SparseVector d, SparseVector q) {
		double ret = 0;
		for (int i = 0; i < q.size(); i++) {
			int w = q.indexAtLoc(i);
			double prob_w_in_q = q.probAlways(w);
			double prob_w_in_col = collWordCounts.probAlways(w);

			double cnt_w_in_d = d.valueAlways(w);
			double cnt_sum_in_d = d.sum();
			double prob_w_in_d = (cnt_w_in_d + dirichlet_prior * prob_w_in_col) / (cnt_sum_in_d + dirichlet_prior);
			prob_w_in_d = (1 - mixture_for_col) * prob_w_in_d + mixture_for_col * prob_w_in_col;

			if (prob_w_in_d > 0) {
				double div = prob_w_in_q * Math.log(prob_w_in_q / prob_w_in_d);
				ret += div;
			}
		}
		return ret;
	}

	private double[][] computeSimilarityMatrix(Indexer<Integer> docIndexer, SparseMatrix docWordCounts) {
		int num_docs = docIndexer.size();
		double[][] ret = ArrayUtils.matrix(num_docs, 0);

		IntCounterMap temp1 = new IntCounterMap();
		IntCounterMap temp2 = new IntCounterMap();

		for (int i = 0; i < num_docs; i++) {
			int docId1 = docIndexer.getObject(i);
			SparseVector dwc1 = docWordCounts.vectorAtRowLoc(i);

			SparseVector sv = new SparseVector(num_docs);

			for (int j = i + 1; j < num_docs; j++) {
				int docId2 = docIndexer.getObject(j);
				SparseVector dwc2 = docWordCounts.vectorAtRowLoc(j);

				double forward_div_sum = computeKLDivergence(dwc1, dwc2);
				double backward_div_sum = computeKLDivergence(dwc2, dwc1);

				double forward_prob = Math.exp(-forward_div_sum);
				double backward_prob = Math.exp(-backward_div_sum);

				temp1.setCount(i, j, forward_prob);
				temp2.setCount(j, i, backward_prob);
			}
		}

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(5);

		for (int docId1 : new TreeSet<Integer>(temp1.keySet())) {
			Counter<Integer> docScores = temp1.getCounter(docId1);
			List<Integer> docIds = docScores.getSortedKeys();

			for (int i = 0; i < docIds.size() && i < num_top_docs; i++) {
				int docId2 = docIds.get(i);
				double score = docScores.getCount(docId2);
				ret[docId1][docId2] = score;
			}
		}

		for (int docId1 : temp2.keySet()) {
			Counter<Integer> docScores = temp2.getCounter(docId1);
			List<Integer> docIds = docScores.getSortedKeys();

			for (int i = 0; i < docIds.size() && i < num_top_docs; i++) {
				int docId2 = docIds.get(i);
				double score = docScores.getCount(docId2);
				ret[docId1][docId2] = score;
			}
		}

		// System.out.println(ArrayUtils.toString(ret, num_top_docs,
		// num_top_docs, true, nf));
		// System.out.println();
		//
		// ArrayMath.normalizeColumns(ret);
		return ret;
	}

	public SparseVector estimate(SparseMatrix docWordCounts) {
		Indexer<Integer> docIndexer = new Indexer<Integer>();

		for (int docId : docWordCounts.rowIndexes()) {
			docIndexer.add(docId);
		}

		double[][] transMatrix = computeSimilarityMatrix(docIndexer, docWordCounts);
		double[] centralities = ArrayMath.doRandomWalkOut(transMatrix, 100, 0.000001, 0.85);
		int[] indexes = new int[docIndexer.size()];

		for (int i = 0; i < docIndexer.size(); i++) {
			int docId = docIndexer.getObject(i);
			indexes[i] = docId;
		}

		SparseVector ret = new SparseVector(indexes, centralities, -1);
		ret.sortByIndex();
		ret.summation();
		return ret;

	}

	public String getLog() {
		return logBuff.toString();
	}
}
