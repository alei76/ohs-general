package ohs.ir.medical.trec.cds_2015;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohs.ir.medical.general.WordCountBox;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.matrix.SparseMatrix;
import ohs.matrix.SparseVector;
import ohs.types.CounterMap;
import ohs.types.DeepListMap;
import ohs.types.Indexer;
import ohs.types.ListMap;

public class ProximityScorer {

	public static double hal(int i, int j, int window_size) {
		return window_size - (j - i) + 1;
	}

	private DeepListMap<Integer, Integer, Integer> docWordLocs;

	private int window_size;

	private Indexer<String> wordIndexer;

	private boolean makeLog;

	private StringBuffer logBuf;

	public ProximityScorer(Indexer<String> wordIndexer) {
		this(wordIndexer, false);
	}

	public ProximityScorer(Indexer<String> wordIndexer, boolean makeLog) {
		this.wordIndexer = wordIndexer;
		this.window_size = window_size;
		this.makeLog = makeLog;
	}

	public double[] getDistances(List<Integer> locs1, List<Integer> locs2) {
		List<Double> dists = new ArrayList<Double>();

		for (int i = 0, k = 0; i < locs1.size(); i++) {
			int loc1 = locs1.get(i);
			for (int j = 0; j < locs2.size(); j++) {
				int loc2 = locs2.get(j);
				double dist = Math.abs(loc1 - loc2);
				dists.add(dist);
			}
		}

		double[] ret = new double[dists.size()];

		for (int i = 0; i < dists.size(); i++) {
			ret[i] = dists.get(i);
		}

		return ret;
	}

	public StringBuffer getLogBuffer() {
		return logBuf;
	}

	public SparseVector score(WordCountBox wcb, SparseVector queryModel) {
		setDocWordLocs(wcb.getDocToWords());

		logBuf = new StringBuffer();

		SparseMatrix docWordCounts = wcb.getDocToWordCounts();

		SparseVector ret = new SparseVector(docWordCounts.rowSize());

		for (int i = 0; i < docWordCounts.rowSize(); i++) {
			int docId = docWordCounts.indexAtLoc(i);
			SparseVector wordCounts = docWordCounts.rowAtLoc(i);
			List<Integer> words = wcb.getDocToWords().get(docId);
			ListMap<Integer, Integer> wordLocs = docWordLocs.get(docId);

			CounterMap<Integer, Integer> cm1 = new CounterMap<Integer, Integer>();
			List<Double> minDists = new ArrayList<Double>();

			for (int j = 0; j < queryModel.size(); j++) {
				int qw1 = queryModel.indexAtLoc(j);
				List<Integer> locs1 = wordLocs.get(qw1);

				if (locs1.size() == 0) {
					continue;
				}

				for (int k = j + 1; k < queryModel.size(); k++) {
					int qw2 = queryModel.indexAtLoc(k);
					List<Integer> locs2 = wordLocs.get(qw2);

					if (locs2.size() == 0) {
						continue;
					}

					double[] dists = getDistances(locs1, locs2);
					double[] minMax = ArrayMath.minMax(dists);
					cm1.incrementCount(qw1, qw2, minMax[0]);
					minDists.add(minMax[0]);
				}

				if (makeLog) {
					StringBuffer sb = new StringBuffer();
					sb.append(String.format("QWord:\t%s\n", wordIndexer.getObject(qw1)));
				}

				if (makeLog) {
					logBuf.append("\n");
				}
			}

			double[] values = new double[minDists.size()];

			ArrayUtils.copy(minDists, values);

			double avg_mean_dist = ArrayMath.mean(values);
			double score = 1 / (avg_mean_dist + 1);
			// double score = CommonFuncs.log2(0.3 + Math.exp(-avg_mean_dist));
			ret.incrementAtLoc(i, docId, score);
		}

		ret.normalize();

		return ret;
	}

	private void setDocWordLocs(ListMap<Integer, Integer> docWords) {
		docWordLocs = new DeepListMap<Integer, Integer, Integer>();

		for (int docId : docWords.keySet()) {
			List<Integer> words = docWords.get(docId);
			ListMap<Integer, Integer> wordLocs = new ListMap<Integer, Integer>();

			for (int i = 0; i < words.size(); i++) {
				int w = words.get(i);
				wordLocs.put(w, i);
			}

			for (int w : wordLocs.keySet()) {
				List<Integer> locs = wordLocs.get(w);
				Collections.sort(locs);
			}

			docWordLocs.put(docId, wordLocs);
		}
	}
}
