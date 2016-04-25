package ohs.nlp.pos;

import java.util.List;
import java.util.Set;
import java.util.Stack;

import ohs.io.FileUtils;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.TokenAttr;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.tree.trie.hash.Trie.TSResult;
import ohs.tree.trie.hash.Trie.TSResult.MatchType;
import ohs.types.SetMap;
import ohs.types.Triple;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class MorphemeAnalyzer {

	public static void main(String[] args) throws Exception {
		System.out.println("proces begins.");

		TextTokenizer t = new TextTokenizer();
		MorphemeAnalyzer a = new MorphemeAnalyzer();

		{
			// String document = "프랑스랑은 세계적인 의상 디자이너 엠마누엘 웅가로가 실내 장식용 직물 디자이너로
			// 나섰다.\n";
			String document = "우승은 프랑스일테니까!\n";

			KDocument doc = t.tokenize(document);
			a.analyze(doc);
		}

		// SejongReader r = new SejongReader(NLPPath.POS_DATA_FILE,
		// NLPPath.POS_TAG_SET_FILE);
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

	private Trie<Character> fixDict;

	private Trie<Character> patDict;

	private SetMap<String, String> analDict;

	public MorphemeAnalyzer() throws Exception {
		readDicts();
	}

	public void analyze(KDocument doc) {
		for (int i = 0; i < doc.size(); i++) {
			KSentence sent = doc.getSentence(i);
			MultiToken[] mts = sent.toMultiTokens();

			for (int j = 0; j < mts.length; j++) {
				MultiToken mt = mts[j];
				String wp = mt.getValue(TokenAttr.WORD);

				Set<String> res = analDict.get(wp, false);

				if (res == null) {
					analyze(wp);
				} else {

				}

				// Set<String> morphemes = analDict.get(eojeol, false);

				// if (morphemes == null) {

				// } else {
				// String s = StrUtils.join(" # ", morphemes);
				// mt.setValue(TokenAttr.POS, s);
				// }
			}
		}

	}

	public Set<String> analyze(String wp) {
		Set<String> ret = Generics.newHashSet();

		Character[] cs = StrUtils.asCharacters(wp);

		List<Triple<Integer, Integer, Set<String>>> list1 = Generics.newArrayList();
		int num_matches = 0;

		{
			int start = 0;

			while (start < cs.length) {
				TSResult<Character> sr = fixDict.find(cs, start);
				if (sr.getMatchType() == MatchType.FAIL) {
					start++;
				} else {
					int end = sr.getMatchLoc() + 1;
					num_matches += (end - start);

					Set<String> set = (Set<String>) sr.getMatchNode().getData();

					list1.add(Generics.newTriple(start, end, set));

					start = end;
				}
			}
		}

		System.out.println(wp.length());
		System.out.println(num_matches);

		List<Triple<Integer, Integer, Set<String>>> list2 = Generics.newArrayList();

		{
			int start = 0;
			while (start < cs.length) {
				TSResult<Character> sr = patDict.find(cs, start);
				if (sr.getMatchType() == MatchType.FAIL) {
					start++;
				} else {
					int end = sr.getMatchLoc() + 1;
					Set<String> set = (Set<String>) sr.getMatchNode().getData();

					list2.add(Generics.newTriple(start, end, set));

					start = end;
				}
			}
		}

		return ret;
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

	public String getString(List<Character> s, int start, int end) {
		StringBuffer sb = new StringBuffer();
		for (int i = start; i < end; i++) {
			sb.append(s.get(i).charValue());
		}
		return sb.toString();
	}

	public void readDicts() throws Exception {
		analDict = Generics.newSetMap();

		for (String line : FileUtils.readLines(NLPPath.DICT_ANALYZED_FILE)) {
			String[] parts = line.split("\t");

			for (int i = 1; i < parts.length; i++) {
				analDict.put(parts[0], parts[1]);
			}
		}

		fixDict = Trie.newTrie();

		for (String line : FileUtils.readLines(NLPPath.DICT_WORD_FILE)) {
			String[] parts = line.split("\t");
			Character[] cs = StrUtils.asCharacters(parts[0]);
			Node<Character> node = fixDict.insert(cs);

			Set<String> set = (Set<String>) node.getData();

			if (set == null) {
				set = Generics.newHashSet(parts.length - 1);
				node.setData(set);
			}

			for (int i = 1; i < parts.length; i++) {
				set.add(parts[i]);
			}
		}

		patDict = Trie.newTrie();

		for (String line : FileUtils.readLines(NLPPath.DICT_SUFFIX_FILE)) {
			String[] parts = line.split("\t");
			Character[] cs = StrUtils.asCharacters(parts[0].substring(1));
			Node<Character> node = patDict.insert(cs);

			Set<String> set = (Set<String>) node.getData();

			if (set == null) {
				set = Generics.newHashSet(parts.length - 1);
				node.setData(set);
			}

			for (int i = 1; i < parts.length; i++) {
				String[] toks = parts[i].split(MultiToken.DELIM_MULTI_TOKEN.replace("+", "\\+"));
				set.add(StrUtils.join(MultiToken.DELIM_MULTI_TOKEN, toks, 1, toks.length));
			}
		}
	}

}
