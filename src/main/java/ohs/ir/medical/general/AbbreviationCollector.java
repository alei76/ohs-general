package ohs.ir.medical.general;

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
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.medical.clef.ehealth_2014.AbbreviationExtractor;
import ohs.ling.struct.Span;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Pair;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

public class AbbreviationCollector {

	public static String capitalize(String s) {
		String[] parts = s.split(" ");
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			part = Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase();
			sb.append(part + " ");
		}
		return sb.toString().trim();
	}

	// public static void extractContext() {
	// TextFileReader reader = new TextFileReader(new
	// File(EHPath.ABBREVIATION_FILE));
	// while (reader.hasNext()) {
	// String[] parts = reader.next().split("\t");
	// String shortForm = parts[0];
	// String longForm = parts[1].toLowerCase();
	// int indexId = Integer.parseInt(parts[2]);
	// }
	// reader.close();
	// }

	public static void extract() throws Exception {
		System.out.println("extract abbreviations.");

		String[] indexDirs = MIRPath.IndexDirNames;
		String[] abbrFileNames = MIRPath.AbbrFileNames;

		for (int i = 3; i < abbrFileNames.length; i++) {
			System.out.printf("extract abbreviations from [%s].\n", indexDirs[i]);

			IndexSearcher is = SearcherUtils.getIndexSearcher(indexDirs[i]);
			IndexReader ir = is.getIndexReader();

			TextFileWriter writer = new TextFileWriter(abbrFileNames[i]);

			AbbreviationExtractor ext = new AbbreviationExtractor();

			int num_docs = ir.maxDoc();

			StopWatch stopWatch = new StopWatch();
			stopWatch.start();

			for (int j = 0; j < num_docs; j++) {
				if ((j + 1) % 10000 == 0) {
					System.out.printf("\r[%d / %d, %s]", j + 1, num_docs, stopWatch.stop());
				}

				Document doc = ir.document(j);
				String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
				String content = doc.getField(CommonFieldNames.CONTENT).stringValue();
				// content = content.replaceAll("<NL>", "\n");

				content = StrUtils.join("\n", NLPUtils.tokenize(content));
				// content = content.replace("( ", "(").replace(" )", ")");
				StringBuffer sb = new StringBuffer();

				String[] sents = content.split("\n");

				for (int k = 0; k < sents.length; k++) {
					String sent = sents[k];
					List<Pair<String, String>> pairs = ext.extract(sent);

					for (Pair<String, String> pair : pairs) {
						String shortForm = pair.getFirst();
						String longForm = pair.getSecond();
						String output = String.format("%s\t%s\t%d\t%d", shortForm, longForm, j, k);
						sb.append(output + "\n");
					}

					//

					// List<Span[]> spansList = getSpans(pairs, sent);
					//
					// for (Span[] spans : spansList) {
					// String shortForm = spans[0].getText();
					// String longForm = spans[1].getText();
					// String output = String.format("%s\t%s\t%d\t%d",
					// shortForm, longForm, j, k);
					// writer.write(output + "\n");
					// }
				}

				String output = sb.toString().trim();

				if (output.length() > 0) {
					writer.write(output + "\n\n");
				}
			}
			System.out.printf("\r[%d / %d, %s]\n", num_docs, num_docs, stopWatch.stop());
			writer.close();
		}

	}

	public static void filter() {
		String[] collDirs = MIRPath.TaskDirs;

		for (int i = 0; i < collDirs.length; i++) {
			String inputFileName = collDirs[i] + "abbrs_group.txt";
			String outputFileName = collDirs[i] + "abbrs_filter.txt";

			TextFileReader reader = new TextFileReader(inputFileName);

			CounterMap<String, String> cm = new CounterMap<String, String>();

			while (reader.hasNext()) {
				List<String> lines = reader.getNextLines();

				String data = lines.get(0);
				Counter<String> c = new Counter<String>();

				for (int j = 1; j < lines.size(); j++) {
					String[] parts = lines.get(j).split("\t");
					String longForm = parts[0];
					int cnt = Integer.parseInt(parts[1]);
					c.setCount(longForm, cnt);
				}

				cm.setCounter(data, c);
			}
			reader.close();

			Iterator<String> iter1 = cm.keySet().iterator();

			while (iter1.hasNext()) {
				String data = iter1.next();

				String[] parts = data.split("\t");
				String shortForm = parts[1];

				if (shortForm.equals("BLAST")) {
					System.out.println();
				}

				Counter<String> longCounts = cm.getCounter(data);
				Set<Character> set = new HashSet<Character>();

				for (int j = 0; j < shortForm.length(); j++) {
					set.add(Character.toLowerCase(shortForm.charAt(j)));
				}

				Iterator<String> iter2 = longCounts.keySet().iterator();
				while (iter2.hasNext()) {
					String longForm = iter2.next();

					if (longForm.toLowerCase().contains(shortForm.toLowerCase())) {
						iter2.remove();
					} else {
						String[] toks = longForm.split(" ");
						int num_matches = 0;

						for (int k = 0; k < toks.length; k++) {
							char ch = toks[k].toLowerCase().charAt(0);
							if (set.contains(ch)) {
								num_matches++;
							}
						}

						double ratio = 1f * num_matches / toks.length;

						if (ratio < 0.5) {
							iter2.remove();
						}
					}
				}

				if (longCounts.size() == 0) {
					iter1.remove();
				}
			}

			Counter<String> shortCounts = cm.getInnerCountSums();

			TextFileWriter writer = new TextFileWriter(outputFileName);

			for (String sf : shortCounts.getSortedKeys()) {
				StringBuffer sb = new StringBuffer();
				sb.append(sf);

				Counter<String> longCounts = cm.getCounter(sf);
				for (String lf : longCounts.getSortedKeys()) {
					int cnt = (int) longCounts.getCount(lf);
					sb.append(String.format("\n%s\t%d", lf, cnt));
				}
				writer.write(sb.toString() + "\n\n");
			}
			writer.close();
		}

	}

	private static List<Span[]> getSpans(List<Pair<String, String>> pairs, String content) {
		List<Span[]> ret = new ArrayList<Span[]>();

		for (int i = 0; i < pairs.size(); i++) {
			Pair<String, String> pair = pairs.get(i);
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

		String[] abbrFileNames = MIRPath.AbbrFileNames;
		String[] collDirs = MIRPath.TaskDirs;

		for (int i = 0; i < abbrFileNames.length; i++) {
			// if (!abbrFileNames[i].contains("clef")) {
			// continue;
			// }

			CounterMap<String, String> shortLongCounts = new CounterMap();

			TextFileReader reader = new TextFileReader(abbrFileNames[i]);
			while (reader.hasNext()) {
				List<String> lines = reader.getNextLines();

				for (int j = 0; j < lines.size(); j++) {
					String line = lines.get(j);
					String[] parts = line.split("\t");
					String shortForm = parts[0];
					String longForm = parts[1];
					int docId = Integer.parseInt(parts[2]);

					// shortForm = capitalize(shortForm);
					longForm = capitalize(longForm);

					Set<Pair<String, String>> set = new HashSet<Pair<String, String>>();
					Pair<String, String> sp = new Pair<String, String>(shortForm, longForm);
					if (!set.contains(sp)) {
						shortLongCounts.incrementCount(shortForm, longForm, 1);
						set.add(sp);
					}

				}
			}
			reader.close();

			Counter<String> short_count = shortLongCounts.getInnerCountSums();

			String outputFileName = collDirs[i] + "abbrs_group.txt";

			TextFileWriter writer = new TextFileWriter(outputFileName);

			List<String> shortForms = short_count.getSortedKeys();

			for (int j = 0; j < shortForms.size(); j++) {
				String shortForm = shortForms.get(j);
				Counter<String> longCounts = shortLongCounts.getCounter(shortForm);

				StringBuffer sb = new StringBuffer();
				sb.append(String.format("ShortForm:\t%s\t%d", shortForm, (int) longCounts.totalCount()));

				for (String longForm : longCounts.getSortedKeys()) {
					int count = (int) longCounts.getCount(longForm);
					sb.append(String.format("\n%s\t%d", longForm, count));
				}
				writer.write(sb.toString() + "\n\n");
			}
			writer.close();
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		extract();
		group();
		filter();
		System.out.println("process ends.");
	}

}
