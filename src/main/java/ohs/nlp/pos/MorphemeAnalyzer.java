package ohs.nlp.pos;

import java.util.List;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.TokenAttr;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.tree.trie.hash.Trie.SearchResult;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.UnicodeUtils;
import ohs.utils.StrUtils;

public class MorphemeAnalyzer {

	public static void main(String[] args) throws Exception {
		System.out.println("proces begins.");

		TextTokenizer t = new TextTokenizer();
		MorphemeAnalyzer a = new MorphemeAnalyzer();

		{
			String document = "프로젝트 대한전산 전체 회의.\n" + "회의 일정은 다음과 같습니다.\n";

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

	public Set<String> analyze(String word) {
		Set<String> ret = Generics.newHashSet();

		String word2 = UnicodeUtils.decomposeToJamo(word);

		SearchResult<Character> sr = sysDict.search(StrUtils.asCharacters(word2.toCharArray()));

		return ret;
	}

	public void analyze(String[] words) {
		List<String>[] ret = new List[words.length];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = Generics.newArrayList();
		}
	}

	private void readAnalyzedDict(String fileName) throws Exception {
		List<String> lines = FileUtils.readLines(fileName);

		analDict = Generics.newSetMap(lines.size());

		for (String line : lines) {
			String[] parts = line.split("\t");
			for (int i = 1; i < parts.length; i++) {
				analDict.put(parts[0], parts[i]);
			}
		}
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
