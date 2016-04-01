package ohs.eden.keyphrase;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.TokenAttr;
import ohs.tree.trie.hash.Trie;
import ohs.tree.trie.hash.Trie.SearchResult;
import ohs.tree.trie.hash.Trie.SearchResult.MatchType;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.Vocab;
import ohs.utils.Generics;
import ohs.utils.StrUtils;
import ohs.utils.TermWeighting;

public class KeyphraseExtractor {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", KeyphraseExtractor.class.getName());
		// run1();
		run2();
		System.out.printf("ends.");
	}

	public static void run1() throws Exception {
		KeywordData data = new KeywordData();

		if (FileUtils.exists(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"))) {
			data.read(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));
		} else {
			data.readText(KPPath.KEYWORD_DATA_FILE);
			data.write(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));
		}

		Trie<Character> korTrie = new Trie<Character>();
		Trie<String> engTrie = new Trie<String>();

		int[] max_kwd_lens = new int[2];

		for (int kwdid : data.getKeywords()) {
			int kwd_freq = data.getKeywordFreqs()[kwdid];

			if (kwd_freq < 10) {
				continue;
			}

			String s = data.getKeywordIndexer().getObject(kwdid);

			String[] parts = s.split("\t");

			// for (int i = 0; i < parts.length; i++) {
			// parts[i] = parts[i].substring(1, parts[i].length() - 1);
			// }

			String korKwd = parts[0];
			String engKwd = parts[1];

			if (!korKwd.equals(DataHandler.NONE) && !korKwd.equals("\"\"") && korKwd.length() > 0) {
				Character[] chs = StrUtils.toCharacters(korKwd);
				korTrie.insert(chs);
				max_kwd_lens[0] = Math.max(max_kwd_lens[0], chs.length);
			}

			if (!engKwd.equals(DataHandler.NONE) && !engKwd.equals("\"\"") && engKwd.length() > 0) {
				String[] words = engKwd.split(" ");
				engTrie.insert(words);
				max_kwd_lens[1] = Math.max(max_kwd_lens[1], words.length);
			}
		}

		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_FILE);
		reader.setPrintNexts(false);

		TextFileWriter writer = new TextFileWriter(KPPath.PATENT_KEYWORD_MATCH_FILE);

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

				String[] texts = { korTitle + "\n" + korAbs, engTitle + "\n" + engAbs };
				Counter<String>[] cs = new Counter[2];

				for (int i = 0; i < texts.length; i++) {
					String text = texts[i];
					Counter<String> kwdCnts = Generics.newCounter();

					if (i == 0) {
						Character[] chs = StrUtils.toCharacters(text);
						int max_kwd_len = max_kwd_lens[i];

						for (int j = 0; j < chs.length; j++) {
							int found = -1;
							for (int k = j + 1; k < j + max_kwd_len && k < chs.length; k++) {
								SearchResult<Character> sr = korTrie.search(chs, j, k);
								if (sr.getMatchType() != MatchType.FAIL && sr.getNode().getCount() > 0) {
									found = k;
								}
							}

							if (found != -1) {
								int dist = found - j;
								if (dist > 1) {
									StringBuffer sb = new StringBuffer();
									for (int s = j; s < found; s++) {
										sb.append(chs[s]);
									}
									String kwd = sb.toString();
									kwdCnts.incrementCount(kwd, 1);
								}
							}
						}
						cs[i] = kwdCnts;
					} else {
						List words = StrUtils.split(text);
						int max_kwd_len = max_kwd_lens[i];

						for (int j = 0; j < words.size(); j++) {
							int found = -1;
							for (int k = j + 1; k < j + max_kwd_len && k < words.size(); k++) {
								SearchResult<String> sr = engTrie.search(words, j, k);
								if (sr.getMatchType() != MatchType.FAIL && sr.getNode().getCount() > 0) {
									found = k;
								}
							}

							if (found != -1) {
								String kwd = StrUtils.join(" ", words, j, found);
								kwdCnts.incrementCount(kwd, 1);
							}
						}
						cs[i] = kwdCnts;
					}
				}

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

				writer.write(String.format("\"%s\"\t\"%s\"\n", cn, String.join(";", cs[0].keySet())));

				// if (++num_docs > 1000) {
				// break;
				// }
			}
		}
		reader.printLast();
		reader.close();
		writer.close();
	}

	public static void run2() throws Exception {

		Vocab vocab = new Vocab();
		vocab.read(KPPath.VOCAB_FILE.replace(".ser", "_pos.ser"));

		CandidateSearcher candSearcher = new CandidateSearcher(KPPath.KEYWORD_POS_CNT_FILE);

		KeyphraseExtractor kwdExtractor = new KeyphraseExtractor(candSearcher, vocab);

		List<String> labels = Generics.newArrayList();
		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_POS_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(10000);

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

				// if (!type.equals("patent")) {
				// continue;
				// }

				String text = korTitle + "\n" + korAbs;
				text = text.trim();

				// if (korKwdStr.length() == 0) {
				// continue;
				// }

				if (text.length() == 0) {
					continue;
				}
				KDocument doc = TaggedTextParser.parse(text);

				// if (doc.sizeOfTokens() < 500) {
				// continue;
				// }

				Set<String> keywordSet = Generics.newHashSet();
				String docText = StrUtils.join("", " ", "\n", doc.getSubValues(TokenAttr.WORD));

				int num_matches = 0;

				for (String kwd : korKwdStr.split(";")) {
					if (kwd.length() > 0) {
						KSentence kwdSent = TaggedTextParser.parse(kwd).toSentence();
						String s = StrUtils.join(" ", "", kwdSent.getSubValues(TokenAttr.WORD));

						keywordSet.add(s);

						if (docText.contains(s)) {
							num_matches++;
						}
					}
				}

				if (num_matches == 0) {
					continue;
				}

				StringBuffer sb = new StringBuffer();

				System.out.println("##User Kewwords:");

				for (String kwd : keywordSet) {
					System.out.println(kwd);
				}

				System.out.println("##System Kewwords:");
				Counter<String> phrScores = kwdExtractor.extract(doc);

				for (String kwd : keywordSet) {
					if (phrScores.containsKey(kwd)) {
						System.out.println(kwd + " *");
					} else {
						System.out.println(kwd);
					}
				}
				System.out.println();

			}

		}
		reader.printLast();
		reader.close();

	}

	private Vocab vocab;

	private CandidateSearcher candSearcher;

	public KeyphraseExtractor(CandidateSearcher candSearcher, Vocab vocab) {
		this.candSearcher = candSearcher;
		this.vocab = vocab;
	}

	public Counter<String> extract(KDocument doc) {
		Counter<String> ret = Generics.newCounter();

		// KSentence[] sents = new KSentence[doc.size()];
		//
		// for (int i = 0; i < doc.size(); i++) {
		// sents[i] = new KSentence(doc.getSentence(i).getSubTokens());
		// }
		// KDocument temp = new KDocument(sents);

		List<KSentence> cands = candSearcher.search(doc);

		if (cands.size() == 0) {
			return ret;
		}

		KSentence sent = doc.toSentence();

		CounterMap<String, String> cm1 = Generics.newCounterMap();
		// CounterMap<String, String> cm2 = Generics.newCounterMap();

		int window_size = 2;

		for (int i = 0; i < cands.size(); i++) {
			KSentence cand1 = cands.get(i);
			String candStr1 = "";

			String content = "";

			for (String word : content.split(" ")) {
				cm1.incrementCount(candStr1, word, 1);
			}

			int left = cand1.getFirst().getStart();
			int right = cand1.getLast().getStart();

			{
				int start = Math.max(left - window_size, 0);
				int end = left;

				if (start >= 0) {
					// String context = sent.joinValues("", " ", new TokenAttr[] { TokenAttr.WORD }, start, end);
					String context = "";
					for (String word : context.split(" ")) {
						cm1.incrementCount(candStr1, word, 1);
					}
				}
			}

			{
				int start = right + 1;
				int end = start + window_size;

				if (end < sent.size()) {
					// String context = sent.joinValues("", " ", new TokenAttr[] { TokenAttr.WORD }, start, end);
					String context = "";
					for (String word : context.split(" ")) {
						cm1.incrementCount(candStr1, word, 1);
					}
				}
			}

			// for (int j = i + 1; j < cands.size(); j++) {
			// KSentence cand2 = cands.get(j);
			// String candStr2 = cand2.joinValues("", "", new TokenAttr[] { TokenAttr.WORD }, 0, cand2.size());
			//
			// int dist = cand2.getFirst().getStart() - cand1.getLast().getStart();
			//
			// if (dist < 2) {
			// cm2.incrementCount(candStr1, candStr2, 1);
			// }
			// }
		}

		for (String cand : cm1.keySet()) {
			Counter<String> wordCnts = cm1.getCounter(cand);
			for (String word : wordCnts.keySet()) {
				double cnt = wordCnts.getCount(word);
				double doc_freq = vocab.getWordDocFreq(word);
				if (doc_freq == 0) {
					doc_freq = vocab.getNumDocs();
				}
				double tfidf = TermWeighting.tfidf(cnt, vocab.getNumDocs(), doc_freq);
				wordCnts.setCount(word, tfidf);
			}
			ret.setCount(cand, wordCnts.average());
		}

		Indexer<String> pIndexer = Generics.newIndexer();
		Indexer<String> wIndexer = Generics.newIndexer();

		for (String p : cm1.keySet()) {
			pIndexer.add(p);
			for (String w : cm1.getCounter(p).keySet()) {
				wIndexer.add(w);
			}
		}

		double[][] mat = ArrayUtils.matrix(pIndexer.size(), wIndexer.size(), 0);

		for (String phr : cm1.keySet()) {
			int k = pIndexer.indexOf(phr);
			Counter<String> c = cm1.getCounter(phr);
			for (Entry<String, Double> e : c.entrySet()) {
				int w = wIndexer.indexOf(e.getKey());
				mat[k][w] = e.getValue();
			}
			ArrayMath.normalizeByL2Norm(mat[k], mat[k]);
		}

		double[][] trans_mat = ArrayMath.outerProduct(mat);

		double[] cents = new double[trans_mat.length];

		for (String phr : ret.keySet()) {
			int p = pIndexer.indexOf(phr);
			cents[p] = ret.getCount(phr);
		}

		ArrayMath.normalizeColumns(trans_mat);

		ArrayMath.randomWalk(trans_mat, cents, 10, 0.00001, 0.85);

		Counter<String> phrCents = Generics.newCounter();

		for (int i = 0; i < pIndexer.size(); i++) {
			phrCents.setCount(pIndexer.getObject(i), cents[i]);
		}

		System.out.printf("Weights:\t%s\n", ret.toString(5));
		System.out.printf("Cents:\t%s\n", phrCents.toString(5));

		return ret;
	}

	public Counter<String> extract(String text) {
		return extract(TaggedTextParser.parse(text));
	}

}
