package ohs.eden.keyphrase;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.tartarus.snowball.ext.PorterStemmer;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.ir.weight.TermWeighting;
import ohs.math.ArrayMath;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.SetMap;
import ohs.types.StrPair;
import ohs.utils.Generics;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;
import ohs.utils.UnicodeUtils;

public class KeywordClusterer {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", KeywordClusterer.class.getName());

		KeywordData kwdData = new KeywordData();

		if (FileUtils.exists(KPPath.KEYWORD_DATA_SER_FILE)) {
			kwdData.read(KPPath.KEYWORD_DATA_SER_FILE);
		} else {
			kwdData.readText(KPPath.KEYWORD_DATA_FILE);
			kwdData.write(KPPath.KEYWORD_DATA_SER_FILE);
		}

		KeywordClusterer kc = new KeywordClusterer(kwdData);
		kc.setTitleData(FileUtils.readStrCounterMap(KPPath.TITLE_DATA_FILE));
		kc.cluster();
		kc.writeClusters(KPPath.KEYWORD_CLUSTER_FILE);

		kwdData.write(KPPath.KEYWORD_DATA_SER_FILE.replace("_data", "_data_clusters"));

		System.out.println("process ends.");
	}

	public static String normalize(String s) {
		return s.replaceAll("[\\p{Punct}\\s]+", "").toLowerCase().trim();
	}

	public static String normalizeEnglish(String s) {
		PorterStemmer stemmer = new PorterStemmer();
		StringBuffer sb = new StringBuffer();
		for (String word : StrUtils.splitPunctuations(s)) {
			stemmer.setCurrent(word.toLowerCase());
			stemmer.stem();
			sb.append(stemmer.getCurrent() + " ");
		}
		return sb.toString().trim();
	}

	private Map<Integer, SparseVector> kwdToWordCnts;

	private Indexer<String> wordIndexer;

	private KeywordData kwdData;

	private Indexer<StrPair> kwdIndexer;

	private SetMap<Integer, Integer> clusterToKwds;

	private int[] kwdToCluster;

	private Map<Integer, Integer> clusterToLabel;

	public KeywordClusterer(KeywordData kwdData) {
		this.kwdData = kwdData;

		kwdIndexer = kwdData.getKeywordIndexer();
	}

	public void cluster() throws Exception {
		kwdToCluster = new int[kwdIndexer.size()];

		clusterToKwds = Generics.newSetMap(kwdIndexer.size());

		for (int i = 0; i < kwdIndexer.size(); i++) {
			clusterToKwds.put(i, i);
			kwdToCluster[i] = i;
		}

		matchExactTwoLanguages();
		selectClusterLabels();
		writeClusters(KPPath.KEYWORD_CLUSTER_TEMP_DIR + "temp-01.txt.gz");

		matchExactKorean();
		selectClusterLabels();
		writeClusters(KPPath.KEYWORD_CLUSTER_TEMP_DIR + "temp-02.txt.gz");

		matchExactEnglish();
		selectClusterLabels();
		writeClusters(KPPath.KEYWORD_CLUSTER_TEMP_DIR + "temp-03.txt.gz");

		matchKoreanGrams();
		selectClusterLabels();
		writeClusters(KPPath.KEYWORD_CLUSTER_TEMP_DIR + "temp-04.txt.gz");

		matchEnglishGrams();
		selectClusterLabels();
		writeClusters(KPPath.KEYWORD_CLUSTER_TEMP_DIR + "temp-05.txt.gz");

		matchTitleContexts();
		selectClusterLabels();
		writeClusters(KPPath.KEYWORD_CLUSTER_TEMP_DIR + "temp-06.txt.gz");

		kwdData.setClusterLabel(clusterToLabel);
		kwdData.setClusters(clusterToKwds);
	}

	private Counter<Integer> computeKeywordScores(Set<Integer> kwdids) {
		Map<Integer, SparseVector> kwdCents = Generics.newHashMap();

		Indexer<String> wordIndexer = Generics.newIndexer();

		for (int kwdid : kwdids) {
			StrPair kwdp = kwdIndexer.getObject(kwdid);

			Counter<Integer> cc = Generics.newCounter();
			String[] two = kwdp.asArray();

			for (int i = 0; i < two.length; i++) {
				String key = two[i];

				Counter<String> c = Generics.newCounter();

				if (i == 0) {
					key = normalize(key);

					if (key.length() > 0) {
						c.incrementCount(key.charAt(0) + "", 1);
					}

					if (key.length() > 1) {
						for (int j = 1; j < key.length(); j++) {
							c.incrementCount(key.substring(j - 1, j), 1);
							c.incrementCount(key.charAt(j) + "", 1);
						}
					}
				} else {
					List<String> words = StrUtils.splitPunctuations(key.toLowerCase());

					if (words.size() > 0) {
						c.incrementCount(words.get(0) + "", 1);
					}

					if (words.size() > 1) {
						for (int j = 1; j < words.size(); j++) {
							c.incrementCount(StrUtils.join("_", words, j - 1, j + 1), 1);
							c.incrementCount(words.get(j), 1);
						}
					}
				}

				for (String word : c.keySet()) {
					cc.incrementCount(wordIndexer.getIndex(word), c.getCount(word));
				}
			}
			// cc.scale(kwd_freq);

			kwdCents.put(kwdid, VectorUtils.toSparseVector(cc));
		}

		TermWeighting.computeTFIDFs(kwdCents.values());

		SparseVector avgCent = VectorMath.average(kwdCents.values());

		Counter<Integer> ret = Generics.newCounter(kwdids.size());

		for (int kwdid : kwdCents.keySet()) {
			int kwd_freq = kwdData.getKeywordFreqs()[kwdid];
			ret.setCount(kwdid, kwd_freq * VectorMath.dotProduct(avgCent, kwdCents.get(kwdid), false));
		}
		ret.normalize();
		return ret;
	}

	private double computeSyllableCosine(int cid1, int cid2) {
		int[] cids = new int[] { cid1, cid2 };
		SparseVector[][] svss = new SparseVector[2][];

		for (int i = 0; i < cids.length; i++) {
			SparseVector[] svs = new SparseVector[2];

			Counter<Integer> c1 = new Counter<Integer>();
			Counter<Integer> c2 = new Counter<Integer>();

			for (int kwdid : clusterToKwds.get(cids[i])) {
				StrPair kwdp = kwdIndexer.getObject(kwdid);
				String korKey = normalize(kwdp.getFirst());
				String engKey = normalizeEnglish(kwdp.getSecond());

				int kwd_freq = kwdData.getKeywordFreqs()[kwdid];

				korKey = UnicodeUtils.decomposeToJamoStr(korKey);
				engKey = normalizeEnglish(engKey).replace(" ", "");

				for (int j = 0; j < korKey.length(); j++) {
					c1.incrementCount((int) korKey.charAt(j), kwd_freq);
				}

				for (int j = 0; j < engKey.length(); j++) {
					c2.incrementCount((int) engKey.charAt(j), kwd_freq);
				}
			}

			svs[0] = VectorUtils.toSparseVector(c1);
			svs[1] = VectorUtils.toSparseVector(c2);

			for (SparseVector sv : svs) {
				VectorMath.unitVector(sv);
			}
			svss[i] = svs;
		}

		double kor_cosine = VectorMath.dotProduct(svss[0][0], svss[1][0]);
		double eng_cosine = VectorMath.dotProduct(svss[0][1], svss[1][1]);
		double cosine = ArrayMath.addAfterScale(kor_cosine, 0.5, eng_cosine);
		return cosine;
	}

	private void matchEnglishGrams() {
		System.out.println("match English grams.");

		for (int iter = 0; iter < 10; iter++) {

			int old_size = clusterToKwds.size();

			Map<Integer, SparseVector> korCents = Generics.newHashMap();
			Map<Integer, SparseVector> engCents = Generics.newHashMap();

			SetMap<Integer, Integer> wordToClusters = Generics.newSetMap();
			SetMap<Integer, Integer> clusterToWords = Generics.newSetMap();

			Indexer<String> gramIndexer = Generics.newIndexer();

			GramGenerator gg = new GramGenerator(3);

			for (Entry<Integer, Set<Integer>> e : clusterToKwds.getEntrySet()) {
				int cid = e.getKey();
				Set<Integer> kwdids = e.getValue();
				Counter<String> engWordCnts = Generics.newCounter();
				Counter<String> korCharCnts = Generics.newCounter();
				Counter<String> engGramCnts = Generics.newCounter();

				for (int kwdid : kwdids) {
					StrPair kwdp = kwdIndexer.get(kwdid);
					String korKey = normalize(kwdp.getFirst());
					String engKey = normalizeEnglish(kwdp.getSecond());

					int kwd_freq = kwdData.getKeywordFreqs()[kwdid];

					for (char c : korKey.toCharArray()) {
						korCharCnts.incrementCount(c + "", kwd_freq);
					}

					for (String word : engKey.split(" ")) {
						engWordCnts.incrementCount(word, kwd_freq);
					}

					for (Gram g : gg.generateQGrams(engKey)) {
						engGramCnts.incrementCount(g.getString(), kwd_freq);
					}
				}

				if (engWordCnts.size() > 0 && engGramCnts.size() > 0) {
					Set<Integer> ws = Generics.newHashSet();

					for (int w : gramIndexer.getIndexes(engWordCnts.keySet())) {
						wordToClusters.put(w, cid);
						ws.add(w);
					}

					clusterToWords.put(cid, ws);
					korCents.put(cid, VectorUtils.toSparseVector(korCharCnts, gramIndexer, true));
					engCents.put(cid, VectorUtils.toSparseVector(engGramCnts, gramIndexer, true));
				}
			}

			TermWeighting.computeTFIDFs(korCents.values());

			TermWeighting.computeTFIDFs(engCents.values());

			StopWatch stopWatch = StopWatch.newStopWatch();

			CounterMap<Integer, Integer> queryToTargets = Generics.newCounterMap();

			List<Integer> cids = Generics.newArrayList(korCents.keySet());

			for (int i = 0; i < cids.size(); i++) {
				if ((i + 1) % 10000 == 0) {
					System.out.printf("\r[%d/%d, %s]", i + 1, cids.size(), stopWatch.stop());
				}

				int qcid = cids.get(i);
				SparseVector qKorCent = korCents.get(qcid);
				SparseVector qEngCent = engCents.get(qcid);

				// String qKwdStr = kwdIndexer.get(qcid);

				Counter<Integer> toCompare = Generics.newCounter();

				for (int w : clusterToWords.get(qcid)) {
					Set<Integer> set = wordToClusters.get(w, false);

					if (set != null) {
						for (int cid : set) {
							if (qcid == cid || queryToTargets.containKey(qcid, cid) || queryToTargets.containKey(cid, qcid)) {

							} else {
								toCompare.incrementCount(cid, 1);
							}
						}
					}
				}

				if (toCompare.size() < 2) {
					continue;
				}

				List<Integer> keys = toCompare.getSortedKeys();

				for (int j = 0; j < keys.size() && j < 100; j++) {
					int tcid = keys.get(j);
					// String tKwdStr = kwdIndexer.get(tcid);

					// if (queryToTargets.containKey(qcid, tcid) || queryToTargets.containKey(tcid, qcid)) {
					// continue;
					// }

					SparseVector tKorCent = korCents.get(tcid);
					SparseVector tEngCent = engCents.get(tcid);

					// if (tKorCent.size() > 0) {
					// double kor_cosine = VectorMath.dotProduct(qKorCent, tKorCent);
					// double eng_cosine = VectorMath.dotProduct(qEngCent, tEngCent);
					// double cosine = ArrayMath.addAfterScale(kor_cosine, 0.5, eng_cosine);
					//
					// if (cosine >= 0.9) {
					// queryToTargets.incrementCount(qcid, tcid, cosine);
					// queryToTargets.incrementCount(tcid, qcid, cosine);
					// }
					// } else {
					double eng_cosine = VectorMath.dotProduct(qEngCent, tEngCent);

					if (eng_cosine >= 0.9) {
						queryToTargets.incrementCount(qcid, tcid, eng_cosine);
						queryToTargets.incrementCount(tcid, qcid, eng_cosine);
					}
					// }
				}
			}

			System.out.printf("\r[%d/%d, %s]\n", cids.size(), cids.size(), stopWatch.stop());

			if (queryToTargets.size() == 0) {
				break;
			}

			Set<Integer> merged = Generics.newHashSet();

			for (int qcid : queryToTargets.keySet()) {
				if (merged.contains(qcid)) {
					continue;
				}

				Counter<Integer> targets = queryToTargets.getCounter(qcid);

				Set<Integer> toMerge = Generics.newHashSet();
				toMerge.add(qcid);

				for (int tcid : targets.keySet()) {
					if (!merged.contains(tcid)) {
						toMerge.add(tcid);
					}
				}

				if (toMerge.size() > 1) {
					merge(toMerge);

					merged.addAll(toMerge);
				}
			}

			int new_size = clusterToKwds.size();

			System.out.printf("[%d -> %d clusters]\n", old_size, new_size);

			selectClusterLabels();
			writeClusters(KPPath.KEYWORD_CLUSTER_TEMP_DIR + String.format("temp-eng-loop-%d.txt.gz", iter));

			korCents = null;
			engCents = null;

			wordToClusters = null;
			clusterToWords = null;

			gramIndexer = null;

			queryToTargets = null;
			cids = null;
		}
	}

	private void matchExactEnglish() {
		System.out.println("match exact English language.");

		int old_size = clusterToKwds.size();

		SetMap<String, Integer> keyToClusters = Generics.newSetMap();

		for (Entry<Integer, Set<Integer>> e : clusterToKwds.getEntrySet()) {
			int cid = e.getKey();
			Set<Integer> kwdids = e.getValue();

			boolean is_candidate = true;

			for (int kwdid : kwdids) {
				StrPair kwdp = kwdIndexer.getObject(kwdid);
				String korKey = normalize(kwdp.getFirst());
				String engKey = normalize(kwdp.getSecond());

				if (korKey.length() == 0 && engKey.length() > 0) {

				} else {
					is_candidate = false;
					break;
				}
			}

			if (is_candidate) {
				StrPair kwdp = kwdIndexer.getObject(cid);
				String korKey = normalize(kwdp.getFirst());
				String engKey = normalize(kwdp.getSecond());
				keyToClusters.put(engKey, cid);
			}
		}

		for (String key : keyToClusters.keySet()) {
			Set<Integer> cids = keyToClusters.get(key);

			if (cids.size() > 1) {
				//
				// for (int cid : cids) {
				// String kwdStr = kwdIndexer.getObject(cid);
				// String[] two = kwdStr.split("\t");
				// String korKey = normalize(two[0]);
				// String engKey = normalize(two[1]);
				//
				// System.out.println(cid + "\t" + kwdStr);
				// }
				// System.out.println("-------------------");
				merge(cids);
			}
		}

		int new_size = clusterToKwds.size();

		System.out.printf("[%d -> %d clusters]\n", old_size, new_size);
	}

	private void matchExactKorean() {
		System.out.println("match exact Korean language.");

		int old_size = clusterToKwds.size();

		SetMap<String, Integer> keyToClusters = Generics.newSetMap();

		for (Entry<Integer, Set<Integer>> e : clusterToKwds.getEntrySet()) {
			int cid = e.getKey();
			Set<Integer> kwdids = e.getValue();

			StrPair kwdp = kwdIndexer.getObject(cid);
			String korKey = normalize(kwdp.getFirst());

			if (korKey.length() > 0) {
				keyToClusters.put(korKey, cid);
			}
		}

		for (String key : keyToClusters.keySet()) {
			Set<Integer> cids = keyToClusters.get(key);

			if (cids.size() > 1) {
				merge(cids);
			}
		}

		int new_size = clusterToKwds.size();

		System.out.printf("[%d -> %d clusters]\n", old_size, new_size);
	}

	private void matchExactTwoLanguages() {
		System.out.println("match exact two languages.");

		int old_size = clusterToKwds.size();

		SetMap<String, Integer> keyToClusters = Generics.newSetMap();

		for (int cid : clusterToKwds.keySet()) {
			for (int kwdid : clusterToKwds.get(cid)) {
				StrPair kwdp = kwdIndexer.getObject(kwdid);
				String korKey = normalize(kwdp.getFirst());
				String engKey = normalizeEnglish(kwdp.getSecond()).replace(" ", "");
				String key = korKey + "\t" + engKey;
				keyToClusters.put(key, cid);
			}
		}

		for (String key : keyToClusters.keySet()) {
			Set<Integer> cids = keyToClusters.get(key);

			// if (cids.contains(178819) || cids.contains(489840)) {
			// System.out.printf(String.format("[%s, %d]\n", key, cids.size()));
			// }

			if (cids.size() > 1) {
				merge(cids);
			}
		}

		int new_size = clusterToKwds.size();

		System.out.printf("[%d -> %d clusters]\n", old_size, new_size);
	}

	private void matchKoreanGrams() {
		System.out.println("match korean grams.");

		for (int iter = 0; iter < 10; iter++) {

			int old_size = clusterToKwds.size();

			Map<Integer, SparseVector> korCents = Generics.newHashMap();
			Map<Integer, SparseVector> engCents = Generics.newHashMap();

			SetMap<Integer, Integer> gramToClusters = Generics.newSetMap();
			SetMap<Integer, Integer> clusterToGrams = Generics.newSetMap();

			Indexer<String> gramIndexer = Generics.newIndexer();

			GramGenerator gg = new GramGenerator(3);

			for (Entry<Integer, Set<Integer>> e : clusterToKwds.getEntrySet()) {
				int cid = e.getKey();
				Set<Integer> kwdids = e.getValue();
				Counter<String> korGramCnts = Generics.newCounter();
				Counter<String> korCharCnts = Generics.newCounter();
				Counter<String> engGramCnts = Generics.newCounter();

				for (int kwdid : kwdids) {
					StrPair kwdp = kwdIndexer.get(kwdid);
					String korKey = normalize(kwdp.getFirst());
					String engKey = normalizeEnglish(kwdp.getSecond());
					int kwd_freq = kwdData.getKeywordFreqs()[kwdid];

					if (!UnicodeUtils.isKorean(korKey)) {
						continue;
					}

					// String s = UnicodeUtils.decomposeToJamo(korKwd);

					for (char c : korKey.toCharArray()) {
						korCharCnts.incrementCount(c + "", kwd_freq);
					}

					for (Gram g : gg.generateQGrams(korKey)) {
						korGramCnts.incrementCount(g.getString(), kwd_freq);
					}

					for (Gram g : gg.generateQGrams(engKey)) {
						engGramCnts.incrementCount(g.getString(), kwd_freq);
					}
				}

				if (korGramCnts.size() > 0 && engGramCnts.size() > 0) {
					Set<Integer> gids = Generics.newHashSet();

					for (int gid : gramIndexer.getIndexes(korGramCnts.keySet())) {
						gramToClusters.put(gid, cid);
						gids.add(gid);
					}

					clusterToGrams.put(cid, gids);
					korCents.put(cid, VectorUtils.toSparseVector(korCharCnts, gramIndexer, true));
					engCents.put(cid, VectorUtils.toSparseVector(engGramCnts, gramIndexer, true));
				}
			}

			TermWeighting.computeTFIDFs(korCents.values());

			TermWeighting.computeTFIDFs(engCents.values());

			StopWatch stopWatch = StopWatch.newStopWatch();

			CounterMap<Integer, Integer> queryToTargets = Generics.newCounterMap();

			List<Integer> cids = Generics.newArrayList(korCents.keySet());

			for (int i = 0; i < cids.size(); i++) {
				if ((i + 1) % 10000 == 0) {
					System.out.printf("\r[%d/%d, %s]", i + 1, cids.size(), stopWatch.stop());
				}

				int qcid = cids.get(i);
				SparseVector qKorCent = korCents.get(qcid);
				SparseVector qEngCent = engCents.get(qcid);

				// String qKwdStr = kwdIndexer.get(qcid);

				Counter<Integer> toCompare = Generics.newCounter();

				for (int gid : clusterToGrams.get(qcid)) {
					Set<Integer> set = gramToClusters.get(gid, false);

					if (set != null) {
						for (int cid : set) {
							if (qcid == cid || queryToTargets.containKey(qcid, cid) || queryToTargets.containKey(cid, qcid)) {

							} else {
								toCompare.incrementCount(cid, 1);
							}
						}
					}
				}

				if (toCompare.size() < 2) {
					continue;
				}

				List<Integer> keys = toCompare.getSortedKeys();

				for (int j = 0; j < keys.size(); j++) {
					int tcid = keys.get(j);
					// String tKwdStr = kwdIndexer.get(tcid);

					// if (queryToTargets.containKey(qcid, tcid) || queryToTargets.containKey(tcid, qcid)) {
					// continue;
					// }

					SparseVector tKorCent = korCents.get(tcid);
					SparseVector tEngCent = engCents.get(tcid);

					double kor_cosine = VectorMath.dotProduct(qKorCent, tKorCent);
					double eng_cosine = VectorMath.dotProduct(qEngCent, tEngCent);
					double cosine = ArrayMath.addAfterScale(kor_cosine, 0.5, eng_cosine);

					if (cosine >= 0.9) {
						queryToTargets.incrementCount(qcid, tcid, cosine);
						queryToTargets.incrementCount(tcid, qcid, cosine);
					} else if (cosine >= 0.75) {
						double cosine2 = computeSyllableCosine(qcid, tcid);

						if (cosine2 >= 0.9) {
							queryToTargets.incrementCount(qcid, tcid, kor_cosine);
							queryToTargets.incrementCount(tcid, qcid, kor_cosine);
						}
					}
				}
			}

			System.out.printf("\r[%d/%d, %s]\n", cids.size(), cids.size(), stopWatch.stop());

			if (queryToTargets.size() == 0) {
				break;
			}

			Set<Integer> merged = Generics.newHashSet();

			for (int qcid : queryToTargets.keySet()) {
				if (merged.contains(qcid)) {
					continue;
				}

				Counter<Integer> targets = queryToTargets.getCounter(qcid);

				Set<Integer> toMerge = Generics.newHashSet();
				toMerge.add(qcid);

				for (int tcid : targets.keySet()) {
					if (!merged.contains(tcid)) {
						toMerge.add(tcid);
					}
				}

				if (toMerge.size() > 1) {
					merge(toMerge);

					merged.addAll(toMerge);
				}
			}

			int new_size = clusterToKwds.size();

			System.out.printf("[%d -> %d clusters]\n", old_size, new_size);

			selectClusterLabels();
			writeClusters(KPPath.KEYWORD_CLUSTER_TEMP_DIR + String.format("temp-kor-loop-%d.txt.gz", iter));

			korCents = null;
			engCents = null;

			gramToClusters = null;
			clusterToGrams = null;

			gramIndexer = null;

			queryToTargets = null;
			cids = null;
		}
	}

	private void matchTitleContexts() {
		System.out.println("match title contexts.");

		for (int iter = 0; iter < 10; iter++) {

			int old_size = clusterToKwds.size();

			Map<Integer, SparseVector> cents = Generics.newHashMap();

			SetMap<Integer, Integer> wordToClusters = Generics.newSetMap();

			for (Entry<Integer, Set<Integer>> e : clusterToKwds.getEntrySet()) {
				int cid = e.getKey();
				Set<Integer> kwdids = e.getValue();

				Counter<Integer> c = Generics.newCounter();

				for (int kwdid : kwdids) {
					StrPair kwdp = kwdIndexer.get(kwdid);
					SparseVector sv = kwdToWordCnts.get(kwdid);

					if (sv == null) {
						continue;
					}

					for (int w : sv.indexes()) {
						wordToClusters.put(w, cid);
					}
					VectorMath.add(sv, c);
				}

				cents.put(cid, VectorUtils.toSparseVector(c));
			}

			TermWeighting.computeTFIDFs(cents.values());

			StopWatch stopWatch = StopWatch.newStopWatch();

			CounterMap<Integer, Integer> queryToTargets = Generics.newCounterMap();

			List<Integer> cids = Generics.newArrayList(cents.keySet());

			for (int i = 0; i < cids.size(); i++) {
				if ((i + 1) % 10000 == 0) {
					System.out.printf("\r[%d/%d, %s]", i + 1, cids.size(), stopWatch.stop());
				}

				int qcid = cids.get(i);
				SparseVector qCent = cents.get(qcid);
				qCent.sortByValue();

				// String qKwdStr = kwdIndexer.get(qcid);

				Counter<Integer> toCompare = Generics.newCounter();

				int search_word_size = qCent.size() / 3;

				for (int j = 0; j < qCent.size() && j < search_word_size; j++) {
					int w = qCent.indexAtLoc(j);
					double weight = qCent.valueAtLoc(j);

					Set<Integer> set = wordToClusters.get(w, false);

					if (set != null) {
						for (int cid : set) {
							if (qcid == cid || queryToTargets.containKey(qcid, cid) || queryToTargets.containKey(cid, qcid)) {

							} else {
								toCompare.incrementCount(cid, weight);
							}
						}
					}
				}

				qCent.sortByIndex();

				if (toCompare.size() < 2) {
					continue;
				}

				List<Integer> keys = toCompare.getSortedKeys();

				for (int j = 0; j < keys.size() && j < 100; j++) {
					int tcid = keys.get(j);

					SparseVector tCent = cents.get(tcid);

					double cosine = VectorMath.dotProduct(qCent, tCent);

					if (cosine >= 0.9) {
						System.out.println(kwdIndexer.getObject(qcid));
						System.out.println(kwdIndexer.getObject(tcid));
						System.out.println();

						queryToTargets.incrementCount(qcid, tcid, cosine);
						queryToTargets.incrementCount(tcid, qcid, cosine);

					}
				}
			}

			System.out.printf("\r[%d/%d, %s]\n", cids.size(), cids.size(), stopWatch.stop());

			if (queryToTargets.size() == 0) {
				break;
			}

			Set<Integer> merged = Generics.newHashSet();

			for (int qcid : queryToTargets.keySet()) {
				if (merged.contains(qcid)) {
					continue;
				}

				Counter<Integer> targets = queryToTargets.getCounter(qcid);

				Set<Integer> toMerge = Generics.newHashSet();
				toMerge.add(qcid);

				for (int tcid : targets.keySet()) {
					if (!merged.contains(tcid)) {
						toMerge.add(tcid);
					}
				}

				if (toMerge.size() > 1) {
					merge(toMerge);

					merged.addAll(toMerge);
				}
			}

			int new_size = clusterToKwds.size();

			System.out.printf("[%d -> %d clusters]\n", old_size, new_size);

			selectClusterLabels();
			writeClusters(KPPath.KEYWORD_CLUSTER_TEMP_DIR + String.format("temp-title-loop-%d.txt.gz", iter));

			cents = null;

			wordToClusters = null;

			queryToTargets = null;
			cids = null;
		}
	}

	private void merge(Collection<Integer> cids) {
		Set<Integer> kwds = Generics.newHashSet();

		for (int cid : cids) {
			Set<Integer> temp = clusterToKwds.removeKey(cid);
			if (temp != null) {
				kwds.addAll(temp);
			}
		}

		int new_cid = min(cids);

		clusterToKwds.put(new_cid, kwds);

		for (int kwdid : kwds) {
			kwdToCluster[kwdid] = new_cid;
		}
	}

	private int min(Collection<Integer> set) {
		int ret = Integer.MAX_VALUE;
		for (int i : set) {
			if (i < ret) {
				ret = i;
			}
		}
		return ret;
	}

	private void selectClusterLabels() {
		System.out.println("select cluster labels.");

		clusterToLabel = Generics.newHashMap();

		for (int cid : clusterToKwds.keySet()) {
			Counter<Integer> kwdScores = computeKeywordScores(clusterToKwds.get(cid));

			List<Integer> kwdids = kwdScores.getSortedKeys();

			int label = -1;

			for (int i = 0; i < kwdids.size(); i++) {
				int kwdid = kwdids.get(i);
				int kwd_freq = kwdData.getKeywordFreqs()[kwdid];

				StrPair kwdp = kwdIndexer.get(kwdid);

				if (kwd_freq < 2) {
					continue;
				}

				String engKwd = kwdp.getSecond().replaceAll("[\\s\\p{Punct}]+", "");

				if (!StrUtils.isUppercase(engKwd)) {
					label = kwdid;
					break;
				}
			}

			if (label == -1) {
				clusterToLabel.put(cid, kwdScores.argMax());
			} else {
				clusterToLabel.put(cid, label);
			}

		}
	}

	public void setTitleData(CounterMap<String, String> cm) {
		kwdToWordCnts = Generics.newHashMap();
		wordIndexer = Generics.newIndexer();

		for (int kwdid : kwdData.getKeywordToDocs().keySet()) {
			Counter<String> c = Generics.newCounter();
			for (int docid : kwdData.getKeywordToDocs().get(kwdid)) {
				String cn = kwdData.getDocIndexer().getObject(docid);
				c.incrementAll(cm.getCounter(cn));
			}
			if (c.size() > 0) {
				kwdToWordCnts.put(kwdid, VectorUtils.toSparseVector(c, wordIndexer, true));
			}
		}
	}

	public void writeClusters(String fileName) {
		TextFileWriter writer = new TextFileWriter(fileName);

		writer.write(String.format("Clusters:\t%d", clusterToKwds.size()));
		writer.write(String.format("\nKeywords:\t%d", clusterToKwds.sizeOfItems()));

		List<Integer> cids = Generics.newArrayList();

		boolean sortAphabetically = false;

		if (sortAphabetically) {
			List<String> keys = Generics.newArrayList();

			for (int cid : clusterToKwds.keySet()) {
				keys.add(kwdIndexer.getObject(cid).join("\t"));
			}

			Collections.sort(keys);

			cids = Generics.newArrayList();
			for (int i = 0; i < keys.size(); i++) {
				int kwdid = kwdIndexer.indexOf(keys.get(i));
				cids.add(kwdid);
			}
		} else {
			Counter<Integer> c = Generics.newCounter();

			for (int cid : clusterToKwds.keySet()) {
				c.incrementCount(cid, clusterToKwds.get(cid).size());
			}
			cids = c.getSortedKeys();
		}

		// {
		// List<Integer> tmp = Generics.newArrayList();
		// int target = 0;
		// for (int i = 0; i < cids.size(); i++) {
		// int cid = cids.get(i);
		// String kwd = getKoreanKeyword(cid);
		// if (kwd.length() == 0) {
		// target = cid;
		// } else {
		// tmp.add(cid);
		// }
		// }
		// tmp.add(target);
		// cids = tmp;
		// }

		for (int i = 0, n = 1; i < cids.size(); i++) {
			int cid = cids.get(i);
			int label = clusterToLabel.get(cid);
			StrPair kwdp = kwdIndexer.getObject(label);

			StringBuffer sb = new StringBuffer();
			sb.append(String.format("No:\t%d", n));
			sb.append(String.format("\nID:\t%d", cid));

			sb.append(String.format("\nLabel:\n%d\t%s", label, kwdp.join("\t")));
			sb.append(String.format("\nKeywords:\t%d", clusterToKwds.get(cid).size()));

			Counter<Integer> c = Generics.newCounter();

			for (int kwdid : clusterToKwds.get(cid)) {
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

		System.out.printf("write [%d] clusters at [%s]\n", clusterToKwds.size(), fileName);
	}

}
