package ohs.eden.keyphrase;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
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

public class KeywordClusterer {

	public static final String NONE = "<none>";

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
		kc.writeClusterText(KPPath.KEYWORD_CLUSTER_FILE);

		data.write(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));

		System.out.println("process ends.");
	}

	private static String normalize(String s) {
		return s.replaceAll("[\\s\\p{Punct}&&[^<>]]+", "").toLowerCase();
	}

	private KeywordData kwdData;

	private Indexer<String> kwdIndexer;

	private CounterMap<Integer, Integer> clusters;

	private int[] keywordToCluster;

	private int[] clusterToCluster;

	private Map<Integer, String> clusterLabel;

	private GramGenerator gg = new GramGenerator(3);

	public KeywordClusterer(KeywordData kwdData) {
		this.kwdData = kwdData;

		kwdIndexer = kwdData.getKeywordIndexer();
	}

	private Indexer<String> buildGramIndexer() {
		Indexer<String> gramIndexer = Generics.newIndexer();

		for (int cid : clusters.keySet()) {
			for (int kwid : clusters.getCounter(cid).keySet()) {
				String keyword = kwdIndexer.getObject(kwid);
				for (String lang : keyword.split("\t")) {
					if (lang.equals(NONE)) {
						continue;
					}

					for (Gram g : gg.generateQGrams(lang.toLowerCase())) {
						gramIndexer.getIndex(g.getString());
					}
				}
			}
		}

		System.out.printf("build [%d] gram indexer\n", gramIndexer.size());
		return gramIndexer;
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
			}
			ret[i] = cids;
		}
		return ret;
	}

	public void cluster() throws Exception {
		clusters = Generics.newCounterMap();

		keywordToCluster = new int[kwdIndexer.size()];

		clusterToCluster = new int[kwdIndexer.size()];

		for (int i = 0; i < kwdIndexer.size(); i++) {
			clusters.setCount(i, i, 1);
			keywordToCluster[i] = i;
		}

		clusterUsingExactMatch();

		clusterUsingExactLanguageMatch(false);

		// filter(3);

		selectClusterLabels();

		clusterUsingGramMatch();

		selectClusterLabels();

		SetMap<Integer, Integer> t = Generics.newSetMap(clusters.size());

		for (int cid : clusters.keySet()) {
			t.put(cid, clusters.getCounter(cid).keySet());
		}

		kwdData.setClusterLabel(clusterLabel);
		kwdData.setClusters(t);
	}

	private void clusterUsingExactLanguageMatch(boolean isEnglish) {
		System.out.println("cluster using exact language match");

		SetMap<String, Integer> keyKeywordMap = Generics.newSetMap();

		for (Entry<Integer, Counter<Integer>> e : clusters.getEntrySet()) {

			Counter<Integer> kwids = e.getValue();

			for (int kwid : kwids.keySet()) {
				String keyword = kwdIndexer.getObject(kwid);
				String key = isEnglish ? keyword.split("\t")[1] : keyword.split("\t")[0];
				key = normalize(key);

				if (key.equals(NONE) || key.length() < 4) {
					continue;
				}
				// key = key.replaceAll("[\\s\\p{Punct}]+", "").toLowerCase();
				keyKeywordMap.put(key, kwid);
			}
		}

		CounterMap<String, Integer> keyClusters = Generics.newCounterMap();

		for (String key : keyKeywordMap.keySet()) {
			for (int kwid : keyKeywordMap.get(key)) {
				int cid = keywordToCluster[kwid];
				keyClusters.incrementCount(key, cid, 1);
			}
		}

		for (String key : keyClusters.keySet()) {
			Set<Integer> cids = keyClusters.getCounter(key).keySet();

			if (cids.size() > 1) {
				Counter<Integer> newCluster = Generics.newCounter();
				int new_cid = min(cids);

				for (int cid : cids) {
					Counter<Integer> c = clusters.removeKey(cid);
					if (c != null) {
						newCluster.incrementAll(c);
					}
				}

				clusters.setCounter(new_cid, newCluster);

				for (int kwid : newCluster.keySet()) {
					keywordToCluster[kwid] = new_cid;
				}

				for (int cid : cids) {
					clusterToCluster[cid] = new_cid;
				}
			}
		}
	}

	private void clusterUsingExactMatch() {
		System.out.println("cluster using exact match");
		SetMap<String, Integer> tm = Generics.newSetMap();

		for (int i = 0; i < kwdIndexer.size(); i++) {
			String kwd = kwdIndexer.getObject(i);
			kwd = kwd.replace("\t", "tab").replaceAll("[\\s\\p{Punct}]+", "").toLowerCase();
			tm.put(kwd, i);
		}

		for (String kwd : tm.keySet()) {
			Set<Integer> kwids = tm.get(kwd);

			if (kwids.size() > 1) {
				Set<Integer> cids = Generics.newHashSet();
				Counter<Integer> newCluster = Generics.newCounter();

				for (int kwid : kwids) {
					int cid = keywordToCluster[kwid];
					cids.add(cid);
					newCluster.incrementAll(clusters.removeKey(cid));
				}

				int new_cid = min(cids);

				clusters.setCounter(new_cid, newCluster);

				for (int kwid : kwids) {
					keywordToCluster[kwid] = new_cid;
				}

				for (int cid : cids) {
					clusterToCluster[cid] = new_cid;
				}
			}
		}

		// printClusters();
	}

	private void clusterUsingGramMatch() {
		System.out.println("cluster using gram match");

		Indexer<String> gramIndexer = buildGramIndexer();

		Map<Integer, SparseVector> cents = getCentroids(gramIndexer);

		int prefix_size = 6;

		int[][] gramPostings = buildGramPostings(cents, prefix_size, gramIndexer.size());

		int[] gram_freqs = getGramFreqs(cents, gramIndexer.size());

		computeWeights(cents, gram_freqs);

		double num_clusters = cents.size();

		StopWatch stopWatch = StopWatch.newStopWatch();

		double cutoff_cosine = 0.9;

		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			List<Integer> cluster = Generics.newArrayList(cents.keySet());

			int chunk_size = cluster.size() / 100;

			Counter<Pair<Integer, Integer>> queryResultPairs = Generics.newCounter();

			for (int j = 0; j < cluster.size(); j++) {
				if ((j + 1) % chunk_size == 0) {
					int progess = (int) ((j + 1f) / cluster.size() * 100);
					System.out.printf("\r[%dth, %d percent - %d/%d, %s]", i + 1, progess, j + 1, cluster.size(), stopWatch.stop());
				}

				int qid = cluster.get(j);

				SparseVector queryCent = cents.get(qid);
				Counter<Integer> candidates = Generics.newCounter();

				queryCent.sortByValue();

				for (int k = 0; k < queryCent.size() && k < prefix_size; k++) {
					int gid = queryCent.indexAtLoc(k);
					double idf = Math.log((num_clusters + 1f) / gram_freqs[gid]);
					for (int cluid : gramPostings[gid]) {
						int new_cluid = clusterToCluster[cluid];
						if (cents.containsKey(new_cluid)) {
							candidates.incrementCount(new_cluid, idf);
						}
					}
				}

				queryCent.sortByIndex();

				candidates.removeKey(qid);

				for (int cid : candidates.getSortedKeys()) {
					SparseVector targetCent = cents.get(cid);
					double cosine = VectorMath.dotProduct(queryCent, targetCent);

					if (cosine < cutoff_cosine) {
						break;
					}

					Pair<Integer, Integer> p1 = Generics.newPair(qid, cid);
					queryResultPairs.incrementCount(p1, cosine);
				}
			}

			System.out.printf("\r[%dth, %d percent - %d/%d, %s]\n", i + 1, 100, cluster.size(), cluster.size(), stopWatch.stop());

			CounterMap<Integer, Integer> queryResults = Generics.newCounterMap();
			Set<Integer> used = Generics.newHashSet();

			for (Pair<Integer, Integer> p : queryResultPairs.getSortedKeys()) {
				int qid = p.getFirst();
				int cid = p.getSecond();

				if (used.contains(cid)) {
					continue;
				}
				used.add(cid);

				double cosine = queryResultPairs.getCount(p);
				queryResults.setCount(qid, cid, cosine);
			}

			Iterator<Integer> iter = queryResults.keySet().iterator();

			while (iter.hasNext()) {
				int qid = iter.next();
				if (used.contains(qid)) {
					iter.remove();
				}
			}

			if (queryResults.size() == 0) {
				break;
			}

			for (int qid : queryResults.keySet()) {
				Set<Integer> cidSet = Generics.newHashSet();
				cidSet.add(qid);
				cidSet.addAll(queryResults.getCounter(qid).keySet());

				int new_cid = min(cidSet);

				// if (cidSet.size() > 1) {
				// System.out.println("###########################");
				// for (int cid2 : cidSet) {
				// String label = clusterLabel.get(cid2);
				// System.out.printf("Label:\t%s\n", label);
				//
				// for (int kwid : clusters.getCounter(cid2).keySet()) {
				// String kwd = kwdIndexer.getObject(kwid);
				// System.out.printf("Keyword:\t%d\t%s\n", kwid, kwd);
				// }
				//
				// System.out.println("-------------------------");
				// }
				// System.out.println("");
				// }
				//
				// System.out.printf("%d -> %d, %s\n", new_cid, qid,
				// queryOutputs.getCounter(qid).keySet());

				Counter<Integer> newCent = Generics.newCounter();
				Counter<Integer> newCluster = Generics.newCounter();

				for (int cid : cidSet) {
					VectorMath.add(cents.remove(cid), newCent);
					newCluster.incrementAll(clusters.removeKey(cid));
				}

				newCent.scale(1f / cidSet.size());

				cents.put(new_cid, VectorUtils.toSparseVector(newCent));

				clusters.setCounter(new_cid, newCluster);

				for (int kwid : newCluster.keySet()) {
					keywordToCluster[kwid] = new_cid;
				}

				for (int cid : cidSet) {
					clusterToCluster[cid] = new_cid;
				}
			}
		}

	}

	private Counter<String>[] computeLabelScores(Set<Integer> kwids) {
		GramGenerator gg = new GramGenerator(3);
		int num_langs = 2;

		Counter<String>[] ret = new Counter[num_langs];

		for (int i = 0; i < num_langs; i++) {
			CounterMap<String, Character> gramProbs = Generics.newCounterMap();

			Counter<Integer> c = Generics.newCounter();

			for (int kwid : kwids) {
				String keyword = kwdIndexer.getObject(kwid);
				String lang = keyword.split("\t")[i];
				if (lang.equals(NONE)) {
					continue;
				}
				int kw_freq = kwdData.getKeywordFreqs()[kwid];
				c.incrementCount(kwid, kw_freq);
			}

			Counter<Integer> backup = Generics.newCounter(c);

			c.pruneKeysBelowThreshold(2);

			if (c.size() == 0) {
				c = backup;
			}

			for (int kwid : c.keySet()) {
				String keyword = kwdIndexer.getObject(kwid);
				String lang = keyword.split("\t")[i];
				int kw_freq = (int) c.getCount(kwid);

				for (Gram g : gg.generateQGrams(lang.toLowerCase())) {
					gramProbs.incrementCount(g.getString().substring(0, 2), g.getString().charAt(2), kw_freq);
				}
			}

			gramProbs.normalize();

			Counter<String> kwdScores = Generics.newCounter();

			for (int kwid : c.keySet()) {
				String keyword = kwdIndexer.getObject(kwid);
				String lang = keyword.split("\t")[i];
				double log_likelihood = computeLoglikelihood(gg.generateQGrams(lang.toLowerCase()), gramProbs);
				kwdScores.incrementCount(lang, log_likelihood);
			}

			if (kwdScores.size() == 0) {
				kwdScores.setCount(NONE, 0);
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

	private void filter(int cutoff) {
		int old_size = clusters.size();

		Iterator<Integer> iter = clusters.keySet().iterator();
		while (iter.hasNext()) {
			int cid = iter.next();
			Counter<Integer> kwids = clusters.getCounter(cid);
			if (kwids.size() < cutoff) {
				kwids.clear();
				iter.remove();
			}
		}

		System.out.printf("filter using cutoff [%d]\n", cutoff);
		System.out.printf("clusters [%d -> %d]\n", old_size, clusters.size());
	}

	public Map<Integer, SparseVector> getCentroids(Indexer<String> gramIndexer) {
		Map<Integer, SparseVector> ret = Generics.newHashMap(clusters.size());
		for (int cid : clusters.keySet()) {
			Counter<Integer> gramCnts = Generics.newCounter();

			for (int kwid : clusters.getCounter(cid).keySet()) {
				String keyword = kwdIndexer.getObject(kwid);
				for (String lang : keyword.split("\t")) {
					if (lang.equals(NONE)) {
						continue;
					}

					for (Gram g : gg.generateQGrams(lang.toLowerCase())) {
						int gid = gramIndexer.indexOf(g.getString());
						if (gid < 0) {
							continue;
						}
						gramCnts.incrementCount(gid, 1);
					}
				}
			}

			if (gramCnts.size() == 0) {
				continue;
			}
			ret.put(cid, VectorUtils.toSparseVector(gramCnts));
		}

		System.out.printf("compute [%d] cluster centroids\n", ret.size());
		return ret;
	}

	private int[] getGramFreqs(Map<Integer, SparseVector> cents, int gram_size) {
		Counter<Integer> c = Generics.newCounter();
		for (int cid : cents.keySet()) {
			for (int kwid : cents.get(cid).indexes()) {
				c.incrementCount(kwid, 1);
			}
		}
		int[] ret = new int[gram_size];
		for (Entry<Integer, Double> e : c.entrySet()) {
			ret[e.getKey()] = e.getValue().intValue();
		}
		return ret;
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

	private void selectClusterLabels() {
		System.out.println("select cluster labels");

		clusterLabel = Generics.newHashMap();

		for (int cid : clusters.keySet()) {
			Set<Integer> kwids = clusters.getCounter(cid).keySet();
			Counter<String>[] scoreData = computeLabelScores(kwids);
			String korLabel = scoreData[0].argMax();
			String engLabel = scoreData[1].argMax();

			clusterLabel.put(cid, korLabel + "\t" + engLabel);
		}
	}

	public void writeClusterText(String fileName) {
		TextFileWriter writer = new TextFileWriter(fileName);

		writer.write(String.format("Clusters:\t%d", clusters.size()));
		writer.write(String.format("\nKeywords:\t%d", (int) clusters.totalCount()));

		List<Integer> cids = clusters.getRowCountSums().getSortedKeys();
		// List<Integer> cids = Generics.newArrayList();
		//
		// {
		// List<String> keys = Generics.newArrayList();
		//
		// Map<Integer, Integer> map = Generics.newHashMap();
		//
		// for (int cid : clusters.keySet()) {
		// int kwdid = clusters.getCounter(cid).argMax();
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

		for (int i = 0, n = 1; i < cids.size(); i++) {
			int cid = cids.get(i);

			StringBuffer sb = new StringBuffer();
			sb.append(String.format("No:\t%d", n));
			sb.append(String.format("\nID:\t%d", cid));
			sb.append(String.format("\nLabel:\t%s", clusterLabel.get(cid)));
			sb.append(String.format("\nKeywords:\t%d", clusters.getCounter(cid).size()));

			Counter<Integer> c = Generics.newCounter();

			for (int kwdid : clusters.getCounter(cid).keySet()) {
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

		System.out.printf("write [%d] clusters at [%s]\n", clusters.size(), fileName);
	}

}
