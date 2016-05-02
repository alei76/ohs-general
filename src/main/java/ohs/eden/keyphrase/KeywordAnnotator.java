package ohs.eden.keyphrase;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.weight.TermWeighting;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.tree.trie.hash.Trie.TSResult;
import ohs.tree.trie.hash.Trie.TSResult.MatchType;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.SetMap;
import ohs.types.StrPair;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class KeywordAnnotator {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		KeywordData kwdData = new KeywordData();

		kwdData.read(KPPath.KEYWORD_DATA_SER_FILE.replace("_data", "_data_clusters"));

		KeywordAnnotator kwdAnno = new KeywordAnnotator(kwdData);

		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_FILE);
		reader.setPrintNexts(false);

		TextFileWriter writer = new TextFileWriter(KPPath.KEYWORD_PATENT_FILE);

		List<String> labels = Generics.newArrayList();
		int num_docs = 0;

		while (reader.hasNext()) {
			reader.print(100000);

			// if (num_docs > 10000) {
			// break;
			// }

			String line = reader.next();
			String[] parts = line.split("\t");

			if (reader.getNumLines() == 1) {
				for (String p : parts) {
					labels.add(p);
				}
			} else {
				if (parts.length != labels.size()) {
					continue;
				}

				for (int j = 0; j < parts.length; j++) {
					if (parts[j].length() > 1) {
						parts[j] = parts[j].substring(1, parts[j].length() - 1);
					}
				}

				String type = parts[0];
				String cn = parts[1];
				String korKwdStr = parts[2];
				String engKwdStr = parts[3];
				String korTitle = parts[4];
				String engTitle = parts[5];
				String korAbs = parts[6];
				String engAbs = parts[7];

				if (!type.equals("patent")) {
					continue;
				}

				Set<Integer> kwdids = kwdAnno.annotateKorean(korTitle + "\n" + korAbs);

				// StringBuffer sb = new StringBuffer();
				// for (int i = 0; i < labels.size(); i++) {
				// String label = labels.get(i);
				// String value = parts[i];
				// sb.append(String.format("%s:\t%s\n", label, value));
				// }
				//
				// sb.append(String.format("KOW KWDS:\t%s\n", cs[0].toString(cs[0].size())));
				// sb.append(String.format("ENG KWDS:\t%s", cs[1].toString(cs[1].size())));
				// writer.write(sb.toString() + "\n\n");

				// writer.write(String.format("\"%s\"\t\"%s\"\n", cn, String.join(";", cs[0].keySet())));

				// if (++num_docs > 1000) {
				// break;
				// }
			}
		}
		reader.printLast();
		reader.close();
		writer.close();

		System.out.printf("ends.");
	}

	private KeywordData kwdData;

	private Trie<Character> korDict;

	private Trie<String> engDict;

	public KeywordAnnotator(KeywordData kwdData) {
		this.kwdData = kwdData;

		buildDicts();

		buildCentroids();
	}

	public Set<Integer> searchKorean(String text) {
		text = KeywordClusterer.normalize(text);

		Character[] cs = StrUtils.asCharacters(text);
		Set<Integer> kwdids = Generics.newHashSet();
		for (int start = 0; start < cs.length; start++) {
			TSResult<Character> sr = korDict.find(cs, start);

			if (sr.getMatchType() == MatchType.FAIL) {
				start++;
			} else {
				int end = sr.getMatchLoc() + 1;

				StringBuffer sb = new StringBuffer();

				for (int j = start; j < end; j++) {
					sb.append(cs[j].charValue());
				}

				String s = sb.toString();

				Set<Integer> set = (Set<Integer>) sr.getMatchNode().getData();

				kwdids.addAll(set);

				start = end - 1;
			}
		}
		return kwdids;
	}

	public Set<Integer> annotateKorean(String text) {
		text = KeywordClusterer.normalize(text);

		Character[] cs = StrUtils.asCharacters(text);
		Set<Integer> kwdids = Generics.newHashSet();
		for (int start = 0; start < cs.length; start++) {
			TSResult<Character> sr = korDict.find(cs, start);

			if (sr.getMatchType() == MatchType.FAIL) {
				start++;
			} else {
				int end = sr.getMatchLoc() + 1;

				StringBuffer sb = new StringBuffer();

				for (int j = start; j < end; j++) {
					sb.append(cs[j].charValue());
				}

				String s = sb.toString();

				Set<Integer> set = (Set<Integer>) sr.getMatchNode().getData();

				kwdids.addAll(set);

				start = end - 1;
			}
		}

		if (kwdids.size() == 0) {
			return kwdids;
		}

		SetMap<Integer, Integer> clusterToKwds = Generics.newSetMap();

		for (int kwdid : kwdids) {
			StrPair kwdp = kwdData.getKeywordIndexer().getObject(kwdid);
			System.out.println(kwdp);

			clusterToKwds.put(kwdToCluster.get(kwdid), kwdid);
		}

		if (clusterToKwds.size() == 1) {
			return kwdids;
		}

		SparseVector input = null;

		{
			Counter<String> c = Generics.newCounter();

			for (Gram g : gg.generateQGrams(text)) {
				c.incrementCount(g.getString(), 1);
			}

			input = VectorUtils.toSparseVector(c, featIndexer, false);

			VectorMath.unitVector(input);
		}

		Counter<Integer> ret = Generics.newCounter();

		for (int cid : clusterToKwds.keySet()) {
			SparseVector cent = cents.get(cid);
			if (cent == null) {
				continue;
			}

			double cosine = VectorMath.cosine(input, cent, false);
			ret.setCount(cid, cosine);
		}

		if (ret.size() > 0) {
			int cid = ret.argMax();

			for (int kwdid : clusterToKwds.get(cid)) {
				StrPair kwdp = kwdData.getKeywordIndexer().getObject(kwdid);
				System.out.println(kwdp);
			}

			return clusterToKwds.get(cid);
		} else {
			return kwdids;
		}
	}

	private Map<Integer, Integer> kwdToCluster;

	private Indexer<String> featIndexer;

	private GramGenerator gg = new GramGenerator(3);

	private Map<Integer, SparseVector> cents;

	private void buildCentroids() {
		featIndexer = Generics.newIndexer();

		cents = Generics.newHashMap();

		for (int cid : kwdData.getClusterToKeywords().keySet()) {

			Counter<String> c = Generics.newCounter();

			for (int kwdid : kwdData.getClusterToKeywords().get(cid)) {
				kwdToCluster.put(kwdid, cid);

				StrPair kwdp = kwdData.getKeywordIndexer().getObject(kwdid);

				String korKwd = kwdp.getFirst();
				String engKwd = kwdp.getSecond();

				String korKey = KeywordClusterer.normalize(korKwd);
				String engKey = KeywordClusterer.normalizeEnglish(engKwd);

				for (Gram g : gg.generateQGrams(korKey)) {
					c.incrementCount(g.getString(), 1);
				}

				for (String word : engKey.split(" ")) {
					c.incrementCount(word, 1);
				}

				cents.put(cid, VectorUtils.toSparseVector(c, featIndexer, true));
			}
		}

		TermWeighting.computeTFIDFs(cents.values());
	}

	private void buildDicts() {

		kwdToCluster = Generics.newHashMap(kwdData.getKeywordIndexer().size());

		for (int cid : kwdData.getClusterToKeywords().keySet()) {
			for (int kwdid : kwdData.getClusterToKeywords().get(cid)) {
				kwdToCluster.put(kwdid, cid);
			}
		}

		korDict = new Trie<Character>();
		engDict = new Trie<String>();

		for (int kwdid : kwdData.getKeywords()) {
			int kwd_freq = kwdData.getKeywordFreqs()[kwdid];

			if (kwd_freq < 10) {
				continue;
			}

			StrPair kwdp = kwdData.getKeywordIndexer().getObject(kwdid);

			// for (int i = 0; i < parts.length; i++) {
			// parts[i] = parts[i].substring(1, parts[i].length() - 1);
			// }

			String korKwd = kwdp.getFirst();
			String engKwd = kwdp.getSecond();

			korKwd = StrUtils.unwrap(korKwd);
			engKwd = StrUtils.unwrap(engKwd);

			korKwd = KeywordClusterer.normalize(korKwd);
			engKwd = KeywordClusterer.normalizeEnglish(engKwd);

			if (korKwd.length() > 0) {
				Node<Character> node = korDict.insert(StrUtils.asCharacters(korKwd));
				Set<Integer> data = (Set<Integer>) node.getData();

				if (data == null) {
					data = Generics.newHashSet();
					node.setData(data);
				}

				data.add(kwdid);
			}

			if (engKwd.length() > 0) {
				Node<String> node = engDict.insert(StrUtils.split(engKwd));
				Set<Integer> data = (Set<Integer>) node.getData();

				if (data == null) {
					data = Generics.newHashSet();
					node.setData(data);
				}

				data.add(kwdid);
			}
		}
	}

}
