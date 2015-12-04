package ohs.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ohs.math.ArrayMath;
import ohs.math.CommonFuncs;
import ohs.math.VectorMath;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseVector;
import ohs.types.ListMap;

/**
 * This class provides well-known IR utilities.
 * 
 * @author Heung-Seon Oh
 * 
 */
public class TermWeighting {

	public static void computeTFIDFs(List<SparseVector> docs) {
		computeTFIDFs(docs, docFreq(docs, getMaxIndex(docs) + 1));
	}

	public static void computeTFIDFs(List<SparseVector> docs, DenseVector docFreqs) {
		System.out.println("compute tfidfs.");
		for (int i = 0; i < docs.size(); i++) {
			SparseVector doc = docs.get(i);

			for (int j = 0; j < doc.size(); j++) {
				int w = doc.indexAtLoc(j);
				double cnt = doc.valueAtLoc(j);
				double tf = Math.log(cnt) + 1;
				double doc_freq = docFreqs.value(w);
				double num_docs = docs.size();
				double idf = doc_freq == 0 ? 0 : Math.log((num_docs + 1) / doc_freq);
				double tfidf = tf * idf;
				doc.setAtLoc(j, tfidf);
			}
			ArrayMath.normalizeByL2Norm(doc.values(), doc.values());
			doc.summation();
		}
	}

	public static DenseVector docFreq(List<SparseVector> docs, int num_terms) {
		DenseVector ret = new DenseVector(num_terms);
		for (SparseVector doc : docs) {
			for (int j = 0; j < doc.size(); j++) {
				int w = doc.indexAtLoc(j);
				ret.increment(w, 1);
			}
		}
		return ret;
	}

	public static int getMaxIndex(List<SparseVector> docs) {
		int ret = 0;
		for (SparseVector x : docs) {
			if (x.size() == 0) {
				continue;
			}
			int indexAtLast = x.indexAtLoc(x.size() - 1);
			if (indexAtLast > ret) {
				ret = indexAtLast;
			}
		}
		return ret;
	}

	public static ListMap<Integer, Integer> groupByLabel(List<SparseVector> docs) {
		ListMap<Integer, Integer> ret = new ListMap<Integer, Integer>();
		for (int i = 0; i < docs.size(); i++) {
			SparseVector x = docs.get(i);
			ret.put(x.label(), i);
		}
		return ret;
	}

	public static List<SparseVector> invertedIndexDoubleVector(List<SparseVector> docs, int num_terms) {
		System.out.println("build inverted index.");

		List<Integer[]>[] lists = new List[num_terms];

		for (int i = 0; i < lists.length; i++) {
			lists[i] = new ArrayList<Integer[]>();
		}

		for (int docId = 0; docId < docs.size(); docId++) {
			SparseVector doc = docs.get(docId);
			for (int j = 0; j < doc.size(); j++) {
				int termId = doc.indexAtLoc(j);
				double termCount = doc.valueAtLoc(j);
				lists[termId].add(new Integer[] { docId, (int) termCount });
			}
		}

		List<SparseVector> ret = new ArrayList<SparseVector>();

		for (int termId = 0; termId < lists.length; termId++) {
			List<Integer[]> list = lists[termId];

			int[] docIds = new int[list.size()];
			double[] termCounts = new double[list.size()];

			for (int j = 0; j < list.size(); j++) {
				docIds[j] = list.get(j)[0];
				termCounts[j] = list.get(j)[1];
			}

			SparseVector postingList = new SparseVector(docIds, termCounts, termId);
			postingList.sortByIndex();

			ret.add(postingList);

			lists[termId].clear();
			lists[termId] = null;
		}
		return ret;
	}

	public static int[][] makeInvertedIndex(List<SparseVector> docs, int num_terms) {
		System.out.println("build inverted index.");

		int[][] ret = new int[num_terms][];
		for (int i = 0; i < num_terms; i++) {
			if ((i + 1) % 100 == 0) {
				System.out.printf("\r[%d/%d]", i + 1, num_terms);
			}
			Set<Integer> set = new TreeSet<Integer>();

			for (int j = 0; j < docs.size(); j++) {
				SparseVector d = docs.get(j);
				if (d.location(i) < 0) {
					continue;
				}
				set.add(j);
			}

			ret[i] = new int[set.size()];
			int loc = 0;
			for (int docId : set) {
				ret[i][loc] = docId;
				loc++;
			}
		}

		System.out.printf("\r[%d/%d]\n", num_terms, num_terms);
		return ret;
	}

	public static void tf_rf(List<SparseVector> xs, int termSize) {
		System.out.println("weight by tf-rf");
		int[][] term_doc = makeInvertedIndex(xs, termSize);
		ListMap<Integer, Integer> label_doc = groupByLabel(xs);

		double N = xs.size();

		for (int labelId : label_doc.keySet()) {
			Set<Integer> termSet = new HashSet<Integer>();
			List<Integer> docIds4Label = label_doc.get(labelId);

			for (int docId : docIds4Label) {
				SparseVector x = xs.get(docId);
				for (int i = 0; i < x.size(); i++) {
					int termId = x.indexAtLoc(i);
					termSet.add(termId);
				}
			}

			SparseVector term_rf = new SparseVector(termSet.size());
			int loc = 0;

			for (int termId : termSet) {
				int[] docIds4Term = term_doc[termId];
				double N11 = 0;
				double N01 = 0;
				double N10 = 0;
				double N00 = 0;

				int loc1 = 0, loc2 = 0;

				while (loc1 < docIds4Label.size() && loc2 < docIds4Term.length) {
					int docId1 = docIds4Label.get(loc1);
					int docId2 = docIds4Term[loc2];

					if (docId1 == docId2) {
						N11++;
						loc1++;
						loc2++;
					} else if (docId1 > docId2) {
						N01++;
						loc2++;
					} else if (docId1 < docId2) {
						N10++;
						loc1++;
					}
				}

				N00 = N - (N11 + N01 + N10);
				double chisquare = CommonFuncs.chisquare(N11, N10, N01, N00);
				double rf = CommonFuncs.log2(2 + N11 / (Math.max(1, N01)));
				term_rf.setAtLoc(loc++, termId, rf);
			}

			term_rf.sortByIndex();

			for (int docId : docIds4Label) {
				SparseVector x = xs.get(docId);
				for (int i = 0; i < x.size(); i++) {
					int termId = x.indexAtLoc(i);
					double count = x.valueAtLoc(i);
					double rf = term_rf.valueAlways(termId);
					x.setAtLoc(i, count * rf);
				}

				VectorMath.unitVector(x);
			}
		}
	}

	public static void weightByBM25(List<SparseVector> docs) {
		System.out.println("weight by bm25.");
		double k_1 = 1.2d;
		double k_3 = 8d;
		double b = 0.75d;
		double avgDocLen = 0;

		for (SparseVector x : docs) {
			if (x.size() == 0) {
				continue;
			}
			avgDocLen += x.sum();
		}

		avgDocLen /= docs.size();

		DenseVector term_docFreq = docFreq(docs, getMaxIndex(docs) + 1);

		for (int i = 0; i < docs.size(); i++) {
			SparseVector x = docs.get(i);
			double docLen = x.sum();
			double norm = 0;

			for (int j = 0; j < x.size(); j++) {
				int termId = x.indexAtLoc(j);
				double tf = x.valueAtLoc(j);
				double value1 = (tf * (k_1 + 1)) / (tf + k_1 * (1 - b + b * (docLen / avgDocLen)));

				double docFreq = term_docFreq.value(termId);
				double numDocs = docs.size();
				double value2 = Math.log((numDocs - docFreq + 0.5) / (docFreq + 0.5));
				double weight = value1 * value2;

				x.setAtLoc(j, weight);
				norm += weight * weight;
			}
			x.scale(1f / norm);
		}
	}

}
