package ohs.entity;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.FileUtils;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.NLPUtils;
import ohs.ir.medical.general.SearcherUtils;
import ohs.ir.medical.general.WordCountBox;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.utils.Generics;

public class EntityContextGenerator {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String inputFileName = ENTPath.ENTITY_LINKER_FILE.replace(".ser", "_org.ser");
		String outputFileName = ENTPath.WIKI_DIR + "context.ser.gz";

		if (inputFileName.contains("_org")) {
			outputFileName = outputFileName.replace(".ser", "_org.ser");
		} else if (inputFileName.contains("_loc")) {
			outputFileName = outputFileName.replace(".ser", "_loc.ser");
		} else if (inputFileName.contains("_per")) {
			outputFileName = outputFileName.replace(".ser", "_per.ser");
		}

		EntityLinker el = new EntityLinker();
		el.read(inputFileName);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		IndexSearcher is = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		EntityContextGenerator ecg = new EntityContextGenerator(el, analyzer, is, outputFileName);
		ecg.generate();

		System.out.println("process ends.");
	}

	private EntityLinker el;

	private Analyzer analyzer;

	private IndexSearcher is;

	private String outputFileName;

	public class EntityContext {

	}

	public EntityContextGenerator(EntityLinker el, Analyzer analyzer, IndexSearcher is, String outputFileName) {
		this.el = el;
		this.analyzer = analyzer;
		this.is = is;
		this.outputFileName = outputFileName;
	}

	public void generate() throws Exception {

		List<Entity> ents = new ArrayList<Entity>(el.getEntityMap().values());

		Indexer<String> wordIndexer = Generics.newIndexer();
		List<SparseVector> svs = Generics.newArrayList();

		for (int i = 0; i < ents.size(); i++) {
			if ((i + 1) % 100 == 0) {
				System.out.printf("\r[%d/%d]", i + 1, ents.size());

			}
			Entity ent = ents.get(i);
			Document doc = is.doc(ent.getId());
			String content = doc.get(IndexFieldName.CONTENT);
			String catStr = doc.get(IndexFieldName.CATEGORY);

			String[] lines = content.split("\n\n");

			// if (lines.length < 2) {
			// continue;
			// }

			List<String> sents = NLPUtils.tokenize(lines[0]);

			if (sents.size() == 0) {
				continue;
			}

			// Counter<String> wcs = WordCountBox.getWordCounts(is.getIndexReader(), ent.getId(), IndexFieldName.CONTENT);
			Counter<String> wcs = AnalyzerUtils.getWordCounts(sents.get(0), analyzer);
			Counter<String> dfs = WordCountBox.getDocFreqs(is.getIndexReader(), IndexFieldName.CONTENT, wcs.keySet());

			for (String word : wcs.keySet()) {
				double cnt = wcs.getCount(word);
				double tf = Math.log(cnt) + 1;
				double df = dfs.getCount(word);
				double idf = Math.log((is.getIndexReader().maxDoc() + 1) / df);
				double tfidf = tf * idf;
				wcs.setCount(word, tfidf);
			}
			SparseVector sv = VectorUtils.toSparseVector(wcs, wordIndexer, true);
			sv.setLabel(ent.getId());

			svs.add(sv);
		}
		System.out.printf("\r[%d/%d]\n", ents.size(), ents.size());

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(outputFileName);
		FileUtils.write(oos, wordIndexer);
		SparseVector.write(oos, svs);
		oos.close();
	}

}
