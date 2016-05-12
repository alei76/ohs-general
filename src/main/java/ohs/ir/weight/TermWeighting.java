package ohs.ir.weight;

import java.util.ArrayList;
import java.util.Collection;
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

	public static boolean print_log = false;

	public static DenseVector collectionWordCnts(Collection<SparseVector> docs) {
		int max_word_id = maxWordIndex(docs) + 1;
		DenseVector ret = new DenseVector(max_word_id);
		for (SparseVector x : docs) {

			for (int i = 0; i < x.size(); i++) {
				ret.increment(x.indexAtLoc(i), x.valueAtLoc(i));
			}
		}
		return ret;
	}

	public static void computeBM25(Collection<SparseVector> docs) {
		computeBM25(docs, 0);
	}

	public static void computeBM25(Collection<SparseVector> docs, DenseVector docFreqs, double sigma) {
		double avg_doc_len = 0;

		for (SparseVector x : docs) {
			avg_doc_len += x.sum();
		}

		avg_doc_len /= docs.size();

		double b = 0.75;
		double k1 = 1.5;

		int w = 0;
		double cnt = 0;
		double doc_freq = 0;
		double doc_len = 0;
		double idf = 0;
		double num_docs = docs.size();
		double numerator = 0;
		double denominator = 0;
		double weight = 0;

		for (SparseVector x : docs) {
			for (int i = 0; i < x.size(); i++) {
				w = x.indexAtLoc(i);
				cnt = x.valueAtLoc(i);
				doc_len = x.sum();
				doc_freq = docFreqs.valueAtLoc(w);
				idf = Math.log((num_docs - doc_freq + 0.5) / (doc_freq + 0.5));

				numerator = cnt * (k1 + 1);
				denominator = cnt + k1 * (1 - b + b * (doc_len / avg_doc_len));
				weight = idf * (numerator / denominator + sigma);
				x.setAtLoc(i, weight);
			}
		}
	}

	public static void computeBM25(Collection<SparseVector> docs, double sigma) {
		computeBM25(docs, documentFreqs(docs), sigma);
	}

	public static void computeBM25Plus(Collection<SparseVector> docs) {
		computeBM25(docs, 1);
	}

	public static void computeDFRee(List<SparseVector> docs, DenseVector collWordCnts) {
		double num_words = collWordCnts.sum();
		int w = 0;
		double tf = 0;
		double prior = 0;
		double posterior = 0;
		double termFrequency = 0;
		double InvPriorCollection = 0;
		double norm = 0;
		double weight = 0;

		for (int i = 0; i < docs.size(); i++) {
			SparseVector x = docs.get(i);
			double docLength = x.sum();

			for (int j = 0; j < x.size(); j++) {
				w = x.indexAtLoc(j);
				tf = x.valueAtLoc(j);
				prior = tf / docLength;
				posterior = (tf + 1d) / (docLength + 1);
				termFrequency = collWordCnts.value(w);
				InvPriorCollection = num_words / termFrequency;
				norm = tf * CommonFuncs.log2(posterior / prior);
				weight = norm * (tf * (-CommonFuncs.log2(prior * InvPriorCollection)) +

						(tf + 1d) * (+CommonFuncs.log2(posterior * InvPriorCollection)) + 0.5 * CommonFuncs.log2(posterior / prior));

				x.setAtLoc(j, weight);
			}

			VectorMath.unitVector(x);
		}
	}

	public static void computeDirichletLanguageModels(List<SparseVector> docs) {
		DenseVector collWordPrs = collectionWordCnts(docs);
		collWordPrs.normalize();

		computeDirichletLanguageModels(docs, collWordPrs, 2000);
	}

	public static void computeDirichletLanguageModels(List<SparseVector> docs, DenseVector collWordPrs, double dirichlet_prior) {
		int w = 0;
		double cnt = 0;
		double cnt_sum_in_doc = 0;
		double mixture_for_coll = 0;
		double pr_w_in_doc = 0;
		double pr_w_in_coll = 0;
		double pr_sum = 0;

		for (int i = 0; i < docs.size(); i++) {
			SparseVector x = docs.get(i);
			cnt_sum_in_doc = x.sum();
			pr_sum = 0;

			for (int j = 0; j < x.size(); j++) {
				w = x.indexAtLoc(j);
				cnt = x.valueAtLoc(j);
				pr_w_in_doc = cnt / cnt_sum_in_doc;
				pr_w_in_coll = collWordPrs.prob(w);
				mixture_for_coll = dirichlet_prior / (cnt_sum_in_doc + dirichlet_prior);
				pr_w_in_doc = (1 - mixture_for_coll) * pr_w_in_doc + mixture_for_coll * pr_w_in_coll;
				pr_sum += pr_w_in_doc;

				x.setAtLoc(j, pr_w_in_doc);
			}
			x.setSum(pr_sum);
		}
	}

	public static void computeParsimoniousLanguageModels(Collection<SparseVector> docs) {
		DenseVector collWordPrs = collectionWordCnts(docs);
		collWordPrs.normalize();

		computeParsimoniousLanguageModels(docs, collWordPrs, 0.5);
	}

	public static void computeParsimoniousLanguageModels(Collection<SparseVector> docs, DenseVector collWordPrs, double mixture_for_coll) {
		int w = 0;
		double cnt = 0;
		double pr_w_in_doc = 0;
		double pr_w_in_coll = 0;
		double mixture_for_doc = 1 - mixture_for_coll;
		double e = 0;
		double pr_sum = 0;
		double dist = 0;

		for (SparseVector x : docs) {
			SparseVector nx = x.copy();
			nx.normalize();

			SparseVector ox = nx.copy();

			for (int j = 0; j < 10; j++) {
				pr_sum = 0;

				for (int k = 0; k < nx.size(); k++) {
					w = nx.indexAtLoc(k);
					pr_w_in_doc = nx.valueAtLoc(k);
					cnt = x.valueAtLoc(k);
					pr_w_in_coll = collWordPrs.value(w);
					e = (mixture_for_doc * pr_w_in_doc)
							/ ArrayMath.addAfterScale(pr_w_in_coll, mixture_for_coll, pr_w_in_doc, mixture_for_doc);
					e = cnt * e;
					pr_sum += e;

					nx.setAtLoc(k, e);
				}
				nx.scale(1f / pr_sum);

				dist = ArrayMath.cosine(ox.values(), nx.values());

				if (dist < 0.00001) {
					break;
				}

				ox = nx.copy();
			}

			x.setIndexes(nx.indexes());
			x.setValues(nx.values());
			x.setSum(nx.sum());
		}
	}

	public static void computeTFIDFs(Collection<SparseVector> docs) {
		computeTFIDFs(docs, documentFreqs(docs));
	}

	public static void computeTFIDFs(Collection<SparseVector> docs, DenseVector docFreqs) {

		if (print_log) {
			print("compute tfidfs.");
		}

		double norm = 0;
		int w = 0;
		double cnt = 0;
		double doc_freq = 0;
		double num_docs = docs.size();
		double weight = 0;
		for (SparseVector doc : docs) {
			norm = 0;
			for (int j = 0; j < doc.size(); j++) {
				w = doc.indexAtLoc(j);
				cnt = doc.valueAtLoc(j);
				doc_freq = docFreqs.value(w);
				weight = tfidf(cnt, num_docs, doc_freq);
				norm += (weight * weight);
				doc.setAtLoc(j, weight);
			}
			norm = Math.sqrt(norm);
			doc.scale(1f / norm);
		}
	}

	public static DenseVector docFreqs(Collection<SparseVector> docs, int word_size) {
		DenseVector ret = new DenseVector(word_size);
		for (SparseVector doc : docs) {
			for (int w : doc.indexes()) {
				ret.increment(w, 1);
			}
		}
		return ret;
	}

	public static DenseVector documentFreqs(Collection<SparseVector> docs) {
		return docFreqs(docs, maxWordIndex(docs) + 1);
	}

	public static ListMap<Integer, Integer> groupByLabel(List<SparseVector> docs) {
		ListMap<Integer, Integer> ret = new ListMap<Integer, Integer>();
		for (int i = 0; i < docs.size(); i++) {
			SparseVector x = docs.get(i);
			ret.put(x.label(), i);
		}
		return ret;
	}

	public static double idf(double num_docs, double doc_freq) {
		return Math.log((num_docs + 1) / (doc_freq));
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

	public static int maxWordIndex(Collection<SparseVector> docs) {
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

	public static void print(String log) {
		System.out.println(log);
	}

	public static double tf(double word_cnt) {
		return Math.log(word_cnt) + 1;
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

	public static double tfidf(double word_cnt, double num_docs, double doc_freq) {
		return tf(word_cnt) * idf(num_docs, doc_freq);
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

		DenseVector term_docFreq = docFreqs(docs, maxWordIndex(docs) + 1);

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
