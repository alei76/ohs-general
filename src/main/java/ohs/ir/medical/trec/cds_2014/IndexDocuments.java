package ohs.ir.medical.trec.cds_2014;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

import ohs.io.TextFileReader;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.medical.general.DocumentIndexer;

/**
 * Construct an inverted index with source document collection.
 * 
 * The index is constructed using Lucene-4.3.1, an open source search engine.
 * 
 * @author Heung-Seon Oh
 * 
 */
public class IndexDocuments {

	public static int ramSize = 5000;

	public static void index() throws Exception {
		System.out.println("index documents.");

		IndexWriter indexWriter = DocumentIndexer.getIndexWriter(CDSPath.INDEX_DIR);

		TextFileReader reader = new TextFileReader(CDSPath.TEXT_COLLECTION_FILE);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(1000);
			String line = reader.next();
			String[] parts = line.split("\t");

			if (parts.length != 2) {
				continue;
			}

			String pmcId = parts[0];
			String content = parts[1].replaceAll("<NL>", "\n");

			// System.out.println(text);
			// System.out.println();

			Document doc = new Document();
			doc.add(new StringField(CommonFieldNames.DOCUMENT_ID, pmcId, Field.Store.YES));
			doc.add(new TextField(CommonFieldNames.CONTENT, content, Store.YES));
			indexWriter.addDocument(doc);
		}
		reader.printLast();
		indexWriter.close();
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		// index();
		System.out.println("process ends.");
	}

}
