package ohs.ngrams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.StrUtils;

public class NGramGenerator {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		NGramGenerator g = new NGramGenerator();
		g.makeTextDump();
		// g.serialize();
		// g.generate();
		System.out.println("process ends.");
	}

	public static StrCounter ngrams(int ngram_size, List<String> words) {
		StrCounter ret = new StrCounter();
		for (int j = 0; j < words.size() - ngram_size + 1; j++) {
			StringBuffer sb = new StringBuffer();
			int size = 0;
			for (int k = j; k < j + ngram_size; k++) {
				sb.append(words.get(k).toLowerCase());
				if (k != (j + ngram_size) - 1) {
					sb.append(" ");
				}
				size++;
			}
			assert ngram_size == size;
			String ngram = sb.toString();
			ret.incrementCount(ngram, 1);
		}
		return ret;
	}

	public static String[] parse(String xmlText) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);

		DocumentBuilder parser = dbf.newDocumentBuilder();

		parser.setEntityResolver(new EntityResolver() {

			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				if (systemId.contains("")) {
					return new InputSource(new StringReader(""));
				}
				return null;
			}
		});

		Document xmlDoc = null;

		try {
			xmlDoc = parser.parse(new InputSource(new StringReader(xmlText)));
		} catch (Exception e) {
			e.printStackTrace();
			return new String[0];
		}
		// org.w3c.dom.Document xmlDoc = parser.parse(docFile);

		String pid = "null";
		String title = "null";
		String abs = "null";
		String body = "null";

		Element titleElem = (Element) xmlDoc.getElementsByTagName("article-title").item(0);
		Element absElem = (Element) xmlDoc.getElementsByTagName("abstract").item(0);
		Element bodyElem = (Element) xmlDoc.getElementsByTagName("body").item(0);
		Element pidElem = (Element) xmlDoc.getElementsByTagName("article-id").item(0);

		if (pidElem != null) {
			pid = pidElem.getTextContent().trim();
		}
		if (titleElem != null) {
			title = titleElem.getTextContent().trim();
		}

		if (absElem != null) {
			abs = absElem.getTextContent().trim();
		}

		if (bodyElem != null) {
			StringBuffer sb = new StringBuffer();
			NodeList nodeList = bodyElem.getElementsByTagName("p");
			for (int k = 0; k < nodeList.getLength(); k++) {
				Element paraElem = (Element) nodeList.item(k);
				String text = paraElem.getTextContent().trim();
				text = text.replaceAll("[\\s]+", " ").trim();
				sb.append(text + "\n");
			}
			body = sb.toString().trim();
		}

		String[] ret = new String[] { pid, title, abs, body };

		for (int i = 0; i < ret.length; i++) {
			ret[i] = ret[i].replaceAll("\n+", "<NL>");
		}

		return ret;
	}

	public void generate() throws Exception {
		int[] ngram_sizes = { 2, 3, 4, 5 };

		int num_blocks = 0;
		int block_size = 100;

		for (int i = 0; i < ngram_sizes.length; i++) {
			int ngram_size = ngram_sizes[i];

			ObjectInputStream ois = IOUtils.openObjectInputStream(NGPath.JOURNAL_SER_FILE);
			int num_docs = ois.readInt();

			for (int j = 0; j < num_docs; j++) {
				int[][] doc = IOUtils.readIntegerMatrix(ois);
			}
		}
	}

	public void makeTextDump() throws Exception {
		TextFileWriter writer = new TextFileWriter(NGPath.JOURNAL_TEXT_FILE);

		File[] dataFiles = new File(NGPath.JOURNAL_DIR).listFiles();

		StrCounterMap cm = new StrCounterMap();
		Set<String> yearSet = new TreeSet<>();

		for (int i = 0; i < dataFiles.length; i++) {
			File dataFile = dataFiles[i];

			if (dataFile.isFile() && dataFile.getName().endsWith(".zip")) {

			} else {
				continue;
			}

			int num_files = 0;

			String journal = dataFile.getName();
			journal = journal.split("_")[0];

			ZipInputStream is = new ZipInputStream(new FileInputStream(dataFile));
			ZipEntry entry = null;
			// read every single entry in TAR file
			while ((entry = is.getNextEntry()) != null) {
				// the following two lines remove the .tar.gz extension for the folder name
				// System.out.println(entry.getName());

				if (!entry.isDirectory()) {
					String fileName = entry.getName();

					if (!fileName.contains("journal")) {
						continue;
					}

					num_files++;

					String year = fileName.split("/")[0];
					year = year.split("_")[0];

					yearSet.add(year);
					cm.incrementCount(journal, year, 1);

					// StringBuffer sb = new StringBuffer();
					//
					// int c;
					//
					// while ((c = is.read()) != -1) {
					// sb.append((char) c);
					// }
					//
					// if (sb.length() > 0) {
					// String[] textParts = parse(sb.toString());
					//
					// if (textParts.length == 4) {
					// String output = year + "\t" + StrUtils.join("\t", textParts);
					// writer.write(output + "\n");
					// }
					// }
				}
			}
			is.close();

			System.out.printf("read [%d] files from [%s]\n", num_files, dataFile.getName());
		}

		List<String> journals = cm.getInnerCountSums().getSortedKeys();
		List<String> years = new ArrayList<String>(yearSet);

		Collections.sort(years);

		StringBuffer sb = new StringBuffer();

		sb.append("Journal");

		for (int i = 0; i < years.size(); i++) {
			sb.append("\t" + years.get(i));
		}

		for (int i = 0; i < journals.size(); i++) {
			String journal = journals.get(i);

			sb.append(String.format("\n%s", journal));

			for (int j = 0; j < years.size(); j++) {
				String year = years.get(j);
				int cnt = (int) cm.getCount(journal, year);
				sb.append(String.format("\t%d", cnt));
			}
		}

		System.out.println(sb.toString());

		// System.out.println(cm.toString());

	}

	private Counter<Integer[]> ngram(int[][] doc, int ngram_size) {

		for (int i = 0; i < doc.length; i++) {
			int[] sent = doc[i];
			int[] ngram = new int[ngram_size];

			for (int j = ngram_size; j < sent.length; j++) {
				int start = j - ngram_size;
				int end = j;

				for (int k = start, loc = 0; k < end; k++, loc++) {
					ngram[loc] = sent[k];
				}
			}
		}

		return null;
	}

	public void serialize() throws Exception {

		int num_docs = IOUtils.countLines(NGPath.JOURNAL_TEXT_FILE);

		Indexer<String> wordIndexer = new Indexer<String>();

		TextFileReader reader = new TextFileReader(NGPath.JOURNAL_TEXT_FILE);
		reader.setPrintNexts(false);

		TextFileWriter writer = new TextFileWriter(NGPath.JOURNAL_SER_FILE);

		ObjectOutputStream oos = IOUtils.openObjectOutputStream(NGPath.JOURNAL_SER_FILE);
		oos.writeInt(num_docs);

		while (reader.hasNext()) {
			reader.print(10000);
			String line = reader.next();
			String[] parts = line.split("\t");
			String year = parts[0];
			String doi = parts[1];
			String title = parts[2];
			String abs = parts[3];
			String body = parts[4].replace("<NL>", "\n");

			String[] sents = body.split("\n");
			int[][] doc = new int[sents.length][];

			for (int i = 0; i < sents.length; i++) {
				List<String> words = StrUtils.split(sents[i]);

				int[] ws = new int[words.size()];

				for (int j = 0; j < words.size(); j++) {
					int w = wordIndexer.getIndex(words.get(j));
					ws[j] = w;
				}
				doc[i] = ws;
			}

			IOUtils.write(oos, doc);
		}
		reader.printLast();
		oos.close();

		IOUtils.write(NGPath.VOC_FILE, wordIndexer);
	}

	private void writeBlock(CounterMap<String, Integer> cm, TextFileWriter writer, int block_id, int block_size) {
		List<String> ngrams = new ArrayList<String>(cm.keySet());
		Collections.sort(ngrams);

		int start = block_id * block_size;
		int end = (block_id + 1) * block_size - 1;

		writer.write(String.format("BLOCK:\t%d\n", block_id));
		writer.write(String.format("RANGE:\t%d-%d\n", start, end));

		for (int j = 0; j < ngrams.size(); j++) {
			String ngram = ngrams.get(j);
			Counter<Integer> docCounts = cm.getCounter(ngram);
			List<Integer> docIds = new ArrayList<Integer>(docCounts.keySet());

			Collections.sort(docIds);

			writer.write(ngram);

			for (int k = 0; k < docIds.size(); k++) {
				int docId = docIds.get(k);
				int cnt = (int) docCounts.getCount(docId);
				writer.write(String.format("\t%d:%d", docId, cnt));
			}
			writer.write("\n");
		}
		writer.write("\n");

	}

}
