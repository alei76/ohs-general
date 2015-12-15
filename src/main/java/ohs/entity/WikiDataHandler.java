package ohs.entity;

import java.io.StringReader;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.types.ListMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class WikiDataHandler {
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		WikiDataHandler dh = new WikiDataHandler();
		// dh.makeTextDump();
		// dh.extractRedirects();
		dh.generateEntitySet();
		System.out.println("process ends.");
	}

	public void generateEntitySet() {
		TextFileReader reader = new TextFileReader(ENTPath.WIKI_TITLE_FILE);
		ListMap<String, String> map = new ListMap<>();

		reader.setPrintNexts(false);

		Counter<String> c = Generics.newCounter();

		Set<String> stopPrefixes = new HashSet<String>();
		stopPrefixes.add("File");
		stopPrefixes.add("Wikipedia");
		stopPrefixes.add("Category");
		stopPrefixes.add("Template");
		stopPrefixes.add("Portal");
		stopPrefixes.add("MediaWiki");
		stopPrefixes.add("Module");
		stopPrefixes.add("Help");
		stopPrefixes.add("Module");
		stopPrefixes.add("P");
		stopPrefixes.add("ISO");
		stopPrefixes.add("UN/LOCODE");
		stopPrefixes.add("MOS");
		stopPrefixes.add("CAT");
		stopPrefixes.add("TimedText");
		stopPrefixes.add("ISO 3166-1");
		stopPrefixes.add("ISO 3166-2");
		stopPrefixes.add("ISO 15924");
		stopPrefixes.add("ISO 639");
		stopPrefixes.add("Topic");
		stopPrefixes.add("Draft");

		while (reader.hasNext()) {
			reader.print(5000000);
			String line = reader.next();
			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.split("\t");

			if (parts.length != 3) {
				System.out.println(line);
				continue;
			}
			String from = parts[0];
			String to = parts[1];
			String disamType = parts[2];

			if (from.charAt(0) == ':') {
				from = from.substring(1);
			}

			if (to.charAt(0) == ':') {
				to = to.substring(1);
			}

			int idx = from.indexOf(":");
			if (idx > 0) {
				String prefix = from.substring(0, idx);
				if (stopPrefixes.contains(prefix)) {
					continue;
				}
				c.incrementCount(prefix, 1);
			}

			if (!to.equals("none")) {
				map.put(to, from);
			} else {
				map.put(from, "S");
			}
		}
		reader.printLast();
		reader.close();

		System.out.println(c.toStringSortedByValues(true, true, 50));

		TextFileWriter writer = new TextFileWriter(ENTPath.DATA_DIR + "entity.txt.gz");

		List<String> desTitles = new ArrayList<String>(map.keySet());
		Collections.sort(desTitles);

		for (int i = 0; i < desTitles.size(); i++) {
			String desTitle = desTitles.get(i);
			List<String> srcTitles = map.get(desTitle);
			if (srcTitles.size() > 1) {
				writer.write(desTitle + "\t" + srcTitles + "\n");
			}
		}
		writer.close();

	}

	public void extractTitles() throws Exception {
		TextFileReader reader = new TextFileReader(ENTPath.WIKI_COL_FILE);
		reader.setPrintNexts(false);

		String r1 = "#REDIRECT \\[\\[([^\\[\\]]+)\\]\\]";
		String r2 = "\\([^\\(\\)]+\\)";

		Pattern p1 = Pattern.compile(r1);
		Pattern p2 = Pattern.compile(r2);

		TextFileWriter writer = new TextFileWriter(ENTPath.WIKI_TITLE_FILE);
		writer.write("Title\tRedirected To\tDisambiguation Type\n");

		while (reader.hasNext()) {
			reader.print(10000);

			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String title = StrUtils.normalizeSpaces(parts[0]);
			String wikiText = parts[1].replace("<NL>", "\n");
			String redirect = "none";
			String disamType = "none";

			title = StrUtils.normalizeSpaces(title);

			Matcher m1 = p1.matcher(wikiText);
			Matcher m2 = p2.matcher(title);

			if (m1.find()) {
				redirect = StrUtils.normalizeSpaces(m1.group(1));
			}

			if (m2.find()) {
				disamType = m2.group();
				disamType = disamType.substring(1, disamType.length() - 1);
			}

			writer.write(String.format("%s\t%s\t%s\n", title, redirect, disamType));
		}
		reader.printLast();
		reader.close();

		writer.close();
	}

	private static String[] parse(String text) throws Exception {
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
		// System.out.println(" nr of paragraphs : " +
		// section.nrOfParagraphs());
		// System.out.println(" nr of tables : " +
		// section.nrOfTables());
		// System.out.println(" nr of nested lists : " +
		// section.nrOfNestedLists());
		// System.out.println(" nr of definition lists: " +
		// section.nrOfDefinitionLists());
		// }
	}
}
