package ohs.ir.task.medical.clef.ehealth_2014;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// removeMarkups();

		// splitSentences();

		readList();

		System.out.println("process ends.");
	}

	public static void readList() throws Exception {
		List<EHealthQuery> qs = EHealthQuery.read(EHPath.QUERY_2014_TEST_FILE, EHPath.DISCHARGE_DIR);

		for (int i = 0; i < qs.size(); i++) {

		}
	}

	public static void removeMarkups() {
		System.out.println("remove markups.");
		File inputDir = new File(EHPath.RAW_COLLECTION_DIR);
		File outputFile = new File(EHPath.COLLECTION_FILE);

		TextFileWriter writer = new TextFileWriter(outputFile);

		int totalDocs = 0;

		List<File> inputFiles = IOUtils.getFilesUnder(inputDir);

		for (int i = 0; i < inputFiles.size(); i++) {
			File inputFile = inputFiles.get(i);

			TextFileReader reader = new TextFileReader(inputFile);
			List<String> lines = new ArrayList<String>();
			int numDocs = 0;

			while (reader.hasNext()) {
				String line = reader.next().trim();

				if (line.equals("")) {
					continue;
				}

				lines.add(line);

				if (line.startsWith("#EOR")) {
					String uid = lines.get(0);
					String date = lines.get(1);
					String url = lines.get(2);
					String html = StrUtils.join("\n", lines, 4, lines.size() - 1);

					if (!uid.startsWith("#UID") || !date.startsWith("#DATE")

					|| !url.startsWith("#TITLE") || !lines.get(3).startsWith("#CONTENT")) {
						System.out.println(StrUtils.join("\n", lines));
						continue;
					}

					uid = uid.substring(5);
					date = date.substring(6);
					url = url.substring(5);

					Document doc = Jsoup.parse(html);
					String content = doc.text();
					content = StrUtils.normalizeSpaces(content);

					StringBuffer sb = new StringBuffer();
					sb.append(uid + "\n");
					sb.append(date + "\n");
					sb.append(url + "\n");
					sb.append(content);

					writer.write(sb.toString() + "\n\n");

					lines = new ArrayList<String>();
					numDocs++;
				}
			}
			reader.close();
			totalDocs += numDocs;

			System.out.printf("%d: [%s] file with [%d] documents \n", i, inputFile.getName(), numDocs);
		}

		writer.close();

		System.out.printf("Total documents: %d\n", totalDocs);
	}

	public static void splitSentences() {
		System.out.println("split sentences.");

		File inputFile = new File(EHPath.COLLECTION_FILE);
		File outputFile = new File(EHPath.COLLECTION_SENTENCE_FILE);

		TextFileReader reader = new TextFileReader(inputFile);
		TextFileWriter writer = new TextFileWriter(outputFile);

		reader.setPrintNexts(true);

		Counter<String> counter = new Counter<String>();

		Set<String> expSet = new HashSet<String>();
		expSet.add("doc");
		expSet.add("docx");
		expSet.add("pdf");
		expSet.add("swf");
		expSet.add("ppt");
		expSet.add("pptx");
		expSet.add("png");
		expSet.add("flv");

		int totalDocs = 0;
		int numDocs = 0;

		while (reader.hasNext()) {
			reader.print(10000);
			List<String> lines = reader.getNextLines();

			totalDocs++;

			// if (reader.getNumNexts() < 248000) {
			// continue;
			// }

			if (lines.size() != 4) {
				continue;
			}

			String uid = lines.get(0);
			String date = lines.get(1);
			String url = lines.get(2);
			String content = lines.get(3);

			Pattern p = Pattern.compile("\\.([a-z]+)$");
			Matcher m = p.matcher(url);

			if (m.find()) {
				String exp = m.group(1).toLowerCase();
				counter.incrementCount(exp, 1);

				if (expSet.contains(exp)) {
					continue;
				}
			}

			numDocs++;

			content = tokenize(content);
			content = content.replaceAll("[\n]+", "<NL>");
			lines.set(3, content);
			String output = StrUtils.join("\n", lines);
			writer.write(output + "\n\n");
		}
		reader.printLast();
		reader.close();
		writer.close();

		System.out.println(counter.toStringSortedByValues(true, true, counter.size()));
		System.out.printf("[%d/%d]\n", numDocs, totalDocs);
	}

	public static String tokenize(String text) {
		TokenizerFactory<? extends HasWord> tf = PTBTokenizer.factory(new CoreLabelTokenFactory(),
				"ptb3Escaping=false,normalizeParentheses=false,normalizeOtherBrackets=false");
		DocumentPreprocessor documentPreprocessor = new DocumentPreprocessor(new StringReader(text));
		documentPreprocessor.setTokenizerFactory(tf);

		StringBuffer sb = new StringBuffer();
		for (List<HasWord> item : documentPreprocessor) {

			for (int i = 0; i < item.size(); i++) {
				sb.append(item.get(i).word());
				if (i != item.size() - 1) {
					sb.append(" ");
				}
			}
			sb.append("\n");
		}
		return sb.toString().trim();
	}
}
