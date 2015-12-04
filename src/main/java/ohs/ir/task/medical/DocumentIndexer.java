package ohs.ir.task.medical;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.parser.Content;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.Section;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.FlushTemplates;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.lucene.common.MyTextField;
import ohs.utils.StrUtils;

/**
 * Construct an inverted index with source document collection.
 * 
 * @author Heung-Seon Oh
 * 
 */
public class DocumentIndexer {

	public static final int ram_size = 5000;

	public static IndexWriter getIndexWriter(String outputDirName) throws Exception {

		IndexWriterConfig iwc = new IndexWriterConfig(MedicalEnglishAnalyzer.getAnalyzer());
		// IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
		iwc.setOpenMode(OpenMode.CREATE);
		iwc.setRAMBufferSizeMB(ram_size);

		IndexWriter ret = new IndexWriter(FSDirectory.open(Paths.get(outputDirName)), iwc);
		return ret;
	}

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

	public static Set<String> getStopSectionNames() {
		String[] stopSectionNames = { "references", "external links", "see also", "notes", "further reading" };
		Set<String> ret = new HashSet<String>();
		for (String s : stopSectionNames) {
			ret.add(s);
		}
		return ret;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DocumentIndexer di = new DocumentIndexer();
		// di.indexTrecCds();
		// di.indexClefEHealth();
		// di.indexOhsumed();
		// di.indexTrecGenomics();

		// di.indexWiki();
		di.makeDocumentIdMap();

		System.out.println("process ends.");
	}

	public DocumentIndexer() {

	}

	public void indexClefEHealth() throws Exception {
		System.out.println("index CLEF eHealth.");
		IndexWriter indexWriter = getIndexWriter(MIRPath.CLEF_EHEALTH_INDEX_DIR);

		TextFileReader reader = new TextFileReader(MIRPath.CLEF_EHEALTH_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(100000);

			String line = reader.next();
			String[] parts = line.split("\t");

			String uid = parts[0];
			String date = parts[1];
			String url = parts[2];
			String content = parts[3].replaceAll("<NL>", "\n");

			Document doc = new Document();
			doc.add(new StringField(IndexFieldName.DOCUMENT_ID, uid, Field.Store.YES));
			doc.add(new StringField(IndexFieldName.URL, url, Field.Store.YES));
			doc.add(new StringField(IndexFieldName.DATE, date, Field.Store.YES));
			doc.add(new MyTextField(IndexFieldName.CONTENT, content, Store.YES));

			indexWriter.addDocument(doc);
		}
		reader.printLast();
		reader.close();

		indexWriter.close();
	}

	public void indexOhsumed() throws Exception {
		System.out.println("index OHSUMED.");

		IndexWriter writer = getIndexWriter(MIRPath.OHSUMED_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.OHSUMED_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(100000);

			String line = reader.next();
			String[] parts = line.split("\t");

			// if (parts.length != 2) {
			// continue;
			// }

			// if (reader.getNumLines() > 10000) {
			// break;
			// }

			String seqId = parts[0];
			String medlineId = parts[1];
			String meshTerms = parts[2];
			String title = parts[3];
			String publicationType = parts[4];
			String abs = parts[5].replace("<NL>", "\n");
			String authors = parts[6];
			String source = parts[7];

			Document doc = new Document();
			doc.add(new StringField(IndexFieldName.DOCUMENT_ID, medlineId, Field.Store.YES));
			doc.add(new MyTextField(IndexFieldName.CONTENT, title + "\n" + abs, Field.Store.YES));
			writer.addDocument(doc);
		}
		reader.printLast();
		writer.close();
	}

	public void indexTrecCds() throws Exception {
		System.out.println("index TREC CDS.");

		IndexWriter writer = getIndexWriter(MIRPath.TREC_CDS_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(100000);
			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 4) {
				continue;
			}

			for (int i = 0; i < parts.length; i++) {
				String s = parts[i];
				if (s.equals("empty")) {
					parts[i] = "";
				}
			}

			String pmcId = parts[0];
			String title = parts[1];
			String abs = parts[2];
			String content = parts[3];
			content = title + "\n" + abs + "\n" + content;
			content = content.replace("<NL>", "\n");

			// System.out.println(text);
			// System.out.println();

			Document doc = new Document();
			doc.add(new StringField(IndexFieldName.DOCUMENT_ID, pmcId, Field.Store.YES));
			doc.add(new TextField(IndexFieldName.TITLE, title, Store.YES));
			doc.add(new MyTextField(IndexFieldName.ABSTRACT, abs, Store.YES));
			doc.add(new MyTextField(IndexFieldName.CONTENT, content, Store.YES));
			writer.addDocument(doc);
		}
		reader.printLast();
		writer.close();
	}

	public void indexTrecGenomics() throws Exception {
		IndexWriter indexWriter = getIndexWriter(MIRPath.TREC_GENOMICS_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.TREC_GENOMICS_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(5000);
			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String id = parts[0];
			String content = parts[1];

			int start = id.lastIndexOf("/");
			int end = id.lastIndexOf(".");
			id = id.substring(start + 1, end);

			Document doc = new Document();
			doc.add(new StringField(IndexFieldName.DOCUMENT_ID, id, Field.Store.YES));
			doc.add(new MyTextField(IndexFieldName.CONTENT, content, Store.YES));

			indexWriter.addDocument(doc);
		}
		reader.close();
		indexWriter.close();
	}

	public void indexWiki() throws Exception {
		Set<String> stopSectionNames = getStopSectionNames();

		IndexWriter writer = getIndexWriter(MIRPath.WIKI_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.WIKI_COL_FILE);
		reader.setPrintNexts(false);

		MediaWikiParserFactory factory = new MediaWikiParserFactory(Language.english);
		factory.setTemplateParserClass(FlushTemplates.class);
		MediaWikiParser parser = factory.createParser();

		while (reader.hasNext()) {
			reader.print(100000);

			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String title = parts[0];
			String wikiText = parts[1].replace("<NL>", "\n");

			ParsedPage pp = parser.parse(wikiText);

			StringBuffer sb = new StringBuffer();
			sb.append(title + "\n");

			for (int i = 0; i < pp.getSections().size(); i++) {
				Section section = pp.getSection(i);
				String secTitle = section.getTitle();

				if (secTitle != null && stopSectionNames.contains(secTitle.toLowerCase())) {
					continue;
				}

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

			String content = sb.toString();
			String redicrect = "";

			{
				String[] lines = content.split("\n");
				String prefix = "REDIRECT ";

				if (lines.length == 2) {
					int idx = lines[1].indexOf(prefix);

					if (idx > -1) {
						redicrect = lines[1].substring(idx + prefix.length());
						System.out.printf("%s -> %s\n", title, redicrect);
						content = "";
					}
				}
			}

			StringBuffer sb2 = new StringBuffer();

			List<Link> categories = pp.getCategories();
			for (Link link : categories) {
				String s = link.getTarget().replaceAll("_", " ");
				sb2.append(s.substring(9) + "\n");
			}

			Document doc = new Document();
			doc.add(new StringField(IndexFieldName.TITLE, title, Store.YES));
			doc.add(new StringField(IndexFieldName.LOWER_TITLE, title.toLowerCase(), Store.YES));
			doc.add(new StringField(IndexFieldName.REDIRECT_TITLE, redicrect.toLowerCase(), Store.YES));
			doc.add(new MyTextField(IndexFieldName.CONTENT, content, Store.YES));
			doc.add(new MyTextField(IndexFieldName.CATEGORY, sb2.toString(), Store.YES));
			writer.addDocument(doc);
		}
		reader.printLast();
		reader.close();

		writer.close();
	}

	public void makeDocumentIdMap() throws Exception {
		String[] indexDirNames = MIRPath.IndexDirNames;
		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		for (int i = 0; i < indexDirNames.length; i++) {
			String indexDirName = indexDirNames[i];
			String docMapFileName = docMapFileNames[i];

			System.out.printf("process [%s].\n", indexDirNames[i]);

			File outputFile = new File(docMapFileName);

			// if (outputFile.exists()) {
			// return;
			// }

			IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(indexDirName);
			IndexReader indexReader = indexSearcher.getIndexReader();

			List<String> docIds = new ArrayList<String>();

			for (int j = 0; j < indexReader.maxDoc(); j++) {
				if ((j + 1) % 100000 == 0) {
					System.out.printf("\r[%d/%d]", j + 1, indexReader.maxDoc());
				}
				Document doc = indexReader.document(j);
				String docId = doc.getField(IndexFieldName.DOCUMENT_ID).stringValue();
				docIds.add(docId);
			}
			System.out.printf("\r[%d/%d]\n", indexReader.maxDoc(), indexReader.maxDoc());

			TextFileWriter writer = new TextFileWriter(docMapFileName);
			for (int j = 0; j < docIds.size(); j++) {
				String output = j + "\t" + docIds.get(j);
				writer.write(output + "\n");
			}
			writer.close();
		}
	}

}
