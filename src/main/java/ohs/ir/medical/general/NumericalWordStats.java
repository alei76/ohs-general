package ohs.ir.medical.general;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import ohs.io.FileUtils;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.types.Counter;
import ohs.types.CounterMap;

public class NumericalWordStats {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		NumericalWordStats a = new NumericalWordStats();
		a.analyze1();

		System.out.println("process ends.");
	}

	public void analyze1() throws Exception {

		String[] vocFileNames = MIRPath.VocFileNames;
		String[] collNames = MIRPath.CollNames;
		CounterMap<String, String> cm1 = new CounterMap<String, String>();
		CounterMap<String, String> cm2 = new CounterMap<String, String>();

		String[] labels = { "Numerical", "Non-numerical" };

		for (int i = 0; i < vocFileNames.length; i++) {
			String vocFileName = vocFileNames[i];
			String collName = collNames[i];

			if (vocFileName.contains("wiki")) {
				continue;
			}
			Counter<String> c = FileUtils.readCounter(vocFileName);

			for (String word : c.keySet()) {
				double cnt = c.getCount(word);
				if (word.contains("#")) {
					cm1.incrementCount(collName, labels[0], cnt);
					cm2.incrementCount(collName, labels[0], 1);
				} else {
					cm1.incrementCount(collName, labels[1], cnt);
					cm2.incrementCount(collName, labels[1], 1);
				}
			}
		}

		{
			StringBuffer sb = new StringBuffer();
			sb.append("##");

			for (int i = 0; i < labels.length; i++) {
				sb.append("\t" + labels[i]);
			}
			sb.append("\n");

			for (int i = 0; i < collNames.length; i++) {
				String collName = collNames[i];
				sb.append(collName);

				for (int j = 0; j < labels.length; j++) {
					String label = labels[j];
					double cnt = cm1.getCount(collName, label);
					sb.append("\t" + (int) cnt);
				}

				if (i != collNames.length - 1) {
					sb.append("\n");
				}
			}

			System.out.println(sb.toString());
		}

		{
			StringBuffer sb = new StringBuffer();
			sb.append("##");

			for (int i = 0; i < labels.length; i++) {
				sb.append("\t" + labels[i]);
			}
			sb.append("\n");

			for (int i = 0; i < collNames.length; i++) {
				String collName = collNames[i];
				sb.append(collName);

				for (int j = 0; j < labels.length; j++) {
					String label = labels[j];
					double cnt = cm2.getCount(collName, label);
					sb.append("\t" + (int) cnt);
				}

				if (i != collNames.length - 1) {
					sb.append("\n");
				}
			}

			System.out.println(sb.toString());
		}

	}

	public void analyze2() throws Exception {
		IndexSearcher[] indexSearchers = SearcherUtils.getIndexSearchers(MIRPath.IndexDirNames);

		for (int i = 0; i < indexSearchers.length; i++) {
			IndexSearcher indexSearcher = indexSearchers[i];
			IndexReader indexReader = indexSearcher.getIndexReader();

			for (int j = 0; j < indexReader.maxDoc(); j++) {
				Document doc = indexReader.document(j);

				Terms termVector = indexReader.getTermVector(j, IndexFieldName.CONTENT);

				if (termVector == null) {
					continue;
				}

				TermsEnum termsEnum = null;
				termsEnum = termVector.iterator();

				BytesRef bytesRef = null;
				PostingsEnum postingsEnum = null;
				Counter<Integer> wcs = new Counter<Integer>();
				Map<Integer, String> temp = new TreeMap<Integer, String>();

				while ((bytesRef = termsEnum.next()) != null) {
					postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.ALL);

					if (postingsEnum.nextDoc() != 0) {
						throw new AssertionError();
					}

					String word = bytesRef.utf8ToString();
					int freq = postingsEnum.freq();

					for (int k = 0; k < freq; k++) {
						final int position = postingsEnum.nextPosition();
						temp.put(position, word);
					}
				}

				List<String> words = new ArrayList<String>(temp.values());
				Set<Integer> numberLocs = new HashSet<Integer>();

				for (int k = 0; k < words.size(); k++) {
					String word = words.get(k);
					if (!word.contains("#")) {
						continue;
					}
					numberLocs.add(k);
				}

				int window_size = 1;

				for (int loc : numberLocs) {
					int start = loc - window_size;
					int end = loc + window_size + 1;

					if (start < 0) {
						start = 0;
					}

					if (end > words.size()) {
						end = words.size();
					}

					StringBuffer sb = new StringBuffer();

					for (int k = start; k < end; k++) {
						sb.append(words.get(k) + "\t");
					}

					System.out.println(sb.toString().trim());
				}

			}
		}
	}
}
