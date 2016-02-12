package ohs.eden.keyphrase;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.SetMap;
import ohs.types.Vocab;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.extractKeywordData();
		// dh.process();
		// dh.analyze();
		dh.makeVocab();
		System.out.println("process ends.");
	}

	final String NONE = "<none>";

	public void extractKeywordData() throws Exception {
		String[] inFileNames = { KPPath.PAPER_DUMP_FILE, KPPath.REPORT_DUMP_FILE };

		SetMap<String, String> keywordDocs = Generics.newSetMap();

		TextFileWriter writer = new TextFileWriter(KPPath.ABSTRACT_FILE);

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

					String cn = parts[0];
					String korAbs = parts[parts.length - 2];
					String engAbs = parts[parts.length - 1];
					String korKeywordStr = null;
					String engKeywordStr = null;

					if (i == 0) {
						korKeywordStr = parts[8];
						engKeywordStr = parts[9];
					} else if (i == 1) {
						korKeywordStr = parts[5];
						engKeywordStr = parts[6];
					}

					List<String> kors = getKeywords(korKeywordStr);
					List<String> engs = getKeywords(engKeywordStr);

					if (kors.size() == 0 && engs.size() == 0) {

					} else if (kors.size() == engs.size()) {
						if (kors.size() > 0) {
							for (int j = 0; j < kors.size(); j++) {
								String kor = kors.get(j);
								String eng = engs.get(j);
								keywordDocs.put(kor + "\t" + eng, cn);
							}
						}
					} else {
						for (String kor : kors) {
							keywordDocs.put(kor + "\t" + NONE, cn);
						}

						for (String eng : engs) {
							keywordDocs.put(NONE + "\t" + eng, cn);
						}
					}

					writer.write(String.format("\"%s\"\t\"%s\"\t\"%s\"\n", cn, korAbs, engAbs));
				}

			}
			reader.close();
		}
		writer.close();

		FileUtils.writeStrSetMap(KPPath.KEYWORD_DATA_FILE, keywordDocs);
		// FileUtils.write(KPPath.PAPER_KOREAN_CONTEXT_FILE, cm2, null, true);
		// FileUtils.write(KPPath.PAPER_ENGLISH_CONTEXT_FILE, cm3, null, true);
		// FileUtils.write(KWPath.PAPER_KEYWORD_FILE, kwCounts, true);

	}

	private List<String> getKeywords(String keywordStr) {
		List<String> ret = Generics.newArrayList();
		for (String kw : keywordStr.split(";")) {
			kw = kw.trim();
			if (kw.length() > 0) {
				ret.add(kw);
			}
		}
		return ret;
	}

	public void analyze() throws Exception {
		KeywordData kwdData = new KeywordData();
		kwdData.read(KPPath.KEYWORD_DATA_FILE.replace(".txt", ".ser"));

		CounterMap<String, String> kwdTypeCounts = Generics.newCounterMap();
		Counter<String> typeCounts = Generics.newCounter();

		for (int kwdid : kwdData.getKeywordDocs().keySet()) {
			String kwd = kwdData.getKeywordIndexer().getObject(kwdid);
			List<Integer> docids = kwdData.getKeywordDocs().get(kwdid);
			for (int docid : docids) {
				String docId = kwdData.getDocIndexer().getObject(docid);
				String[] parts = docId.split("_");
				String type = parts[0];
				String cn = parts[1];
				kwdTypeCounts.incrementCount("KWD", type, 1);
			}
		}

		FileUtils.writeStrCounterMap(KPPath.KEYWORD_TEMP_FILE, kwdTypeCounts);

	}

	public void makeVocab() throws Exception {
		TextFileReader reader = new TextFileReader(KPPath.ABSTRACT_FILE);

		Indexer<String> wordIndexer = Generics.newIndexer();
		Counter<Integer> wordDocFreqs = Generics.newCounter();
		Counter<Integer> wordCnts = Generics.newCounter();

		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");

			for (int i = 0; i < parts.length; i++) {
				parts[i] = parts[i].substring(1, parts[i].length() - 1);
			}

			String cn = parts[0];
			String korAbs = parts[1];
			String engAbs = parts[2];

			Counter<String> c = Generics.newCounter();

			if (!korAbs.equals(NONE)) {
				c.incrementAll(getWordCounts(korAbs));
			}

			if (!engAbs.equals(NONE)) {
				c.incrementAll(getWordCounts(engAbs));
			}

			for (Entry<String, Double> e : c.entrySet()) {
				String word = e.getKey();
				double cnt = e.getValue();
				int w = wordIndexer.getIndex(word);
				wordDocFreqs.incrementCount(w, 1);
				wordCnts.incrementCount(w, cnt);
			}
		}
		reader.close();

		int[] word_cnts = new int[wordIndexer.size()];
		int[] word_doc_freqs = new int[wordIndexer.size()];

		for (int i = 0; i < wordIndexer.size(); i++) {
			word_cnts[i] = (int) wordCnts.getCount(i);
			word_doc_freqs[i] = (int) wordDocFreqs.getCount(i);
		}

		Vocab vocab = new Vocab(wordIndexer, word_cnts, word_doc_freqs);
		vocab.write(KPPath.VOCAB_FILE);

	}

	private Counter<String> getWordCounts(String text) {
		Counter<String> ret = Generics.newCounter();
		text = text.replace(". ", " .\n");
		for (String sent : text.split("\n")) {
			for (String word : StrUtils.split(sent)) {
				ret.incrementCount(word.toLowerCase(), 1);
			}
		}
		return ret;
	}

	public void process() throws Exception {
		KeywordData kwdData = new KeywordData();
		kwdData.read(KPPath.KEYWORD_DATA_FILE.replace(".txt", ".ser"));

		Indexer<String> kwdIndexer = kwdData.getKeywordIndexer();
		ListMap<Integer, Integer> docKeywords = Generics.newListMap();
		for (int kwdid : kwdData.getKeywordDocs().keySet()) {
			List<Integer> docids = kwdData.getKeywordDocs().get(kwdid);

			for (int docid : docids) {
				docKeywords.put(docid, kwdid);
			}
		}

		TextFileReader reader = new TextFileReader(KPPath.ABSTRACT_FILE);
		TextFileWriter writer = new TextFileWriter(KPPath.KEYWORD_EXTRACTOR_DIR + "raw_tagged.txt");

		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");

			for (int i = 0; i < parts.length; i++) {
				parts[i] = parts[i].substring(1, parts[i].length() - 1);
			}

			String cn = parts[0];
			int docid = kwdData.getDocIndexer().indexOf(cn);
			String korAbs = parts[1];
			String engAbs = parts[2];

			List<Integer> kwdids = docKeywords.get(docid, false);

			if (kwdids == null) {
				continue;
			}

			String[] abss = { korAbs, engAbs };
			Set<String>[] kwdSets = new HashSet[2];

			for (int j = 0; j < kwdSets.length; j++) {
				kwdSets[j] = Generics.newHashSet();
			}

			for (int kwdid : kwdids) {
				String keyword = kwdIndexer.getObject(kwdid);
				String korKwd = keyword.split("\t")[0];
				String engKwd = keyword.split("\t")[1];
				kwdSets[0].add(korKwd);
				kwdSets[1].add(engKwd);
			}

			for (int j = 0; j < kwdSets.length; j++) {
				if (j != 0) {
					continue;
				}

				String abs = abss[j];

				Set<String> kwdSet = kwdSets[j];
				List<String> founds = Generics.newArrayList();

				for (String kwd : kwdSet) {
					if (abs.contains(kwd)) {
						founds.add(kwd);
					}
				}

				if (founds.size() > 0) {
					writer.write(String.format("Keywords:\t%s\n", StrUtils.join("\t", founds)));
					writer.write(String.format("ABS:\t%s\n\n", abs));
				}

			}
		}
		reader.close();
		writer.close();
	}

}
