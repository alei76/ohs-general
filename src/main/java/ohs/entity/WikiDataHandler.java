package ohs.entity;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.types.CounterMap;

public class WikiDataHandler {
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		WikiDataHandler dh = new WikiDataHandler();
		dh.makeTextDump();
		// dh.extractRedirects();

		System.out.println("process ends.");
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

	public void extractRedirects() throws Exception {

		TextFileReader reader = new TextFileReader(ENTPath.KOREAN_WIKI_TEXT_FILE);
		reader.setPrintNexts(false);

		MediaWikiParser parser = new MediaWikiParserFactory().createParser();

		String regex1 = "#REDIRECT \\[\\[([^\\[\\]]+)\\]\\]";
		String regex2 = "^([^:]+)\\:";

		Pattern p1 = Pattern.compile(regex1);
		Pattern p2 = Pattern.compile(regex2);

		Counter<String> c1 = new Counter<String>();
		Counter<String> c2 = new Counter<String>();

		CounterMap<String, String> cm1 = new CounterMap<String, String>();

		while (reader.hasNext()) {
			reader.print(100000);
			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}
			String title = parts[0];
			String wikiText = parts[1].replace("<NL>", "\n").trim();

			Matcher m2 = p2.matcher(title);
			if (m2.find()) {
				c1.incrementCount(m2.group(1), 1);
				continue;
			}

			c2.incrementCount(title, 1);

			Matcher m1 = p1.matcher(wikiText);

			if (m1.find()) {
				String redirect = m1.group(1).trim();
				if (redirect.length() > 0) {
					cm1.incrementCount(redirect, title, 1);
				}
			}
		}
		reader.printLast();
		reader.close();

		IOUtils.write(ENTPath.KOREAN_WIKI_REDIRECT_FILE, cm1);

		List<String> titles = new ArrayList<String>(c2.keySet());
		Collections.sort(titles);

		TextFileWriter writer = new TextFileWriter(ENTPath.KOREAN_WIKI_TITLE_FILE);

		for (int i = 0; i < titles.size(); i++) {
			String title = titles.get(i);
			writer.write(title + "\n");
		}
		writer.close();

		System.out.println(c1.toStringSortedByValues(true, true, c1.size()));

	}

	public void makeTextDump() throws Exception {
		TextFileReader reader = new TextFileReader(ENTPath.KOREAN_WIKI_XML_FILE);
		// TextFileWriter writer = new TextFileWriter(ENTPath.KOREAN_WIKI_TEXT_FILE);

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
					// writer.write(output + "\n");

					System.out.println(title);
					System.out.println(sb.toString() + "\n\n");
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
		// writer.close();

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
}
