package ohs.eden.keyphrase;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import ohs.io.TextFileReader;
import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.Vocab;
import ohs.utils.Generics;
import ohs.utils.KoreanUtils;
import ohs.utils.StrUtils;
import ohs.utils.TermWeighting;

public class KeywordExtractor {

	public static void main(String[] args) throws Exception {
		System.out.printf("[%s] begins.\n", KeywordExtractor.class.getName());
		test1();
		// test2();
		System.out.printf("ends.");
	}

	public static void test1() throws Exception {
		Vocab vocab = new Vocab();
		vocab.read(KPPath.VOCAB_FILE.replace(".ser", "_all.ser"));

		KeywordData kwdData = new KeywordData();
		kwdData.read(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));

		KeywordExtractor e = new KeywordExtractor(vocab, kwdData.getKeywordIndexer().getObjects());

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
		vocab.read(KPPath.VOCAB_FILE.replace(".ser", "_all.ser"));

		KeywordData kwdData = new KeywordData();
		kwdData.read(KPPath.KEYWORD_DATA_FILE.replace("txt", "ser"));

		KeywordExtractor ext = new KeywordExtractor(vocab, kwdData.getKeywordIndexer().getObjects());

		TextFileReader reader = new TextFileReader(KPPath.PATENT_DUMP_FILE);
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}
			// if (reader.getNumLines() > 10) {
			// break;
			// }
			String line = reader.next();
			String[] parts = line.split("\t");

			for (int j = 0; j < parts.length; j++) {
				if (parts[j].length() > 1) {
					parts[j] = parts[j].substring(1, parts[j].length() - 1);
				}
			}

			String applno = parts[0];
			String kor_title = parts[1];
			String eng_title = parts[2];
			String cn = parts[3];
			String korAbs = parts[4];
			String cm = parts[5];

			korAbs = StringEscapeUtils.unescapeHtml(korAbs);

			ext.extract(korAbs);

			System.out.println(line);
		}
		reader.close();
	}

	private Vocab vocab;

	private GramGenerator gg = new GramGenerator(3);

	private List<String> keywords;

	private Counter<String> kwdGramCnts;

	public KeywordExtractor(Vocab vocab, List<String> keywords) {
		this.vocab = vocab;
		this.keywords = keywords;

		prepareKeywordQGrams();
	}

	public Counter<String> extract(String text) {
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

	public Counter<String> extract4(String text) {
		Counter<String> ret = Generics.newCounter();

		int window_size = 2;
		int gram_size = 2;

		text = text.replace(". ", ".\n");
		String[] sents = text.split("\n");
		List[] wss = new List[sents.length];

		CounterMap<String, String> phraseGramCnts = Generics.newCounterMap();
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

						{
							int c_start = Math.max(0, p_start - window_size);
							int c_end = p_start;

							for (int n = c_start; n < c_end && n < words.size(); n++) {
								for (Gram gram : gg.generateQGrams(words.get(n))) {
									phraseGramCnts.incrementCount(phrase, gram.getString(), 1);
								}
							}
						}

						{
							int c_start = p_end;
							int c_end = p_end + window_size;
							for (int n = c_start; n < c_end && n < words.size(); n++) {
								for (Gram gram : gg.generateQGrams(words.get(n))) {
									phraseGramCnts.incrementCount(phrase, gram.getString(), 1);
								}
							}
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

		double check_sum = ArrayMath.normalize(mixtures);

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

		for (String phrase : phraseWeights.getSortedKeys()) {
			phraseIndexer.add(phrase);
		}

		Indexer<String> gramIndexer = Generics.newIndexer();

		for (String gram : gramWeights.getSortedKeys()) {
			gramIndexer.add(gram);
		}

		double[][] phraseGramMat = ArrayUtils.matrix(phraseIndexer.size(), gramIndexer.size(), 0);

		for (String phrase : phraseGramCnts.keySet()) {
			int pid = phraseIndexer.indexOf(phrase);
			Counter<String> c = phraseGramCnts.getCounter(phrase);
			for (String gram : c.keySet()) {
				int gid = gramIndexer.indexOf(gram);
				phraseGramMat[pid][gid] = gramWeights.getCount(gram);
			}
		}

		double[][] trans_mat = ArrayMath.outerProduct(phraseGramMat);

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
