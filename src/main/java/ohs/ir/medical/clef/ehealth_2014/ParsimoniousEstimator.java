package ohs.ir.medical.clef.ehealth_2014;

import java.text.NumberFormat;

import ohs.math.ArrayMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Indexer;

/**
 * This class provides parsimonious language model (PLM) estimation methods.
 * 
 * PLM assigns high probabilities to topical terms but low probabilities to common terms using EM algorithm. It can be used to select a set
 * of terms and construct a smaller transition matrix.
 * 
 * 
 * 1 . Hiemstra, D., Robertson, S., and Zaragoza, H. 2004. Parsimonious language models for information retrieval. Proceedings of the 27th
 * annual international ACM SIGIR conference on Research and development in information retrieval, ACM, 178–185.
 * 
 * 2. Na, S.-H., Kang, I.-S., and Lee, J.-H. 2007. Parsimonious translation models for information retrieval. Information Processing &
 * Management 43, 1, 121–145.
 * 
 * 
 * @author Heung-Seon Oh
 * 
 */
public class ParsimoniousEstimator {

	public enum SelectType {
		Ratio, Size
	}

	public static void main(String args[]) throws Exception {
		System.out.println("process begins");

		System.out.println("probess ends.");
	}

	private int max_iter;

	private double epsilon;

	private double document_mixture;

	private Indexer<String> wordIndexer;

	private SparseVector collWordCounts;

	public ParsimoniousEstimator(Indexer<String> wordIndexer, SparseVector collWordCounts, int max_iter, double document_mixture) {
		this.wordIndexer = wordIndexer;
		this.collWordCounts = collWordCounts;
		this.max_iter = max_iter;
		this.document_mixture = document_mixture;
		epsilon = 0.000001;
	}

	/**
	 * Run EM to estimate a parsimonious language model
	 * 
	 * 
	 * @param term_count
	 * @return
	 */
	public SparseVector estimate(SparseVector term_count) {
		SparseVector ret = term_count.copy();
		ret.normalize();

		double log_likelihood = ArrayMath.sumLogs(ret.values());
		double old_log_likelihood = log_likelihood;

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(2);
		nf.setGroupingUsed(false);

		System.out.printf("0th iter:\t%s\n", VectorUtils.toCounter(ret, wordIndexer));

		for (int j = 0; j < max_iter; j++) {
			for (int k = 0; k < term_count.size(); k++) {
				int w = term_count.indexAtLoc(k);
				double count_w_in_doc = term_count.valueAtLoc(k);
				double prob_w_in_doc = ret.valueAtLoc(k);
				double prob_w_in_collection = collWordCounts.probAlways(w);
				double e = count_w_in_doc
						* ((document_mixture * prob_w_in_doc) / (document_mixture * prob_w_in_doc + (1 - document_mixture)
								* prob_w_in_collection));
				ret.setAtLoc(k, e);
			}
			ret.normalizeAfterSummation();

			log_likelihood = ArrayMath.sumLogs(ret.values());

			double diff = log_likelihood - old_log_likelihood;

			if (old_log_likelihood - log_likelihood < epsilon) {
				break;
			}
			old_log_likelihood = log_likelihood;
		}

		System.out.printf("%dth iter:\t%s\n", max_iter, VectorUtils.toCounter(ret, wordIndexer));

		return ret;
	}
}
