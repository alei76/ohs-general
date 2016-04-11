package ohs.nlp.pos;

import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;

import ohs.eden.keyphrase.TaggedTextParser;
import ohs.io.FileUtils;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.tree.trie.hash.Trie.SearchResult;
import ohs.tree.trie.hash.Trie.SearchResult.MatchType;
import ohs.types.Counter;
import ohs.types.Indexer;
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
			String document = "직업이라고눈\n프로젝트 대한전산 전체 회의.\n" + "회의 일정은 다음과 같습니다.\n";

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

	private SetMap<String, String> analDict;

	private Trie<Character> userDict;

	private Trie<Character> sysDict;

	public MorphemeAnalyzer() throws Exception {
		readAnalyzedDict(NLPPath.DICT_ANALYZED_FILE);
		readSystemDict(NLPPath.DICT_SYSTEM_FILE);
	}

	public void analyze(KDocument doc) {
		for (int i = 0; i < doc.size(); i++) {
			KSentence sent = doc.getSentence(i);
			MultiToken[] mts = sent.toMultiTokens();

			for (int j = 0; j < mts.length; j++) {
				MultiToken mt = mts[j];
				String eojeol = mt.getValue(TokenAttr.WORD);
				Set<String> morphemes = analDict.get(eojeol, false);

				if (morphemes == null) {
					morphemes = analyze(eojeol);
				} else {
					String s = StrUtils.join(" # ", morphemes);
					mt.setValue(TokenAttr.POS, s);
				}
			}
		}

		System.out.println();
	}

	public Set<String> analyze(String eojeol) {
		Set<String> ret = Generics.newHashSet();

		String s = UnicodeUtils.decomposeToJamo(eojeol);

		Character[] cs = StrUtils.asCharacters(s.toCharArray());

		search(cs, 0, 1);

		// for (int i = 0; i < cs.length; i++) {
		//
		// for (int j = i + 1; j < cs.length; j++) {
		// SearchResult<Character> sr = trie.search(cs, i, j);
		//
		// if (sr.getMatchType() == MatchType.EXACT) {
		//
		// } else {
		//
		// }
		// }
		// }

		return ret;
	}

	private void search(Character[] cs, int start, int end) {
		if (start >= 0 && end <= cs.length) {
			SearchResult<Character> sr = trie.search(cs, start, end);

			if (sr.getMatchType() == MatchType.EXACT) {
				search(cs, start, end + 1);
			} else {
				search(cs, end - 1, end);
				System.out.println();

			}
		}

	}

	public void analyze(String[] words) {
		List<String>[] ret = new List[words.length];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = Generics.newArrayList();
		}
	}

	private Indexer<String> posIndexer;

	private Trie<Character> trie;

	private void readAnalyzedDict(String fileName) throws Exception {
		List<String> lines = FileUtils.readLines(fileName);

		analDict = Generics.newSetMap(lines.size());

		posIndexer = Generics.newIndexer();

		trie = Trie.newTrie();

		for (String line : lines) {
			String[] parts = line.split("\t");
			for (int i = 1; i < parts.length; i++) {
				analDict.put(parts[0], parts[i]);

				String[] subParts = parts[i].split(MultiToken.DELIM_MULTI_TOKEN.replace("+", "\\+"));

				for (int j = 0; j < subParts.length; j++) {
					String[] two = subParts[j].split(Token.DELIM_TOKEN);
					String word = two[0];
					String pos = two[1];

					StringBuffer sb = new StringBuffer();
					for (int k = 0; k < word.length(); k++) {
						sb.append(UnicodeUtils.decomposeToJamo(word.charAt(k)));
					}

					Node<Character> node = trie.insert(StrUtils.asCharacters(sb.toString().toCharArray()));

					Counter<Integer> c = (Counter<Integer>) node.getData();

					if (c == null) {
						c = Generics.newCounter();
						node.setData(c);
					}

					c.incrementCount(posIndexer.getIndex(pos), 1);
				}
			}
		}

		trie.trimToSize();
	}

	private void readSystemDict(String fileName) throws Exception {
		List<String> lines = FileUtils.readLines(fileName);

		sysDict = Trie.newTrie();

		for (String line : lines) {
			String[] parts = line.split("\t");
			String word = parts[0];
			String pos = parts[1];

			String word2 = UnicodeUtils.decomposeToJamo(word);

			sysDict.insert(StrUtils.asCharacters(word2.toCharArray()));
		}

		sysDict.trimToSize();
	}

}
