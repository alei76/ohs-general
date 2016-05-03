package ohs.eden.keyphrase;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.linear.SparseRealVector;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.weight.TermWeighting;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.tree.trie.hash.Trie.TSResult;
import ohs.tree.trie.hash.Trie.TSResult.MatchType;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.SetMap;
import ohs.types.StrPair;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class KeywordMapper {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		KeywordData kwdData = new KeywordData();

		kwdData.read(KPPath.KEYWORD_DATA_CLUSTER_SER_FILE);

		CounterMap<String, String> cnToWordCnts = FileUtils.readStrCounterMap(KPPath.TITLE_DATA_FILE);

		KeywordMapper kwdMapper = new KeywordMapper(kwdData, cnToWordCnts);

		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_POS_FILE);
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
				String korKwdStr = parts[2].split(" => ")[0];
				String korKwdStrPos = parts[2].split(" => ")[1];
				String engKwdStr = parts[3];
				String korTitle = parts[4];
				String engTitle = parts[5];
				String korAbs = parts[6];
				String engAbs = parts[7];

				KDocument doc = TaggedTextParser.parse(korTitle + "\\n" + korAbs);
				Counter<String> c = Generics.newCounter();
				//
				for (Token t : doc.getSubTokens()) {
					String word = t.getValue(TokenAttr.WORD);
					String pos = t.getValue(TokenAttr.POS);
					if (pos.startsWith("N")) {
						c.incrementCount(word.toLowerCase(), 1);
					}
				}

				List<String> words = Generics.newArrayList();

				{
					String[][][] vals = doc.getSubValues(TokenAttr.WORD);

					for (int i = 0; i < vals.length; i++) {
						for (int j = 0; j < vals[i].length; j++) {
							for (int k = 0; k < vals[i][j].length; k++) {
								words.add(vals[i][j][k]);
							}
						}
					}
				}

				System.out.println();

				// if (!type.equals("patent")) {
				// continue;
				// }

				Set<Integer> kwdids = kwdMapper.map(StrUtils.join(" ", words), c);
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

	private Map<Integer, Integer> kwdToCluster;

	private Indexer<String> wordIndexer;

	private GramGenerator gg = new GramGenerator(3);

	private Map<Integer, SparseVector> kwdToWordCnts;

	private Map<Integer, SparseVector> cents;

	public KeywordMapper(KeywordData kwdData, CounterMap<String, String> cnToWordCnts) {
		this.kwdData = kwdData;

		buildDicts();

		buildCentroids(cnToWordCnts);
	}

	public Set<Integer> map(String text, Counter<String> contexts) {
		text = KeywordClusterer.normalize(text);

		Character[] cs = StrUtils.asCharacters(text);
		Set<Integer> kwdids = Generics.newHashSet();
		for (int i = 0; i < cs.length; i++) {

			for (int j = i + 1; j < cs.length; j++) {
				TSResult<Character> sr = korDict.search(cs, i, j);

				if (sr.getMatchType() == MatchType.EXACT_KEYS_WITH_DATA) {
					StringBuffer sb = new StringBuffer();

					for (int k = i; k < j; k++) {
						sb.append(cs[k].charValue());
					}

					String s = sb.toString();

					System.out.println(s);

					Set<Integer> set = (Set<Integer>) sr.getMatchNode().getData();
					kwdids.addAll(set);
				} else if (sr.getMatchType() == MatchType.EXACT_KEYS_WITHOUT_DATA) {

				} else {
					break;
				}
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

		SparseVector input = VectorUtils.toSparseVector(contexts, wordIndexer, false);

		VectorMath.unitVector(input);

		Counter<Integer> catCosines = Generics.newCounter();

		for (int cid : clusterToKwds.keySet()) {
			SparseVector cent = cents.get(cid);
			if (cent == null) {
				continue;
			}

			double cosine = VectorMath.cosine(input, cent, false);
			catCosines.setCount(cid, cosine);
		}

		if (catCosines.size() > 1) {
			List<Integer> cids = catCosines.getSortedKeys();

			kwdids = Generics.newHashSet();

			double cutoff = 0.5;

			while (kwdids.isEmpty() && cutoff >= 0) {

				for (int i = 0; i < cids.size(); i++) {
					int cid = cids.get(i);
					double cosine = catCosines.getCount(cid);

					if (cosine < cutoff) {
						break;
					}

					for (int kwdid : clusterToKwds.get(cid)) {
						kwdids.add(kwdid);
						StrPair kwdp = kwdData.getKeywordIndexer().getObject(kwdid);
						System.out.println(kwdp);
					}
				}

				cutoff -= 0.1;

			}

		}

		return kwdids;
	}

	private void buildCentroids(CounterMap<String, String> cnToWords) {
		kwdToWordCnts = Generics.newHashMap();
		wordIndexer = Generics.newIndexer();

		for (int kwdid : kwdData.getKeywordToDocs().keySet()) {
			Counter<String> c = Generics.newCounter();
			for (int docid : kwdData.getKeywordToDocs().get(kwdid)) {
				String cn = kwdData.getDocIndexer().getObject(docid);
				c.incrementAll(cnToWords.getCounter(cn));
			}
			if (c.size() > 0) {
				kwdToWordCnts.put(kwdid, VectorUtils.toSparseVector(c, wordIndexer, true));
			}
		}

		cents = Generics.newHashMap();

		for (int cid : kwdData.getClusterToKeywords().keySet()) {

			Counter<Integer> c = Generics.newCounter();

			for (int kwdid : kwdData.getClusterToKeywords().get(cid)) {
				SparseVector x = kwdToWordCnts.get(kwdid);

				if (x == null) {
					continue;
				}
				VectorMath.add(x, c);
			}
			cents.put(cid, VectorUtils.toSparseVector(c));
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
			int kwd_freq = kwdData.getKeywordFreq(kwdid);

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

}
