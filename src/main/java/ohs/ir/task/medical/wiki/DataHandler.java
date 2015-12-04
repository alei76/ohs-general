package ohs.ir.task.medical.wiki;

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.task.medical.MIRPath;
import ohs.ir.task.medical.SearcherUtils;

/**
 * @author ohs
 * 
 */
public class DataHandler {

	public static void extractRedirects() throws Exception {
		IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
		IndexReader indexReader = indexSearcher.getIndexReader();

		MediaWikiParser parser = new MediaWikiParserFactory().createParser();

		String regex = "#REDIRECT \\[\\[([^\\[\\]]+)\\]\\]";
		Pattern p = Pattern.compile(regex);

		TextFileWriter writer = new TextFileWriter(MIRPath.WIKI_REDIRECT_TITLE_FILE);
		writer.write("FROM\tTO\n");

		for (int i = 0; i < indexReader.maxDoc(); i++) {
			org.apache.lucene.document.Document doc = indexReader.document(i);

			String docId = doc.getField(IndexFieldName.DOCUMENT_ID).stringValue().trim();
			String title = doc.getField(IndexFieldName.TITLE).stringValue().trim();
			String wikiText = doc.getField(IndexFieldName.CONTENT).stringValue().replace("<NL>", "\n").trim();

			Matcher m = p.matcher(wikiText);

			if (m.find()) {
				String redirect = m.group(1).trim();
				if (redirect.length() > 0) {
					writer.write(title + "\t" + redirect + "\n");
				}
			}
		}
		writer.close();
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		makeTextDump();
		// extractRedirects();
		// SmithWatermanScorer();
		// test2();
		System.out.println("process ends.");
	}

	public static void makeTextDump() throws Exception {
		TextFileReader reader = new TextFileReader(MIRPath.WIKI_XML_DUMP_FILE);
		TextFileWriter writer = new TextFileWriter(MIRPath.WIKI_COL_FILE);

		reader.setPrintNexts(false);

		StringBuffer sb = new StringBuffer();
		boolean isPage = false;
		int num_docs = 0;

		while (reader.hasNext()) {
			reader.print(100000);
			String line = reader.next();

			if (line.trim().startsWith("<page>")) {
				isPage = true;
				sb.append(line + "\n");
			} else if (line.trim().startsWith("</page>")) {
				sb.append(line);

				// System.out.println(sb.toString() + "\n\n");

				String[] values = parse(sb.toString());

				boolean isFilled = true;

				for (String v : values) {
					if (v == null) {
						isFilled = false;
						break;
					}
				}

				if (isFilled) {
					String title = values[0].trim();
					String wikiText = values[1].replaceAll("\n", "<NL>").trim();
					String output = String.format("%s\t%s", title, wikiText);
					writer.write(output + "\n");
				}

				sb = new StringBuffer();
				isPage = false;
			} else {
				if (isPage) {
					sb.append(line + "\n");
				}
			}
		}
		reader.printLast();
		reader.close();
		writer.close();

		System.out.printf("# of documents:%d\n", num_docs);

		// MediaWikiParserFactory pf = new MediaWikiParserFactory();
		// MediaWikiParser parser = pf.createParser();
		// ParsedPage pp = parser.parse(wikiText);
		//
		// ParsedPage pp2 = new ParsedPage();

		//
		// // get the sections
		// for (Section section : pp.getSections()) {
		// System.out.println("section : " + section.getTitle());
		// System.out.println(" nr of paragraphs      : " +
		// section.nrOfParagraphs());
		// System.out.println(" nr of tables          : " +
		// section.nrOfTables());
		// System.out.println(" nr of nested lists    : " +
		// section.nrOfNestedLists());
		// System.out.println(" nr of definition lists: " +
		// section.nrOfDefinitionLists());
		// }
	}

	public static String[] parse(String text) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder p1 = dbf.newDocumentBuilder();

		Document xmlDoc = p1.parse(new InputSource(new StringReader(text)));

		Element docElem = xmlDoc.getDocumentElement();

		String[] nodeNames = { "title", "text" };

		String[] values = new String[nodeNames.length];

		for (int j = 0; j < nodeNames.length; j++) {
			NodeList nodes = docElem.getElementsByTagName(nodeNames[j]);
			if (nodes.getLength() > 0) {
				values[j] = nodes.item(0).getTextContent().trim();
			}
		}
		return values;
	}

	public static void test() throws Exception {
		TextFileReader reader = new TextFileReader(MIRPath.WIKI_XML_DUMP_FILE);
		reader.setPrintNexts(false);

		StringBuffer sb = new StringBuffer();
		boolean isPage = false;
		int num_docs = 0;

		while (reader.hasNext()) {
			reader.print(100000);
			String line = reader.next();

			if (line.trim().startsWith("<page>")) {
				isPage = true;
				sb.append(line + "\n");
			} else if (line.trim().startsWith("</page>")) {
				sb.append(line);

				// System.out.println(sb.toString() + "\n\n");

				String[] values = parse(sb.toString());

				boolean isFilled = true;

				for (String v : values) {
					if (v == null) {
						isFilled = false;
						break;
					}
				}

				if (isFilled) {
					String id = values[0].trim();
					String title = values[1].trim();
					String wikiText = values[2].replaceAll("\n", "<NL>").trim();
					String output = String.format("%s\t%s\t%s", id, title, wikiText);

					if (title.toLowerCase().equals("cholera")) {
						System.out.println(title);
						System.out.println(wikiText);
						System.out.println();
					}
				}

				sb = new StringBuffer();
				isPage = false;
			} else {
				if (isPage) {
					sb.append(line + "\n");
				}
			}
		}
		reader.printLast();
		reader.close();

		System.out.printf("# of documents:%d\n", num_docs);
	}

	public static void test2() throws Exception {
		TextFileReader reader = new TextFileReader(MIRPath.WIKI_COL_FILE);
		reader.setPrintNexts(false);

		StringBuffer sb = new StringBuffer();
		boolean isPage = false;
		int num_docs = 0;

		while (reader.hasNext()) {
			reader.print(10000);
			String line = reader.next();
			String[] parts = line.split("\t");

			String title = parts[2];
			String wikiText = parts[3];

			if (title.toLowerCase().equals("cholera")) {
				System.out.println(title);
				System.out.println(wikiText.replace("<NL>", "\n"));
				System.out.println();
			}

		}
		reader.printLast();
		reader.close();

		System.out.printf("# of documents:%d\n", num_docs);
	}

}
