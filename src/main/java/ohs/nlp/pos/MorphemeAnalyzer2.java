package ohs.nlp.pos;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.TokenAttr;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.tree.trie.hash.Trie.TSResult;
import ohs.tree.trie.hash.Trie.TSResult.MatchType;
import ohs.types.Indexer;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;
import ohs.utils.UnicodeUtils;

public class MorphemeAnalyzer2 {

	public static void main(String[] args) throws Exception {
		System.out.println("proces begins.");

		TextTokenizer t = new TextTokenizer();
		MorphemeAnalyzer2 a = new MorphemeAnalyzer2();
		a.buildDicts();

		{
			String document = "프랑스의 세계적인 의상 디자이너 엠마누엘 웅가로가 실내 장식용 직물 디자이너로 나섰다.\n";

			KDocument doc = t.tokenize(document);
			a.analyze(doc);
		}

		// SejongReader r = new SejongReader(NLPPath.POS_DATA_FILE, NLPPath.POS_TAG_SET_FILE);
		// while (r.hasNext()) {
		// KDocument doc = r.next();
		//
		// StringBuffer sb = new StringBuffer();
		//
		// for (int i = 0; i < doc.size(); i++) {
		// MTSentence sent = doc.getSentence(i);
		// MultiToken[] mts = MultiToken.toMultiTokens(sent.getTokens());
		//
		// for (int j = 0; j < mts.length; j++) {
		// sb.append(mts[j].getValue(TokenAttr.WORD));
		// if (j != mts.length - 1) {
		// sb.append(" ");
		// }
		// }
		// if (i != doc.size() - 1) {
		// sb.append("\n");
		// }
		// }
		//
		// KDocument newDoc = t.tokenize(sb.toString());
		// a.analyze(newDoc);
		//
		// }
		// r.close();

		System.out.println("proces ends.");
	}

	private SetMap<String, String> analDict;

	private Trie<Character> wordDict;

	private Indexer<String> posIndexer;

	private SetMap<Integer, Character> locToJosas;

	private SetMap<Integer, Character> locToEomis;

	public MorphemeAnalyzer2() {

	}

	public void buildDicts() throws Exception {
		if (FileUtils.exists(NLPPath.DICT_SER_FILE)) {
			ObjectInputStream ois = FileUtils.openObjectInputStream(NLPPath.DICT_SER_FILE);
			wordDict = new Trie<Character>();
			wordDict.read(ois);

			ois.close();

			buildStartLocs();

		} else {
			// buildAnalysisDict(NLPPath.DICT_ANALYZED_FILE);
			buildSystemDict(NLPPath.DICT_SYSTEM_FILE);

			ObjectOutputStream oos = FileUtils.openObjectOutputStream(NLPPath.DICT_SER_FILE);
			wordDict.write(oos);
			oos.close();
		}
	}

	public void analyze(KDocument doc) {
		for (int i = 0; i < doc.size(); i++) {
			KSentence sent = doc.getSentence(i);
			MultiToken[] mts = sent.toMultiTokens();

			for (int j = 0; j < mts.length; j++) {
				MultiToken mt = mts[j];
				String eojeol = mt.get(TokenAttr.WORD);
				// Set<String> morphemes = analDict.get(eojeol, false);

				// if (morphemes == null) {
				analyze(eojeol);
				// } else {
				// String s = StrUtils.join(" # ", morphemes);
				// mt.setValue(TokenAttr.POS, s);
				// }
			}
		}

		System.out.println();
	}

	private Character[] cs;

	private char[][] jasos;
	
	

	public Set<String> analyze(String word) {
		Set<String> ret = Generics.newHashSet();

		int L = word.length();

		cs = StrUtils.asCharacters(word);
		jasos = UnicodeUtils.decomposeToJamo(word);

		List<Integer> josaLocs = getStartLocs(word, locToJosas);
		List<Integer> eomiLocs = getStartLocs(word, locToEomis);

		if (eomiLocs.size() > 0) {
			System.out.println(word);
			System.out.println(josaLocs);
			System.out.println(eomiLocs);
		}

		return ret;
	}
	
	

	public void analyze(String[] words) {
		List<String>[] ret = new List[words.length];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = Generics.newArrayList();
		}
	}

	private List<Integer> getEoMalEoMiLocs(String word) {
		List<Integer> ret = Generics.newArrayList();
		int L = word.length();

		for (int i = L - 1; i >= 0; i--) {
			if (i == L - 1 && locToEomis.contains(0, word.charAt(i))) {
				ret.add(i);
			}

			if (locToEomis.contains(1, word.charAt(i))) {
				if (i >= 0 && locToEomis.contains(0, word.charAt(i - 1))) {
					ret.add(i);
				}
			} else {
				break;
			}
		}
		return ret;
	}

	// private Trie<Character> trie;

	private List<Integer> getStartLocs(String word, SetMap<Integer, Character> locToChars) {
		List<Integer> ret = Generics.newArrayList();
		int L = word.length();

		for (int i = L - 1; i >= 0; i--) {
			if (i == L - 1 && locToChars.contains(0, word.charAt(i))) {
				ret.add(i);
			}

			if (locToChars.contains(1, word.charAt(i))) {
				if (i >= 0 && locToChars.contains(0, word.charAt(i - 1))) {
					ret.add(i);
				}
			} else {
				break;
			}
		}
		return ret;
	}

	private void buildAnalysisDict(String fileName) throws Exception {
		analDict = Generics.newSetMap();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.split("\t");
			for (int i = 1; i < parts.length; i++) {
				analDict.put(parts[0], parts[i]);
			}
		}
		reader.close();
	}

	private void buildStartLocs() {

		locToEomis = Generics.newSetMap();

		locToJosas = Generics.newSetMap();

		for (Node<Character> node : wordDict.getNodes()) {
			Set<SJTag> tags = (Set<SJTag>) node.getData();

			if (tags == null) {
				continue;
			}

			String word = node.getKeyPath("");

			for (SJTag pos : tags) {
				if (pos.toString().startsWith("J")) {
					for (int i = 0, j = 0; i < word.length(); i++) {
						char c = word.charAt(i);

						if (UnicodeUtils.isInRange(UnicodeUtils.HANGUL_SYLLABLES_RANGE, c)) {
							locToJosas.put(j == 0 ? 0 : 1, c);
							j++;
						}
					}
				}

				for (SJTag t : SJTag.EO_MAL_EO_MI) {
					if (pos == t) {
						for (int i = 0, j = 0; i < word.length(); i++) {
							char c = word.charAt(i);

							if (UnicodeUtils.isInRange(UnicodeUtils.HANGUL_SYLLABLES_RANGE, c)) {
								locToEomis.put(j == 0 ? 0 : 1, c);
								j++;
							}
						}
						break;
					}
				}
			}

		}

	}

	private void buildSystemDict(String fileName) throws Exception {
		List<String> lines = FileUtils.readLines(fileName);

		wordDict = Trie.newTrie();

		locToJosas = Generics.newSetMap();

		locToEomis = Generics.newSetMap();

		for (String line : lines) {
			String[] parts = line.split("\t");
			String word = parts[0];
			SJTag pos = SJTag.valueOf(parts[1]);

			Node<Character> node = wordDict.insert(StrUtils.asCharacters(word));

			Set<SJTag> tags = (Set<SJTag>) node.getData();

			if (tags == null) {
				tags = Generics.newHashSet();
				node.setData(tags);
			}

			tags.add(pos);
		}

		wordDict.trimToSize();
	}

	private void search(Character[] cs, int start, int end) {
		if (start >= 0 && end <= cs.length) {
			TSResult<Character> sr = wordDict.search(cs, start, end);

			if (sr.getMatchType() == MatchType.EXACT_KEYS_WITH_DATA) {
				search(cs, start, end + 1);
			} else {
				search(cs, end - 1, end);
				System.out.println();
			}
		}
	}

}
