package ohs.ir.medical.clef.ehealth_2015;

import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.FlushTemplates;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import ohs.io.TextFileReader;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.medical.general.DocumentIndexer;
import ohs.ir.medical.general.MIRPath;

public class WikiIndexer4ICD {

	public static void indexWiki() throws Exception {

		Set<String> stopSectionNames = DocumentIndexer.getStopSectionNames();

		IndexWriter indexWriter = DocumentIndexer.getIndexWriter(MIRPath.WIKI_INDEX_DIR);

		TextFileReader reader = new TextFileReader(MIRPath.WIKI_COL_FILE);
		reader.setPrintNexts(false);

		MediaWikiParserFactory factory = new MediaWikiParserFactory(Language.english);
		factory.setTemplateParserClass(FlushTemplates.class);
		MediaWikiParser parser = factory.createParser();

		while (reader.hasNext()) {
			reader.print(10000);

			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String title = parts[0];
			String wikiText = parts[1].replace("<NL>", "\n");

			// if (wikiText.startsWith("#REDIRECT")) {
			// continue;
			// }

			// if (reader.getNumLines() > 1000) {
			// break;
			// }

			// if (title.startsWith("List of")) {
			// continue;
			// }

			ParsedPage pp = parser.parse(wikiText);

			// StringBuffer sb = new StringBuffer();
			// sb.append(title + "\n");

			// for (int i = 0; i < pp.getSections().size(); i++) {
			// Section section = pp.getSection(i);
			// String secTitle = section.getTitle();
			//
			// if (secTitle != null && stopSectionNames.contains(secTitle.toLowerCase())) {
			// continue;
			// }
			//
			// List<String> sents = new ArrayList<String>();
			//
			// for (int j = 0; j < section.getContentList().size(); j++) {
			// Content content = section.getContentList().get(j);
			//
			// String[] ss = content.getText().split("[\\n]+");
			//
			// for (String s : ss) {
			// s = s.trim();
			// if (s.length() > 0) {
			// sents.add(s);
			// }
			// }
			// }
			//
			// String s = StrUtils.join("\n", sents);
			//
			// if (s.length() > 0) {
			// sb.append(s + "\n\n");
			// }
			// }

			StringBuffer sb2 = new StringBuffer();

			List<Link> categories = pp.getCategories();
			for (Link link : categories) {
				String s = link.getTarget().replaceAll("_", " ");
				sb2.append(s.substring(9) + "\n");
			}

			Document doc = new Document();
			doc.add(new StringField(IndexFieldName.TITLE, title, Store.YES));
			doc.add(new StringField(IndexFieldName.REDIRECT_TITLE, title.toLowerCase(), Store.YES));
			// doc.add(new MyTextField(IndexFieldName.CONTENT, sb.toString(), Store.YES));
			doc.add(new TextField(IndexFieldName.CONTENT, wikiText, Store.YES));
			doc.add(new TextField(IndexFieldName.CATEGORY, sb2.toString(), Store.YES));
			indexWriter.addDocument(doc);
		}
		reader.printLast();
		reader.close();

		indexWriter.close();
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// indexWiki();

		System.out.println("process ends.");
	}

}
