package ohs.ir.medical.trec.cds_2014;

import ohs.io.FileUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;

public class ExplicitSemanticModel {

	public static ExplicitSemanticModel read(Indexer<String> wordIndexer, SparseVector wordDocFreqs, int numDocs) throws Exception {
		System.out.println("read ESA.");

		Indexer<String> conceptIndexer = FileUtils.readStrIndexer(CDSPath.ICD10_CONCEPT_INDEXER_FILE);
		Indexer<String> categoryIndexer = FileUtils.readStrIndexer(CDSPath.ICD10_CATEGORY_INDEXER_FILE);

		SparseMatrix wordConceptWeights = SparseMatrix.read(CDSPath.ICD10_WORD_CONCEPT_MAP_FILE);
		SparseMatrix conceptCategoryWeights = SparseMatrix.read(CDSPath.ICD10_CONCEPT_CATEGORY_MAP_FILE);

		for (int i = 0; i < wordConceptWeights.rowSize(); i++) {
			wordConceptWeights.vectorAtRowLoc(i).sortByValue();
		}

		for (int i = 0; i < conceptCategoryWeights.rowSize(); i++) {
			conceptCategoryWeights.vectorAtRowLoc(i).sortByValue();
		}

		return new ExplicitSemanticModel(wordIndexer, conceptIndexer, categoryIndexer, wordDocFreqs, wordConceptWeights,
				conceptCategoryWeights, numDocs);
	}

	private Indexer<String> wordIndexer;

	private Indexer<String> conceptIndexer;

	private Indexer<String> categoryIndexer;

	private SparseVector wordDocFreqs;

	private SparseMatrix wordConceptWeights;

	private SparseMatrix conceptCategoryWeights;

	private int numDocs;

	private int numConceptsPerWord = 10;

	private int numConceptsPerDocument = 200;

	private int numCategoriesPerConcept = 20;

	public ExplicitSemanticModel(

	Indexer<String> wordIndexer, Indexer<String> conceptIndexer, Indexer<String> categoryIndexer,

	SparseVector wordDocFreqs, SparseMatrix wordConceptWeights, SparseMatrix conceptCategoryWeights, int numDocs) {

		this.wordIndexer = wordIndexer;
		this.conceptIndexer = conceptIndexer;
		this.categoryIndexer = categoryIndexer;
		this.wordDocFreqs = wordDocFreqs;
		this.wordConceptWeights = wordConceptWeights;
		this.conceptCategoryWeights = conceptCategoryWeights;
		this.numDocs = numDocs;
	}

	public double computeSimilarity(SparseVector input1, SparseVector input2) {
		SparseVector conceptWeights1 = getConceptWeights(input1);
		SparseVector conceptWeights2 = getConceptWeights(input2);
		double cosine = VectorMath.cosine(conceptWeights1, conceptWeights2, false);
		return cosine;
	}

	public Indexer<String> getCategoryIndexer() {
		return categoryIndexer;
	}

	public SparseVector getCategoryWeights(SparseVector conceptWeights) {
		Counter<Integer> weightSums = new Counter<Integer>();

		for (int i = 0; i < conceptWeights.size(); i++) {
			int conceptId = conceptWeights.indexAtLoc(i);
			SparseVector categoryWeights = conceptCategoryWeights.rowAlways(conceptId);

			if (categoryWeights == null) {
				continue;
			}

			for (int j = 0; j < categoryWeights.size(); j++) {
				int cat = categoryWeights.indexAtLoc(j);
				double weight = categoryWeights.valueAtLoc(j);
				weightSums.incrementCount(cat, weight);
			}
		}

		SparseVector ret = VectorUtils.toSparseVector(weightSums);
		ret.setDim(categoryIndexer.size());
		return ret;
	}

	public Indexer<String> getConceptIndexer() {
		return conceptIndexer;
	}

	public SparseVector getConceptWeights(SparseVector wordCounts) {
		Counter<Integer> weightSums = new Counter<Integer>();

		for (int i = 0; i < wordCounts.size(); i++) {
			int w = wordCounts.indexAtLoc(i);
			SparseVector conceptWeights = wordConceptWeights.rowAlways(w);

			if (conceptWeights == null) {
				continue;
			}

			for (int j = 0; j < conceptWeights.size() && j < numConceptsPerWord; j++) {
				int c = conceptWeights.indexAtLoc(j);
				double weight = conceptWeights.valueAtLoc(j);
				weightSums.incrementCount(c, weight);
			}
		}

		SparseVector ret = VectorUtils.toSparseVector(weightSums);
		ret.keepTopN(numConceptsPerDocument);
		ret.setDim(conceptIndexer.size());
		return ret;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public SparseVector getWordWeights(SparseVector wordCounts) {
		SparseVector ret = wordCounts.copy();
		double norm = 0;
		for (int i = 0; i < wordCounts.size(); i++) {
			int w = wordCounts.indexAtLoc(i);
			double tf = wordCounts.valueAtLoc(i);

			tf = Math.log(tf) + 1;

			double docFreq = wordDocFreqs.valueAlways(w);
			// double tf = 1 + (cnt == 0 ? 0 : Math.log(cnt));
			double idf = docFreq == 0 ? 0 : Math.log((numDocs + 1) / docFreq);
			double tfidf = tf * idf;
			norm += tfidf * tfidf;
			ret.setAtLoc(i, tfidf);
		}
		norm = Math.sqrt(norm);
		ret.scale(1f / norm);
		return ret;
	}
}
