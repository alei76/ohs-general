package ohs.ir.medical.general;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;

import ohs.io.TextFileReader;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MyTextField;

/**
 * Construct an inverted index with source document collection.
 * 
 * @author Heung-Seon Oh
 * 
 */
public class SentenceIndexer {

	public static final int ram_size = 5000;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// String[] sentFileNames = MIRPath.SentFileNames;
		// String[] sentIndexDirNames = MIRPath.SentIndexDirNames;

		// for (int i = 0; i < sentIndexDirNames.length; i++) {
		// new SentenceIndexer(sentFileNames[i], sentIndexDirNames[i]).index();
		// }

		System.out.println("process ends.");
	}

	private String sentFileName;

	private String indexDirName;

	public SentenceIndexer(String sentFileName, String indexDirName) {
		this.sentFileName = sentFileName;
		this.indexDirName = indexDirName;
	}

	public void index() throws Exception {
		System.out.printf("index sentences in [%s] to [%s]\n", sentFileName, indexDirName);

		IndexWriter iw = DocumentIndexer.getIndexWriter(indexDirName);

		TextFileReader reader = new TextFileReader(sentFileName);
		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.printProgress();

			String line = reader.next();
			String[] parts = line.split("\t");

			String docId = parts[0];
			String no = parts[1];
			String sent = parts[2];

			Document doc = new Document();
			doc.add(new StringField(CommonFieldNames.DOCUMENT_ID, docId, Field.Store.YES));
			doc.add(new StringField(CommonFieldNames.SENTENCE_ID, no, Field.Store.YES));
			doc.add(new MyTextField(CommonFieldNames.CONTENT, sent, Store.YES));

			iw.addDocument(doc);
		}
		reader.printProgress();
		reader.close();

		iw.close();
	}

}
