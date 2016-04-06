package ohs.eden.keyphrase;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.Pair;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;
import ohs.utils.TermWeighting;
import ohs.utils.UnicodeUtils;

public class KeywordClusterer {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", KeywordClusterer.class.getName());

		KeywordData data = new KeywordData();

		if (FileUtils.exists(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"))) {
			data.read(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));
		} else {
			data.readText(KPPath.KEYWORD_DATA_FILE);
			data.write(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));
		}

		KeywordClusterer kc = new KeywordClusterer(data);
		kc.cluster();
		kc.writeClusters(KPPath.KEYWORD_CLUSTER_FILE);

		// data.write(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));

		System.out.println("process ends.");
	}

	private static String normalize(String s) {
		return s.replaceAll("[\\p{Punct}\\s]+", "").toLowerCase();
	}

	private KeywordData kwdData;

	private Indexer<String> kwdIndexer;

	private CounterMap<Integer, Integer> clusterToKwds;

	private int[] kwdToCluster;

	private Map<Integer, String> clusterLabel;

	private GramGenerator gg = new GramGenerator(2);

	private int prefix_size = 6;

	public KeywordClusterer(KeywordData kwdData) {
		this.kwdData = kwdData;

		kwdIndexer = kwdData.getKeywordIndexer();
	}

	private int[][] buildGramPostings(Map<Integer, SparseVector> cents, int prefix_size, int gram_size) {
		SetMap<Integer, Integer> gramPostings = Generics.newSetMap();
		for (int cid : cents.keySet()) {
			SparseVector cent = cents.get(cid);
			cent.sortByValue();
			for (int i = 0; i < cent.size() && i < prefix_size; i++) {
				gramPostings.put(cent.indexAtLoc(i), cid);
			}
			cent.sortByIndex();
		}
		System.out.printf("build [%d] gram postings\n", gramPostings.size());

		int[][] ret = new int[gram_size][];
		for (int i = 0; i < ret.length; i++) {
			int[] cids = new int[0];
			Set<Integer> set = gramPostings.get(i, false);

			if (set != null) {
				cids = new int[set.size()];
				int loc = 0;
				for (int cid : set) {
					cids[loc++] = cid;
				}
				set.clear();

				Arrays.sort(cids);
			}
			ret[i] = cids;
		}
		return ret;
	}

	public void cluster() throws Exception {
		kwdToCluster = new int[kwdIndexer.size()];

		clusterToKwds = Generics.newCounterMap(kwdIndexer.size());

		for (int i = 0; i < kwdIndexer.size(); i++) {
			clusterToKwds.setCount(i, i, 1);
			kwdToCluster[i] = i;
		}

		matchTwoLanguages();

		// selectClusterLabels();
		// writeClusters(KPPath.KEYWORD_CLUSTER_FILE.replace(".txt", "-01.txt"));

		matchSingleLanguage(false);

		// selectClusterLabels();
		// writeClusters(KPPath.KEYWORD_CLUSTER_FILE.replace(".txt", "-02.txt"));

		matchKoreanCharacters();

		selectClusterLabels();
		writeClusters(KPPath.KEYWORD_CLUSTER_FILE.replace(".txt", "-03.txt"));

		// exactLanguageMatch(true);

		// selectClusterLabels();

		// writeClusterText(KPPath.KEYWORD_CLUSTER_FILE.replace(".txt", "-03.txt"));

		// filter(3);

		// hierarchicalAgglomerativeClustering();

		selectClusterLabels();

		SetMap<Integer, Integer> t = Generics.newSetMap(clusterToKwds.size());

		for (int cid : clusterToKwds.keySet()) {
			t.put(cid, clusterToKwds.keySetOfCounter(cid));
		}

		kwdData.setClusterLabel(clusterLabel);
		kwdData.setClusters(t);
	}

	private Counter<String>[] computeLabelScores(Set<Integer> kwdids) {
		GramGenerator gg = new GramGenerator(3);
		int num_langs = 2;

		Counter<String>[] ret = new Counter[num_langs];

		for (int i = 0; i < num_langs; i++) {
			CounterMap<String, Character> gramProbs = Generics.newCounterMap();

			for (int kwdid : kwdids) {
				String kwdStr = kwdIndexer.getObject(kwdid);
				String kwd = kwdStr.split("\t")[i];
				kwd = normalize(kwd);
				int kw_freq = kwdData.getKeywordFreqs()[kwdid];

				for (Gram g : gg.generateQGrams(kwd.toLowerCase())) {
					gramProbs.incrementCount(g.getString().substring(0, 2), g.getString().charAt(2), kw_freq);
				}
			}

			gramProbs.normalize();

			Counter<String> kwdScores = Generics.newCounter();

			for (int kwdid : kwdids) {
				String kwdStr = kwdIndexer.getObject(kwdid);
				String kwd = kwdStr.split("\t")[i];
				kwd = normalize(kwd);
				double log_likelihood = computeLoglikelihood(gg.generateQGrams(kwd), gramProbs);
				kwdScores.incrementCount(kwd, log_likelihood);
			}

			if (kwdScores.size() == 0) {
				kwdScores.setCount("\"\"", 0);
			}

			double max = kwdScores.max();
			double score_sum = 0;

			for (String lang : kwdScores.keySet()) {
				double score = kwdScores.getCount(lang);
				score = Math.exp(score - max);
				kwdScores.setCount(lang, score);
				score_sum += score;
			}
			kwdScores.scale(1f / score_sum);
			ret[i] = kwdScores;
		}
		return ret;
	}

	private double computeLoglikelihood(Gram[] gs, CounterMap<String, Character> bigramProbs) {
		double ret = 0;
		for (Gram g : gs) {
			double prob = bigramProbs.getCount(g.getString().substring(0, 2), g.getString().charAt(2));
			if (prob > 0) {
				ret += Math.log(prob);
			}
		}
		return ret;
	}

	private void computeWeights(Map<Integer, SparseVector> cents, int[] gram_freqs) {
		double num_clusters = cents.size();

		for (int cid : cents.keySet()) {
			SparseVector cent = cents.get(cid);
			double norm = 0;
			for (int i = 0; i < cent.size(); i++) {
				int gid = cent.indexAtLoc(i);
				double cnt = cent.valueAtLoc(i);
				double tf = Math.log(cnt) + 1;
				double gram_freq = gram_freqs[gid];
				double idf = gram_freq == 0 ? 0 : Math.log((num_clusters + 1) / gram_freq);
				double tfidf = tf * idf;
				cent.setAtLoc(i, tfidf);
				norm += (tfidf * tfidf);
			}
			norm = Math.sqrt(norm);
			cent.scale(1f / norm);
		}
	}

	private String getKoreanKeyword(int kwdid) {
		return kwdIndexer.getObject(kwdid).split("\t")[0];
	}

	private double computeSyllableCosine(int cid1, int cid2) {
		int[] cids = new int[] { cid1, cid2 };
		SparseVector[] vs = new SparseVector[2];

		for (int i = 0; i < cids.length; i++) {
			Counter<Integer> c = new Counter<Integer>();
			for (int kwdid : clusterToKwds.keySetOfCounter(cids[i])) {
				String kwd = getKoreanKeyword(kwdid);
				String key = normalize(kwd);
				key = UnicodeUtils.decomposeToJamo(key);

				for (int j = 0; j < key.length(); j++) {
					c.incrementCount((int) key.charAt(j), 1);
				}
			}
			vs[i] = new SparseVector(c);
			VectorMath.unitVector(vs[i]);
		}
		return VectorMath.dotProduct(vs[0], vs[1], false);

	}

	private void matchKoreanCharacters() {
		System.out.println("match korean characters");

		int old_size = clusterToKwds.size();

		Map<Integer, SparseVector> clusterToChars = Generics.newHashMap();
		SetMap<Integer, Integer> gramToClusters = Generics.newSetMap();
		Indexer<String> gramIndexer = Generics.newIndexer();
		Counter<Integer> gramFreqs = Generics.newCounter();

		for (Entry<Integer, Counter<Integer>> e : clusterToKwds.getEntrySet()) {
			int cid = e.getKey();
			Counter<Integer> kwdids = e.getValue();
			Counter<Integer> chCnts = Generics.newCounter();

			for (int kwdid : kwdids.keySet()) {
				String kwd = getKoreanKeyword(kwdid);
				String key = normalize(kwd);

				if (key.length() < 2) {
					continue;
				}

				// String s = UnicodeUtils.decomposeToJamo(korKwd);
				for (char c : key.toCharArray()) {
					chCnts.incrementCount((int) c, 1);
				}

				Counter<Integer> gramCnts = gg.generateQGrams(key, gramIndexer, true);

				for (int gid : gramCnts.keySet()) {
					gramToClusters.put(gid, cid);
					gramFreqs.incrementCount(gid, 1);
				}
			}

			if (chCnts.size() > 0) {
				SparseVector sv = VectorUtils.toSparseVector(chCnts);
				clusterToChars.put(cid, sv);
			}
		}

		SparseVector chFreqs = null;

		{
			Counter<Integer> freqs = Generics.newCounter();
			for (int cid : clusterToChars.keySet()) {
				SparseVector sv = clusterToChars.get(cid);
				for (int ch : sv.indexes()) {
					freqs.incrementCount(ch, 1);
				}
			}
			chFreqs = VectorUtils.toSparseVector(freqs);
		}

		double num_clusters = clusterToKwds.keySet().size();

		for (SparseVector sv : clusterToChars.values()) {
			for (int i = 0; i < sv.size(); i++) {
				int ch = sv.indexAtLoc(i);
				double ch_cnt = sv.valueAtLoc(i);
				double cluster_freq = chFreqs.valueAlways(ch);
				double weight = TermWeighting.tfidf(ch_cnt, num_clusters, cluster_freq);
				sv.setAtLoc(i, weight);
			}
			VectorMath.unitVector(sv);
		}

		List<Integer> cids = Generics.newArrayList(clusterToChars.keySet());

		CounterMap<Integer, Integer> toMerge = Generics.newCounterMap();

		StopWatch stopWatch = StopWatch.newStopWatch();

		for (int i = 0; i < cids.size(); i++) {
			if ((i + 1) % 1000 == 0) {
				System.out.printf("\r[%d/%d, %s]", i + 1, cids.size(), stopWatch.stop());
			}
			int cid1 = cids.get(i);
			String kwd1 = getKoreanKeyword(cid1);
			String key = normalize(kwd1);

			// if (cid1 != 1072666) {
			// continue;
			// }

			Counter<Integer> gramIDFs = gg.generateQGrams(key, gramIndexer, false);

			for (int gid : gramIDFs.keySet()) {
				double idf = TermWeighting.idf(num_clusters, gramFreqs.getCount(gid));
				gramIDFs.setCount(gid, idf);
			}

			Counter<Integer> toCompare = Generics.newCounter();

			List<Integer> gids = gramIDFs.getSortedKeys();

			for (int j = 0; j < gids.size() && j < prefix_size; j++) {
				int gid = gids.get(j);
				toCompare.incrementAll(gramToClusters.get(gid, false), gramIDFs.getCount(gid));
			}

			if (toCompare.size() == 0) {
				continue;
			}

			SparseVector sv1 = clusterToChars.get(cid1);

			List<Integer> keys = toCompare.getSortedKeys();

			for (int j = 0; j < keys.size(); j++) {
				int cid2 = keys.get(j);
				String kwd2 = getKoreanKeyword(cid2);

				if (cid1 == cid2) {
					continue;
				}

				// if (cid2 != 1072717) {
				// continue;
				// }

				if (toMerge.containKey(cid1, cid2) || toMerge.containKey(cid2, cid1)) {
					continue;
				}

				SparseVector sv2 = clusterToChars.get(cid2);
				double cosine = VectorMath.dotProduct(sv1, sv2);

				if (cosine >= 0.9) {
					toMerge.incrementCount(cid1, cid2, cosine);
					toMerge.incrementCount(cid2, cid1, cosine);
				} else if (cosine >= 0.75) {
					double cosine2 = computeSyllableCosine(cid1, cid2);

					if (cosine2 >= 0.9) {
						toMerge.incrementCount(cid1, cid2, cosine);
						toMerge.incrementCount(cid2, cid1, cosine);
					}
				}
			}
		}

		System.out.printf("\r[%d/%d, %s]\n", cids.size(), cids.size(), stopWatch.stop());

		Set<Integer> merged = Generics.newHashSet();

		for (int cid1 : toMerge.keySet()) {
			Counter<Integer> c = toMerge.getCounter(cid1);
			List<Integer> keys = c.getSortedKeys();

			if (merged.contains(cid1)) {
				continue;
			}

			for (int i = 0; i < keys.size(); i++) {
				int cid2 = keys.get(i);
				double cosine = c.getCount(cid2);

				if (merged.contains(cid2)) {
					continue;
				}

				merged.add(cid1);
				merged.add(cid2);

				// System.out.printf("[%s] + [%s]\n", kwdIndexer.getObject(cid1), kwdIndexer.getObject(cid2));

				Counter<Integer> newCluster = Generics.newCounter();
				newCluster.incrementAll(clusterToKwds.removeKey(cid1));
				newCluster.incrementAll(clusterToKwds.removeKey(cid2));

				int new_cid = Math.min(cid1, cid2);

				clusterToKwds.setCounter(new_cid, newCluster);

				break;
			}
		}

		for (int cid : clusterToKwds.keySet()) {
			for (int kwdid : clusterToKwds.keySetOfCounter(cid)) {
				kwdToCluster[kwdid] = cid;
			}
		}

		int new_size = clusterToKwds.size();

		System.out.printf("[%d -> %d clusters]\n", old_size, new_size);

	}

	private void matchSingleLanguage(boolean isEnglish) {
		System.out.println("match language (English: " + isEnglish + ")");

		int old_size = clusterToKwds.size();

		CounterMap<String, Integer> keyToClusters = Generics.newCounterMap();

		for (Entry<Integer, Counter<Integer>> e : clusterToKwds.getEntrySet()) {
			int cid = e.getKey();
			Counter<Integer> kwdids = e.getValue();

			for (int kwdid : kwdids.keySet()) {
				String kwdStr = kwdIndexer.getObject(kwdid);
				String[] two = kwdStr.split("\t");
				String key = StrUtils.value(isEnglish, two[1], two[0]);
				key = normalize(key);
				// if (key.length() < 4) {
				// continue;
				// }
				keyToClusters.incrementCount(key, cid, 1);
			}
		}

		for (String key : keyToClusters.keySet()) {
			Set<Integer> cids = keyToClusters.keySetOfCounter(key);

			Counter<Integer> kwds = Generics.newCounter();

			for (int cid : cids) {
				Counter<Integer> tmp = clusterToKwds.removeKey(cid);
				if (tmp != null) {
					kwds.incrementAll(tmp);
				}
			}

			int new_cid = min(cids);

			clusterToKwds.setCounter(new_cid, kwds);

			for (int kwdid : kwds.keySet()) {
				kwdToCluster[kwdid] = new_cid;
			}
		}

		int new_size = clusterToKwds.size();

		System.out.printf("[%d -> %d clusters]\n", old_size, new_size);
	}

	private void matchTwoLanguages() {
		System.out.println("match two languages");

		int old_size = clusterToKwds.size();

		SetMap<String, Integer> keyToKwds = Generics.newSetMap();

		for (int i = 0; i < kwdIndexer.size(); i++) {
			String key = kwdIndexer.getObject(i);
			key = key.replace("\t", "tab").replaceAll("[\\p{Punct}]+", "").toLowerCase();
			keyToKwds.put(key, i);
		}

		for (String kwd : keyToKwds.keySet()) {
			Set<Integer> kwdids = keyToKwds.get(kwd);

			if (kwdids.size() > 1) {
				Set<Integer> cids = Generics.newHashSet();
				Counter<Integer> newCluster = Generics.newCounter();

				for (int kwid : kwdids) {
					int cid = kwdToCluster[kwid];
					cids.add(cid);
					newCluster.incrementAll(clusterToKwds.removeKey(cid));
				}

				int new_cid = min(cids);

				clusterToKwds.setCounter(new_cid, newCluster);

				for (int kwdid : kwdids) {
					kwdToCluster[kwdid] = new_cid;
				}
			}
		}

		int new_size = clusterToKwds.size();

		System.out.printf("[%d -> %d clusters]\n", old_size, new_size);

	}

	private int min(Set<Integer> set) {
		int ret = Integer.MAX_VALUE;
		for (int i : set) {
			if (i < ret) {
				ret = i;
			}
		}
		return ret;
	}

	public void printClusters() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("Clusters:\t%d", clusterToKwds.size()));
		sb.append(String.format("\nKeywords:\t%d", (int) clusterToKwds.totalCount()));

		List<Integer> cids = clusterToKwds.getRowCountSums().getSortedKeys();
		// List<Integer> cids = Generics.newArrayList();
		//
		// {
		// List<String> keys = Generics.newArrayList();
		//
		// Map<Integer, Integer> map = Generics.newHashMap();
		//
		// for (int cid : clusterToKwds.keySet()) {
		// int kwdid = clusterToKwds.getCounter(cid).argMax();
		// map.put(kwdid, cid);
		// String kwd = kwdIndexer.getObject(kwdid);
		// keys.add(kwd);
		// }
		//
		// Collections.sort(keys);
		//
		// cids = Generics.newArrayList();
		// for (int i = 0; i < keys.size(); i++) {
		// int kwid = kwdIndexer.indexOf(keys.get(i));
		// int cid = map.get(kwid);
		// cids.add(cid);
		// }
		// }

		for (int i = 0, n = 1; i < cids.size() && i < 10; i++) {
			int cid = cids.get(i);
			sb.append(String.format("No:\t%d", n));
			sb.append(String.format("\nID:\t%d", cid));
			sb.append(String.format("\nLabel:\t%s", clusterLabel.get(cid)));
			sb.append(String.format("\nKeywords:\t%d", clusterToKwds.getCounter(cid).size()));

			Counter<Integer> c = Generics.newCounter();

			for (int kwdid : clusterToKwds.getCounter(cid).keySet()) {
				c.setCount(kwdid, kwdData.getKeywordFreqs()[kwdid]);
			}

			n++;

			List<Integer> kwdids = c.getSortedKeys();
			for (int j = 0; j < kwdids.size() && j < 5; j++) {
				int kwid = kwdids.get(j);
				int kw_freq = kwdData.getKeywordFreqs()[kwid];
				sb.append(String.format("\n%d:\t%d\t%s\t%d", j + 1, kwid, kwdIndexer.getObject(kwid), kw_freq));
			}
			sb.append("\n\n");
		}

		System.out.println(sb.toString());
	}

	private void selectClusterLabels() {
		System.out.println("select cluster labels");

		clusterLabel = Generics.newHashMap();

		for (int cid : clusterToKwds.keySet()) {
			Set<Integer> kwdids = clusterToKwds.getCounter(cid).keySet();
			Counter<String>[] scoreData = computeLabelScores(kwdids);
			String korLabel = scoreData[0].argMax();
			String engLabel = scoreData[1].argMax();

			clusterLabel.put(cid, korLabel + "\t" + engLabel);
		}
	}

	public void writeClusters(String fileName) {
		TextFileWriter writer = new TextFileWriter(fileName);

		writer.write(String.format("Clusters:\t%d", clusterToKwds.size()));
		writer.write(String.format("\nKeywords:\t%d", (int) clusterToKwds.totalCount()));

		List<Integer> cids = Generics.newArrayList();

		boolean sortAphabetically = false;

		if (sortAphabetically) {
			List<String> keys = Generics.newArrayList();
			Map<Integer, Integer> map = Generics.newHashMap();

			for (int cid : clusterToKwds.keySet()) {
				int kwdid = clusterToKwds.getCounter(cid).argMax();
				map.put(kwdid, cid);
				String kwd = kwdIndexer.getObject(kwdid);
				keys.add(kwd);
			}

			Collections.sort(keys);

			cids = Generics.newArrayList();
			for (int i = 0; i < keys.size(); i++) {
				int kwdid = kwdIndexer.indexOf(keys.get(i));
				int cid = map.get(kwdid);
				cids.add(cid);
			}
		} else {
			cids = clusterToKwds.getRowCountSums().getSortedKeys();
		}

		{
			List<Integer> tmp = Generics.newArrayList();
			int target = 0;
			for (int i = 0; i < cids.size(); i++) {
				int cid = cids.get(i);
				String kwd = getKoreanKeyword(cid);
				if (kwd.length() == 0) {
					target = cid;
				} else {
					tmp.add(cid);
				}
			}
			tmp.add(target);
			cids = tmp;
		}

		for (int i = 0, n = 1; i < cids.size(); i++) {
			int cid = cids.get(i);

			StringBuffer sb = new StringBuffer();
			sb.append(String.format("No:\t%d", n));
			sb.append(String.format("\nID:\t%d", cid));
			sb.append(String.format("\nLabel:\t%s", clusterLabel.get(cid)));
			sb.append(String.format("\nKeywords:\t%d", clusterToKwds.getCounter(cid).size()));

			Counter<Integer> c = Generics.newCounter();

			for (int kwdid : clusterToKwds.getCounter(cid).keySet()) {
				c.setCount(kwdid, kwdData.getKeywordFreqs()[kwdid]);
			}

			n++;

			List<Integer> kwids = c.getSortedKeys();
			for (int j = 0; j < kwids.size(); j++) {
				int kwid = kwids.get(j);
				int kw_freq = kwdData.getKeywordFreqs()[kwid];
				sb.append(String.format("\n%d:\t%d\t%s\t%d", j + 1, kwid, kwdIndexer.getObject(kwid), kw_freq));
			}
			writer.write("\n\n" + sb.toString());
		}
		writer.close();

		System.out.printf("write [%d] clusterToKwds at [%s]\n", clusterToKwds.size(), fileName);
	}

}
