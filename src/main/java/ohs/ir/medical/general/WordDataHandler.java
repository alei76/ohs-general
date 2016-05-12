package ohs.ir.medical.general;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.StrUtils;

public class WordDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		WordDataHandler dh = new WordDataHandler();
		String[] indexDirNames = MIRPath.IndexDirNames;
		String[] vocFileNames = MIRPath.VocFileNames;

		String[] bigramFileNames = { MIRPath.TREC_CDS_PROXIMITY_DIR, MIRPath.CLEF_EHEALTH_DIR, MIRPath.OHSUMED_DIR, MIRPath.WIKI_DIR };
		String[] bigramFileNames2 = { MIRPath.TREC_CDS_PROXIMITY_DIR, MIRPath.CLEF_EHEALTH_DIR, MIRPath.OHSUMED_DIR, MIRPath.WIKI_DIR };

		for (int i = 0; i < bigramFileNames.length; i++) {
			bigramFileNames[i] = bigramFileNames[i] + "bigrams.txt";
			bigramFileNames2[i] = bigramFileNames2[i] + "bigrams_filtered.txt";
		}

		// {
		// for (int i = 4; i < indexDirNames.length; i++) {
		// dh.makeVocabulary(indexDirNames[i], vocFileNames[i]);
		// }
		// dh.mergeVocabularies(vocFileNames, MIRPath.VOCAB_FILE);
		// }

		// dh.extractBigrams(indexDirNames[0], bigramFileNames[0]);

		dh.process(vocFileNames[0], bigramFileNames[0], bigramFileNames2[0]);

		System.out.println("process ends.");
	}

	public void extractBigrams(String indexDirName, String outputFileName) throws Exception {
		IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(indexDirName);
		IndexReader indexReader = indexSearcher.getIndexReader();

		TextFileWriter writer = new TextFileWriter(outputFileName);

		int max_docs = indexReader.maxDoc();
		for (int i = 0; i < max_docs; i++) {
			if ((i + 1) % 1000 == 0) {
				System.out.printf("\r[%d/%d]", i + 1, max_docs);
			}

			Document doc = indexReader.document(i);
			String title = doc.get(CommonFieldNames.TITLE);

			Terms termVector = indexReader.getTermVector(i, CommonFieldNames.CONTENT);

			if (termVector == null) {
				continue;
			}

			TermsEnum termsEnum = null;
//			termsEnum = termVector.iterator();

			BytesRef bytesRef = null;
			PostingsEnum postingsEnum = null;
			Map<Integer, String> wordLocs = new HashMap<Integer, String>();

			while ((bytesRef = termsEnum.next()) != null) {
				postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.ALL);

				if (postingsEnum.nextDoc() != 0) {
					throw new AssertionError();
				}

				String word = bytesRef.utf8ToString();
				int freq = postingsEnum.freq();

				for (int k = 0; k < freq; k++) {
					final int position = postingsEnum.nextPosition();
					wordLocs.put(position, word);
				}
			}

			List<Integer> locs = new ArrayList<Integer>(wordLocs.keySet());
			Collections.sort(locs);

			List<String> words = new ArrayList<String>();

			for (int loc : locs) {
				words.add(wordLocs.get(loc));
			}

			CounterMap<String, String> cm = new CounterMap<String, String>();

			for (int j = 1; j < words.size(); j++) {
				String w1 = words.get(j - 1);
				String w2 = words.get(j);
				cm.incrementCount(w1, w2, 1);
			}

			if (cm.size() == 0) {
				continue;
			}

			List<String> ws1 = cm.getOutKeyCountSums().getSortedKeys();
			StringBuffer sb = new StringBuffer();
			sb.append(String.format("DocID:\t%d\n", i));

			for (int j = 0; j < ws1.size(); j++) {
				String w1 = ws1.get(j);
				Counter<String> c = cm.getCounter(w1);

				List<String> ws2 = c.getSortedKeys();

				sb.append(String.format("%s:%d", w1, (int) c.totalCount()));

				for (int k = 0; k < ws2.size(); k++) {
					String w2 = ws2.get(k);
					int cnt = (int) c.getCount(w2);
					sb.append(String.format("\t%s:%d", w2, cnt));
				}

				if (j != ws1.size() - 1) {
					sb.append("\n");
				}
			}

			writer.write(sb.toString());

			if (i != max_docs - 1) {
				writer.write("\n\n");
			}
		}

		writer.close();

	}

	public void makeVocabulary(String indexDirName, String vocFileName) throws Exception {
		System.out.printf("make a vocabulary from [%s]\n", indexDirName);
		IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(indexDirName);

		IndexReader indexReader = indexSearcher.getIndexReader();

		Fields fields = MultiFields.getFields(indexReader);
		Terms terms = fields.terms(CommonFieldNames.CONTENT);

		Counter<String> c = new Counter<String>();

		TermsEnum termsEnum = terms.iterator();
		BytesRef bytesRef = null;

		while ((bytesRef = termsEnum.next()) != null) {
			String word = bytesRef.utf8ToString();
			int docFreq = termsEnum.docFreq();
			double cnt = termsEnum.totalTermFreq();
			c.incrementCount(word, cnt);
		}

		TextFileWriter writer = new TextFileWriter(vocFileName);
		for (String word : c.getSortedKeys()) {
			double cnt = c.getCount(word);
			writer.write(word + "\t" + (int) cnt + "\n");
		}
		writer.close();
	}

	public void mergeVocabularies(String[] inputFileNames, String outputFileName) throws Exception {
		Counter<String> counter = new Counter<String>();
		for (int i = 0; i < inputFileNames.length; i++) {
			Counter<String> c = FileUtils.readStrCounter(inputFileNames[i]);
			counter.incrementAll(c);
		}
		FileUtils.writeStrCounter(outputFileName, counter);
	}

	public void process(String vocFileName, String cmFileName, String outputFileName) throws Exception {
		Indexer<String> wordIndexer = new Indexer<String>();
		Counter<Integer> collWordCounts = new Counter<Integer>();

		{
			Counter<String> c = FileUtils.readStrCounter(vocFileName);

			for (String word : c.getSortedKeys()) {
				int w = wordIndexer.getIndex(word);
				double cnt = c.getCount(word);
				collWordCounts.setCount(w, cnt);
			}
		}

		CounterMap<Integer, Integer> ccm = new CounterMap<Integer, Integer>();

		int mininum_cnt = 100;

		TextFileReader reader = new TextFileReader(cmFileName);
		reader.setPrintNexts(true);

		while (reader.hasNext()) {
			reader.printProgress();

			if (reader.getNumNexts() > 2000) {
				break;
			}

			List<String> lines = reader.getNextLines();

			String[] parts = lines.get(0).split("\t");
			int docId = Integer.parseInt(parts[1]);

			CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();

			for (int i = 1; i < lines.size(); i++) {
				String line = lines.get(i);
				parts = line.split("\t");
				int w1 = -1;
				String word1 = null;
				String word2 = null;

				for (int j = 0; j < parts.length; j++) {
					String[] two = StrUtils.split2Two(":", parts[j]);

					if (j == 0) {
						word1 = two[0];
						w1 = wordIndexer.indexOf(word1);
						double cnt_w_in_coll = collWordCounts.getCount(w1);
						if (cnt_w_in_coll < mininum_cnt || word1.contains("#")) {
							break;
						}
					} else {
						word2 = two[0];
						double cnt = Double.parseDouble(two[1]);
						int w2 = wordIndexer.indexOf(word2);
						double cnt_w_in_coll = collWordCounts.getCount(w2);
						if (cnt_w_in_coll < mininum_cnt || word2.contains("#")) {
							continue;
						}
						cm.incrementCount(w1, w2, cnt);
					}
				}
			}
			ccm.incrementAll(cm);
		}
		reader.printProgress();
		reader.close();

		collWordCounts.normalize();

		TextFileWriter writer = new TextFileWriter(outputFileName);

		for (int w : ccm.keySet()) {
			Counter<Integer> c1 = ccm.getCounter(w);
			c1.normalize();

			// Counter<Integer> c2 = new Counter<Integer>();
			//
			// for (int t : c1.keySet()) {
			// double prob_w_to_t = c1.getCount(t);
			// double prob_w_in_coll = collWordCounts.getCount(t);
			// double mixture = 0.5;
			// prob_w_to_t = ArrayMath.addAfterScale(new double[] { prob_w_to_t, prob_w_in_coll }, new double[] { 1 - mixture, mixture });
			// prob_w_to_t = Math.log(prob_w_to_t);
			// c1.setCount(t, prob_w_to_t);
			// }
			// ccm.setCounter(w, c1);
		}

		ccm = ccm.invert();

		List<Integer> ws1 = ccm.getOutKeyCountSums().getSortedKeys();

		for (int i = 0; i < ws1.size(); i++) {
			int w1 = ws1.get(i);
			String word1 = wordIndexer.getObject(w1);
			Counter<Integer> c = ccm.getCounter(w1);

			List<Integer> ws2 = c.getSortedKeys();

			StringBuffer sb = new StringBuffer();
			sb.append(String.format("%s:%f", word1, c.totalCount()));

			for (int j = 0; j < ws2.size(); j++) {
				int w2 = ws2.get(j);
				double cnt = c.getCount(w2);
				String word2 = wordIndexer.getObject(w2);
				sb.append(String.format("\t%s:%f", word2, cnt));
			}
			writer.write(sb.toString());

			if (i != ws1.size() - 1) {
				writer.write("\n");
			}
		}
		writer.close();

	}

}
