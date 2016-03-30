package ohs.nlp.pos;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.TokenAttr;
import ohs.tree.trie.hash.Trie;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.KoreanUtils;
import ohs.utils.StrUtils;

public class MorphemeAnalyzer {

	private SetMap<String, String> analDict;

	private Trie<Character> userDict;

	private Trie<Character> sysDict;

	private void readSystemDict(String fileName) throws Exception {
		List<String> lines = FileUtils.readLines(fileName);

		sysDict = Trie.newTrie();

		for (String line : lines) {
			String[] parts = line.split("\t");
			String word = parts[0];
			String pos = parts[1];

			String word2 = KoreanUtils.decomposeToJamo(word);

			sysDict.insert(StrUtils.toCharacters(word2.toCharArray()));
		}

		sysDict.trimToSize();

		System.out.println();
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

	public MorphemeAnalyzer() throws Exception {
		readAnalyzedDict(NLPPath.DICT_ANALYZED_FILE);
		readSystemDict(NLPPath.DICT_SYSTEM_FILE);
	}

	public static void main(String[] args) throws Exception {
		System.out.println("proces begins.");

		TextTokenizer t = new TextTokenizer();
		MorphemeAnalyzer a = new MorphemeAnalyzer();

		SejongReader r = new SejongReader(NLPPath.POS_DATA_FILE, NLPPath.POS_TAG_SET_FILE);
		while (r.hasNext()) {
			KDocument doc = r.next();

			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < doc.size(); i++) {
				KSentence sent = doc.getSentence(i);
				MultiToken[] mts = MultiToken.toMultiTokens(sent.getTokens());

				for (int j = 0; j < mts.length; j++) {
					sb.append(mts[j].getValue(TokenAttr.WORD));
					if (j != mts.length - 1) {
						sb.append(" ");
					}
				}
				if (i != doc.size() - 1) {
					sb.append("\n");
				}
			}

			KDocument newDoc = t.tokenize(sb.toString());
			a.analyze(newDoc);
			;

		}
		r.close();

		System.out.println("proces ends.");
	}

	public void analyze(KDocument doc) {

		for (int i = 0; i < doc.size(); i++) {
			KSentence sent = doc.getSentence(i);
			MultiToken[] mts = MultiToken.toMultiTokens(sent.getTokens());

			for (int j = 0; j < mts.length; j++) {
				MultiToken mt = mts[j];
				String text = mt.getValue(TokenAttr.WORD);
				Set<String> cands = analDict.get(text, false);

				if (cands == null) {

				} else {
					String s = String.join(" # ", cands);
					mt.setValue(TokenAttr.POS, s);
				}
			}
		}

		System.out.println();

	}

	public void analyze(String[] words) {
		List<String>[] ret = new List[words.length];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = Generics.newArrayList();
		}
	}

}
