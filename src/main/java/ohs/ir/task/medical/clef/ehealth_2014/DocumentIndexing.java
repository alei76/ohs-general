package ohs.ir.task.medical.clef.ehealth_2014;

import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

import ohs.io.TextFileReader;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.task.medical.DocumentIndexer;
import ohs.utils.StopWatch;

/**
 * Construct an inverted index with source document collection.
 * 
 * The index is constructed using Lucene-4.3.1, an open source search engine.
 * 
 * @author Heung-Seon Oh
 * 
 */
public class DocumentIndexing {

	public static int ramSize = 5000;

	public static void index() throws Exception {
		System.out.println("index documents.");

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		IndexWriter indexWriter = DocumentIndexer.getIndexWriter(EHPath.INDEX_DIR);
		TextFileReader reader = new TextFileReader(EHPath.COLLECTION_SENTENCE_FILE);
		reader.setPrintNexts(true);

		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();

			reader.print(1000);

			if (lines.size() != 4) {
				continue;
			}

			// if (reader.getNumNexts() > 1000) {
			// break;
			// }

			String uid = lines.get(0);
			String date = lines.get(1);
			String url = lines.get(2);
			String content = lines.get(3).replaceAll("<NL>", "\n");

			Document doc = new Document();
			doc.add(new StringField(IndexFieldName.DOCUMENT_ID, uid, Field.Store.YES));
			doc.add(new StringField(IndexFieldName.URL, url, Field.Store.YES));
			doc.add(new StringField(IndexFieldName.DATE, date, Field.Store.YES));
			doc.add(new TextField(IndexFieldName.CONTENT, content, Store.YES));

			indexWriter.addDocument(doc);
		}
		reader.printLast();
		reader.close();

		indexWriter.close();
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		index();
		System.out.println("process ends.");
	}

}
