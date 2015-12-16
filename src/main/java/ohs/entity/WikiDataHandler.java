package ohs.entity;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.medical.general.SearcherUtils;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class WikiDataHandler {
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		WikiDataHandler dh = new WikiDataHandler();
		// dh.makeTextDump();
		// dh.extractTitles();
		// dh.extractDisamTypes();
		// dh.extractJobWords();
		// dh.makeEntitySet();
		dh.extractNames();
		System.out.println("process ends.");
	}

	private Pattern pp1 = Pattern.compile("^(\\d{1,4} )?(births|birth)$");
	private Pattern pp2 = Pattern.compile("^(\\d{1,4} )?(deaths|death)$");

	private boolean isPersonName(String catStr) {
		boolean foundBirth = false;
		boolean foundDeath = false;

		for (String cat : catStr.split("\n")) {
			Matcher m = pp1.matcher(cat);
			if (m.find()) {
				foundBirth = true;
				// System.out.println(m.group());
			}

			m = pp2.matcher(cat);
			if (m.find()) {
				foundDeath = true;
			}

			if (foundBirth && foundDeath) {
				break;
			}
		}

		boolean ret = false;
		if (foundBirth || foundDeath) {
			ret = true;
		}
		return ret;
	}

	private boolean isOrganizationName(String catStr) {
		return catStr.contains("establishments in");
	}

	private String getRedirect(String content) {
		Matcher m = rp1.matcher(content);
		String ret = null;
		if (m.find()) {
			ret = StrUtils.normalizeSpaces(m.group(1));
		}
		return ret;
	}

	private String getDisambiguationType(String title) {
		String ret = null;
		Matcher m = rp2.matcher(title);
		if (m.find()) {
			ret = m.group();
			ret = ret.substring(1, ret.length() - 1);
		}
		return ret;
	}

	public void extractNames() throws Exception {
		IndexSearcher is = SearcherUtils.getIndexSearcher("../../data/medical_ir/wiki/index");
		IndexReader ir = is.getIndexReader();

		Set<String> stopPrefixes = getStopPrefixes();

		Map<String, String> nameMap = new HashMap<String, String>();

		for (int i = 0; i < ir.maxDoc(); i++) {
			if ((i + 1) % 100000 == 0) {
				System.out.printf("\r[%d/%d]", i + 1, ir.maxDoc());
			}

			org.apache.lucene.document.Document doc = ir.document(i);
			String title = doc.get(IndexFieldName.TITLE);

			if (!accept(stopPrefixes, title)) {
				continue;
			}

			String content = doc.get(IndexFieldName.CONTENT);
			String catStr = doc.get(IndexFieldName.CATEGORY);

			String to = doc.get(IndexFieldName.REDIRECT_TITLE);
			String disamType = getDisambiguationType(title);
			

			if (isPersonName(catStr)) {
				// writer.write(title + "\n");
				nameMap.put(title, to == null ? "none" : to);
			} else if (isOrganizationName(catStr)) {
				// writer.write(title + "\n");
			}
		}

		List<String> names = new ArrayList<>(nameMap.keySet());
		Collections.sort(names);

		TextFileWriter writer = new TextFileWriter(ENTPath.PERSON_NAME_FILE);
		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			String rediret = nameMap.get(name);
			writer.write(String.format("%s\t%s\n", name, rediret));
		}
		writer.close();

		System.out.printf("\r[%d/%d]\n", ir.maxDoc(), ir.maxDoc());
	}

	public void extractJobWords() throws Exception {
		Counter<String> c = new Counter<String>();

		for (String line : IOUtils.readLines(ENTPath.WIKI_DISAM_TYPE_FILE)) {
			String disamType = line.split("\t")[0];
			for (String word : disamType.split(" ")) {
				word = word.toLowerCase();
				if (word.endsWith("or") || word.endsWith("er") || word.endsWith("ian") || word.endsWith("ist") || word.endsWith("ee")) {
					c.incrementCount(word, 1);
				}
			}
		}

		c.pruneKeysBelowThreshold(5);
		IOUtils.write(ENTPath.JOB_ALL_WORDS_FILE, c);
	}

	private static Set<String> getStopPrefixes() {
		Set<String> ret = new HashSet<String>();
		ret.add("File");
		ret.add("Wikipedia");
		ret.add("Category");
		ret.add("Template");
		ret.add("Portal");
		ret.add("MediaWiki");
		ret.add("Module");
		ret.add("Help");
		ret.add("Module");
		ret.add("P");
		ret.add("ISO");
		ret.add("UN/LOCODE");
		ret.add("MOS");
		ret.add("CAT");
		ret.add("TimedText");
		ret.add("ISO 3166-1");
		ret.add("ISO 3166-2");
		ret.add("ISO 15924");
		ret.add("ISO 639");
		ret.add("Topic");
		ret.add("Draft");
		return ret;
	}

	private boolean accept(Set<String> stopPrefixes, String title) {
		int idx = title.indexOf(":");
		if (idx > 0) {
			String prefix = title.substring(0, idx);
			if (stopPrefixes.contains(prefix)) {
				return false;
			}
		}
		return true;
	}

	public void extractDisamTypes() throws Exception {
		TextFileReader reader = new TextFileReader(ENTPath.WIKI_TITLE_FILE);

		reader.setPrintNexts(false);

		Counter<String> c = Generics.newCounter();
		CounterMap<String, String> cm = Generics.newCounterMap();

		while (reader.hasNext()) {
			reader.print(5000000);
			String line = reader.next();
			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.split("\t");

			if (parts.length != 4) {
				System.out.println(line);
				continue;
			}
			String from = parts[0];
			String to = parts[1];
			String fromType = parts[2];
			String toType = parts[3];

			if (!fromType.equals("none") && !fromType.equals("disambiguation")) {
				c.incrementCount(fromType, 1);
				cm.incrementCount(fromType, from, 1);
			}

			if (!toType.equals("none") && !fromType.equals("disambiguation")) {
				c.incrementCount(toType, 1);
				cm.incrementCount(toType, to, 1);
			}
		}
		reader.printLast();
		reader.close();

		for (String key : cm.keySet()) {
			cm.getCounter(key).keepTopNKeys(5);
		}
		IOUtils.write(ENTPath.WIKI_DISAM_TYPE_FILE, c);
		IOUtils.write(ENTPath.WIKI_DISAM_TYPE_INSTANCE_FILE, cm);
	}

	public void makeEntitySet() throws Exception {
		TextFileReader reader = new TextFileReader(ENTPath.WIKI_TITLE_FILE);
		reader.setPrintNexts(false);

		ListMap<Integer, Integer> entVariants = new ListMap<Integer, Integer>();
		Map<Integer, Integer> entDisamTypes = new HashMap<Integer, Integer>();

		Set<String> stopPrefixes = getStopPrefixes();

		Counter<String> jobWords = IOUtils.readCounter(ENTPath.JOB_WORDS_FILE);

		String regex = String.format("(%s)", StrUtils.join("|", new ArrayList<String>(jobWords.keySet())));
		Pattern p = Pattern.compile(regex);

		Indexer<String> entIndexer = new Indexer<String>();
		Indexer<String> disamTypeIndexer = new Indexer<String>();

		while (reader.hasNext()) {
			reader.print(5000000);
			String line = reader.next();
			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.split("\t");

			if (parts.length != 4) {
				System.out.println(line);
				continue;
			}
			String from = parts[0];
			String to = parts[1];
			String fromType = parts[2];
			String toType = parts[2];

			if (from.length() > 0 && from.charAt(0) == ':') {
				from = from.substring(1);
			}

			if (to.length() > 0 && to.charAt(0) == ':') {
				to = to.substring(1);
			}

			Matcher m1 = p.matcher(fromType);
			Matcher m2 = p.matcher(toType);

			int idx = from.indexOf(":");
			if (idx > 0) {
				String prefix = from.substring(0, idx);
				if (stopPrefixes.contains(prefix)) {
					continue;
				}
			}

			if (!to.equals("none")) {
				entVariants.put(entIndexer.getIndex(to), entIndexer.getIndex(from));
			} else {
				entVariants.put(entIndexer.getIndex(from), entIndexer.getIndex(from));
			}

			if (!fromType.equals("none")) {
				entDisamTypes.put(entIndexer.getIndex(from), disamTypeIndexer.getIndex(fromType));
			}

			if (!toType.equals("none")) {
				entDisamTypes.put(entIndexer.getIndex(to), disamTypeIndexer.getIndex(toType));
			}
		}
		reader.printLast();
		reader.close();

		List<String> titles = new ArrayList<String>(entIndexer.getObjects());
		Collections.sort(titles);

		TextFileWriter writer = new TextFileWriter(ENTPath.WIKI_ENTITY_FILE);
		for (int i = 0; i < titles.size(); i++) {
			String title = titles.get(i);
			List<Integer> vars = entVariants.get(entIndexer.indexOf(title));
			writer.write(title + "\t" + StrUtils.join("\t", entIndexer.getObjects(vars)) + "\n");
		}
		writer.close();
	}

	private Pattern rp1 = Pattern.compile("#REDIRECT \\[\\[([^\\[\\]]+)\\]\\]");
	private Pattern rp2 = Pattern.compile("\\([^\\(\\)]+\\)");

	public void extractTitles() throws Exception {
		TextFileReader reader = new TextFileReader(ENTPath.WIKI_COL_FILE);
		reader.setPrintNexts(false);

		Pattern rp1 = Pattern.compile("#REDIRECT \\[\\[([^\\[\\]]+)\\]\\]");
		Pattern rp2 = Pattern.compile("\\([^\\(\\)]+\\)");

		TextFileWriter writer = new TextFileWriter(ENTPath.WIKI_TITLE_FILE);
		writer.write("From\tTo\tFrom-Type\tTo-Type\n");

		while (reader.hasNext()) {
			reader.print(10000);

			// if (reader.getNumLines() > 100000) {
			// break;
			// }

			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String from = StrUtils.normalizeSpaces(parts[0]);
			String wikiText = parts[1].replace("<NL>", "\n");

			String to = "none";
			String toType = "none";
			String fromType = "none";

			from = StrUtils.normalizeSpaces(from);

			Matcher m1 = rp1.matcher(wikiText);
			Matcher m2 = rp2.matcher(from);

			if (m1.find()) {
				to = StrUtils.normalizeSpaces(m1.group(1));
			}

			if (m2.find()) {
				fromType = m2.group();
				fromType = fromType.substring(1, fromType.length() - 1);
			}

			m2 = rp2.matcher(to);

			if (m2.find()) {
				toType = m2.group();
				toType = toType.substring(1, toType.length() - 1);
			}
			writer.write(String.format("%s\t%s\t%s\t%s\n", from, to, fromType, toType));
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
