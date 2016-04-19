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
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.tree.trie.hash.Trie.SearchResult;
import ohs.tree.trie.hash.Trie.SearchResult.MatchType;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;
import ohs.utils.UnicodeUtils;

public class MorphemeAnalyzer {

	public static void main(String[] args) throws Exception {
		System.out.println("proces begins.");

		TextTokenizer t = new TextTokenizer();
		MorphemeAnalyzer a = new MorphemeAnalyzer();

		{
			// String document = "프랑스의 세계적인 의상 디자이너 엠마누엘 웅가로가 실내 장식용 직물 디자이너로 나섰다.\n";
			String document = "프랑스의\n";

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
		// KSentence sent = doc.getSentence(i);
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

	private Trie<Character> wordDict;

	private void writeConnectionRules(ObjectOutputStream oos) throws Exception {
		oos.writeInt(conRules.size());

		for (SJTag t1 : conRules.keySet()) {
			oos.writeInt(t1.ordinal());
			Set<SJTag> tags = conRules.get(t1);
			oos.writeInt(tags.size());
			for (SJTag t2 : tags) {
				oos.writeInt(t2.ordinal());
			}
		}
	}

	private void readConnectionRules(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();

		conRules = Generics.newSetMap(size);

		for (int i = 0; i < size; i++) {
			SJTag t1 = SJTag.values()[ois.readInt()];
			int size2 = ois.readInt();
			Set<SJTag> tags = Generics.newHashSet(size2);

			for (int j = 0; j < size2; j++) {
				SJTag t2 = SJTag.values()[ois.readInt()];
				tags.add(t2);
			}
			conRules.put(t1, tags);
		}
	}

	public MorphemeAnalyzer() throws Exception {
		if (FileUtils.exists(NLPPath.DICT_SER_FILE)) {
			ObjectInputStream ois = FileUtils.openObjectInputStream(NLPPath.DICT_SER_FILE);
			wordDict = new Trie<Character>();
			wordDict.read(ois);

			readConnectionRules(ois);

			ois.close();
		} else {
			buildDict(NLPPath.DICT_ANALYZED_FILE);

			ObjectOutputStream oos = FileUtils.openObjectOutputStream(NLPPath.DICT_SER_FILE);
			wordDict.write(oos);
			writeConnectionRules(oos);
			oos.close();
		}

		// readSystemDict(NLPPath.DICT_SYSTEM_FILE);
	}

	public void analyze(KDocument doc) {
		for (int i = 0; i < doc.size(); i++) {
			KSentence sent = doc.getSentence(i);
			MultiToken[] mts = sent.toMultiTokens();

			for (int j = 0; j < mts.length; j++) {
				MultiToken mt = mts[j];
				String eojeol = mt.getValue(TokenAttr.WORD);
				// Set<String> morphemes = analDict.get(eojeol, false);

				// if (morphemes == null) {
				analyze(eojeol);
				// } else {
				// String s = StrUtils.join(" # ", morphemes);
				// mt.setValue(TokenAttr.POS, s);
				// }
			}
		}

	}

	public Set<String> analyze(String word) {
		Set<String> ret = Generics.newHashSet();

		List<Character> cs = Generics.newArrayList();

		for (int i = 0; i < word.length(); i++) {
			for (char c : UnicodeUtils.decomposeToJamo(word.charAt(i))) {
				if ((int) c != 0) {
					cs.add(c);
				}
			}
		}

		Set<SJTag> tags = Generics.newHashSet();

		find(cs, 0, tags);

		return ret;
	}

	private void find(List<Character> cs, int i, Set<SJTag> prevTags) {

		for (int j = i + 1; j <= cs.size(); j++) {
			String str = getString(cs, i, j);
			SearchResult<Character> sr = wordDict.search(cs, i, j);
			Set<SJTag> tags = (Set<SJTag>) sr.getNode().getData();

			if (sr.getMatchType() == MatchType.EXACT) {
				if (tags != null) {
					Set<SJTag> nextTags = Generics.newHashSet();

					if (i == 0) {
						nextTags = Generics.newHashSet(tags);

						System.out.printf("[%s, %d, %d, %s]\n", str, i, j, nextTags);

						find(cs, j, nextTags);
					} else {
						if (prevTags.size() > 0) {
							for (SJTag prevTag : prevTags) {
								for (SJTag tag : tags) {
									if (conRules.contains(prevTag, tag)) {
										nextTags.add(tag);
									}
								}
							}

							if (nextTags.size() > 0) {
								System.out.printf("[%s, %d, %d, %s]\n", getString(cs, i, j), i, j, nextTags);

								find(cs, j, nextTags);
							}
						}
					}

				} else {
					// System.out.println();
				}
			} else {
				if (i != 0) {
					break;
				}
			}
		}
	}

	public String getString(List<Character> s, int start, int end) {
		StringBuffer sb = new StringBuffer();
		for (int i = start; i < end; i++) {
			sb.append(s.get(i).charValue());
		}
		return sb.toString();
	}

	public void analyze(String[] words) {
		List<String>[] ret = new List[words.length];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = Generics.newArrayList();
		}
	}

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

	private SetMap<SJTag, SJTag> conRules;

	private void buildDict(String fileName) throws Exception {

		wordDict = Trie.newTrie();

		conRules = Generics.newSetMap();

		TextFileReader reader = new TextFileReader(fileName);

		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.split("\t");
			for (int i = 1; i < parts.length; i++) {
				String[] subParts = parts[i].split(MultiToken.DELIM_MULTI_TOKEN.replace("+", "\\+"));
				String[] words = new String[subParts.length];
				SJTag[] poss = new SJTag[subParts.length];

				for (int j = 0; j < subParts.length; j++) {
					String[] two = subParts[j].split(Token.DELIM_TOKEN);
					words[j] = two[0];
					poss[j] = SJTag.valueOf(two[1]);
				}

				for (int j = 0; j < words.length; j++) {
					// if (words[j].contains("프랑스")) {
					// System.out.println();
					// }
					char[][] jasos = UnicodeUtils.decomposeToJamo(words[j]);

					StringBuffer sb = new StringBuffer();

					for (int m = 0; m < jasos.length; m++) {
						for (int n = 0; n < jasos[m].length; n++) {
							if ((int) jasos[m][n] != 0) {
								sb.append(jasos[m][n]);
							}
						}
					}

					Node<Character> node = wordDict.insert(StrUtils.asCharacters(sb.toString()));

					Set<SJTag> tags = (Set<SJTag>) node.getData();

					if (tags == null) {
						tags = Generics.newHashSet();
						node.setData(tags);
					}
					tags.add(poss[j]);
				}

				if (poss.length > 1) {
					for (int j = 1; j < poss.length; j++) {
						conRules.put(poss[j - 1], poss[j]);
					}
				}

			}
		}
		reader.close();

		wordDict.trimToSize();
	}

}
