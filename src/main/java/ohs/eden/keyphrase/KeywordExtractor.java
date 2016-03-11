package ohs.eden.keyphrase;

import java.util.List;
import java.util.Map.Entry;

import ohs.io.TextFileReader;
import ohs.ling.types.Document;
import ohs.ling.types.Sentence;
import ohs.ling.types.TokenAttr;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.Vocab;
import ohs.utils.Generics;
import ohs.utils.StrUtils;
import ohs.utils.TermWeighting;

public class KeywordExtractor {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", KeywordExtractor.class.getName());
		// test1();
		test2();
		System.out.printf("ends.");
	}

	public static void test1() throws Exception {
		Vocab vocab = new Vocab();
		vocab.read(KPPath.VOCAB_FILE.replace(".ser", "_all.ser"));

		KeywordData kwdData = new KeywordData();
		kwdData.read(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));

		KeywordExtractor e = new KeywordExtractor(null, vocab, kwdData.getKeywordIndexer().getObjects());

		TextFileReader reader = new TextFileReader(KPPath.KEYWORD_EXTRACTOR_DIR + "raw_tagged.txt");
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String keywordStr = lines.get(0);
			String text = lines.get(1);

			System.out.println(keywordStr);
			e.extract(text);
		}
		reader.close();

	}

	public static void test2() throws Exception {
		Vocab vocab = new Vocab();
		vocab.read(KPPath.VOCAB_FILE.replace(".ser", "_pos.ser"));

		// KeywordData kwdData = new KeywordData();
		// kwdData.read(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));

		CandidateSearcher candSearcher = new CandidateSearcher(KPPath.KEYWORD_POS_CNT_FILE);

		KeywordExtractor kwdExtractor = new KeywordExtractor(candSearcher, vocab, null);

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

				String text = korTitle + "\n" + korAbs;
				text = text.trim();

				if (korKwdStr.length() == 0) {
					continue;
				}

				if (text.length() == 0) {
					continue;
				}
				Document doc = TaggedTextParser.parse(text);

				if (doc.sizeOfTokens() < 500) {
					continue;
				}

				System.out.println("##User Kewwords:");
				for (String kwd : korKwdStr.split(";")) {
					Sentence kwdSent = TaggedTextParser.parse(kwd).toSentence();

					System.out.println(kwdSent.joinValues("", "", new TokenAttr[] { TokenAttr.WORD }, 0, kwdSent.size()));
				}

				System.out.println("##System Kewwords:");
				Counter<String> kwds = kwdExtractor.extract(doc);
				System.out.println(kwds.toStringSortedByValues(true, true, 20, "\t"));
				System.out.println();
			}

		}
		reader.printLast();
		reader.close();

	}

	private Vocab vocab;

	private GramGenerator gg = new GramGenerator(3);

	private List<String> keywords;

	private Counter<String> kwdGramCnts;

	private CandidateSearcher candSearcher;

	public KeywordExtractor(CandidateSearcher candSearcher, Vocab vocab, List<String> keywords) {
		this.candSearcher = candSearcher;
		this.vocab = vocab;
		this.keywords = keywords;

		// prepareKeywordQGrams();
	}

	public Counter<String> extract(Document doc) {
		Counter<String> ret = Generics.newCounter();

		List<Sentence> cands = candSearcher.search(doc);

		if (cands.size() == 0) {
			return ret;
		}

		Sentence sent = doc.toSentence();

		CounterMap<String, String> cm1 = Generics.newCounterMap();
		// CounterMap<String, String> cm2 = Generics.newCounterMap();

		int window_size = 2;

		for (int i = 0; i < cands.size(); i++) {
			Sentence cand1 = cands.get(i);
			String candStr1 = cand1.joinValues("", "", new TokenAttr[] { TokenAttr.WORD }, 0, cand1.size());
			String content = cand1.joinValues("", " ", new TokenAttr[] { TokenAttr.WORD }, 0, cand1.size());

			for (String word : content.split(" ")) {
				cm1.incrementCount(candStr1, word, 1);
			}

			int left = cand1.getFirst().getStart();
			int right = cand1.getLast().getStart();

			{
				int start = Math.max(left - window_size, 0);
				int end = left;

				if (start >= 0) {
					String context = sent.joinValues("", " ", new TokenAttr[] { TokenAttr.WORD }, start, end);
					for (String word : context.split(" ")) {
						cm1.incrementCount(candStr1, word, 1);
					}
				}
			}

			{
				int start = right + 1;
				int end = start + window_size;

				if (end < sent.size()) {
					String context = sent.joinValues("", " ", new TokenAttr[] { TokenAttr.WORD }, start, end);
					for (String word : context.split(" ")) {
						cm1.incrementCount(candStr1, word, 1);
					}
				}
			}

			// for (int j = i + 1; j < cands.size(); j++) {
			// Sentence cand2 = cands.get(j);
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

		System.out.printf("Weights:\t%s\n", ret);
		System.out.printf("Cents:\t%s\n", phrCents);

		return ret;
	}

	public Counter<String> extract(String text) {
		return extract(TaggedTextParser.parse(text));
	}

	public Counter<String> extract00(String text) {
		Counter<String> ret = Generics.newCounter();

		int window_size = 2;
		int gram_size = 2;

		text = text.replace(". ", ".\n");
		String[] sents = text.split("\n");
		List[] wss = new List[sents.length];

		CounterMap<String, String> phraseMapCnts = Generics.newCounterMap();
		Counter<String> gramCnts = Generics.newCounter();
		Counter<String> phraseCnts = Generics.newCounter();
		Counter<String> wordCnts = Generics.newCounter();

		for (int i = 0; i < sents.length; i++) {
			List<String> words = StrUtils.split(sents[i].toLowerCase().trim());
			for (int j = 0; j < words.size(); j++) {
				for (int k = 1; k <= gram_size; k++) {
					int p_start = j;
					int p_end = j + k;

					if (p_end < words.size()) {
						String phrase = StrUtils.join("_", words, p_start, p_end);
						phraseCnts.incrementCount(phrase, 1);

						int c_start = p_end;
						int c_end = Math.min(p_end + window_size, words.size());

						for (int n = c_start; n < c_end; n++) {
							String word = words.get(n);
							phraseMapCnts.incrementCount(phrase, word, 1);
							phraseMapCnts.incrementCount(word, phrase, 1);
						}
					}
				}

				String word = words.get(j);
				wordCnts.incrementCount(word, 1);

				for (Gram gram : gg.generateQGrams(word)) {
					gramCnts.incrementCount(gram.getString(), 1);
				}
			}
		}

		Counter<String> gramWeights = Generics.newCounter();
		for (String gram : gramCnts.keySet()) {
			double cnt = gramCnts.getCount(gram);
			double doc_freq = vocab.getWordDocFreq(gram);
			if (doc_freq == 0) {
				doc_freq = vocab.getNumDocs();
			}
			double tfidf = TermWeighting.tfidf(cnt, vocab.getNumDocs(), doc_freq);
			gramWeights.setCount(gram, tfidf);
		}

		Counter<String> gramProbs = Generics.newCounter();

		double[] mixtures = { 1, 1, 1 };

		ArrayMath.normalize(mixtures);

		for (String gram : gramCnts.keySet()) {
			double prob = gramCnts.getProbability(gram);
			double prob_in_kwd_data = kwdGramCnts.getProbability(gram);
			double prob_in_col = vocab.getWordProb(gram);
			double[] probs = { prob, prob_in_kwd_data, prob_in_col };
			double new_prob = ArrayMath.dotProduct(mixtures, probs);
			gramProbs.setCount(gram, new_prob);
		}

		Counter<String> phraseWeights = Generics.newCounter();
		Counter<String> phraseProbs = Generics.newCounter();

		for (String phrase : phraseCnts.keySet()) {
			Gram[] grams = gg.generateQGrams(phrase);
			double[] weights = new double[grams.length];
			double[] probs = new double[grams.length];
			for (int j = 0; j < grams.length; j++) {
				weights[j] = gramWeights.getCount(grams[j].getString());
				probs[j] = gramProbs.getCount(grams[j].getString());
			}
			phraseWeights.setCount(phrase, ArrayMath.mean(weights));
			phraseProbs.setCount(phrase, ArrayMath.mean(probs));
		}

		Indexer<String> phraseIndexer = Generics.newIndexer();

		for (String p : phraseMapCnts.keySet()) {
			phraseIndexer.add(p);
		}

		double[][] trans_mat = ArrayUtils.diagonal(phraseIndexer.size(), 1);

		for (String p1 : phraseMapCnts.keySet()) {
			int pid1 = phraseIndexer.indexOf(p1);
			double weight1 = phraseProbs.getCount(p1);
			Counter<String> c = phraseMapCnts.getCounter(p1);
			for (String p2 : c.getSortedKeys()) {
				int pid2 = phraseIndexer.indexOf(p2);
				double weight2 = phraseProbs.getCount(p2);
				double cocont = c.getCount(p2);
				trans_mat[pid1][pid2] = cocont * weight1 * weight2;
				trans_mat[pid2][pid1] = cocont * weight1 * weight2;
			}
		}

		ArrayMath.normalizeColumns(trans_mat);

		double[] cents = ArrayUtils.array(phraseIndexer.size(), 1f / phraseIndexer.size());

		ArrayMath.randomWalk(trans_mat, cents, 10, 0.00001, 0.85);

		Counter<String> phraseCents = Generics.newCounter();

		for (int i = 0; i < phraseIndexer.size(); i++) {
			phraseCents.setCount(phraseIndexer.getObject(i), cents[i]);
		}

		System.out.println("Cnts\t" + phraseCnts);
		System.out.println("Weights\t" + phraseWeights);
		System.out.println("Probs\t" + phraseProbs);
		System.out.println("Cents\t" + phraseCents);
		System.out.println();

		return ret;
	}

	private void prepareKeywordQGrams() {
		kwdGramCnts = Generics.newCounter();

		for (String kwd : keywords) {
			for (Gram gram : gg.generateQGrams(kwd.toLowerCase())) {
				kwdGramCnts.incrementCount(gram.getString(), 1);
			}
		}
	}

}
