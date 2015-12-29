package ohs.entity;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
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
import ohs.types.ListMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class WikiDataHandler {
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

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		WikiDataHandler dh = new WikiDataHandler();
		// dh.makeTextDump();
		dh.extractNames();
		// dh.extractCategories();
		System.out.println("process ends.");
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

	private Pattern pp1 = Pattern.compile("^(\\d{1,4} )?(births|birth)$");

	private Pattern pp2 = Pattern.compile("^(\\d{1,4} )?(deaths|death)$");

	private Pattern rp1 = Pattern.compile("#REDIRECT \\[\\[([^\\[\\]]+)\\]\\]");

	private Pattern rp2 = Pattern.compile("\\([^\\(\\)]+\\)");

	private Pattern lp1 = Pattern.compile("(rivers|cities|towns|mountains|seas|bridges|airports|buildings|places) (established )?(of|in)");

	private Pattern op1 = Pattern.compile(
			"(organizations|organisations|companies|agencies|institutions|institutes|clubs|universities|schools|colleges) (established|establishments|based) in");

	private boolean accept(Set<String> stopPrefixes, String title) {
		int idx = title.indexOf(":");
		if (idx > 0) {
			String prefix = title.substring(0, idx);
			if (stopPrefixes.contains(prefix)) {
				return false;
			}
		}

		if (title.startsWith("List of")) {
			return false;
		}

		return true;
	}

	public void extractCategories() throws Exception {
		IndexSearcher is = SearcherUtils.getIndexSearcher("../../data/medical_ir/wiki/index");
		IndexReader ir = is.getIndexReader();

		Set<String> stopPrefixes = getStopPrefixes();

		Counter<String> c = Generics.newCounter();

		for (int i = 0; i < ir.maxDoc(); i++) {
			if ((i + 1) % 100000 == 0) {
				System.out.printf("\r[%d/%d]", i + 1, ir.maxDoc());
			}

			// if ((i + 1) % 3000000 == 0) {
			// break;
			// }

			// if (i == 1000) {
			// break;
			// }

			String title = ir.document(i).get(IndexFieldName.TITLE);

			if (!accept(stopPrefixes, title)) {
				continue;
			}

			String catStr = ir.document(i).get(IndexFieldName.CATEGORY).toLowerCase();
			String redirect = ir.document(i).get(IndexFieldName.REDIRECT_TITLE);

			if (redirect.length() > 0) {
				continue;
			}

			for (String cat : catStr.split("\n")) {
				cat = cat.replaceAll("[\\d]+", "<D>");
				c.incrementCount(cat, 1);
			}
		}

		IOUtils.write(ENTPath.WIKI_DIR + "cats.txt", c, false);

		System.out.printf("\r[%d/%d]\n", ir.maxDoc(), ir.maxDoc());
	}

	public void extractNames() throws Exception {
		IndexSearcher is = SearcherUtils.getIndexSearcher("../../data/medical_ir/wiki/index");
		IndexReader ir = is.getIndexReader();

		Set<String> stopPrefixes = getStopPrefixes();

		ListMap<String, String> titleVariantMap = new ListMap<String, String>();

		int type = 1;
		String outputFileName = ENTPath.NAME_PERSON_FILE;

		if (type == 2) {
			outputFileName = ENTPath.NAME_ORGANIZATION_FILE;
		} else if (type == 3) {
			outputFileName = ENTPath.NAME_LOCATION_FILE;
		}

		CounterMap<String, String> cm = Generics.newCounterMap();
		Map<String, Integer> titleIdMap = Generics.newHashMap();

		for (int i = 0; i < ir.maxDoc(); i++) {
			if ((i + 1) % 100000 == 0) {
				System.out.printf("\r[%d/%d]", i + 1, ir.maxDoc());
			}

			// if (i == 1000) {
			// break;
			// }

			String title = ir.document(i).get(IndexFieldName.TITLE);

			if (!accept(stopPrefixes, title)) {
				continue;
			}

			String catStr = ir.document(i).get(IndexFieldName.CATEGORY).toLowerCase();
			String redirect = ir.document(i).get(IndexFieldName.REDIRECT_TITLE);

			if (isValidTitle(type, catStr)) {
				boolean isAdded = false;
				if (redirect.length() > 0) {
					ScoreDoc[] hits = is.search(new TermQuery(new Term(IndexFieldName.TITLE, redirect)), 1).scoreDocs;
					if (hits.length == 1) {
						String catStr2 = is.doc(hits[0].doc).get(IndexFieldName.CATEGORY).toLowerCase();
						if (isValidTitle(type, catStr2)) {
							titleVariantMap.put(redirect, title);
							titleIdMap.put(redirect, hits[0].doc);

							isAdded = true;
							Counter<String> c = Generics.newCounter();
							for (String word : StrUtils.split("\\W+", catStr2)) {
								c.incrementCount(word, 1);
							}
							cm.setCounter(redirect, c);
						}
					}
				}
				if (!isAdded) {
					titleVariantMap.put(title, "");
					titleIdMap.put(title, i);

					Counter<String> c = Generics.newCounter();
					for (String word : StrUtils.split("\\W+", catStr)) {
						c.incrementCount(word, 1);
					}
					cm.setCounter(title, c);
				}
			}
		}

		List<String> keys = new ArrayList<String>(titleVariantMap.keySet());
		Collections.sort(keys);

		TextFileWriter writer = new TextFileWriter(outputFileName);
		for (int i = 0; i < keys.size(); i++) {
			String title = keys.get(i);
			int id = titleIdMap.get(title);
			String catStr = "none";

			if (cm.getCounter(title).size() > 0) {
				Counter<String> c = cm.getCounter(title);
				catStr = c.toStringSortedByValues(true, false, c.size(), " ");
			}

			List<String> variants = titleVariantMap.get(title);
			Iterator<String> iter = variants.iterator();
			while (iter.hasNext()) {
				String variant = iter.next();
				if (variant.length() == 0) {
					iter.remove();
				}
			}
			String[] two = splitDisambiguationType(title);
			writer.write(String.format("%d\t%s\t%s\t%s\t%s\n", id, two[0], two[1] == null ? "none" : two[1], catStr,
					variants.size() == 0 ? "none" : StrUtils.join("|", variants)));
		}
		writer.close();

		System.out.printf("\r[%d/%d]\n", ir.maxDoc(), ir.maxDoc());
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

	private String getRedirect(String content) {
		Matcher m = rp1.matcher(content);
		String ret = null;
		if (m.find()) {
			ret = StrUtils.normalizeSpaces(m.group(1));
		}
		return ret;
	}

	private boolean isLocationName(String catStr) {
		boolean ret = false;
		if (catStr.contains("places") || catStr.contains("cities") || catStr.contains("countries") || catStr.contains("provinces")
				|| catStr.contains("states") || catStr.contains("territories")) {
			ret = true;
		}
		return ret;
	}

	private boolean isOrganizationName(String catStr) {
		boolean ret = false;
		Matcher m = op1.matcher(catStr);
		if (m.find()) {
			ret = true;
		}
		return ret;
	}

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

	private boolean isValidTitle(int type, String catStr) {
		boolean ret = false;
		if (type == 1) {
			ret = isPersonName(catStr);
		} else if (type == 2) {
			ret = isOrganizationName(catStr);
		} else if (type == 3) {
			ret = isLocationName(catStr);
		}
		return ret;
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

	private String[] splitDisambiguationType(String title) {
		String[] ret = new String[2];
		Matcher m = rp2.matcher(title);
		if (m.find()) {
			String disamType = m.group();
			title = title.replace(disamType, "").trim();
			disamType = disamType.substring(1, disamType.length() - 1);
			ret[0] = title;
			ret[1] = disamType;
		} else {
			ret[0] = title;
		}
		return ret;
	}
}
