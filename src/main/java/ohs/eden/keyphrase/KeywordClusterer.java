package ohs.eden.keyphrase;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.io.EncodingPrintWriter.out;

import java.util.Set;

import ohs.eden.linker.Entity;
import ohs.eden.linker.EntityLinker;
import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
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
		System.out.println("process begins.");

		KeywordData rkd = new KeywordData();

		if (FileUtils.exists(KPPath.KEYWORD_FILE.replace("txt", "ser"))) {
			rkd.read(KPPath.KEYWORD_FILE.replace("txt", "ser"));
		} else {
			rkd.readFromText(KPPath.KEYWORD_FILE);
			rkd.write(KPPath.KEYWORD_FILE.replace("txt", "ser"));
		}

		KeywordClusterer kc = new KeywordClusterer(rkd);
		kc.cluster();
		kc.write(KPPath.KEYWORD_CLUSTER_FILE);

		System.out.println("process ends.");
	}

	private static String normalize(String s) {
		return s.replaceAll("[\\s\\p{Punct}&&[^<>]]+", "").toLowerCase();
	}

	private KeywordData kwdData;

	private Indexer<String> kwdIndexer;

	private CounterMap<Integer, Integer> clusterKwds;

	private Map<Integer, Integer> kwCluster;

	private Map<Integer, String> clusterLabel;

	private GramGenerator gg = new GramGenerator(3);

	public KeywordClusterer(KeywordData kwData) {
		this.kwdData = kwData;

		kwdIndexer = kwData.getKeywordIndexer();
	}

	private Indexer<String> buildGramIndexer() {
		Indexer<String> gramIndexer = Generics.newIndexer();

		for (int cid : clusterKwds.keySet()) {
			for (int kwid : clusterKwds.getCounter(cid).keySet()) {
				String keyword = kwdIndexer.getObject(kwid);
				for (String lang : keyword.split("\t")) {
					if (lang.equals(NONE)) {
						continue;
					}

					for (Gram g : gg.generate(String.format("<%s>", lang.toLowerCase()))) {
						gramIndexer.getIndex(g.getString());
					}
				}
			}
		}

		System.out.printf("build [%d] gram indexer\n", gramIndexer.size());
		return gramIndexer;
	}

	private SetMap<Integer, Integer> buildGramPostings(CounterMap<Integer, Integer> cents, int prefix_size) {
		SetMap<Integer, Integer> ret = Generics.newSetMap();

		for (int cid : cents.keySet()) {
			List<Integer> gids = cents.getCounter(cid).getSortedKeys();
			for (int i = 0; i < gids.size() && i < prefix_size; i++) {
				int gid = gids.get(i);
				ret.put(gid, cid);
			}
		}
		System.out.printf("build [%d] gram postings\n", ret.size());
		return ret;
	}

	public void cluster() throws Exception {
		clusterKwds = Generics.newCounterMap();

		kwCluster = Generics.newHashMap();

		for (int i = 0; i < kwdIndexer.size(); i++) {
			clusterKwds.setCount(i, i, 1);
			kwCluster.put(i, i);
		}

		clusterUsingExactMatch();

		clusterUsingExactLanguageMatch(false);

		// filter(3);

		selectClusterLabels();

		clusterUsingGramMatch();

		selectClusterLabels();
	}

	private void clusterUsingExactLanguageMatch(boolean isEnglish) {
		System.out.println("cluster using exact language match");

		SetMap<String, Integer> keyKeywordMap = Generics.newSetMap();

		for (Entry<Integer, Counter<Integer>> e : clusterKwds.getEntrySet()) {

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

		Counter<Integer> kwClusterFreqs = Generics.newCounter();

		for (String key : keyKeywordMap.keySet()) {
			for (int kwid : keyKeywordMap.get(key)) {
				int cid = kwCluster.get(kwid);
				kwClusterFreqs.incrementCount(cid, 1);
			}
		}

		CounterMap<String, Integer> keyClusterMap = Generics.newCounterMap();

		for (String key : keyKeywordMap.keySet()) {
			for (int kwid : keyKeywordMap.get(key)) {
				int cid = kwCluster.get(kwid);
				// double kw_cluster_freq = kwClusterFreqs.getCount(cid);
				// if (kw_cluster_freq > 1) {
				// System.out.println(key + "\t" + kw_cluster_freq);
				// continue;
				// }

				// if (visited.contains(cid)) {
				// continue;
				// }
				// visited.add(cid);
				keyClusterMap.incrementCount(key, cid, 1);
			}
		}

		// System.out.println(keyClusterMap.invert());
		// System.out.println();

		for (String key : keyClusterMap.keySet()) {
			Set<Integer> cids = keyClusterMap.getCounter(key).keySet();

			if (cids.size() > 1) {
				Counter<Integer> newCluster = Generics.newCounter();
				int new_cid = min(cids);

				for (int cid : cids) {
					Counter<Integer> c = clusterKwds.removeKey(cid);
					if (c != null) {
						newCluster.incrementAll(c);
					}
				}

				for (int kwid : newCluster.keySet()) {
					clusterKwds.setCount(new_cid, kwid, 1);
					kwCluster.put(kwid, new_cid);
				}
			}
		}
	}

	private void clusterUsingExactMatch() {
		System.out.println("cluster using exact match");
		SetMap<String, Integer> tm = Generics.newSetMap();

		for (int i = 0; i < kwdIndexer.size(); i++) {
			String keyword = kwdIndexer.getObject(i);
			keyword = keyword.replace("\t", "tab").replaceAll("[\\s\\p{Punct}]+", "").toLowerCase();
			tm.put(keyword, i);
		}

		for (String keyword : tm.keySet()) {
			List<Integer> kwids = Generics.newArrayList(tm.get(keyword));

			if (kwids.size() > 1) {
				Set<Integer> cids = Generics.newHashSet();
				Counter<Integer> newCluster = Generics.newCounter();

				for (int i = 0; i < kwids.size(); i++) {
					int kwid = kwids.get(i);
					int cid = kwCluster.get(kwid);
					cids.add(cid);
					newCluster.incrementAll(clusterKwds.removeKey(cid));
				}

				int new_cid = min(cids);

				for (int kwid : kwids) {
					clusterKwds.setCount(new_cid, kwid, 1);
					kwCluster.put(kwid, new_cid);
				}
			}
		}

		// printClusters();
	}

	private void clusterUsingGramMatch() {
		System.out.println("cluster using ngrams");

		Indexer<String> gramIndexer = buildGramIndexer();

		CounterMap<Integer, Integer> cents = getCentroids(gramIndexer);

		int prefix_size = 6;

		SetMap<Integer, Integer> gramPostings = buildGramPostings(cents, prefix_size);

		int[] gram_freqs = getGramFreqs(cents, gramIndexer.size());

		computeWeights(cents, gram_freqs);

		double num_clusters = cents.size();

		StopWatch stopWatch = StopWatch.newStopWatch();

		double cutoff_cosine = 0.9;

		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			List<Integer> cluster = Generics.newArrayList(cents.keySet());

			int chunk_size = cluster.size() / 100;

			Counter<Pair<Integer, Integer>> queryOutputPairs = Generics.newCounter();
			Set<Integer> searched = Generics.newHashSet();

			for (int j = 0; j < cluster.size(); j++) {
				if ((j + 1) % chunk_size == 0) {
					int progess = (int) ((j + 1f) / cluster.size() * 100);
					System.out.printf("\r[%dth, %d percent - %d/%d, %s]", i + 1, progess, j + 1, cluster.size(),
							stopWatch.stop());
				}

				int qid = cluster.get(j);

				if (searched.contains(qid)) {
					continue;
				}

				Counter<Integer> queryCent = cents.getCounter(qid);
				Counter<Integer> outputs = Generics.newCounter();
				List<Integer> gids = queryCent.getSortedKeys();

				for (int k = 0; k < gids.size() && k < prefix_size; k++) {
					int gid = gids.get(k);
					double idf = Math.log((num_clusters + 1f) / gram_freqs[gid]);
					for (int cid : gramPostings.get(gid)) {
						outputs.incrementCount(cid, idf);
					}
				}

				outputs.removeKey(qid);

				searched.add(qid);
				searched.addAll(outputs.keySet());

				for (int cid : outputs.getSortedKeys()) {
					double cosine = cents.getCounter(cid).dotProduct(queryCent);

					if (cosine < cutoff_cosine) {
						break;
					}

					Pair<Integer, Integer> p1 = Generics.newPair(qid, cid);
					queryOutputPairs.incrementCount(p1, cosine);
				}
			}

			System.out.printf("\r[%dth, %d percent - %d/%d, %s]\n", i + 1, 100, cluster.size(), cluster.size(),
					stopWatch.stop());

			CounterMap<Integer, Integer> queryOutputs = Generics.newCounterMap();
			Set<Integer> used = Generics.newHashSet();

			for (Pair<Integer, Integer> p : queryOutputPairs.getSortedKeys()) {
				int qid = p.getFirst();
				int cid = p.getSecond();

				if (used.contains(cid)) {
					continue;
				}
				used.add(cid);

				double cosine = queryOutputPairs.getCount(p);
				queryOutputs.setCount(qid, cid, cosine);
			}

			Iterator<Integer> iter = queryOutputs.keySet().iterator();

			while (iter.hasNext()) {
				int qid = iter.next();
				if (used.contains(qid)) {
					iter.remove();
				}
			}

			if (queryOutputs.size() == 0) {
				break;
			}

			for (int qid : queryOutputs.keySet()) {
				Counter<Integer> newCent = Generics.newCounter();
				Counter<Integer> newCluster = Generics.newCounter();

				Set<Integer> cidSet = Generics.newHashSet();
				cidSet.add(qid);
				cidSet.addAll(queryOutputs.getCounter(qid).keySet());

				int new_cid = min(cidSet);

				// if (cidSet.size() > 1) {
				// System.out.println("###########################");
				// for (int cid2 : cidSet) {
				// String label = clusterLabel.get(cid2);
				// System.out.printf("Label:\t%s\n", label);
				//
				// for (int kwid : clusterKwds.getCounter(cid2).keySet()) {
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

				for (int cid : cidSet) {
					newCent.incrementAll(cents.removeKey(cid));
					newCluster.incrementAll(clusterKwds.removeKey(cid));
				}

				newCent.scale(1f / cidSet.size());

				cents.setCounter(new_cid, newCent);

				clusterKwds.setCounter(new_cid, newCluster);
				for (int kwid : newCluster.keySet()) {
					kwCluster.put(kwid, new_cid);
				}

				for (int cid : cidSet) {
					gramPostings.replaceAll(cid, new_cid);
				}
			}
		}

	}

	private void clusterUsingStringSearcher() throws Exception {

		List<Entity> ents = Generics.newArrayList(clusterKwds.size());
		List<String[]> entVariants = Generics.newArrayList(clusterKwds.size());

		for (int cid : clusterKwds.keySet()) {
			// String label = clusterLabel.get(cid);
			Set<Integer> kwids = clusterKwds.getCounter(cid).keySet();
			Set<String> vars = Generics.newHashSet();

			for (int kwid : kwids) {
				String keyword = kwdIndexer.getObject(kwid);
				String[] langs = keyword.split("\t");
				for (String lang : langs) {
					if (lang.equals(NONE)) {
						continue;
					}
					vars.add(lang);
				}
			}

			ents.add(new Entity(cid, "", null));
			entVariants.add(vars.toArray(new String[vars.size()]));
		}

		EntityLinker entLinker = new EntityLinker();
		entLinker.train(ents, entVariants);

		CounterMap<Integer, Integer> cm = Generics.newCounterMap();

		for (int cid : clusterKwds.keySet()) {
			Set<Integer> kwids = clusterKwds.getCounter(cid).keySet();
			Set<String> input = Generics.newHashSet();

			for (int kwid : kwids) {
				String keyword = kwdIndexer.getObject(kwid);
				String[] langs = keyword.split("\t");
				for (String lang : langs) {
					if (lang.equals(NONE)) {
						continue;
					}
					input.add(lang.toLowerCase());
				}
			}

			Counter<Entity> c = Generics.newCounter();

			for (String s : input) {
				c.incrementAll(entLinker.link(s));
			}

			for (Entry<Entity, Double> e : c.entrySet()) {
				cm.incrementCount(cid, e.getKey().getId(), e.getValue());
			}
		}

	}

	private Counter<String>[] computeLabelScores(Set<Integer> kwids) {
		GramGenerator gg = new GramGenerator(3);
		int num_langs = 2;

		Counter<String>[] ret = new Counter[num_langs];

		for (int i = 0; i < num_langs; i++) {
			CounterMap<String, Character> ngramProbs = Generics.newCounterMap();

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

				for (Gram g : gg.generate(lang.toLowerCase())) {
					ngramProbs.incrementCount(g.getString().substring(0, 2), g.getString().charAt(2), kw_freq);
				}
			}

			ngramProbs.normalize();

			Counter<String> kwScores = Generics.newCounter();

			for (int kwid : c.keySet()) {
				String keyword = kwdIndexer.getObject(kwid);
				String lang = keyword.split("\t")[i];
				double log_likelihood = computeLoglikelihood(gg.generate(lang.toLowerCase()), ngramProbs);
				kwScores.incrementCount(lang, log_likelihood);
			}

			if (kwScores.size() == 0) {
				kwScores.setCount(NONE, 0);
			}

			double max = kwScores.max();
			double score_sum = 0;

			for (String lang : kwScores.keySet()) {
				double score = kwScores.getCount(lang);
				score = Math.exp(score - max);
				kwScores.setCount(lang, score);
				score_sum += score;
			}
			kwScores.scale(1f / score_sum);
			ret[i] = kwScores;
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

	private void computeWeights(CounterMap<Integer, Integer> cents, int[] gram_freqs) {
		double num_clusters = cents.size();

		for (int cid : cents.keySet()) {
			Counter<Integer> cent = cents.getCounter(cid);
			double norm = 0;
			for (Entry<Integer, Double> e : cent.entrySet()) {
				int gid = e.getKey();
				double cnt = e.getValue();
				double tf = Math.log(cnt) + 1;
				double gram_freq = gram_freqs[gid];
				double idf = gram_freq == 0 ? 0 : Math.log((num_clusters + 1) / gram_freq);
				double tfidf = tf * idf;
				cent.setCount(gid, tfidf);
				norm += (tfidf * tfidf);
			}
			norm = Math.sqrt(norm);
			cent.scale(1f / norm);
		}

	}

	private void filter(int cutoff) {
		int old_size = clusterKwds.size();

		Iterator<Integer> iter = clusterKwds.keySet().iterator();
		while (iter.hasNext()) {
			int cid = iter.next();
			Counter<Integer> kwids = clusterKwds.getCounter(cid);
			if (kwids.size() < cutoff) {
				kwids.clear();
				iter.remove();
			}
		}

		System.out.printf("filter using cutoff [%d]\n", cutoff);
		System.out.printf("clusters [%d -> %d]\n", old_size, clusterKwds.size());
	}

	public CounterMap<Integer, Integer> getCentroids(Indexer<String> gramIndexer) {
		CounterMap<Integer, Integer> ret = Generics.newCounterMap(clusterKwds.size());
		for (int cid : clusterKwds.keySet()) {
			Counter<Integer> gramCnts = Generics.newCounter();

			for (int kwid : clusterKwds.getCounter(cid).keySet()) {
				String keyword = kwdIndexer.getObject(kwid);
				for (String lang : keyword.split("\t")) {
					if (lang.equals(NONE)) {
						continue;
					}

					for (Gram g : gg.generate(String.format("<%s>", lang.toLowerCase()))) {
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
			ret.setCounter(cid, gramCnts);
		}

		System.out.printf("compute [%d] cluster centroids\n", ret.size());
		return ret;
	}

	private int[] getGramFreqs(CounterMap<Integer, Integer> cents, int gram_size) {
		Counter<Integer> c = Generics.newCounter();
		for (int cid : cents.keySet()) {
			for (int kwid : cents.getCounter(cid).keySet()) {
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

	private void printClusters() {
		System.out.printf("Clusters:\t%d\n", clusterKwds.size());

		List<Integer> cids = Generics.newArrayList();
		for (int cid : clusterKwds.keySet()) {
			Counter<Integer> kwids = clusterKwds.getCounter(cid);

			if (kwids.size() > 1) {
				cids.add(cid);
			}
		}

		for (int i = 0; i < 10 && i < cids.size(); i++) {
			int cid = cids.get(i);

			Counter<Integer> kwids = clusterKwds.getCounter(cid);

			List<Integer> temp = kwids.getSortedKeys();

			StringBuffer sb = new StringBuffer();
			sb.append(String.format("Cluster ID:\t%d", cid));

			for (int j = 0; j < temp.size(); j++) {
				int kwid = temp.get(j);
				sb.append(String.format("\n%d:\t%s", j + 1, kwdIndexer.getObject(kwid)));
			}

			System.out.println(sb.toString() + "\n");
		}
		System.out.println();
	}

	private void selectClusterLabels() {
		System.out.println("select cluster labels");

		clusterLabel = Generics.newHashMap();

		for (int cid : clusterKwds.keySet()) {
			Set<Integer> kwids = clusterKwds.getCounter(cid).keySet();
			Counter<String>[] scoreData = computeLabelScores(kwids);
			String korLabel = scoreData[0].argMax();
			String engLabel = scoreData[1].argMax();

			clusterLabel.put(cid, korLabel + "\t" + engLabel);
		}
	}

	public void write(String fileName) {
		TextFileWriter writer = new TextFileWriter(fileName);

		writer.write(String.format("Clusters:\t%d", clusterKwds.size()));
		writer.write(String.format("\nKeywords:\t%d", (int) clusterKwds.totalCount()));

		// List<Integer> cids = clusterKwds.getInnerCountSums().getSortedKeys();
		List<Integer> cids = Generics.newArrayList();

		{
			List<String> keys = Generics.newArrayList();

			Map<Integer, Integer> map = Generics.newHashMap();

			for (int cid : clusterKwds.keySet()) {
				int kwid = clusterKwds.getCounter(cid).argMax();
				map.put(kwid, cid);
				String kwd = kwdIndexer.getObject(kwid);
				keys.add(kwd);
			}

			Collections.sort(keys);

			cids = Generics.newArrayList();
			for (int i = 0; i < keys.size(); i++) {
				int kwid = kwdIndexer.indexOf(keys.get(i));
				int cid = map.get(kwid);
				cids.add(cid);
			}
		}

		// int cutoff = 10;

		for (int i = 0, n = 1; i < cids.size(); i++) {
			int cid = cids.get(i);

			StringBuffer sb = new StringBuffer();
			sb.append(String.format("C-Number:\t%d", n));
			sb.append(String.format("\nC-ID:\t%d", cid));
			sb.append(String.format("\nC-Label:\t%s", clusterLabel.get(cid)));
			sb.append(String.format("\nKeywords:\t%d", clusterKwds.getCounter(cid).size()));

			Counter<Integer> c = Generics.newCounter();

			for (int kwid : clusterKwds.getCounter(cid).keySet()) {
				c.setCount(kwid, kwdData.getKeywordFreqs()[kwid]);
			}

			// if (c.size() < cutoff) {
			// continue;
			// }

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

		System.out.printf("write [%d] clusters at [%s]\n", clusterKwds.size(), fileName);
	}

}
