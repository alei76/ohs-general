package ohs.ir.medical.trec.cds_2014;

import java.io.File;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.TextFileReader;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.SearcherUtils;
import ohs.math.VectorUtils;
import ohs.matrix.SparseMatrix;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

public class BuildAbbreviationModel {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		BuildAbbreviationModel b = new BuildAbbreviationModel();
		b.build();
		System.out.println("process ends.");
	}

	public void build() throws Exception {

		IndexSearcher indexSearcher = SearcherUtils.getIndexSearcher(CDSPath.INDEX_DIR);

		File vocDir = new File(CDSPath.VOCABULARY_DIR);

		if (!vocDir.exists()) {
			VocabularyData.make(indexSearcher.getIndexReader());
		}

		VocabularyData vocData = VocabularyData.read(vocDir);
		Indexer<String> wordIndexer = vocData.getWordIndexer();

		MedicalEnglishAnalyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		CounterMap<Integer, Integer> counterMap = new CounterMap<Integer, Integer>();

		TextFileReader reader = new TextFileReader(new File(CDSPath.ABBREVIATION_FILTERED_FILE));
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String shortForm = lines.get(0).toLowerCase();
			int sf = wordIndexer.indexOf(shortForm);

			if (sf < 0) {
				continue;
			}

			for (int i = 1; i < lines.size(); i++) {
				String[] parts = lines.get(i).split("\t");
				String longForm = parts[0];

				if (longForm.toLowerCase().contains(shortForm.toLowerCase())) {
					continue;
				}

				double count = Double.parseDouble(parts[1]);

				TokenStream ts = analyzer.tokenStream(IndexFieldName.CONTENT, longForm);
				CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
				ts.reset();

				Counter<Integer> counter = new Counter<Integer>();

				while (ts.incrementToken()) {
					String word = attr.toString();
					int w = wordIndexer.indexOf(word);
					if (w < 0) {
						continue;
					}
					counter.incrementCount(w, count);
				}
				ts.end();
				ts.close();

				if (counter.size() > 1) {
					counterMap.setCounter(sf, counter);
				}
			}
		}
		reader.close();

		SparseMatrix ret = VectorUtils.toSpasreMatrix(counterMap);
		ret.setRowDim(wordIndexer.size());
		ret.setRowDim(wordIndexer.size());
		ret.normalizeRows();

		ret.write(CDSPath.ABBREVIATION_MODEL_FILE);
	}

}
