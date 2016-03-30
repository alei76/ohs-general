package ohs.eden.keyphrase;

import java.util.List;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.TokenAttr;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.types.Counter;
import ohs.utils.Generics;

public class CandidateSearcher {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		// CandidateSearcher.extractKeywordPatterns();

		CandidateSearcher cs = new CandidateSearcher(KPPath.KEYWORD_POS_CNT_FILE);

		List<String> labels = Generics.newArrayList();

		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_POS_FILE);
		while (reader.hasNext()) {
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

				if (korAbs.length() > 0) {
					KDocument doc = TaggedTextParser.parse(korAbs);

					cs.search(doc);

					System.out.println();

				}
			}
		}
		reader.close();

		System.out.println("process ends.");
	}

	private Counter<String> patCnts;

	private Trie<String> trie;

	public CandidateSearcher(String patFileName) throws Exception {
		readPatterns(patFileName);
	}

	private void readPatterns(String patFileName) throws Exception {
		patCnts = Generics.newCounter();

		int cutoff = 10;

		TextFileReader reader = new TextFileReader(patFileName);
		while (reader.hasNext()) {
			String line = reader.next();
			if (line.startsWith(FileUtils.LINE_SIZE)) {
				continue;
			}
			String[] two = line.split("\t");
			String pat = two[0];
			double cnt = Double.parseDouble(two[1]);

			if (cnt < cutoff) {
				break;
			}

			String[] toks = pat.split(" ");
			String[] subToks = toks[toks.length - 1].split("\\+");
			String lastSubtok = subToks[subToks.length - 1];

			if (lastSubtok.equals("NNG") || lastSubtok.equals("NNP")) {

			} else {
				continue;
			}

			patCnts.setCount(pat, cnt);
		}
		reader.close();

		trie = new Trie<String>();

		for (String pat : patCnts.keySet()) {
			trie.insert(pat.split(" "));
		}
	}

	public List<KSentence> search(KDocument input) {
		List<KSentence> ret = Generics.newArrayList();

		for (int i = 0; i < input.size(); i++) {
			KSentence sent = input.getSentence(i);

			String[] poss = sent.getValues(TokenAttr.POS);

			for (int s = 0; s < poss.length;) {
				int found = -1;
				for (int e = s + 1; e < poss.length; e++) {
					Node<String> node = trie.search(poss, s, e);

					if (node != null) {
						if (node.getCount() > 0) {
							found = e;
						}
					}

					if (node == null) {
						break;
					}
				}

				if (found == -1) {
					s++;
				} else {
					KSentence cand = new KSentence(sent.getTokens(s, found));
					ret.add(cand);
					s = found;
				}
			}
		}
		return ret;
	}

}
