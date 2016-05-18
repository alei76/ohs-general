package ohs.ir.medical.clef.ehealth_2016;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.ir.eval.PerformanceEvaluator;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.KLDivergenceScorer;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.RelevanceModelBuilder;
import ohs.ir.medical.general.SearcherUtils;
import ohs.ir.medical.general.WordCountBox;
import ohs.ir.medical.query.BaseQuery;
import ohs.ir.medical.query.QueryReader;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class Experiments {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		Experiments e = new Experiments();
		// e.searchByQLD();
		// e.searchByKLD();
		e.searchByKLDFB();
		e.searchByKldFbWordVecs01();
		e.searchByKldFbWordVecs02();

		System.out.println("process ends.");
	}

	private MedicalEnglishAnalyzer analyzer = MedicalEnglishAnalyzer.newAnalyzer();

	private String queryFileName = MIRPath.CLEF_EHEALTH_QUERY_2016_FILE;

	private String indexDirName = MIRPath.CLUEWEB_INDEX_DIR;

	private String resDirName = MIRPath.CLEF_EHEALTH_OUTPUT_RESULT_2016_DIR;

	private String docIdMapFileNames = MIRPath.CLUEWEB_DOC_ID_MAP_FIE;

	// private String[] logDirNames = MIRPath.LogDirNames;
	//
	// private String[] relFileNames = MIRPath.RelevanceFileNames;

	private IndexSearcher is = SearcherUtils.getIndexSearcher(indexDirName);

	private List<BaseQuery> bqs = QueryReader.readQueries(queryFileName);

	// private String[] docPriorFileNames = MIRPath.DocPriorFileNames;

	public Experiments() throws Exception {

	}

	public void computeAvgVector(String text, Set<String> stopwords, Word2VecModel model, double[] avgVec) {
		Counter<String> c = Generics.newCounter();

		for (String word : text.split("[\\p{Punct}\\s]+")) {
			word = word.toLowerCase().trim();

			if (stopwords.contains(word) || word.length() == 0) {
				continue;
			}
			c.incrementCount(word, 1);
		}

		ArrayUtils.setAll(avgVec, 0);

		int num_words = 0;

		for (String word : c.keySet()) {
			double[] vec = model.getVector(word);
			if (vec.length > 0) {
				int cnt = (int) c.getCount(word);
				ArrayMath.addAfterScale(vec, cnt, avgVec, 1, avgVec);
				num_words += cnt;
			}
		}

		if (num_words > 1) {
			ArrayMath.scale(avgVec, 1f / num_words, avgVec);
		}
	}

	public String expand(String text, Set<String> stopwords, Word2VecModel model, double[] q, SparseVector sims) {
		Counter<String> c = Generics.newCounter();

		for (String word : text.split("[\\p{Punct}\\s]+")) {
			word = word.toLowerCase().trim();

			if (stopwords.contains(word) || word.length() == 0) {
				continue;
			}
			c.incrementCount(word, 1);
		}

		computeAvgVector(text, stopwords, model, q);

		for (int i = 0; i < model.getVocab().size(); i++) {
			double cosine = ArrayMath.cosine(q, model.getVector(i));
			sims.setAtLoc(i, cosine);
		}

		Counter<String> ret = Generics.newCounter();

		sims.sortByValue();

		for (int i = 0; i < sims.size(); i++) {
			int w = sims.indexAtLoc(i);
			double cosine = sims.valueAtLoc(i);
			String word = model.getVocab().getWord(w);

			if (c.containsKey(word)) {
				continue;
			}

			if (cosine < 0.5) {
				break;
			}

			if (ret.size() == 5) {
				break;
			}

			ret.setCount(word, cosine);
		}

		sims.sortByIndex();

		return text + "\t\t" + StrUtils.join(" ", ret.keySet());
	}

	public void searchByKLD() throws Exception {
		System.out.println("search by KLD.");

		CounterMap<String, String> sr = PerformanceEvaluator.readSearchResults(resDirName + "qld.txt.gz");

		String outputFileName = resDirName + "kld.txt.gz";

		TextFileWriter writer = new TextFileWriter(outputFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			System.out.println(bq);

			SparseVector docScores = toSparseVector(sr.getCounter(bq.getId()));

			Indexer<String> wordIndexer = Generics.newIndexer();
			Counter<String> qwcs = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			SparseVector qlm = VectorUtils.toSparseVector(qwcs, wordIndexer, true);
			qlm.normalize();

			WordCountBox wcb = WordCountBox.getWordCountBox(is, docScores, wordIndexer);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.score(wcb, qlm);

			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();

	}

	public void searchByKLDFB() throws Exception {
		System.out.println("search by KLD FB.");

		CounterMap<String, String> sr = PerformanceEvaluator.readSearchResults(resDirName + "qld.txt.gz");

		String outputFileName = resDirName + "kld-fb.txt.gz";

		TextFileWriter writer = new TextFileWriter(outputFileName);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);

			Indexer<String> wordIndexer = Generics.newIndexer();
			Counter<String> queryWordCnts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			SparseVector qlm = VectorUtils.toSparseVector(queryWordCnts, wordIndexer, true);
			qlm.normalize();

			SparseVector eqlm = qlm.copy();

			SparseVector docScores = toSparseVector(sr.getCounter(bq.getId()));

			docScores.keepTopN(1000);

			WordCountBox wcb = WordCountBox.getWordCountBox(is, docScores, wordIndexer, CommonFieldNames.CONTENT);

			RelevanceModelBuilder rmb = new RelevanceModelBuilder();
			SparseVector rm = rmb.getRelevanceModel(wcb, docScores);
			// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3,
			// docScores);

			double rm_mixture = 0.5;

			eqlm = VectorMath.addAfterScale(qlm, 1 - rm_mixture, rm, rm_mixture);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.score(wcb, eqlm);

			System.out.println(bq);
			System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qlm, wordIndexer));
			System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(eqlm, wordIndexer));

			SearcherUtils.write(writer, bq.getId(), docScores);
		}

		writer.close();
	}

	public void searchByKldFbWordVecs01() throws Exception {
		System.out.println("search by KLD FB Word Vectors.");

		CounterMap<String, String> sr = PerformanceEvaluator.readSearchResults(resDirName + "qld.txt.gz");

		Set<String> stopwords = FileUtils.readStrSet(MIRPath.STOPWORD_INQUERY_FILE);

		Word2VecModel model = new Word2VecModel();
		model.readObject("../../data/medical_ir/wiki/wiki_medical_word2vec_model.ser.gz");

		double[] qv = new double[model.sizeOfVector()];
		double[] dv = new double[model.sizeOfVector()];

		// Word2VecSearcher searcher = new Word2VecSearcher(model, stopwords);

		String outputFileName = resDirName + "kld-fb_wv-01.txt";

		TextFileWriter writer = new TextFileWriter(outputFileName);

		for (int j = 0; j < bqs.size(); j++) {
			BaseQuery bq = bqs.get(j);

			Indexer<String> wordIndexer = Generics.newIndexer();
			Counter<String> queryWordCnts = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			computeAvgVector(bq.getSearchText(), stopwords, model, qv);

			SparseVector docScores = toSparseVector(sr.getCounter(bq.getId()));
			docScores.normalize();

			SparseVector docCosines = docScores.copy();

			for (int k = 0; k < docScores.size(); k++) {
				int d = docScores.indexAtLoc(k);

				String content = is.doc(d).get(CommonFieldNames.CONTENT);

				computeAvgVector(content, stopwords, model, dv);

				double cosine = ArrayMath.cosine(qv, dv);
				docCosines.set(k, cosine);
			}

			ArrayMath.multiply(docScores.values(), docCosines.values(), docScores.values());

			docScores.normalizeAfterSummation();

			SparseVector qlm = VectorUtils.toSparseVector(queryWordCnts, wordIndexer, true);
			qlm.normalize();

			SparseVector eqlm = qlm.copy();

			docScores.normalizeAfterSummation();

			WordCountBox wcb = WordCountBox.getWordCountBox(is, docScores, wordIndexer, CommonFieldNames.CONTENT);

			RelevanceModelBuilder rmb = new RelevanceModelBuilder();
			SparseVector rm = rmb.getRelevanceModel(wcb, docScores);
			// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3,
			// docScores);

			double rm_mixture = 0.5;

			eqlm = VectorMath.addAfterScale(qlm, 1 - rm_mixture, rm, rm_mixture);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			docScores = scorer.score(wcb, eqlm);

			System.out.println(bq);
			System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qlm, wordIndexer));
			System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(eqlm, wordIndexer));

			SearcherUtils.write(writer, bq.getId(), docScores);
		}

		writer.close();
	}

	public void searchByKldFbWordVecs02() throws Exception {
		System.out.println("search by KLD FB Word Vector Exp.");

		CounterMap<String, String> sr = PerformanceEvaluator.readSearchResults(resDirName + "qld.txt.gz");

		Set<String> stopwords = FileUtils.readStrSet(MIRPath.STOPWORD_INQUERY_FILE);

		Word2VecModel model = new Word2VecModel();
		model.readObject("../../data/medical_ir/wiki/wiki_medical_word2vec_model.ser.gz");

		SparseVector sims = new SparseVector(ArrayUtils.arrayRange(model.getVocab().size()));
		double[] q = new double[model.sizeOfVector()];

		String outputFileName = resDirName + "kld-fb_wv-02.txt";

		TextFileWriter writer = new TextFileWriter(outputFileName);

		for (int j = 0; j < bqs.size(); j++) {
			BaseQuery bq = bqs.get(j);

			Indexer<String> wordIndexer = Generics.newIndexer();
			Counter<String> qwcs1 = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);

			SparseVector qlm1 = VectorUtils.toSparseVector(qwcs1, wordIndexer, true);
			qlm1.normalize();

			String newSearchText = expand(bq.getSearchText(), stopwords, model, q, sims);

			Counter<String> qwcs2 = AnalyzerUtils.getWordCounts(newSearchText, analyzer);

			SparseVector qlm2 = VectorUtils.toSparseVector(qwcs2, wordIndexer, true);
			qlm2.normalize();

			SparseVector docScores = toSparseVector(sr.getCounter(bq.getId()));

			WordCountBox wcb = WordCountBox.getWordCountBox(is, docScores, wordIndexer, CommonFieldNames.CONTENT);

			KLDivergenceScorer scorer = new KLDivergenceScorer();
			// scorer.score(wcb, qlm1);

			RelevanceModelBuilder rmb = new RelevanceModelBuilder();
			SparseVector rm = rmb.getRelevanceModel(wcb, docScores);
			// SparseVector prm = rmb.getPositionalRelevanceModel(qLM, wcb3,
			// docScores);

			double mixture = 0.5;

			SparseVector qlm3 = VectorMath.addAfterScale(qlm2, 1 - mixture, rm, mixture);

			docScores = scorer.score(wcb, qlm3);

			docScores.normalizeAfterSummation();

			System.out.println(bq);
			System.out.printf("QM1:\t%s\n", VectorUtils.toCounter(qlm1, wordIndexer));
			System.out.printf("QM2:\t%s\n", VectorUtils.toCounter(qlm2, wordIndexer));
			System.out.printf("QM3:\t%s\n", VectorUtils.toCounter(qlm3, wordIndexer));

			SearcherUtils.write(writer, bq.getId(), docScores);
		}

		writer.close();
	}

	public void searchByQLD() throws Exception {
		System.out.println("search by QLD.");

		FileUtils.deleteFilesUnder(resDirName);

		String outFileName = resDirName + "qld.txt.gz";

		TextFileWriter writer = new TextFileWriter(outFileName);

		for (int j = 0; j < bqs.size(); j++) {
			BaseQuery bq = bqs.get(j);

			BooleanQuery lbq = AnalyzerUtils.getQuery(AnalyzerUtils.getWords(bq.getSearchText(), analyzer));

			SparseVector docScores = SearcherUtils.search(lbq, is, 5000);
			SearcherUtils.write(writer, bq.getId(), docScores);
		}
		writer.close();
	}

	private SparseVector toSparseVector(Counter<String> c) {
		SparseVector ret = new SparseVector(c.size());
		int loc = 0;
		for (Entry<String, Double> e : c.entrySet()) {
			ret.incrementAtLoc(loc, Integer.parseInt(e.getKey()), e.getValue());
			loc++;
		}
		ret.sortByIndex();
		return ret;
	}

}
