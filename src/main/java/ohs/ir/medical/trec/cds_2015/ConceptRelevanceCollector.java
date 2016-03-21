package ohs.ir.medical.trec.cds_2015;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.DocumentIdMapper;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.SearcherUtils;
import ohs.ir.medical.query.BaseQuery;
import ohs.ir.medical.query.QueryReader;
import ohs.ir.medical.query.RelevanceReader;
import ohs.matrix.SparseVector;

public class ConceptRelevanceCollector {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ConceptRelevanceCollector c = new ConceptRelevanceCollector();
		c.collect();

		System.out.println("process ends.");
	}

	public void collect() throws Exception {
		String[] queryFileNames = MIRPath.QueryFileNames;

		String[] indexDirNames = { MIRPath.TREC_CDS_INDEX_DIR, MIRPath.CLEF_EHEALTH_INDEX_DIR, MIRPath.OHSUMED_INDEX_DIR };

		String[] relDataFileNames = MIRPath.RelevanceFileNames;

		String[] docMapFileNames = MIRPath.DocIdMapFileNames;

		String[] queryDocFileNames = { MIRPath.TREC_CDS_CONCEPT_QUERY_DOC_FILE, MIRPath.CLEF_EHEALTH_CONCEPT_QUERY_DOC_FILE,
				MIRPath.OHSUMED_CONCEPT_QUERY_DOC_FILE };

		IndexSearcher[] indexSearchers = SearcherUtils.getIndexSearchers(indexDirNames);

		Analyzer analyzer = MedicalEnglishAnalyzer.newAnalyzer();

		IndexSearcher wikiSearcher = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		for (int i = 0; i < queryFileNames.length; i++) {
			List<BaseQuery> bqs = new ArrayList<BaseQuery>();
			StrCounterMap queryRels = new StrCounterMap();

			// if (!queryFileNames[i].contains("ohsumed")) {
			// continue;
			// }

			String queryFileName = queryFileNames[i];
			String relvFileName = relDataFileNames[i];

			if (i == 0) {
				bqs = QueryReader.readTrecCdsQueries(queryFileName);
				queryRels = RelevanceReader.readTrecCdsRelevances(relvFileName);
			} else if (i == 1) {
				bqs = QueryReader.readClefEHealthQueries(queryFileName);
				queryRels = RelevanceReader.readClefEHealthRelevances(relvFileName);
			} else if (i == 2) {
				bqs = QueryReader.readOhsumedQueries(queryFileName);
				queryRels = RelevanceReader.readOhsumedRelevances(relvFileName);
			}

			IndexSearcher indexSearcher = indexSearchers[i];

			StrBidMap docIdMap = DocumentIdMapper.readDocumentIdMap(docMapFileNames[i]);

			List<SparseVector> docRelData = DocumentIdMapper.mapDocIdsToIndexIds(bqs, queryRels, docIdMap);

			TextFileWriter writer = new TextFileWriter(queryDocFileNames[i]);

			for (int j = 0; j < bqs.size(); j++) {
				BaseQuery bq = bqs.get(j);
				StrCounter c = AnalyzerUtils.getWordCounts(bq.getSearchText(), analyzer);
				SparseVector queryConceptWeights = SearcherUtils.search(AnalyzerUtils.getQuery(c), wikiSearcher, 100);

				writer.write(String.format("Q_%d:\t", j + 1) + getString(queryConceptWeights) + "\n");

				SparseVector docRels = docRelData.get(j);
				docRels.sortByValue();

				for (int k = 0; k < docRels.size(); k++) {
					int docId = docRels.indexAtLoc(k);
					double rel = docRels.valueAtLoc(k);

					Document doc = indexSearcher.getIndexReader().document(docId);

					Terms terms = indexSearcher.getIndexReader().getTermVector(docId, CommonFieldNames.CONTENT);

					if (terms == null) {
						continue;
					}

					TermsEnum termsEnum = terms.iterator();

					BytesRef bytesRef = null;
					PostingsEnum postingsEnum = null;
					StrCounter cc = new StrCounter();

					while ((bytesRef = termsEnum.next()) != null) {
						postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.ALL);

						if (postingsEnum.nextDoc() != 0) {
							throw new AssertionError();
						}

						String word = bytesRef.utf8ToString();

						if (word.equals("null")) {
							continue;
						}
						int freq = postingsEnum.freq();
						cc.incrementCount(word, freq);

						// for (int k = 0; k < freq; k++) {
						// final int position = postingsEnum.nextPosition();
						// }
					}

					cc.keepTopNKeys(50);

					SparseVector docConceptWeights = SearcherUtils.search(AnalyzerUtils.getQuery(cc), wikiSearcher, 100);
					writer.write(rel + "\t" + getString(docConceptWeights) + "\n");
				}
				writer.write("\n");
			}
			writer.close();
		}
	}

	private StrCounter getConcepts(IndexReader reader, SparseVector conceptWeights) {
		StrCounter ret = new StrCounter();

		return ret;
	}

	private String getString(SparseVector sv) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sv.size(); i++) {
			int cid = sv.indexAtLoc(i);
			double score = sv.valueAtLoc(i);
			sb.append(String.format("%text:%f", cid, score));

			if (i != sv.size() - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}
}
