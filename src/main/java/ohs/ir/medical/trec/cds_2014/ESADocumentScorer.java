package ohs.ir.medical.trec.cds_2014;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.ArrayMath;
import ohs.math.LA;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

public class ESADocumentScorer {
	private Indexer<String> wordIndexer;

	private MedicalEnglishAnalyzer analyzer;

	private IndexReader indexReader;

	private SparseVector collWordCounts;

	private SparseVector docFreqs;

	private ExplicitSemanticModel esm;

	private double[] esaWeights = new double[3];

	private double[] esaSimilarties = new double[3];

	public ESADocumentScorer(IndexReader indexReader, MedicalEnglishAnalyzer analyzer, VocabularyData vocData, ExplicitSemanticModel esm)
			throws Exception {
		this.indexReader = indexReader;
		this.analyzer = analyzer;
		this.esm = esm;

		collWordCounts = vocData.getCollectionWordCountData();
		docFreqs = vocData.getDocumentFrequencies();
		wordIndexer = vocData.getWordIndexer();

		esaWeights = new double[] { 1, 1, 1 };

		ArrayMath.normalize(esaWeights);
	}

	private Counter<String> getCounter(List<String> words) {
		Counter<String> ret = new Counter<String>();
		for (String word : words) {
			ret.incrementCount(word, 1);
		}
		return ret;
	}

	private SparseMatrix getDocumentWordCounts(SparseVector indexScores) throws Exception {
		CounterMap<Integer, Integer> docWordCounts = new CounterMap<Integer, Integer>();

		Set<Integer> toRemove = new TreeSet<Integer>();

		for (int j = 0; j < indexScores.size(); j++) {
			int indexId = indexScores.indexAtLoc(j);
			Terms termVector = indexReader.getTermVector(indexId, CommonFieldNames.CONTENT);

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

		SparseMatrix ret = VectorUtils.toSpasreMatrix(docWordCounts);
		ret.setRowDim(indexReader.maxDoc());
		ret.setColDim(wordIndexer.size());

		for (int i = 0; i < ret.rowSize(); i++) {
			ret.vectorAtRowLoc(i).setDim(wordIndexer.size());
		}

		return ret;
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
		SparseVector queryWordCounts = VectorUtils.toSparseVector(getCounter(queryWords), wordIndexer);

		SparseVector wordWeights4Query = esm.getWordWeights(queryWordCounts);
		SparseVector conceptWeights4Query = esm.getConceptWeights(queryWordCounts);
		SparseVector categoryWeights4Query = esm.getCategoryWeights(conceptWeights4Query);

		// System.out.println("[Query]");
		// System.out.printf("Word Weights:\t%s\n", VectorUtils.toCounter(wordWeights4Query, esm.getWordIndexer()));
		// System.out.printf("Concept Weights:\t%s\n", VectorUtils.toCounter(conceptWeights4Query, esm.getConceptIndexer()));
		// System.out.printf("Category Weights:\t%s\n", VectorUtils.toCounter(categoryWeights4Query, esm.getCategoryIndexer()));
		// System.out.println();

		SparseMatrix docWordCounts = getDocumentWordCounts(docScores);

		Counter<Integer> ret = new Counter<Integer>();

		for (int i = 0; i < docWordCounts.rowSize(); i++) {
			int docId = docWordCounts.indexAtRowLoc(i);
			SparseVector wordCounts = docWordCounts.rowAtLoc(i);
			SparseVector wordWeights4Document = esm.getWordWeights(wordCounts);
			SparseVector conceptWeights4Document = esm.getConceptWeights(wordCounts);
			SparseVector categoryWeights4Document = esm.getCategoryWeights(conceptWeights4Document);

			// System.out.printf("[KDocument %d]\n", i + 1);
			// System.out.printf("Word Weights:\t%s\n", VectorUtils.toCounter(wordWeights4Document, esm.getWordIndexer()));
			// System.out.printf("Concept Weights:\t%s\n", VectorUtils.toCounter(conceptWeights4Document, esm.getConceptIndexer()));
			// System.out.printf("Category Weights:\t%s\n", VectorUtils.toCounter(categoryWeights4Document, esm.getCategoryIndexer()));
			// System.out.println();

			double wordCosine = VectorMath.cosine(wordWeights4Query, wordWeights4Document, false);
			double conceptCosine = VectorMath.cosine(conceptWeights4Query, conceptWeights4Document, false);
			double categoryCosine = VectorMath.cosine(categoryWeights4Query, categoryWeights4Document, false);

			esaSimilarties[0] = wordCosine;
			esaSimilarties[1] = conceptCosine;
			esaSimilarties[2] = categoryCosine;

			// ArrayMath.normalize(esaSimilarties);

			double newScore = LA.dotProduct(esaWeights, esaSimilarties);

			ret.setCount(docId, newScore);
		}
		ret.normalize();
		return ret;
	}

	private SparseVector updateModel(SparseVector targetModel, SparseVector otherModel, double other_mixture) {
		Counter<Integer> ret = new Counter<Integer>();

		for (int i = 0; i < targetModel.size(); i++) {
			int w = targetModel.indexAtLoc(i);
			double prob = targetModel.probAtLoc(i);
			ret.incrementCount(w, (1 - other_mixture) * prob);
		}
		for (int i = 0; i < otherModel.size(); i++) {
			int w = otherModel.indexAtLoc(i);
			double prob = otherModel.probAtLoc(i);
			ret.incrementCount(w, other_mixture * prob);
		}
		ret.normalize();

		System.out.printf("Update Model:\t%s\n\n", VectorUtils.toCounter(ret, wordIndexer));
		return VectorUtils.toSparseVector(ret);
	}
}
