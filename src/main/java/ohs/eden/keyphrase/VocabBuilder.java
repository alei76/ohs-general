package ohs.eden.keyphrase;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import ohs.io.TextFileReader;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.Vocab;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class VocabBuilder {

	public static final String NONE = "<none>";

	public static void buildGramVocab() throws Exception {

		String[] inFileNames = { KPPath.PAPER_DUMP_FILE, KPPath.REPORT_DUMP_FILE, KPPath.PATENT_DUMP_FILE };

		Indexer<String> wordIndexer = Generics.newIndexer();
		Counter<Integer> wordDocFreqs = Generics.newCounter();
		Counter<Integer> wordCnts = Generics.newCounter();
		int num_docs = 0;

		GramGenerator gg = new GramGenerator(3);

		for (int i = 0; i < inFileNames.length; i++) {
			String inFileName = inFileNames[i];

			TextFileReader reader = new TextFileReader(inFileName);
			List<String> labels = Generics.newArrayList();

			while (reader.hasNext()) {
				// if (reader.getNumLines() > 100000) {
				// break;
				// }

				String line = reader.next();
				String[] parts = line.split("\t");

				for (int j = 0; j < parts.length; j++) {
					if (parts[j].length() > 1) {
						parts[j] = parts[j].substring(1, parts[j].length() - 1);
					}
				}

				if (reader.getNumLines() == 1) {
					for (String p : parts) {
						labels.add(p);
					}
				} else {
					if (parts.length != labels.size()) {
						continue;
					}

					Counter<String> c = Generics.newCounter();

					if (i == 0) {
						String korAbs = parts[10];
						String engAbs = parts[11];

						if (korAbs.length() > 0) {
							korAbs = StringEscapeUtils.unescapeHtml(korAbs);
							for (Gram gram : gg.generateQGrams(korAbs.toLowerCase())) {
								c.incrementCount(gram.getString(), 1);
							}
						}

						if (engAbs.length() > 0) {
							engAbs = StringEscapeUtils.unescapeHtml(engAbs);
							for (Gram gram : gg.generateQGrams(engAbs.toLowerCase())) {
								c.incrementCount(gram.getString(), 1);
							}
						}
					} else if (i == 1) {
						String korAbs = parts[7];
						String engAbs = parts[8];

						if (korAbs.length() > 0) {
							korAbs = StringEscapeUtils.unescapeHtml(korAbs);
							for (Gram gram : gg.generateQGrams(korAbs.toLowerCase())) {
								c.incrementCount(gram.getString(), 1);
							}
						}

						if (engAbs.length() > 0) {
							engAbs = StringEscapeUtils.unescapeHtml(engAbs);
							for (Gram gram : gg.generateQGrams(engAbs.toLowerCase())) {
								c.incrementCount(gram.getString(), 1);
							}
						}
					} else if (i == 3) {
						String applno = parts[0];
						String kor_title = parts[1];
						String eng_title = parts[2];
						String cn = parts[3];
						String korAbs = parts[4];
						String cm = parts[5];

						if (korAbs.length() > 0) {
							korAbs = StringEscapeUtils.unescapeHtml(korAbs);
							for (Gram gram : gg.generateQGrams(korAbs.toLowerCase())) {
								c.incrementCount(gram.getString(), 1);
							}
						}
					}
					for (String gram : c.keySet()) {
						double cnt = c.getCount(gram);
						int gid = wordIndexer.getIndex(gram);
						wordCnts.incrementCount(gid, cnt);
						wordDocFreqs.incrementCount(gid, 1);
					}
					num_docs++;
				}

			}
			reader.close();
		}

		int[] word_cnts = new int[wordIndexer.size()];
		int[] word_doc_freqs = new int[wordIndexer.size()];

		for (int i = 0; i < wordIndexer.size(); i++) {
			word_cnts[i] = (int) wordCnts.getCount(i);
			word_doc_freqs[i] = (int) wordDocFreqs.getCount(i);
		}

		Vocab vocab = new Vocab(wordIndexer, word_cnts, word_doc_freqs, num_docs);
		vocab.write(KPPath.VOCAB_FILE.replace(".ser", "_all.ser"));
	}

	public static void buildVocabPos() throws Exception {
		Indexer<String> wordIndexer = Generics.newIndexer();
		Counter<Integer> wordDocFreqs = Generics.newCounter();
		Counter<Integer> wordCnts = Generics.newCounter();
		List<String> labels = Generics.newArrayList();
		int num_docs = 0;

		TextFileReader reader = new TextFileReader(KPPath.SINGLE_DUMP_POS_FILE);
		reader.setPrintNexts(false);
		while (reader.hasNext()) {
			reader.printProgress();

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

				String text = StrUtils.join("\n", new String[] { korTitle, korAbs });

				if (text.length() > 0) {
					KDocument doc = TaggedTextParser.parse(text);

					doc.getSubValues(TokenAttr.WORD);

					Counter<String> c = Generics.newCounter();

					for (Token t : doc.getSubTokens()) {
						c.incrementCount(t.get(TokenAttr.WORD).toLowerCase(), 1);
					}

					for (String word : c.keySet()) {
						int cnt = (int) c.getCount(word);
						int w = wordIndexer.getIndex(word);
						wordCnts.incrementCount(w, cnt);
						wordDocFreqs.incrementCount(w, 1);
					}
					num_docs++;
				}
			}
		}
		reader.printProgress();
		reader.close();

		int[] word_cnts = new int[wordIndexer.size()];
		int[] word_doc_freqs = new int[wordIndexer.size()];

		for (int i = 0; i < wordIndexer.size(); i++) {
			word_cnts[i] = (int) wordCnts.getCount(i);
			word_doc_freqs[i] = (int) wordDocFreqs.getCount(i);
		}

		Vocab vocab = new Vocab(wordIndexer, word_cnts, word_doc_freqs, num_docs);
		vocab.write(KPPath.VOCAB_FILE.replace(".ser", "_pos.ser"));
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// buildGramVocab2();

		buildVocabPos();

		System.out.println("ends begins.");
	}

}
