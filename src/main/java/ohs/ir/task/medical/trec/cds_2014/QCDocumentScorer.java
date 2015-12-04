package ohs.ir.task.medical.trec.cds_2014;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.ArrayMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

public class QCDocumentScorer {

	private Indexer<String> wordIndexer;

	private MedicalEnglishAnalyzer analyzer;

	private IndexReader indexReader;

	private SparseVector docFreqData;

	private QueryClassifier queryClassifier;

	public QCDocumentScorer(IndexReader indexReader, MedicalEnglishAnalyzer analyzer, VocabularyData vocData, QueryClassifier queryClassifier)
			throws Exception {
		this.indexReader = indexReader;
		this.analyzer = analyzer;

		docFreqData = vocData.getDocumentFrequencies();
		wordIndexer = vocData.getWordIndexer();

		this.queryClassifier = queryClassifier;
	}

	private Counter<String> getCounter(List<String> words) {
		Counter<String> ret = new Counter<String>();
		for (String word : words) {
			ret.incrementCount(word, 1);
		}
		return ret;
	}

	public SparseMatrix getDocumentWordCounts(SparseVector indexScores) throws Exception {

		CounterMap<Integer, Integer> docWordCounts = new CounterMap<Integer, Integer>();

		Set<Integer> toRemove = new TreeSet<Integer>();

		for (int j = 0; j < indexScores.size(); j++) {
			int indexId = indexScores.indexAtLoc(j);
			Terms termVector = indexReader.getTermVector(indexId, IndexFieldName.CONTENT);

			if (termVector == null) {
				toRemove.add(indexId);
				continue;
			}

			TermsEnum reuse = null;
			TermsEnum iterator = termVector.iterator();
			BytesRef ref = null;
			DocsAndPositionsEnum docsAndPositions = null;
			Counter<Integer> counter = new Counter<Integer>();

			while ((ref = iterator.next()) != null) {
				docsAndPositions = iterator.docsAndPositions(null, docsAndPositions);
				if (docsAndPositions.nextDoc() != 0) {
					throw new AssertionError();
				}
				String word = ref.utf8ToString();
				int w = wordIndexer.indexOf(word);

				if (w < 0) {
					continue;
				}

				int freq = docsAndPositions.freq();

				counter.incrementCount(w, freq);

				// for (int j = 0; j < freq; ++j) {
				// final int position = docsAndPositions.nextPosition();
				// }
			}

			docWordCounts.setCounter(indexId, counter);
		}

		indexScores.prune(toRemove);

		SparseMatrix sm = VectorUtils.toSpasreMatrix(docWordCounts, indexReader.maxDoc(), wordIndexer.size(), -1);
		return sm;
	}

	private SparseVector getModel(Counter<String> wordCounts) {
		SparseVector ret = VectorUtils.toSparseVector(wordCounts, wordIndexer);
		ret.normalize();
		System.out.printf("Model:\t%s\n", VectorUtils.toCounter(ret, wordIndexer));
		return ret;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public Counter<Integer> score(List<String> queryWords, SparseVector docScores) throws Exception {
		SparseVector queryModel = getModel(getCounter(queryWords));

		SparseVector queryTypeScores = queryClassifier.score(queryModel);

		System.out.println(VectorUtils.toCounter(queryTypeScores, queryClassifier.getLabelIndexer()));

		SparseMatrix docWordCountsData = getDocumentWordCounts(docScores);

		List<SparseVector> docTypeScoresData = new ArrayList<SparseVector>();

		Counter<Integer> ret = new Counter<Integer>();

		for (int i = 0; i < docWordCountsData.rowSize(); i++) {
			SparseVector docWordCounts = docWordCountsData.vectorAtRowLoc(i);
			SparseVector docTypeScores = queryClassifier.score(docWordCounts);
			docTypeScoresData.add(docTypeScores);

			int docId = docWordCountsData.indexAtRowLoc(i);
			double prevScore = docScores.valueAlways(docId);
			double cosine = ArrayMath.cosine(queryTypeScores.values(), docTypeScores.values());
			double newScore = prevScore * cosine;
			ret.incrementCount(docId, newScore);
		}
		ret.normalize();
		return ret;
	}

}
