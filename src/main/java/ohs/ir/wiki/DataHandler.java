package ohs.ir.wiki;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
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

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.parser.Content;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.Section;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.FlushTemplates;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.SearcherUtils;
import ohs.types.ListMap;
import ohs.utils.StrUtils;

/**
 * @author ohs
 * 
 */
public class DataHandler {

	public static void extractRedirects2() throws Exception {
		IndexSearcher is = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
		IndexReader ir = is.getIndexReader();

		MediaWikiParser parser = new MediaWikiParserFactory().createParser();

		String regex = "#REDIRECT \\[\\[([^\\[\\]]+)\\]\\]";
		Pattern p = Pattern.compile(regex);

		TextFileWriter writer = new TextFileWriter(MIRPath.WIKI_REDIRECT_TITLE_FILE);
		writer.write("FROM\tTO\n");

		for (int i = 0; i < ir.maxDoc(); i++) {
			org.apache.lucene.document.Document doc = ir.document(i);

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
		DataHandler dh = new DataHandler();
		// makeTextDump();
		// extractRedirects();
		dh.extractTitles();
		// SmithWatermanScorer();
		// test2();
		System.out.println("process ends.");
	}

	public void extractTitles() throws Exception {
		TextFileReader reader = new TextFileReader(MIRPath.WIKI_COL_FILE);
		reader.setPrintNexts(false);

		String r1 = "#REDIRECT \\[\\[([^\\[\\]]+)\\]\\]";
		String r2 = "\\([^\\(\\)]+\\)";

		Pattern p1 = Pattern.compile(r1);
		Pattern p2 = Pattern.compile(r2);

		TextFileWriter writer = new TextFileWriter(MIRPath.WIKI_TITLE_FILE);
		writer.write("Title\tRedirected To\tDisambiguation Type\n");

		MediaWikiParserFactory factory = new MediaWikiParserFactory(Language.english);
		factory.setTemplateParserClass(FlushTemplates.class);
		MediaWikiParser parser = factory.createParser();

		ListMap<String, String> map = new ListMap<>();

		while (reader.hasNext()) {
			reader.print(10000);

			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String title = parts[0];
			String wikiText = parts[1].replace("<NL>", "\n");
			String redirect = "none";
			String disamType = "none";
			boolean isDisambiguationPage = false;

			Matcher m1 = p1.matcher(wikiText);
			Matcher m2 = p2.matcher(title);

			if (m1.find()) {
				redirect = m1.group(1).trim();
			}

			if (m2.find()) {
				disamType = m2.group().substring(1, m2.group().length() - 1);
			}

			writer.write(String.format("%s\t%s\t%s\n", title, redirect, disamType));

		}
		reader.printLast();
		reader.close();

		writer.close();
	}

	private void parseDisambiguation(MediaWikiParser parser, String title, String wikiText) {
		ParsedPage pp = parser.parse(wikiText);

		StringBuffer sb = new StringBuffer();
		sb.append(title + "\n");

		for (int i = 0; i < pp.getSections().size(); i++) {
			Section section = pp.getSection(i);
			String secTitle = section.getTitle();

			List<String> sents = new ArrayList<String>();

			for (int j = 0; j < section.getContentList().size(); j++) {
				Content content = section.getContentList().get(j);

				String[] ss = content.getText().split("[\\n]+");

				for (String s : ss) {
					s = s.trim();
					if (s.length() > 0) {
						sents.add(s);
					}
				}
			}

			String s = StrUtils.join("\n", sents);

			if (s.length() > 0) {
				sb.append(s + "\n\n");
			}
		}
	}

	public void makeTextDump() throws Exception {
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

				String[] values = parseXml(sb.toString());

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

	public static String[] parseXml(String text) throws Exception {
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

				String[] values = parseXml(sb.toString());

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
