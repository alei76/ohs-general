package ohs.ir.medical.clef.ehealth_2014;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.medical.general.SearcherUtils;
import ohs.ling.struct.Span;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.StopWatch;

public class AbbreviationExtracting {

	public static void extract() throws Exception {
		System.out.println("extract abbreviations.");

		IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(EHPath.INDEX_DIR);
		IndexReader indexReader = indexSearcher.getIndexReader();

		File outputFile = new File(EHPath.ABBREVIATION_FILE);

		TextFileWriter writer = new TextFileWriter(outputFile);

		AbbreviationExtractor ext = new AbbreviationExtractor();

		int maxDoc = indexReader.maxDoc();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < maxDoc; i++) {
			if ((i + 1) % 1000 == 0) {
				System.out.printf("\r[%d / %d, %s]", i + 1, maxDoc, stopWatch.stop());
			}
			Document doc = indexReader.document(i);
			String docId = doc.getField(IndexFieldName.DOCUMENT_ID).stringValue();
			String date = doc.getField(IndexFieldName.DATE).stringValue();
			String url = doc.getField(IndexFieldName.URL).stringValue();
			String content = doc.getField(IndexFieldName.CONTENT).stringValue();
			content = content.replaceAll("<NL>", "\n");

			String[] sents = content.split("\n");

			for (int j = 0; j < sents.length; j++) {
				String sent = sents[j];
				List<StrPair> pairs = ext.extract(sent);
				List<Span[]> spansList = getSpans(pairs, sent);

				for (Span[] spans : spansList) {
					String shortForm = spans[0].getString();
					String longForm = spans[1].getString();
					String output = String.format("%s\t%s\t%d\t%d", shortForm, longForm, i, j);
					writer.write(output + "\n");
				}
			}
		}
		System.out.printf("\r[%d / %d, %s]\n", maxDoc, maxDoc, stopWatch.stop());
		writer.close();
	}

	// public static void extractContext() {
	// TextFileReader reader = new TextFileReader(new File(EHPath.ABBREVIATION_FILE));
	// while (reader.hasNext()) {
	// String[] parts = reader.next().split("\t");
	// String shortForm = parts[0];
	// String longForm = parts[1].toLowerCase();
	// int indexId = Integer.parseInt(parts[2]);
	// }
	// reader.close();
	// }

	public static void filter() {
		TextFileReader reader = new TextFileReader(EHPath.ABBREVIATION_GROUPED_FILE);

		CounterMap<String, String> short_long_count = new CounterMap<String, String>();

		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();

			String shortForm = lines.get(0);
			Counter<String> counter = new Counter<String>();

			for (int i = 1; i < lines.size(); i++) {
				String[] parts = lines.get(i).split("\t");
				String longForm = parts[0];
				int count = Integer.parseInt(parts[1]);
				counter.setCount(longForm, count);
			}

			short_long_count.setCounter(shortForm, counter);
		}
		reader.close();

		Iterator<String> iter1 = short_long_count.keySet().iterator();

		while (iter1.hasNext()) {
			String shortForm = iter1.next();
			Counter<String> long_count = short_long_count.getCounter(shortForm);
			Set<Character> set = new HashSet<Character>();

			for (int i = 0; i < shortForm.length(); i++) {
				set.add(Character.toLowerCase(shortForm.charAt(i)));
			}

			Iterator<String> iter2 = long_count.keySet().iterator();
			while (iter2.hasNext()) {
				String longForm = iter2.next();

				if (longForm.toLowerCase().contains(shortForm.toLowerCase())) {
					iter2.remove();
				} else {
					String[] toks = longForm.split(" ");
					int numMatches = 0;

					for (int i = 0; i < toks.length; i++) {
						char ch = toks[i].toLowerCase().charAt(0);
						if (set.contains(ch)) {
							numMatches++;
						}
					}

					double ratio = 1f * numMatches / toks.length;

					if (ratio < 0.5) {
						iter2.remove();
					}
				}
			}

			if (long_count.size() == 0) {
				iter1.remove();
			}
		}

		Counter<String> short_count = short_long_count.getInnerCountSums();

		TextFileWriter writer = new TextFileWriter(EHPath.ABBREVIATION_FILTERED_FILE);

		for (String shortForm : short_count.getSortedKeys()) {
			StringBuffer sb = new StringBuffer();
			sb.append(shortForm);

			Counter<String> long_count = short_long_count.getCounter(shortForm);
			for (String longForm : long_count.getSortedKeys()) {
				int count = (int) long_count.getCount(longForm);
				sb.append(String.format("\n%s\t%d", longForm, count));
			}
			writer.write(sb.toString() + "\n\n");
		}
		writer.close();
	}

	private static List<Span[]> getSpans(List<StrPair> pairs, String content) {
		List<Span[]> ret = new ArrayList<Span[]>();

		for (int i = 0; i < pairs.size(); i++) {
			StrPair pair = pairs.get(i);
			String shortForm = pair.getFirst();
			String longForm = pair.getSecond();

			String regex = String.format("(%s)(\\s)?\\((\\s)?(%s)(\\s)?\\)", shortForm, longForm);
			Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(content);

			while (m.find()) {
				String g = m.group();
				String g1 = m.group(1);
				String g4 = m.group(4);

				Span[] spans = new Span[2];
				spans[0] = new Span(m.start(1), g1);
				spans[1] = new Span(m.start(4), g4);

				ret.add(spans);
			}
		}
		return ret;
	}

	public static void group() {
		CounterMap<String, String> short_long_count = new CounterMap<String, String>();

		TextFileReader reader = new TextFileReader(EHPath.ABBREVIATION_FILE);
		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");
			String shortForm = parts[0];
			String longForm = parts[1].toLowerCase();
			int indexId = Integer.parseInt(parts[2]);
			short_long_count.incrementCount(shortForm, longForm, 1);
		}
		reader.close();

		for (String shortForm : short_long_count.keySet()) {
			Counter<String> long_count = short_long_count.getCounter(shortForm);
			// long_count.pruneKeysBelowThreshold(5);
		}

		Counter<String> short_count = short_long_count.getInnerCountSums();

		TextFileWriter writer = new TextFileWriter(new File(EHPath.ABBREVIATION_GROUPED_FILE));

		for (String shortForm : short_count.getSortedKeys()) {
			StringBuffer sb = new StringBuffer();
			sb.append(shortForm);

			Counter<String> long_count = short_long_count.getCounter(shortForm);
			for (String longForm : long_count.getSortedKeys()) {
				int count = (int) long_count.getCount(longForm);
				sb.append(String.format("\n%s\t%d", longForm, count));
			}
			writer.write(sb.toString() + "\n\n");
		}
		writer.close();
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		extract();
		group();
		filter();
		System.out.println("process ends.");
	}

}
