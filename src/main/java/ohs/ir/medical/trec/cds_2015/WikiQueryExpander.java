package ohs.ir.medical.trec.cds_2015;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.medical.general.RelevanceModelBuilder;
import ohs.ir.medical.general.SearcherUtils;
import ohs.ir.medical.general.WordCountBox;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class WikiQueryExpander {

	// public static void main(String[] args) throws Exception {
	// System.out.println("process begins.");
	//
	// IndexSearcher trecIndexSearcher = SearcherUtils.getIndexSearcher(MIRPath.TREC_CDS_INDEX_DIR);
	// IndexSearcher wikiIndexSearcher = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
	// Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();
	//
	// List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2014_FILE);
	//
	// String resultFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "wiki.txt";
	// String logFileName = MIRPath.TREC_CDS_OUTPUT_RESULT_2015_DIR + "wiki_log.txt";
	//
	// StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(MIRPath.TREC_CDS_DOCUMENT_ID_MAP_FILE);
	// StrCounterMap relevanceData = RelevanceReader.readTrecCdsRelevances(MIRPath.TREC_CDS_RELEVANCE_JUDGE_2014_FILE);
	//
	// WikiQueryExpander expander = new WikiQueryExpander(wikiIndexSearcher, analyzer, true, 2000, 0.5, 20, 50);
	// // WikiQueryExpander ex = new WikiQueryExpander(wikiIndexSearcher, analyzer, true, 2000, 0.5, 20, 100);
	//
	// TextFileWriter writer = new TextFileWriter(resultFileName);
	// TextFileWriter logWriter = new TextFileWriter(logFileName);
	//
	// for (int i = 0; i < bqs.size(); i++) {
	// BaseQuery bq = bqs.get(i);
	// Indexer<String> wordIndexer = new Indexer<String>();
	//
	// Counter<String> qWordCounts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);
	// SparseVector queryModel = VectorUtils.toSparseVector(qWordCounts, wordIndexer, true);
	// queryModel.normalize();
	//
	// BooleanQuery lbq = AnalyzerUtils.getQuery(VectorUtils.toCounter(queryModel, wordIndexer));
	// SparseVector docScores = SearcherUtils.search(lbq, trecIndexSearcher, 1000);
	// logWriter.write(expander.getLogBuffer().toString() + "\n\n");
	// ResultWriter.write(writer, bq.getId(), docScores);
	// }
	//
	// writer.close();
	// logWriter.close();
	//
	// Experiments.evalute();
	//
	// System.out.println("process ends.");
	// }

	private IndexSearcher wikiIndexSearcher;

	private Analyzer analyzer;

	private TextFileWriter logWriter;

	private Indexer<String> wordIndexer;

	private double dirichlet_prior;

	private double mixture_for_rm;

	private int num_exp_concepts;

	private WordCountBox wcb;

	private TextFileWriter writer;

	private StringBuffer logBuf;

	private boolean makeLog;

	private SparseVector queryModel;

	private int num_top_concepts;

	public WikiQueryExpander(IndexSearcher wikiIndexSearcher, Analyzer analyzer) {
		this(wikiIndexSearcher, analyzer, false, 2000, 0.5, 5, 20);
	}

	public WikiQueryExpander(IndexSearcher wikiIndexSearcher, Analyzer analyzer, boolean makeLog, double dirichlet_prior,
			double mixture_for_rm, int num_top_concepts, int num_exp_concepts) {
		super();
		this.wikiIndexSearcher = wikiIndexSearcher;
		this.analyzer = analyzer;
		this.makeLog = makeLog;
		this.dirichlet_prior = dirichlet_prior;
		this.mixture_for_rm = mixture_for_rm;
		this.num_exp_concepts = num_exp_concepts;
		this.num_top_concepts = num_top_concepts;
	}

	public SparseVector expand(Indexer<String> wordIndexer, SparseVector queryModel) throws Exception {
		logBuf = new StringBuffer();

		BooleanQuery searchQuery1 = AnalyzerUtils.getQuery(VectorUtils.toCounter(queryModel, wordIndexer));

		SparseVector conceptScores1 = SearcherUtils.search(searchQuery1, wikiIndexSearcher, 50);
		Counter<Integer> c = new Counter<Integer>();

		for (int i = 0; i < conceptScores1.size(); i++) {
			int cid = conceptScores1.indexAtLoc(i);
			double score = conceptScores1.valueAtLoc(i);
			Document article = wikiIndexSearcher.doc(cid);
			String concept = article.get(CommonFieldNames.TITLE);
			String categories = article.get(CommonFieldNames.CATEGORY);

			if (concept.startsWith("List of") || concept.startsWith("Wikipedia:")) {
				continue;
			}
			c.setCount(cid, score);
		}

		conceptScores1 = VectorUtils.toSparseVector(c);
		conceptScores1.keepTopN(num_top_concepts);

		WordCountBox wcb = WordCountBox.getWordCountBox(wikiIndexSearcher.getIndexReader(), conceptScores1, wordIndexer);

		// conceptScores = KLDivergenceScorer.scoreDocuments(wcb, queryModel);

		RelevanceModelBuilder rmb = new RelevanceModelBuilder(num_top_concepts, num_exp_concepts, dirichlet_prior);
		SparseVector rm = rmb.getRelevanceModel(wcb, conceptScores1);

		SparseVector ret = VectorMath.addAfterScale(queryModel, rm, 1 - mixture_for_rm, mixture_for_rm);

		// KLDivergenceScorer kldScorer = new KLDivergenceScorer();
		// SparseVector conceptScores2 = kldScorer.scoreDocuments(wcb, expQueryModel);

		// BidMap<Integer, String> conceptMap = new BidMap<Integer, String>();
		// ListMap<String, String> categoryMap = new ListMap<String, String>();
		//
		// for (int i = 0; i < conceptScores2.size(); i++) {
		// int cid = conceptScores2.indexAtLoc(i);
		// double score2 = conceptScores2.valueAtLoc(i);
		// double score1 = conceptScores1.valueAlways(cid);
		//
		// KDocument article = wikiIndexSearcher.doc(cid);
		// String concept = article.get(CommonFieldNames.TITLE);
		// String categories = article.get(CommonFieldNames.CATEGORY);
		// conceptMap.put(cid, concept);
		//
		// for (String cat : categories.split("\n")) {
		// categoryMap.put(concept, cat);
		// }
		// }
		//
		// List<String> concepts = new ArrayList<String>();
		//
		// StringBuffer qb2 = new StringBuffer();
		// qb2.append(qb1.toString() + "\n\n");
		//
		// conceptScores2.sortByValue();
		//
		// for (int i = 0; i < conceptScores2.size() && concepts.size() < num_exp_concepts; i++) {
		// int cid = conceptScores2.indexAtLoc(i);
		// String concept = conceptMap.getValue(cid);
		// if (concept.startsWith("List of") || concept.startsWith("Wikipedia:")) {
		// continue;
		// }
		// concepts.add(concept);
		// //
		// // qb2.append(concept);
		// // qb2.append("\n");
		// }
		//
		// relevanceModel.sortByValue();
		//
		// List<String> words = new ArrayList<String>();
		//
		// for (int i = 0; i < relevanceModel.size() && words.size() < num_exp_concepts; i++) {
		// int w = relevanceModel.indexAtLoc(i);
		// String word = wordIndexer.getObject(w);
		// if (word.startsWith("#")) {
		// continue;
		// }
		// words.add(word);
		// }
		//
		// qb2.append(StrUtils.join("\n", words));
		//
		// Counter<String> qwcs2 = AnalyzerUtils.getWordCounts(qb2.toString(), analyzer);

		if (makeLog) {
			// logBuf.append(String.format("ID:\t%s\n", bq.getId()));
			logBuf.append(String.format("QM1:\t%s\n", VectorUtils.toCounter(queryModel, wordIndexer)));
			logBuf.append(String.format("QM2:\t%s\n", VectorUtils.toCounter(ret, wordIndexer)));
			logBuf.append(String.format("RM:\t%s", VectorUtils.toCounter(rm, wordIndexer)));
			// logBuf.append("Concepts:\n");
			//
			// SparseVector ranking1 = conceptScores1.ranking();
			// SparseVector ranking2 = conceptScores2.ranking();
			// ranking2.sortByValue(false);
			//
			// for (int j = 0; j < ranking2.size() && j < 20; j++) {
			// int cid = ranking2.indexAtLoc(j);
			// String concept = conceptMap.getValue(cid);
			//
			// int rank1 = (int) ranking1.valueAlways(cid);
			// int rank2 = (int) ranking2.valueAtLoc(j);
			//
			// logBuf.append(String.format("%d:\t%s\t%d\t%d", j + 1, concept, rank1, rank2));
			// if (j != ranking2.size() - 1) {
			// logBuf.append("\n");
			// }
			// }
			// logBuf.append("Words:\n");
		}
		return ret;
	}

	public StringBuffer getLogBuffer() {
		return logBuf;
	}

	public SparseVector getQueryModel() {
		return queryModel;
	}
}
