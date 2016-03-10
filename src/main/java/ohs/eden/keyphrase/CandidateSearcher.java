package ohs.eden.keyphrase;

import java.util.List;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.ling.types.Document;
import ohs.ling.types.Sentence;
import ohs.ling.types.Token;
import ohs.ling.types.TokenAttr;
import ohs.tree.trie.Node;
import ohs.tree.trie.Trie;
import ohs.types.Counter;
import ohs.utils.Generics;

public class CandidateSearcher {
	public static void extractKeywordPatterns() throws Exception {
		Counter<String> patCnts = Generics.newCounter();

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

				String[] korKwds = korKwdStr.split(";");

				for (String kwd : korKwds) {
					Sentence sent = parse(kwd).get(0);
					String pat = String.join(" ", sent.getValues(TokenAttr.POS));
					patCnts.incrementCount(pat, 1);
				}
			}
		}
		reader.close();

		FileUtils.writeStrCounter(KPPath.POS_CNT_FILE, patCnts);
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		// CandidateSearcher.extractKeywordPatterns();

		CandidateSearcher cs = new CandidateSearcher();
		cs.readPatterns();

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
					Document doc = parse(korAbs);

					cs.search(doc);

					System.out.println();

				}
			}
		}
		reader.close();

		System.out.println("process ends.");
	}

	public static Document parse(String s) {
		String[] lines = s.split("\n");
		Sentence[] sents = new Sentence[lines.length];

		for (int i = 0; i < lines.length; i++) {
			String[] parts = lines[i].split(" ");
			Token[] toks = new Token[parts.length];

			int loc = 0;

			for (int j = 0; j < parts.length; j++) {
				String part = parts[j];
				String[] subParts = part.split("#P#");

				Token[] subToks = new Token[subParts.length];

				for (int k = 0; k < subParts.length; k++) {
					String subPart = subParts[k];
					String[] two = subPart.split("#S#");

					Token t = new Token(loc++, two[0]);
					t.setValue(TokenAttr.POS, two[1]);

					subToks[k] = t;
				}

				Token t = new Token();
				t.setSubTokens(subToks);

				toks[j] = t;
			}
			sents[i] = new Sentence(toks);
		}

		Document doc = new Document(sents);
		return doc;
	}

	private Counter<String> patCnts;

	private Trie<String> trie;

	public void readPatterns() throws Exception {
		patCnts = Generics.newCounter();

		TextFileReader reader = new TextFileReader(KPPath.POS_CNT_FILE);
		while (reader.hasNext()) {
			String line = reader.next();
			if (line.startsWith(FileUtils.LINE_SIZE)) {
				continue;
			}
			String[] two = line.split("\t");
			String pat = two[0];
			double cnt = Double.parseDouble(two[1]);

			if (cnt < 10) {
				break;
			}

			if (!pat.contains("NN")) {
				continue;
			}
			patCnts.setCount(pat, cnt);
		}
		reader.close();

		trie = new Trie<String>();

		for (String pat : patCnts.keySet()) {
			String[] keys = pat.split(" ");
			trie.insert(keys);
		}
	}

	public void search(Document doc) {
		for (int i = 0; i < doc.size(); i++) {
			Sentence sent = doc.get(i);

			String[] poss = sent.getValues(TokenAttr.POS);

			Set<String> set = Generics.newHashSet();

			for (int start = 0; start < poss.length;) {
				int found = -1;
				for (int end = start + 1; end < poss.length; end++) {
					Node<String> node = trie.search(poss, start, end);

					if (node != null) {
						found = end;
					}

					if (node == null) {
						break;
					}
				}

				if (found == -1) {
					start++;
				} else {
					Sentence candidate = new Sentence(sent.getTokens(start, found));

					System.out.println(candidate.joinValues());

					start = found;
				}

			}
		}
	}

}
