package ohs.ir.medical.general;

import java.nio.file.Paths;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;

import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Indexer;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class SearcherUtils {

	public static IndexSearcher getIndexSearcher(String indexDirName) throws Exception {
		System.out.printf("open an index at [%s]\n", indexDirName);
		IndexSearcher ret = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(indexDirName))));
		ret.setSimilarity(new LMDirichletSimilarity());
		// indexSearcher.setSimilarity(new BM25Similarity());
		// indexSearcher.setSimilarity(new DFRSimilarity(new BasicModelBE(), new
		// AfterEffectB(), new NormalizationH1()));
		return ret;
	}

	public static IndexSearcher[] getIndexSearchers(String[] indexDirNames) throws Exception {
		IndexSearcher[] ret = new IndexSearcher[indexDirNames.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = getIndexSearcher(indexDirNames[i]);
		}
		return ret;
	}

	public static QueryParser getQueryParser() throws Exception {
		QueryParser ret = new QueryParser(IndexFieldName.CONTENT, MedicalEnglishAnalyzer.getAnalyzer());
		return ret;
	}

	public static SparseVector search(Query q, IndexSearcher is, int top_k) throws Exception {
		TopDocs topDocs = is.search(q, top_k);
		int num_docs = topDocs.scoreDocs.length;

		SparseVector ret = new SparseVector(num_docs);
		for (int i = 0; i < topDocs.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = topDocs.scoreDocs[i];
			ret.incrementAtLoc(i, scoreDoc.doc, scoreDoc.score);
		}
		ret.sortByIndex();
		return ret;
	}

	public static SparseVector search(SparseVector qm, Indexer<String> wordIndexer, IndexSearcher is, int top_k) throws Exception {
		Query q = AnalyzerUtils.getQuery(VectorUtils.toCounter(qm, wordIndexer));
		return search(q, is, top_k);
	}

	public static void write(TextFileWriter writer, String queryId, SparseVector docScores) {
		docScores.sortByValue();
		for (int i = 0; i < docScores.size(); i++) {
			int docId = docScores.indexAtLoc(i);
			double score = docScores.valueAtLoc(i);
			writer.write(queryId + "\t" + docId + "\t" + score + "\n");
		}
		docScores.sortByIndex();
	}

}
