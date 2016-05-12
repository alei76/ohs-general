package ohs.ir.medical.general;

import java.text.NumberFormat;

import ohs.math.ArrayMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;

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
public class ParsimoniousLanguageModelEstimator {

	public static void main(String args[]) throws Exception {
		System.out.println("process begins");

		System.out.println("probess ends.");
	}

	WordCountBox wcb;

	private int max_iters = 100;

	private double epsilon = 0.000001;

	private double coll_mixture = 0.5;

	private boolean printLogs;

	public ParsimoniousLanguageModelEstimator(WordCountBox wcb) {
		this.wcb = wcb;
	}

	public void estimate() {

		SparseMatrix docWordCounts = wcb.getDocWordCounts();

		for (int i = 0; i < docWordCounts.rowSize(); i++) {
			SparseVector wordCnts = docWordCounts.vectorAtRowLoc(i);
			SparseVector wordProbs = wordCnts.copy();

			estimate(wordCnts);
		}

	}

	public SparseVector estimate(SparseVector wordCnts) {
		SparseVector plm = wordCnts.copy();
		plm.normalize();

		double log_likelihood = ArrayMath.sumLogProb(plm.values());
		double old_log_likelihood = log_likelihood;

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(2);
		nf.setGroupingUsed(false);

		wcb.getDocWordCounts();

		System.out.println("<=== start ===>");
		System.out.println(VectorUtils.toCounter(plm, wcb.getWordIndexer()));

		for (int i = 0; i < max_iters; i++) {
			SparseVector oldPlm = plm.copy();

			for (int j = 0; j < wordCnts.size(); j++) {
				int w = wordCnts.indexAtLoc(j);
				double cnt_w_in_doc = wordCnts.valueAtLoc(j);
				double pr_w_in_doc = plm.valueAtLoc(j);
				double pr_w_in_coll = wcb.getCollWordCounts().valueAlways(w) / wcb.getCountSum();
				double e = cnt_w_in_doc * ((coll_mixture * pr_w_in_doc) / (coll_mixture * pr_w_in_doc + (1 - coll_mixture) * pr_w_in_coll));
				plm.setAtLoc(j, e);
			}
			plm.normalizeAfterSummation();

			Counter<String> diff = new Counter<>();

			for (int j = 0; j < plm.size(); j++) {
				int w = plm.indexAtLoc(j);
				double pr1 = plm.valueAtLoc(j);
				double pr2 = oldPlm.valueAtLoc(j);
				diff.setCount(wcb.getWordIndexer().getObject(w), pr1 - pr2);
			}

			// System.out.println(VectorUtils.toCounter(plm, wcb.getWordIndexer()));
			System.out.println(diff);

			// log_likelihood = ArrayMath.sumLogs(plm.values());

			log_likelihood = ArrayMath.jensenShannonDivergence(plm.values(), oldPlm.values());

			if (old_log_likelihood - log_likelihood < epsilon) {
				break;
			}

			old_log_likelihood = log_likelihood;
		}
		return plm;
	}

	public void setPrintLogs(boolean printLogs) {
		this.printLogs = printLogs;
	}

}
