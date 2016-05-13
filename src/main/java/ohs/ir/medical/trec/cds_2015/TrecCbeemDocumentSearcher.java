package ohs.ir.medical.trec.cds_2015;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import ohs.io.TextFileWriter;
import ohs.ir.eval.RankComparator;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.medical.general.HyperParameter;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.SearcherUtils;
import ohs.ir.medical.general.WordCountBox;
import ohs.ir.medical.query.BaseQuery;
import ohs.math.ArrayMath;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.matrix.Vector;
import ohs.types.Indexer;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class TrecCbeemDocumentSearcher {

	private TextFileWriter logWriter;

	private IndexSearcher[] indexSearchers;

	private Analyzer analyzer;

	private int num_colls;

	private HyperParameter hyperParam;

	private StringBuffer logBuf;

	private Indexer<String> wordIndexer;

	private SparseVector[] collDocScores;

	private WordCountBox[] collWordCountBoxes;

	private DenseVector[] collDocPriors;

	private BaseQuery bq;

	private boolean makeLog = false;

	private AbbrQueryExpander abbrQueryExpander;

	public TrecCbeemDocumentSearcher(IndexSearcher[] indexSearchers, DenseVector[] docPriorData, HyperParameter hyperParameter,
			Analyzer analyzer, boolean makeLog) throws Exception {
		super();
		this.indexSearchers = indexSearchers;
		this.collDocPriors = docPriorData;
		this.hyperParam = hyperParameter;
		this.analyzer = analyzer;
		this.makeLog = makeLog;

		num_colls = indexSearchers.length;

		abbrQueryExpander = new AbbrQueryExpander(analyzer, MIRPath.ABBREVIATION_FILTERED_FILE);
	}

	private SparseVector[] computeRelevanceModels() throws IOException {
		double[] cnt_sum_in_each_coll = getCollWordCountSums();
		double cnt_sum_in_all_colls = ArrayMath.sum(cnt_sum_in_each_coll);

		int num_fb_docs = hyperParam.getNumFBDocs();
		double dirichlet_prior = hyperParam.getDirichletPrior();
		double mixture_for_all_colls = hyperParam.getMixtureForAllCollections();
		boolean useDocPrior = hyperParam.isUseDocPrior();

		SparseVector[] ret = new SparseVector[num_colls];

		for (int i = 0; i < num_colls; i++) {
			SparseVector docScores = collDocScores[i];
			docScores.sortByValue();

			SparseMatrix docWordCountBox = collWordCountBoxes[i].getDocWordCounts();
			SparseVector collWordCounts = collWordCountBoxes[i].getCollWordCounts();
			DenseVector docPriors = collDocPriors[i];

			SparseVector rm = new SparseVector(collWordCounts.size());

			for (int j = 0; j < collWordCounts.size(); j++) {
				int w = collWordCounts.indexAtLoc(j);

				double[] cnt_w_in_each_coll = new double[num_colls];
				double cnt_w_in_all_colls = 0;

				for (int k = 0; k < num_colls; k++) {
					cnt_w_in_each_coll[k] = collWordCountBoxes[k].getCollWordCounts().valueAlways(w);
					cnt_w_in_all_colls += cnt_w_in_each_coll[k];
				}

				double cnt_w_in_coll = cnt_w_in_each_coll[i];
				double cnt_sum_in_coll = cnt_sum_in_each_coll[i];

				double prob_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;
				double prob_w_in_all_colls = cnt_w_in_all_colls / cnt_sum_in_all_colls;

				for (int k = 0; k < docScores.size() && k < num_fb_docs; k++) {
					int docId = docScores.indexAtLoc(k);
					double doc_weight = docScores.valueAtLoc(k);

					SparseVector wordCounts = docWordCountBox.rowAlways(docId);
					double cnt_w_in_doc = wordCounts.valueAlways(w);
					double cnt_sum_in_doc = wordCounts.sum();
					double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
					double prob_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;

					// double prob_w_in_doc = (cnt_w_in_doc + dirichlet_prior *
					// prob_w_in_coll) / (cnt_sum_in_doc + dirichlet_prior);
					prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;
					prob_w_in_doc = (1 - mixture_for_all_colls) * prob_w_in_doc + mixture_for_all_colls * prob_w_in_all_colls;

					double doc_prior = useDocPrior ? cnt_sum_in_doc : 1;
					double prob_w_in_fb_model = doc_weight * prob_w_in_doc * doc_prior;

					if (prob_w_in_fb_model > 0) {
						rm.incrementAtLoc(j, w, prob_w_in_fb_model);
					}
				}
			}
			docScores.sortByIndex();
			rm.normalize();

			ret[i] = rm;
		}
		return ret;
	}

	private double[] getCollWordCountSums() {
		double[] ret = new double[num_colls];
		for (int i = 0; i < num_colls; i++) {
			ret[i] = collWordCountBoxes[i].getCountSum();
		}
		return ret;
	}

	private SparseVector score(int colId, SparseVector qLM) {
		double[] cnt_sum_in_each_coll = getCollWordCountSums();
		double cnt_sum_in_all_colls = ArrayMath.sum(cnt_sum_in_each_coll);

		SparseMatrix docWordCountBox = collWordCountBoxes[colId].getDocWordCounts();
		SparseVector collWordCounts = collWordCountBoxes[colId].getCollWordCounts();

		double dirichlet_prior = hyperParam.getDirichletPrior();
		double mixture_for_all_colls = hyperParam.getMixtureForAllCollections();

		SparseVector ret = new SparseVector(docWordCountBox.rowSize());

		for (int i = 0; i < qLM.size(); i++) {
			int w = qLM.indexAtLoc(i);
			double prob_w_in_query = qLM.valueAtLoc(i);

			double[] cnt_w_in_each_coll = new double[num_colls];
			double cnt_w_in_all_colls = 0;

			for (int j = 0; j < num_colls; j++) {
				cnt_w_in_each_coll[j] = collWordCountBoxes[j].getCollWordCounts().valueAlways(w);
				cnt_w_in_all_colls += cnt_w_in_each_coll[j];
			}

			double cnt_w_in_coll = cnt_w_in_each_coll[colId];
			double cnt_sum_in_coll = cnt_sum_in_each_coll[colId];

			double prob_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;
			double prob_w_in_all_colls = cnt_w_in_all_colls / cnt_sum_in_all_colls;

			for (int j = 0; j < docWordCountBox.rowSize(); j++) {
				int docId = docWordCountBox.indexAtLoc(j);
				SparseVector wordCounts = docWordCountBox.rowAtLoc(j);
				double cnt_w_in_doc = wordCounts.valueAlways(w);
				double cnt_sum_in_doc = wordCounts.sum();
				double mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				double prob_w_in_doc = cnt_w_in_doc / cnt_sum_in_doc;
				// double prob_w_in_doc = (cnt_w_in_doc + dirichlet_prior *
				// prob_w_in_coll) / (cnt_sum_in_doc + dirichlet_prior);

				prob_w_in_doc = (1 - mixture_for_coll) * prob_w_in_doc + mixture_for_coll * prob_w_in_coll;
				prob_w_in_doc = (1 - mixture_for_all_colls) * prob_w_in_doc + mixture_for_all_colls * prob_w_in_all_colls;

				if (prob_w_in_doc > 0) {
					double div = prob_w_in_query * Math.log(prob_w_in_query / prob_w_in_doc);
					ret.incrementAtLoc(j, docId, div);
				}
			}
		}

		for (int i = 0; i < ret.size(); i++) {
			double sum_div = ret.valueAtLoc(i);
			double approx_prob = Math.exp(-sum_div);
			ret.setAtLoc(i, approx_prob);
		}
		ret.summation();

		return ret;
	}

	private SparseVector search(int colId, BaseQuery bq, SparseVector docRels) throws Exception {
		wordIndexer = new Indexer<String>();
		logBuf = new StringBuffer();

		collDocScores = new SparseVector[num_colls];
		this.bq = bq;

		List<String> qWords = AnalyzerUtils.getWords(bq.getSearchText(), analyzer);

		StrCounter qWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);
		bq.setLuceneQuery(AnalyzerUtils.getQuery(qWords));

		SparseVector qLM = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
		qLM.normalize();

		// SparseVector expQueryModel = wikiQueryExpander.expand(wordIndexer, queryModel);
		BooleanQuery expSearchQuery = AnalyzerUtils.getQuery(VectorUtils.toCounter(qLM, wordIndexer));

		for (int i = 0; i < num_colls; i++) {
			Query searchQuery = bq.getLuceneQuery();
			// if (i == colId) {
			// searchQuery = expSearchQuery;
			// }
			collDocScores[i] = SearcherUtils.search(searchQuery, indexSearchers[i], hyperParam.getTopK());
		}

		setWordCountBoxes();

		SparseVector ret = search(colId, qLM, docRels);
		return ret;
	}

	public void search(int colId, List<BaseQuery> baseQueries, List<SparseVector> queryDocRels, String resultFileName, String logFileName)
			throws Exception {
		if (logFileName != null) {
			logWriter = new TextFileWriter(logFileName);
			makeLog = true;
		}

		TextFileWriter writer = new TextFileWriter(resultFileName);

		for (int i = 0; i < baseQueries.size(); i++) {
			BaseQuery baseQuery = baseQueries.get(i);
			SparseVector docRels = null;

			if (queryDocRels != null) {
				docRels = queryDocRels.get(i);
			}

			SparseVector docScores = search(colId, baseQuery, docRels);

			SearcherUtils.write(writer, baseQuery.getId(), docScores);

			if (logWriter != null) {
				logWriter.write(logBuf.toString().trim() + "\n\n");
			}
		}

		writer.close();
		if (logWriter != null) {
			logWriter.close();
		}
	}

	public SparseVector search(int colId, SparseVector qLM, SparseVector docRels) throws Exception {
		if (hyperParam.isUseDoubleScoring()) {
			for (int i = 0; i < num_colls; i++) {
				collDocScores[i] = score(i, qLM);
			}
		}

		SparseVector[] rms = computeRelevanceModels();

		double[] mixture_for_each_coll_rm = new double[num_colls];

		double score_in_target_coll = 0;
		double score_sum_except_target_coll = 0;

		for (int i = 0; i < num_colls; i++) {
			double coll_prior = 0;
			SparseVector docScores = collDocScores[i];
			docScores.sortByValue();

			double num_docs = 0;
			for (int j = 0; j < docScores.size() && j < hyperParam.getNumFBDocs(); j++) {
				coll_prior += docScores.valueAtLoc(j);
				num_docs++;
			}

			coll_prior /= num_docs;

			docScores.sortByIndex();

			mixture_for_each_coll_rm[i] = coll_prior;

			if (i == colId) {
				score_in_target_coll = coll_prior;
			} else {
				score_sum_except_target_coll += coll_prior;
			}
		}

		if (hyperParam.isSmoothCollectionMixtures()) {
			double avg = ArrayMath.mean(mixture_for_each_coll_rm);
			// mixture_for_each_coll_rm[targetId] += (0.5 * avg);
			mixture_for_each_coll_rm[colId] += (avg);
		}

		ArrayMath.normalize(mixture_for_each_coll_rm, mixture_for_each_coll_rm);

		SparseVector cbeem = VectorMath.addAfterScale(rms, mixture_for_each_coll_rm);
		cbeem.removeZeros();
		cbeem.keepTopN(hyperParam.getNumFBWords());
		cbeem.normalize();

		double[] mixture_for_each_qm = { 1 - hyperParam.getMixtureForFeedbackModel(), hyperParam.getMixtureForFeedbackModel() };
		ArrayMath.normalize(mixture_for_each_qm);

		SparseVector expQLM = VectorMath.addAfterScale(new Vector[] { qLM, cbeem }, mixture_for_each_qm);

		// SparseVector ret = scoreDocuments(colId, expQueryModel);

		BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(expQLM, wordIndexer));
		SparseVector ret = SearcherUtils.search(lbq, indexSearchers[colId], hyperParam.getTopK());
		ret.normalize();

		if (makeLog) {
			logBuf.append(bq.toString() + "\n");
			logBuf.append(String.format("QM1:\t%s\n", VectorUtils.toCounter(qLM, wordIndexer).toString()));
			logBuf.append(String.format("QM2:\t%s\n", VectorUtils.toCounter(expQLM, wordIndexer).toString()));

			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(4);

			for (int i = 0; i < rms.length; i++) {
				SparseVector rm = rms[i];
				double mixture = mixture_for_each_coll_rm[i];
				logBuf.append(

				String.format("RM%d (%s):\t%s\n", i + 1, nf.format(mixture), VectorUtils.toCounter(rm, wordIndexer).toString()));
			}

			logBuf.append(String.format("RMM:\t%s\n\n", VectorUtils.toCounter(cbeem, wordIndexer).toString()));

			if (docRels != null) {
				logBuf.append(RankComparator.compareRankings(collDocScores[colId], ret, docRels));
			}
			logBuf.append("\n");
		}

		return ret;
	}

	public void setMakeLog(boolean makeLog) {
		this.makeLog = makeLog;
	}

	private void setWordCountBoxes() throws Exception {
		collWordCountBoxes = new WordCountBox[num_colls];

		for (int i = 0; i < num_colls; i++) {
			SparseVector docScores = collDocScores[i];
			IndexReader indexReader = indexSearchers[i].getIndexReader();
			collWordCountBoxes[i] = WordCountBox.getWordCountBox(indexReader, docScores, wordIndexer);
		}
	}

}
