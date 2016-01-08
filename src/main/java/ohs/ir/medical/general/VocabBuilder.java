package ohs.ir.medical.general;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import ohs.io.FileUtils;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.types.Counter;

public class VocabBuilder {

	public static void build(String indexDirName, String outputFileName) throws Exception {
		IndexSearcher is = SearcherUtils.getIndexSearcher(indexDirName);

		Fields fields = MultiFields.getFields(is.getIndexReader());

		Terms t = fields.terms(IndexFieldName.CONTENT);

		System.out.println(t.getSumTotalTermFreq());

		TermsEnum te = t.iterator();

		BytesRef bytesRef = null;
		PostingsEnum pe = null;

		Counter<String> c = new Counter<String>();

		while ((bytesRef = te.next()) != null) {
			pe = te.postings(pe, PostingsEnum.ALL);
			pe.nextDoc();
			// if (pe.nextDoc() != 0) {
			// throw new AssertionError();
			// }

			long cnt = te.totalTermFreq();
			String word = bytesRef.utf8ToString();

			c.incrementCount(word, cnt);
		}

		FileUtils.write(outputFileName, c);
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// for (int i = 0; i < MIRPath.IndexDirNames.length; i++) {
		// build(MIRPath.IndexDirNames[i], MIRPath.VocFileNames[i]);
		// }

		// merge(MIRPath.VocFileNames, MIRPath.VOCAB_FILE);

		// for (int i = 0; i < MIRPath.TaskDirs.length; i++) {
		// String outputFileName = MIRPath.TaskDirs[i] + "vocab2.txt";
		// build2(MIRPath.IndexDirNames[i], outputFileName);
		// }

		System.out.println("process ends.");
	}

	public static void merge(String[] inputFileNames, String outputFileName) throws Exception {
		Counter<String> c = new Counter<String>();

		for (String inputFileName : inputFileNames) {
			c.incrementAll(FileUtils.readCounter(inputFileName));
		}

		FileUtils.write(outputFileName, c);
	}

}
