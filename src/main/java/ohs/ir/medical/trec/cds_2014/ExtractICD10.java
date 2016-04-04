package ohs.ir.medical.trec.cds_2014;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import de.tudarmstadt.ukp.wikipedia.parser.NestedList;
import de.tudarmstadt.ukp.wikipedia.parser.NestedListContainer;
import de.tudarmstadt.ukp.wikipedia.parser.NestedListElement;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.Section;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.SearcherUtils;
import ohs.types.Counter;
import ohs.types.ListMap;
import ohs.utils.StrUtils;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class ExtractICD10 {

	private static String getName(String text) {
		String regex = "\\([^\\(\\)]+\\)";
		Pattern p = Pattern.compile(regex);

		Matcher m = p.matcher(text);

		StringBuffer sb = new StringBuffer();

		while (m.find()) {
			m.appendReplacement(sb, "");
		}
		m.appendTail(sb);

		text = sb.toString().trim();
		return text;
	}

	public static Set<String> getStopSectionTitleSet() {
		String[] stopSectionTitles = { "See also", "Further reading", "References", "External links" };
		Set<String> ret = new TreeSet<String>();
		for (String sectionTitle : stopSectionTitles) {
			ret.add(sectionTitle.toLowerCase());
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		ExtractICD10 ext = new ExtractICD10();
		ext.extractTopLevelChapters();
		// ext.extractStructure();
		// ext.refineStructure();
		// ext.attachWikiPages();
		System.out.println("process ends.");
	}

	private static void recursive(NestedListContainer node, List<String> items) {
		for (int i = 0; i < node.getNestedLists().size(); i++) {
			NestedList nl = node.getNestedList(i);

			if (nl instanceof NestedListContainer) {
				recursive((NestedListContainer) nl, items);
			} else if (nl instanceof NestedListElement) {
				recursive((NestedListElement) nl, items);
			}
		}
	}

	private static void recursive(NestedListElement node, List<String> items) {
		String title = getName(node.getText());

		if (title.length() > 0) {

			if (title.equals("Latent syphilis, unspecified as early or late")) {
				System.out.println();
			}

			StringBuffer sb = new StringBuffer();
			if (node.getTemplates().size() > 0) {
				sb.append(String.format("\nCode:\t%s", StrUtils.join("-", node.getTemplates().get(0).getParameters())));
			} else {
				// sb.append("\n");
			}

			sb.append(String.format("\nTitle:\t%s", title));

			for (int i = 0; i < node.getLinks().size(); i++) {
				Link link = node.getLinks().get(i);
				sb.append(String.format("\n%s:\t%d\t%s\t%s", link.getType().toString(), i + 1, link.getText(), link.getTarget()));
			}
			items.add(sb.toString().trim());
		}
	}

	private Map<String, Integer> searchCacheMap;

	private IndexSearcher indexSearcher;

	private MedicalEnglishAnalyzer analyzer;

	public ExtractICD10() throws Exception {
		analyzer = MedicalEnglishAnalyzer.newAnalyzer();

		indexSearcher = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);
		indexSearcher.setSimilarity(new LMDirichletSimilarity());

		searchCacheMap = new HashMap<String, Integer>();
	}

	public void attachWikiPages() throws Exception {
		Counter<String> sectionCounts = new Counter<String>();
		int numRedirects = 0;

		Set<String> stopSectionTitleSet = getStopSectionTitleSet();

		// Map<String, String> redirectMap = readRedirects();

		String regex = "#REDIRECT \\[\\[([^\\[\\]]+)\\]\\]";
		Pattern p = Pattern.compile(regex);

		MediaWikiParser parser = new MediaWikiParserFactory(Language.english).createParser();

		TextFileReader reader = new TextFileReader(CDSPath.ICD10_REFINED_HIERARCHY_FILE);
		TextFileWriter writer = new TextFileWriter(CDSPath.ICD10_REFINED_HIERARCHY_PAGE_ATTACHED_FILE);
		TextFileWriter writer2 = new TextFileWriter(CDSPath.ICD10_LOG_FILE);

		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();

			for (int i = 2; i < lines.size(); i++) {
				String line = lines.get(i);
				String[] parts = line.split("\t");
				String wikiTitle = parts[1].replace("_", " ").toLowerCase();
				String wikiKey = parts[2].replace("_", " ").toLowerCase();

				String searchKey = wikiKey;

				boolean isSearching = true;

				ParsedPage page = null;
				Document doc = null;

				while (isSearching) {
					doc = searchDocument(searchKey);

					if (doc == null) {
						break;
					}

					String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
					String title = doc.getField(CommonFieldNames.TITLE).stringValue();
					String wikiText = doc.getField(CommonFieldNames.CONTENT).stringValue();

					page = parser.parse(wikiText);

					if (page.getText().startsWith("REDIRECT")) {
						Matcher m = p.matcher(wikiText);
						if (m.find()) {
							String redirectTo = m.group(1).trim().toLowerCase().replace("_", " ");
							if (searchKey.equals(redirectTo) || redirectTo.length() == 0) {
								isSearching = false;
							} else {
								searchKey = redirectTo;
								numRedirects++;
							}
						} else {
							isSearching = false;
						}
					} else {
						isSearching = false;
					}
				}

				if (doc == null) {
					writer2.write(String.format("Fail to find [%s]\n", wikiKey));
					continue;
				}

				String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
				String title = doc.getField(CommonFieldNames.TITLE).stringValue();
				String wikiText = doc.getField(CommonFieldNames.CONTENT).stringValue();

				boolean isRelatedToDisease = false;

				for (int j = 0; j < page.getSections().size(); j++) {
					Section section = page.getSection(j);
					String sectionTitle = section.getTitle();

					if (sectionTitle != null && stopSectionTitleSet.contains(sectionTitle.toLowerCase())) {
						isRelatedToDisease = true;
						break;
					}
				}

				// System.out.println(page.getText());

				StringBuffer sb = new StringBuffer();

				line = parts[0] + "\t" + isRelatedToDisease + "\t" + title + "\t" + wikiText.replace("\n", "<NL>");
				lines.set(i, line);

				// writer.write(sb.toString());
			}
			String output = StrUtils.join("\n", lines);

			writer.write(output + "\n\n");
		}

		reader.close();
		writer.close();
		writer2.close();

		System.out.println(sectionCounts.toStringSortedByValues(true, false, sectionCounts.size()));
		System.out.println(numRedirects);

		// System.out.println(sectionCounts.toString());
	}

	public void extractStructure() throws Exception {
		Set<String> stopSectionTitleSet = getStopSectionTitleSet();

		String icdText = FileUtils.readText(CDSPath.ICD10_TOP_LEVEL_CHAPTER_FILE);

		String[] lines = icdText.split("\n");

		TextFileWriter writer = new TextFileWriter(CDSPath.ICD10_HIERARCHY_FILE);

		MediaWikiParser parser = new MediaWikiParserFactory().createParser();

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String[] parts = line.split("\t");

			String chapterName = parts[0];
			String url = parts[1];

			writer.write(chapterName + "\n\n");

			// if (!wikiTitle.contains("Codes for special purposes")) {
			// continue;
			// }

			Document doc = searchDocument(chapterName);

			if (doc == null) {
				continue;
			}

			String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
			String title = doc.getField(CommonFieldNames.TITLE).stringValue();
			String wikiText = doc.getField(CommonFieldNames.CONTENT).stringValue();

			ParsedPage page = parser.parse(wikiText);

			if (page == null) {
				continue;
			}

			page.setName(title);

			StringBuffer sb = new StringBuffer();

			for (int k = 0; k < page.getSections().size(); k++) {
				Section section = page.getSection(k);

				String sectionTitle = section.getTitle();

				if (sectionTitle != null && stopSectionTitleSet.contains(sectionTitle.toLowerCase())) {
					continue;
				}

				System.out.println(sectionTitle);

				// String regex = "^[A-Z][0-9][0-9]";
				// Pattern p = Pattern.compile(regex);
				//
				// if (!p.matcher(sectionTitle).find()) {
				// System.out.println(sectionTitle);
				// continue;
				// }

				// if (wikiTitle.contains("Codes for special purposes") &&
				// sectionName == null) {
				// sb.append("Section Name:\tU00-U99");
				// } else if(){

				// }

				List<String> items = new ArrayList<String>();
				items.add(String.format("Section Name:\t%s", sectionTitle));

				// System.out.printf(" level : %d\n", section.getLevel());
				// System.out.printf(" formats : %d\n",
				// section.getFormats().size());
				// System.out.printf(" paragraphs : %d\n",
				// section.getParagraphs().size());
				// System.out.printf(" tables : %d\n",
				// section.getTables().size());
				// System.out.printf(" links : %d\n",
				// section.getLinks().size());
				// System.out.printf(" templates : %d\n",
				// section.getTemplates().size());
				// System.out.printf(" definitions : %d\n",
				// section.getDefinitionLists().size());
				// System.out.println();

				List<NestedListContainer> containers = section.getNestedLists();

				for (int m = 0; m < containers.size(); m++) {
					NestedListContainer container = containers.get(m);
					recursive(container, items);
				}

				if (items.size() == 1) {
					continue;
				}

				sb.append(StrUtils.join("\n\n", items));
			}

			writer.write(sb.toString());

			if (i != lines.length - 1) {
				writer.write("\n\n");
			}
		}

		writer.close();
	}

	public void extractTopLevelChapters() throws Exception {
		String text = FileUtils.readText(CDSPath.ICD10_HTML_FILE);

		String prefix = "<td><a href=\"http://en.wikipedia.org/wiki/";
		String regex = "\"([^\"]+)\"";
		Pattern p = Pattern.compile(regex);

		StringBuffer sb = new StringBuffer();

		for (String line : text.split("\n")) {
			int start = line.indexOf(prefix);

			if (line.contains("<td><a href=\"http://en.wikipedia.org/wiki/ICD-10_Chapter")) {
				Matcher m = p.matcher(line);

				List<String> list = new ArrayList<String>();

				while (m.find()) {
					list.add(m.group(1));
				}

				sb.append(list.get(1) + "\t" + list.get(0) + "\n");
			}
		}

		FileUtils.write(CDSPath.ICD10_TOP_LEVEL_CHAPTER_FILE, sb.toString().trim());
	}

	private Map<String, String> readRedirects() {
		System.out.println("read redirects.");
		Map<String, String> ret = new HashMap<String, String>();

		TextFileReader reader = new TextFileReader(MIRPath.WIKI_REDIRECT_TITLE_FILE);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String from = parts[0].toLowerCase();
			String to = parts[1].toLowerCase();

			if (reader.getNumLines() == 1 || from.equals(to)) {
				continue;
			}

			if (from.contains("bile ducts")) {
				System.out.println(line);
			}

			ret.put(from, to);
		}
		reader.close();
		System.out.println("read ends.");
		return ret;
	}

	public void refineStructure() throws Exception {
		List<String> items = new ArrayList<String>();

		TextFileReader reader = new TextFileReader(new File(CDSPath.ICD10_HIERARCHY_FILE));
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			items.add(StrUtils.join("\n", lines));
		}
		reader.close();

		List<Integer> chapterLocs = new ArrayList<Integer>();
		List<Integer> sectionLocs = new ArrayList<Integer>();
		List<Integer> codeLocs = new ArrayList<Integer>();

		ListMap<String, String> map = new ListMap<String, String>(true);

		for (int i = 0; i < items.size(); i++) {
			String item = items.get(i);

			if (item.startsWith("ICD-10 Chapter")) {
				chapterLocs.add(i);
			} else if (item.startsWith("Section Name:")) {
				sectionLocs.add(i);
			} else if (item.startsWith("Code:")) {
				codeLocs.add(i);
			}
		}

		System.out.println(chapterLocs);
		System.out.println(sectionLocs);
		System.out.println(codeLocs);

		for (int i = 0; i < codeLocs.size() - 1; i++) {
			int start = codeLocs.get(i);
			int end = codeLocs.get(i + 1);

			String code = null;
			String label = null;
			List<String> wikiTitles = new ArrayList<String>();
			List<String> wikiKeys = new ArrayList<String>();

			for (int j = start; j < end; j++) {
				String item = items.get(j);
				wikiKeys.clear();
				wikiTitles.clear();

				String[] lines = item.split("\n");

				for (int k = 0; k < lines.length; k++) {
					String line = lines[k];
					if (line.startsWith("Code:")) {
						code = line.split("\t")[1];
					} else if (line.startsWith("Title:")) {
						label = line.split("\t")[1];
					} else if (line.startsWith("INTERNAL")) {
						wikiTitles.add(line.split("\t")[2]);
						wikiKeys.add(line.split("\t")[3]);
					}
				}

				String regex = "^[A-Z]\\-[0-9][0-9]";
				Pattern p = Pattern.compile(regex);

				if (!p.matcher(code).find()) {
					continue;
				}

				String tempKey = code + "\t" + label;

				for (int k = 0; k < wikiKeys.size(); k++) {
					String tempValue = wikiTitles.get(k) + "\t" + wikiKeys.get(k);

					System.out.printf("%s\t%s\t%s\t%s\n", code, label, tempKey, tempValue);
					map.put(tempKey, tempValue);
				}
			}
		}

		System.out.println(map);

		StringBuffer sb = new StringBuffer();

		List<String> keys = new ArrayList<String>(map.keySet());

		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			List<String> internals = map.get(key);

			sb.append(i + 1);
			sb.append("\n" + key);

			for (int j = 0; j < internals.size(); j++) {
				sb.append(String.format("\n%d\t%s", j + 1, internals.get(j)));
			}

			if (i != keys.size()) {
				sb.append("\n\n");
			}
		}

		FileUtils.write(CDSPath.ICD10_REFINED_HIERARCHY_FILE, sb.toString());
	}

	private Document searchDocument(String wikiTitle) throws Exception {
		System.out.println(wikiTitle);

		Document ret = null;

		Integer docId = searchCacheMap.get(wikiTitle.toLowerCase());

		if (docId == null) {
			BooleanQuery searchQuery = new BooleanQuery();
			searchQuery.add(new BooleanClause(new TermQuery(new Term(CommonFieldNames.TITLE, wikiTitle.toLowerCase())), Occur.SHOULD));
			TopDocs topDocs = indexSearcher.search(searchQuery, 1);
			if (topDocs.scoreDocs.length > 0) {
				ret = indexSearcher.getIndexReader().document(topDocs.scoreDocs[0].doc);
			}

			if (ret == null) {
				searchCacheMap.put(wikiTitle.toLowerCase(), -1);
			} else {
				searchCacheMap.put(wikiTitle.toLowerCase(), docId);
			}

		} else if (docId == -1) {

		} else {
			indexSearcher.getIndexReader().document(docId.intValue());
		}

		return ret;
	}
}
