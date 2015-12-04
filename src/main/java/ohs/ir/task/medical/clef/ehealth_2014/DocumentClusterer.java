package ohs.ir.task.medical.clef.ehealth_2014;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ohs.math.ArrayMath;
import ohs.math.VectorMath;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.SetMap;

public class DocumentClusterer {

	private SparseVector collWordCounts;

	private SparseVector docFreqs;

	private SparseMatrix docWordCounts;

	private SparseVector docScores;

	private int num_docs_in_collection;

	private int top_k;

	private SetMap<Integer, Integer> clusterDocMap;

	private List<SparseVector> clusterWordCountData;

	private Map<Integer, Integer> docClusterMap;

	private double merge_threshold;

	private Indexer<String> wordIndexer;

	public DocumentClusterer(SparseVector docScores, SparseMatrix docWordCounts, SparseVector collWordCounts, SparseVector docFreqs,
			int doc_size_in_collection, int top_k, double merge_threshold, Indexer<String> wordIndexer) {
		this.docScores = docScores;
		this.collWordCounts = collWordCounts;
		this.docWordCounts = docWordCounts;
		this.docFreqs = docFreqs;
		this.num_docs_in_collection = doc_size_in_collection;
		this.top_k = top_k;
		this.merge_threshold = merge_threshold;
		this.wordIndexer = wordIndexer;
	}

	public double computeCosineSimilarity(SparseVector doc1, SparseVector doc2) {
		double ret = 0;
		double dirichlet_prior = 1500;

		double norm1 = 0;
		double norm2 = 0;
		double dotProduct = 0;

		int i = 0, j = 0;
		while (i < doc1.size() && j < doc2.size()) {
			int w = doc1.indexAtLoc(i);
			int t = doc2.indexAtLoc(j);

			double count_w_in_doc1 = doc1.valueAtLoc(i);
			double count_w_in_doc2 = doc2.valueAtLoc(j);

			double count_sum_in_doc1 = doc1.sum();
			double count_sum_in_doc2 = doc2.sum();

			if (w == t) {
				double prob_w_in_collection = collWordCounts.probAlways(w);

				double prob_w_in_doc1 = (count_w_in_doc1 + dirichlet_prior * prob_w_in_collection) / (count_sum_in_doc1 + dirichlet_prior);
				double prob_w_in_doc2 = (count_w_in_doc2 + dirichlet_prior * prob_w_in_collection) / (count_sum_in_doc2 + dirichlet_prior);

				dotProduct += (prob_w_in_doc1 * prob_w_in_doc2);
				norm1 += prob_w_in_doc1 * prob_w_in_doc1;
				norm2 += prob_w_in_doc2 * prob_w_in_doc2;

				i++;
				j++;
			} else if (w > t) {
				double prob_t_in_collection = collWordCounts.probAlways(t);
				double prob_w_in_doc2 = (count_w_in_doc2 + dirichlet_prior * prob_t_in_collection) / (count_sum_in_doc2 + dirichlet_prior);

				norm2 += prob_w_in_doc2 * prob_w_in_doc2;
				j++;
			} else if (w < t) {
				double prob_w_in_collection = collWordCounts.probAlways(w);
				double prob_w_in_doc1 = (count_w_in_doc1 + dirichlet_prior * prob_w_in_collection) / (count_sum_in_doc1 + dirichlet_prior);

				norm1 += prob_w_in_doc1 * prob_w_in_doc1;
				i++;
			}
		}

		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);
		double cosine = ArrayMath.cosine(dotProduct, norm1, norm2);
		return cosine;
	}

	public double computeCosineSimilarity2(SparseVector doc1, SparseVector doc2) {
		double ret = 0;
		double norm1 = 0;
		double norm2 = 0;
		double dotProduct = 0;

		// int termId = x.indexAtLoc(j);
		// double tf = x.valueAtLoc(j);
		//
		// if (tf > 0) {
		// tf = Math.log(tf) + 1;
		// }
		//
		// double docFreq = term_docFreq.value(termId);
		// double numDocs = xs.size();
		// // double tf = 1 + (count == 0 ? 0 : Math.log(count));
		// double idf = docFreq == 0 ? 0 : Math.log((numDocs + 1) / docFreq);
		// double tfidf = tf * idf;
		// x.setAtLoc(j, tfidf);
		// norm += tfidf * tfidf;

		int i = 0, j = 0;
		while (i < doc1.size() && j < doc2.size()) {
			int w = doc1.indexAtLoc(i);
			int t = doc2.indexAtLoc(j);

			double count_w_in_doc1 = doc1.valueAtLoc(i);
			double count_t_in_doc2 = doc2.valueAtLoc(j);

			double doc_freq_w = docFreqs.valueAlways(w);
			double doc_freq_t = docFreqs.valueAlways(t);

			if (w == t) {
				double tf_w = 1 + (count_w_in_doc1 == 0 ? 0 : Math.log(count_w_in_doc1));
				double tf_t = 1 + (count_t_in_doc2 == 0 ? 0 : Math.log(count_t_in_doc2));
				double idf = doc_freq_w == 0 ? 0 : Math.log((num_docs_in_collection + 1) / doc_freq_w);
				double tfidf_w = tf_w * idf;
				double tfidf_t = tf_t * idf;

				dotProduct += (tfidf_w * tfidf_t);
				norm1 += tfidf_w * tfidf_w;
				norm2 += tfidf_t * tfidf_t;

				i++;
				j++;
			} else if (w > t) {
				double tf_t = 1 + (count_t_in_doc2 == 0 ? 0 : Math.log(count_t_in_doc2));
				double idf_t = doc_freq_t == 0 ? 0 : Math.log((num_docs_in_collection + 1) / doc_freq_t);
				double tfidf_t = tf_t * idf_t;
				norm2 += tfidf_t * tfidf_t;
				j++;
			} else if (w < t) {
				double tf_w = 1 + (count_w_in_doc1 == 0 ? 0 : Math.log(count_w_in_doc1));
				double idf_w = doc_freq_w == 0 ? 0 : Math.log((num_docs_in_collection + 1) / doc_freq_w);
				double tfidf_w = tf_w * idf_w;
				norm1 += tfidf_w * tfidf_w;
				i++;
			}
		}

		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);
		double cosine = ArrayMath.cosine(dotProduct, norm1, norm2);
		return cosine;
	}

	public void doClustering() {
		performHAC();
	}

	public SetMap<Integer, Integer> getClusterDocumentMap() {
		return clusterDocMap;
	}

	public List<SparseVector> getClusterWordCountData() {
		return clusterWordCountData;
	}

	public Map<Integer, Integer> getDocumentClusterMap() {
		return docClusterMap;
	}

	private void performHAC() {
		docScores.sortByValue();

		SetMap<Integer, Integer> tempClusterDocMap = new SetMap<Integer, Integer>();
		Map<Integer, SparseVector> tempClusterWordCountData = new TreeMap<Integer, SparseVector>();

		for (int i = 0; i < docScores.size() && i < top_k; i++) {
			int docId = docScores.indexAtLoc(i);
			int cId = tempClusterDocMap.size();
			tempClusterDocMap.put(cId, docId);
			tempClusterWordCountData.put(cId, docWordCounts.rowAlways(docId).copy());
		}

		docScores.sortByIndex();

		for (int i = 0; i < 1000; i++) {
			List<Integer> clusterIds = new ArrayList<Integer>(tempClusterDocMap.keySet());
			int num_clusters = clusterIds.size();

			if (num_clusters == 1) {
				break;
			}

			int best_cId1 = -1, best_cId2 = -1;
			double best_sim = 0;
			double[][] sims = new double[num_clusters][num_clusters];

			for (int j = 0; j < clusterIds.size(); j++) {
				int cId1 = clusterIds.get(j);
				SparseVector sv1 = tempClusterWordCountData.get(cId1);

				for (int k = j + 1; k < clusterIds.size(); k++) {
					int cId2 = clusterIds.get(k);
					SparseVector sv2 = tempClusterWordCountData.get(cId2);

					double sim = computeCosineSimilarity(sv1, sv2);
					sims[j][k] = sim;
					sims[k][j] = sim;

					if (sim > best_sim) {
						best_sim = sim;
						best_cId1 = cId1;
						best_cId2 = cId2;
					}
				}
			}

			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(5);

			// System.out.printf("[%d] clusters, similarity [%s] between [%s] and [%s]\n",
			//
			// tempClusterDocMap.size(), nf.format(best_sim), best_cId1,
			// best_cId2);

			if (best_sim > merge_threshold) {
				SparseVector sv1 = tempClusterWordCountData.remove(best_cId1);
				SparseVector sv2 = tempClusterWordCountData.remove(best_cId2);
				// System.out.println(VectorUtils.toCounter(sv1, wordIndexer));
				// System.out.println(VectorUtils.toCounter(sv2, wordIndexer));
				// System.out.println(ArrayUtils.toString(simMatrix,
				// simMatrix.length, simMatrix.length, true,
				// NumberFormat.getInstance()));
				// System.out.println();
				VectorMath.add(sv1, sv2, sv1);

				tempClusterWordCountData.put(best_cId1, sv1);

				Set<Integer> docsInCluster1 = tempClusterDocMap.remove(best_cId1);
				Set<Integer> docsInCluster2 = tempClusterDocMap.remove(best_cId2);
				docsInCluster1.addAll(docsInCluster2);
				tempClusterDocMap.put(best_cId1, docsInCluster1);
			} else {
				break;
			}

			Counter<Integer> clusterDocCounts = new Counter<Integer>();

			for (int cId : tempClusterDocMap.keySet()) {
				clusterDocCounts.setCount(cId, tempClusterDocMap.get(cId).size());
			}

			System.out.printf("[%d]th iter\t[%d] clusters\t%s\n", i + 1, clusterDocCounts.size(), clusterDocCounts.toString());
		}

		clusterDocMap = new SetMap<Integer, Integer>();
		docClusterMap = new TreeMap<Integer, Integer>();

		for (int cId : tempClusterDocMap.keySet()) {
			Set<Integer> docIds = tempClusterDocMap.get(cId);
			int newClusterId = clusterDocMap.keySet().size();
			clusterDocMap.put(newClusterId, docIds);

			for (int docId : docIds) {
				docClusterMap.put(docId, cId);
			}
		}

		Counter<Integer> clusterDocCounts = new Counter<Integer>();

		for (int cId : clusterDocMap.keySet()) {
			clusterDocCounts.setCount(cId, clusterDocMap.get(cId).size());
		}

		System.out.printf("[%d] clusters\t%s\n", clusterDocCounts.size(), clusterDocCounts.toString());

		clusterWordCountData = new ArrayList<SparseVector>(tempClusterWordCountData.values());
	}

	private void performKMeans() {

	}

}
