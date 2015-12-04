package ohs.ir.task.medical.trec.cds_2014;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.ui.Model;

import ohs.io.IOUtils;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;

public class QueryClassifier implements Serializable {

	private static final long serialVersionUID = -3273222430839071709L;

	public static Counter<String> analyze(String text, Analyzer analyzer) throws Exception {
		Counter<String> ret = new Counter<String>();

		String[] lines = text.split("[\n]+");

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			TokenStream ts = analyzer.tokenStream(IndexFieldName.CONTENT, line);
			CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
			ts.reset();

			while (ts.incrementToken()) {
				String word = attr.toString();
				ret.incrementCount(word, 1);
			}
			ts.end();
			ts.close();
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		MedicalEnglishAnalyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		VocabularyData vocData = VocabularyData.read(new File(CDSPath.VOCABULARY_DIR));

		QueryClassifier queryClassifier = QueryClassifier.read(vocData.getWordIndexer());

		List<CDSQuery> cdsQueries = CDSQuery.read(CDSPath.TEST_QUERY_FILE);

		double numCorrect = 0;

		for (int i = 0; i < cdsQueries.size(); i++) {
			CDSQuery cdsQuery = cdsQueries.get(i);
			String qId = cdsQuery.getId();

			// System.out.printf("%dth query: %s\n", i + 1, cdsQuery.getDescription());

			StringBuffer qBuff = new StringBuffer();
			qBuff.append(cdsQuery.getDescription());

			Counter<String> queryCounts = analyze(qBuff.toString(), analyzer);

			SparseVector scores = queryClassifier.score(VectorUtils.toSparseVector(queryCounts, queryClassifier.getFeatureIndexer()));

			Counter<String> typeScores = VectorUtils.toCounter(scores, queryClassifier.getLabelIndexer());

			String answer = cdsQuery.getType();
			String predict = typeScores.argMax();

			if (answer.equals(predict)) {
				numCorrect++;
			}

			System.out.printf("Ans: %s -> Pred:\t %s\n", answer, predict);
		}

		System.out.printf("accuracy:\t%s\n", numCorrect / cdsQueries.size());

		System.out.println("process ends.");
	}

	public static QueryClassifier read(Indexer<String> featureIndexer) throws Exception {
		Indexer<String> labelIndexer = IOUtils.readIndexer(CDSPath.QUERY_CLASSIFIER_TYPE_INDEXER_FILE);
		Model model = Model.load(new File(CDSPath.QUERY_CLASSIFIER_MODEL_FILE));
		return new QueryClassifier(model, labelIndexer, featureIndexer);
	}

	private Model model;

	private Indexer<String> labelIndexer;

	private Indexer<String> featureIndexer;

	public QueryClassifier(Model model, Indexer<String> labelIndexer, Indexer<String> featureIndexer) {
		this.model = model;
		this.labelIndexer = labelIndexer;
		this.featureIndexer = featureIndexer;
	}

	public Indexer<String> getFeatureIndexer() {
		return featureIndexer;
	}

	public Indexer<String> getLabelIndexer() {
		return labelIndexer;
	}

	public Counter<String> score(Counter<String> query) {
		return VectorUtils.toCounter(score(VectorUtils.toSparseVector(query, featureIndexer)), labelIndexer);
	}

	public List<SparseVector> score(List<SparseVector> queries) {
		List<SparseVector> ret = new ArrayList<SparseVector>();
		for (int i = 0; i < queries.size(); i++) {
			SparseVector query = queries.get(i);
			SparseVector label_score = score(query);
			label_score.setLabel(query.label());
			ret.add(label_score);
		}
		return ret;
	}

	public SparseVector score(SparseVector query) {
		query = query.copy();

		VectorMath.unitVector(query);

		Feature[] input = new Feature[query.size()];

		for (int i = 0; i < query.size(); i++) {
			int index = query.indexAtLoc(i) + 1;
			double value = query.valueAtLoc(i);
			assert index >= 0;
			input[i] = new FeatureNode(index + 1, value);
		}

		int[] labels = model.getLabels();
		double[] prob_estimates = new double[labels.length];

		// Linear.predictProbability(model, input, prob_estimates);

		Linear.predictValues(model, input, prob_estimates);

		SparseVector ret = new SparseVector(labels, prob_estimates, query.label());
		VectorMath.normalizeBySigmoid(ret);
		return ret;
	}

}
