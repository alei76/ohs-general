package ohs.entity;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.FileUtils;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.CommonFieldNames;
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
import ohs.utils.StopWatch;

public class EntityContextGenerator {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String inputFileName = ENTPath.ENTITY_LINKER_FILE.replace(".ser", "_loc.ser");
		String outputFileName = ENTPath.ENTITY_CONTEXT_FILE;

		if (inputFileName.contains("_org")) {
			outputFileName = outputFileName.replace(".ser", "_org.ser");
		} else if (inputFileName.contains("_loc")) {
			outputFileName = outputFileName.replace(".ser", "_loc.ser");
		} else if (inputFileName.contains("_per")) {
			outputFileName = outputFileName.replace(".ser", "_per.ser");
		} else if (inputFileName.contains("_title")) {
			outputFileName = outputFileName.replace(".ser", "_title.ser");
		}

		EntityLinker el = new EntityLinker();
		el.read(inputFileName);

		Analyzer analyzer = MedicalEnglishAnalyzer.newAnalyzer();

		IndexSearcher is = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		EntityContextGenerator ecg = new EntityContextGenerator(el, analyzer, is, outputFileName);
		ecg.generate();

		System.out.println("process ends.");
	}

	private EntityLinker el;

	private Analyzer analyzer;

	private IndexSearcher is;

	private String outputFileName;

	public EntityContextGenerator(EntityLinker el, Analyzer analyzer, IndexSearcher is, String outputFileName) {
		this.el = el;
		this.analyzer = analyzer;
		this.is = is;
		this.outputFileName = outputFileName;
	}

	public void generate() throws Exception {

		List<Entity> ents = new ArrayList<Entity>(el.getEntityMap().values());

		Indexer<String> wordIndexer = Generics.newIndexer();
		Map<Integer, SparseVector> contVecs = Generics.newHashMap();

		int num_chunks = ents.size() / 100;
		StopWatch stopWatch = StopWatch.newStopWatch();
		stopWatch.start();

		Counter<String> docFreqs = WordCountBox.getDocFreqs(is.getIndexReader(), CommonFieldNames.CONTENT);

		for (int i = 0; i < ents.size(); i++) {

			if ((i + 1) % num_chunks == 0) {
				int progess = (int) ((i + 1f) / ents.size() * 100);
				System.out.printf("\r[%d percent, %s]", progess, stopWatch.stop());
			}

			Entity ent = ents.get(i);
			Document doc = is.doc(ent.getId());
			String content = doc.get(CommonFieldNames.CONTENT);
			String catStr = doc.get(CommonFieldNames.CATEGORY);

			String[] lines = content.split("\n\n");

			// if (lines.length < 2) {
			// continue;
			// }

			List<String> sents = NLPUtils.tokenize(lines[0]);

			if (sents.size() == 0) {
				continue;
			}

			Counter<String> wcs = WordCountBox.getWordCounts(is.getIndexReader(), ent.getId(), CommonFieldNames.CONTENT);

			Counter<String> wcs1 = Generics.newCounter();
			wcs1.incrementAll(AnalyzerUtils.getWordCounts(sents.get(0), analyzer));
			wcs1.incrementAll(AnalyzerUtils.getWordCounts(catStr, analyzer));

			double doc_len = wcs.totalCount();
			double idf_sum = 0;

			for (String word : wcs1.keySet()) {
				double cnt = wcs.getCount(word);
				double tf = Math.log(cnt) + 1;
				double df = docFreqs.getCount(word);
				double idf = Math.log((is.getIndexReader().maxDoc() + 1) / df);
				double tfidf = tf * idf;
				wcs1.setCount(word, tfidf);
				idf_sum += idf;
			}

			double avg_idf = idf_sum / wcs.size();

			SparseVector sv = VectorUtils.toSparseVector(wcs1, wordIndexer, true);
			sv.setLabel(ent.getId());

			contVecs.put(ent.getId(), sv);
		}
		System.out.printf("\r[%d percent, %s]\n", 100, stopWatch.stop());

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(outputFileName);

		EntityContexts entContexts = new EntityContexts(wordIndexer, contVecs);
		entContexts.write(oos);
		oos.close();
	}

}
