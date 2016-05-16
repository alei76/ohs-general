package ohs.ir.medical.general;

import java.io.File;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.lucene.common.MyTextField;
import ohs.types.BidMap;
import ohs.types.SetMap;
import ohs.utils.Generics;
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
		FileUtils.deleteFilesUnder(outputDirName);

		IndexWriterConfig iwc = new IndexWriterConfig(MedicalEnglishAnalyzer.newAnalyzer());
		// IndexWriterConfig iwc = new IndexWriterConfig(new
		// StandardAnalyzer());
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
		// di.indexWikiDbDump();
		di.indexClueWeb12();
		di.makeDocumentIdMap();

		System.out.println("process ends.");
	}

	public DocumentIndexer() {

	}

	public void indexClefEHealth() throws Exception {
		System.out.println("index CLEF eHealth.");
		IndexWriter iw = getIndexWriter(MIRPath.CLEF_EHEALTH_INDEX_DIR);

		TextFileReader reader = new TextFileReader(MIRPath.CLEF_EHEALTH_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.printProgress();

			String line = reader.next();
			String[] parts = line.split("\t");

			String uid = parts[0];
			String date = parts[1];
			String url = parts[2];
			String content = parts[3].replaceAll("\\n", "\n");

			Document doc = new Document();
			doc.add(new StringField(CommonFieldNames.DOCUMENT_ID, uid, Field.Store.YES));
			doc.add(new StringField(CommonFieldNames.URL, url, Field.Store.YES));
			doc.add(new StringField(CommonFieldNames.DATE, date, Field.Store.YES));
			doc.add(new MyTextField(CommonFieldNames.CONTENT, content, Store.YES));

			iw.addDocument(doc);
		}
		reader.printProgress();
		reader.close();

		iw.close();
	}

	public void indexClueWeb12() throws Exception {
		System.out.println("index ClueWeb12.");

		IndexWriter iw = getIndexWriter(MIRPath.CLUEWEB_INDEX_DIR);

		List<File> files = FileUtils.getFilesUnder(MIRPath.CLUEWEB_TEXT_DIR);

		Collections.sort(files);

		for (int i = 0; i < files.size(); i++) {

			List<String> lines = FileUtils.readLines(files.get(i).getPath());

			for (int j = 1; j < lines.size(); j++) {
				String line = lines.get(j);
				String[] parts = line.split("\t");

				if (parts.length > 4) {
					String ss = StrUtils.join("", parts, 3);

					parts = new String[] { parts[0], parts[1], parts[2], ss };
				}

				if (parts.length != 4) {
					System.out.printf("[%s]\n", line);
					continue;
				}

				if (parts[0].length() == 0 || parts[1].length() == 0 || parts[2].length() == 0) {
					System.out.printf("[%s]\n", line);
					continue;
				}

				parts = StrUtils.unwrap(parts);

				String id = parts[0];
				String text = parts[1];
				String uri = parts[2];
				String linkStr = parts[3];

				StringBuffer sb = new StringBuffer();

				String[] ss = text.split("<nl>");
				for (int k = 0; k < ss.length; k++) {
					String[] toks = ss[k].split(" ");
					for (int l = 0; l < toks.length; l++) {
						String tok = toks[l].trim();
						if (tok.length() > 0) {
							if (tok.startsWith("tbi:")) {
								sb.append(tok.substring(4).replace("_", " "));
							} else {
								sb.append(tok);
							}
						}

						if (l != toks.length - 1) {
							sb.append(" ");
						}
					}

					if (k != ss.length - 1) {
						sb.append("\n");
					}
				}

				List<String> links = Generics.newArrayList();

				if (linkStr.length() > 0) {
					for (String link : linkStr.split("<tab>")) {
						int idx = link.indexOf(":");
						if (idx > -1) {
							String type = link.substring(0, idx);
							String url = link.substring(idx + 1);
							if (url.length() > 0 && url.startsWith("http")) {
								links.add(url);
							}
						}
					}
				}

				Document doc = new Document();
				doc.add(new StringField(CommonFieldNames.DOCUMENT_ID, id, Field.Store.YES));
				doc.add(new StringField(CommonFieldNames.URL, uri, Field.Store.YES));
				doc.add(new MyTextField(CommonFieldNames.CONTENT, sb.toString(), Store.YES));
				doc.add(new TextField(CommonFieldNames.LINKS, StrUtils.join("\n", links), Store.YES));

				iw.addDocument(doc);
			}
		}

		iw.close();

	}

	public void indexOhsumed() throws Exception {
		System.out.println("index OHSUMED.");

		IndexWriter iw = getIndexWriter(MIRPath.OHSUMED_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.OHSUMED_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.printProgress();

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
			String abs = parts[5].replace("\\n", "\n");
			String authors = parts[6];
			String source = parts[7];

			Document doc = new Document();
			doc.add(new StringField(CommonFieldNames.DOCUMENT_ID, medlineId, Field.Store.YES));
			doc.add(new MyTextField(CommonFieldNames.CONTENT, title + "\n" + abs, Field.Store.YES));
			iw.addDocument(doc);
		}
		reader.printProgress();
		iw.close();
	}

	public void indexTrecCds() throws Exception {
		System.out.println("index TREC CDS.");

		IndexWriter iw = getIndexWriter(MIRPath.TREC_CDS_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.TREC_CDS_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.printProgress();
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
			content = content.replace("\\n", "\n");

			// System.out.println(text);
			// System.out.println();

			Document doc = new Document();
			doc.add(new StringField(CommonFieldNames.DOCUMENT_ID, pmcId, Field.Store.YES));
			doc.add(new TextField(CommonFieldNames.TITLE, title, Store.YES));
			doc.add(new MyTextField(CommonFieldNames.ABSTRACT, abs, Store.YES));
			doc.add(new MyTextField(CommonFieldNames.CONTENT, content, Store.YES));
			iw.addDocument(doc);
		}
		reader.printProgress();
		iw.close();
	}

	public void indexTrecGenomics() throws Exception {
		IndexWriter iw = getIndexWriter(MIRPath.TREC_GENOMICS_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.TREC_GENOMICS_COL_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.printProgress();
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
			doc.add(new StringField(CommonFieldNames.DOCUMENT_ID, id, Field.Store.YES));
			doc.add(new MyTextField(CommonFieldNames.CONTENT, content, Store.YES));

			iw.addDocument(doc);
		}
		reader.close();
		iw.close();
	}

	public void indexWikiDbDump() throws Exception {
		Set<String> stopSecNames = getStopSectionNames();

		SetMap<Integer, Integer> toToFrom = Generics.newSetMap();

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_redirects.ser.gz");
			Map<Integer, Integer> m = FileUtils.readIntMap(ois);
			ois.close();

			for (Entry<Integer, Integer> e : m.entrySet()) {
				toToFrom.put(e.getValue(), e.getKey());
			}
		}

		SetMap<Integer, Integer> pageToCats = Generics.newSetMap();

		{
			TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_catlinks.txt.gz");
			while (reader.hasNext()) {
				String line = reader.next();

				String[] parts = StrUtils.unwrap(line.split("\t"));

				if (parts.length != 3) {
					System.out.println(line);
					continue;
				}

				int pageid = Integer.parseInt(parts[0]);
				int parent_id = Integer.parseInt(parts[1]);
				String cl_type = parts[2];

				if (cl_type.equals("page")) {
					pageToCats.put(pageid, parent_id);
				}
			}
			reader.close();
		}

		BidMap<Integer, String> idToCat = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_cats.ser.gz");
			List<Integer> ids = FileUtils.readIntList(ois);
			List<String> titles = FileUtils.readStrList(ois);
			List<Integer> catPages = FileUtils.readIntList(ois);
			List<Integer> catSubcats = FileUtils.readIntList(ois);
			ois.close();

			idToCat = Generics.newBidMap(ids.size());
			// pageCnts = Generics.newCounter();
			// subCatCnts = Generics.newCounter();

			for (int i = 0; i < ids.size(); i++) {
				idToCat.put(ids.get(i), titles.get(i));
				// pageCnts.setCount(ids.get(i), catPages.get(i));
				// subCatCnts.setCount(ids.get(i), catSubcats.get(i));
			}
		}

		Map<Integer, String> idToTitle = Generics.newHashMap();

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_titles.ser.gz");
			idToTitle = FileUtils.readIntStrMap(ois);
			ois.close();
		}

		IndexWriter iw = getIndexWriter(MIRPath.WIKI_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_pages.txt.gz");
		reader.setPrintNexts(false);

		MediaWikiParserFactory factory = new MediaWikiParserFactory(Language.english);
		factory.setTemplateParserClass(FlushTemplates.class);

		MediaWikiParser parser = factory.createParser();

		while (reader.hasNext()) {
			reader.printProgress();

			String line = reader.next();

			// System.out.println(line);
			String[] parts = line.split("\t");

			if (parts.length > 3) {
				parts = new String[] { parts[0], parts[1], StrUtils.join("\t", parts, 2) };
			}

			parts = StrUtils.unwrap(parts);

			int pageid = Integer.parseInt(parts[0]);

			String title = parts[1];
			String wikiText = parts[2].replace("\\\\n", "\n");
			String catStr = "";
			String redStr = "";

			{
				StringBuffer sb = new StringBuffer();
				Set<Integer> cats = pageToCats.get(pageid, false);

				if (cats != null) {
					for (int cid : cats) {
						String cat = idToCat.getValue(cid);
						sb.append(String.format("%s\t%d\n", cat, cid));
					}
				}
				catStr = sb.toString().trim();
			}

			{
				StringBuffer sb = new StringBuffer();
				Set<Integer> froms = toToFrom.get(pageid, false);

				if (froms != null) {
					for (int from : froms) {
						sb.append(String.format("%s\t%d\n", idToTitle.get(from), from));
					}
				}
				redStr = sb.toString().trim();
			}

			ParsedPage pp = parser.parse(wikiText);

			if (pp == null) {
				continue;
			}

			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < pp.getSections().size(); i++) {
				Section sec = pp.getSection(i);
				String secName = sec.getTitle();

				if (secName != null && stopSecNames.contains(secName.toLowerCase())) {
					continue;
				}

				List<String> sents = new ArrayList<String>();

				for (int j = 0; j < sec.getContentList().size(); j++) {
					Content content = sec.getContentList().get(j);
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

			String content = sb.toString().trim();
			String redicrect = "";
			boolean is_redirect = false;

			{
				String[] lines = content.split("\n");
				String prefix = "REDIRECT ";

				if (lines.length == 2) {
					int idx = lines[1].indexOf(prefix);

					if (idx > -1) {
						is_redirect = true;
						redicrect = lines[1].substring(idx + prefix.length());
						// System.out.printf("%s -> %s\n", title, redicrect);
						// content = "";
					}
				}
			}

			if (is_redirect) {
				continue;
			}

			Document doc = new Document();
			doc.add(new StringField(CommonFieldNames.DOCUMENT_ID, pageid + "", Store.YES));
			doc.add(new StringField(CommonFieldNames.TITLE, title, Store.YES));
			doc.add(new StringField(CommonFieldNames.CATEGORY, catStr, Store.YES));
			doc.add(new TextField(CommonFieldNames.REDIRECTS, redStr, Store.YES));
			doc.add(new MyTextField(CommonFieldNames.CONTENT, content, Store.YES));
			iw.addDocument(doc);
		}
		reader.printProgress();
		reader.close();
		iw.close();

	}

	public void indexWikiXML() throws Exception {
		Set<String> stopSectionNames = getStopSectionNames();

		IndexWriter iw = getIndexWriter(MIRPath.WIKI_INDEX_DIR);
		TextFileReader reader = new TextFileReader(MIRPath.WIKI_COL_FILE);
		reader.setPrintNexts(false);

		MediaWikiParserFactory factory = new MediaWikiParserFactory(Language.english);
		factory.setTemplateParserClass(FlushTemplates.class);
		MediaWikiParser parser = factory.createParser();

		while (reader.hasNext()) {
			reader.printProgress();

			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String title = parts[0];
			String wikiText = parts[1].replace("\\n", "\n");

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
			doc.add(new StringField(CommonFieldNames.TITLE, title, Store.YES));
			// doc.add(new StringField(CommonFieldNames.LOWER_TITLE,
			// title.toLowerCase(), Store.YES));
			// doc.add(new StringField(CommonFieldNames.REDIRECT_TITLE,
			// redicrect, Store.YES));
			// doc.add(new StringField(CommonFieldNames.LOWER_REDIRECT_TITLE,
			// redicrect.toLowerCase(), Store.YES));
			doc.add(new MyTextField(CommonFieldNames.CONTENT, content, Store.YES));
			doc.add(new MyTextField(CommonFieldNames.CATEGORY, sb2.toString(), Store.YES));
			iw.addDocument(doc);
		}
		reader.printProgress();
		reader.close();

		iw.close();
	}

	public void makeDocumentIdMap() throws Exception {
		String[] indexDirNames = MIRPath.IndexDirNames;
		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		for (int i = 3; i < indexDirNames.length; i++) {
			String indexDirName = indexDirNames[i];
			String docMapFileName = docMapFileNames[i];

			System.out.printf("process [%s].\n", indexDirNames[i]);

			File outputFile = new File(docMapFileName);

			// if (outputFile.exists()) {
			// return;
			// }

			IndexSearcher is = SearcherUtils.getIndexSearcher(indexDirName);
			IndexReader ir = is.getIndexReader();

			List<String> docIds = new ArrayList<String>();

			for (int j = 0; j < ir.maxDoc(); j++) {
				if ((j + 1) % 100000 == 0) {
					System.out.printf("\r[%d/%d]", j + 1, ir.maxDoc());
				}
				Document doc = ir.document(j);
				String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
				docIds.add(docId);
			}
			System.out.printf("\r[%d/%d]\n", ir.maxDoc(), ir.maxDoc());

			TextFileWriter writer = new TextFileWriter(docMapFileName);
			for (int j = 0; j < docIds.size(); j++) {
				String output = j + "\t" + docIds.get(j);
				writer.write(output + "\n");
			}
			writer.close();
		}
	}

}
