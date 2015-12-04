package ohs.ir.task.medical;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.math.ArrayMath;
import ohs.math.VectorUtils;
import ohs.matrix.DenseVector;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.utils.StopWatch;

/**
 * 
 * @author Heung-Seon Oh
 * 
 */
public class DocumentPriorEstimator {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String[] indexDirNames = MIRPath.IndexDirNames;

		String[] docPriorFileNames = MIRPath.DocPriorFileNames;

		IndexSearcher[] indexSearchers = new IndexSearcher[indexDirNames.length];

		for (int i = 0; i < indexDirNames.length; i++) {
			indexSearchers[i] = SearcherUtils.getIndexSearcher(indexDirNames[i]);
		}

		DocumentPriorEstimator ds = new DocumentPriorEstimator(indexSearchers, docPriorFileNames);
		ds.estimate();

		System.out.println("process ends.");
	}

	private IndexSearcher[] iss;

	private String[] docPriorFileNames;

	private int num_colls;

	private double mixture_for_all_colls = 0;

	private double dirichlet_prior = 1500;

	public DocumentPriorEstimator(IndexSearcher[] iss, String[] docPriorFileNames) {
		super();
		this.iss = iss;
		this.docPriorFileNames = docPriorFileNames;
		num_colls = iss.length;
	}

	private void estimate() throws Exception {
		double[] cnt_sum_in_each_coll = new double[num_colls];
		double[] num_docs_in_each_coll = new double[num_colls];
		double cnt_sum_in_all_colls = 0;

		for (int i = 0; i < num_colls; i++) {
			Counter<Integer> counter = new Counter<Integer>();
			IndexReader ir = iss[i].getIndexReader();
			cnt_sum_in_each_coll[i] = ir.getSumTotalTermFreq(IndexFieldName.CONTENT);
			num_docs_in_each_coll[i] = ir.maxDoc();
			cnt_sum_in_all_colls += cnt_sum_in_each_coll[i];
		}

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < num_colls; i++) {
			IndexReader ir = iss[i].getIndexReader();
			TextFileWriter writer = new TextFileWriter(docPriorFileNames[i].replace("ser", "txt"));

			DenseVector docPriors = new DenseVector(ir.maxDoc());

			for (int j = 0; j < ir.maxDoc(); j++) {
				if ((j + 1) % 10000 == 0) {
					System.out.printf("\r%dth coll [%d/%d docs, %s]", i + 1, j + 1, (int) num_docs_in_each_coll[i], stopWatch.stop());
				}
				Document doc = ir.document(j);

				Terms termVector = ir.getTermVector(j, IndexFieldName.CONTENT);

				if (termVector == null) {
					continue;
				}

				Indexer<String> wordIndexer = new Indexer<String>();
				SparseVector docWordCounts = null;
				SparseVector[] collWordCountData = new SparseVector[num_colls];

				{
					TermsEnum reuse = null;
					TermsEnum iterator = termVector.iterator();
					BytesRef ref = null;
					DocsAndPositionsEnum docsAndPositions = null;
					Counter<Integer> counter = new Counter<Integer>();

					while ((ref = iterator.next()) != null) {
						docsAndPositions = iterator.docsAndPositions(null, docsAndPositions);
						if (docsAndPositions.nextDoc() != 0) {
							throw new AssertionError();
						}

						String word = ref.utf8ToString();
						int w = wordIndexer.getIndex(word);
						int freq = docsAndPositions.freq();

						counter.incrementCount(w, freq);
					}
					docWordCounts = VectorUtils.toSparseVector(counter);
				}

				for (int k = 0; k < num_colls; k++) {
					Counter<Integer> counter = new Counter<Integer>();

					for (int w = 0; w < wordIndexer.size(); w++) {
						String word = wordIndexer.getObject(w);
						Term termInstance = new Term(IndexFieldName.CONTENT, word);
						double count = iss[k].getIndexReader().totalTermFreq(termInstance);
						counter.setCount(w, count);
					}
					collWordCountData[k] = VectorUtils.toSparseVector(counter);
				}

				double sum_log_probs = 0;

				for (int k = 0; k < docWordCounts.size(); k++) {
					int w = docWordCounts.indexAtLoc(k);
					double cnt_w_in_doc = docWordCounts.valueAtLoc(k);
					String word = wordIndexer.getObject(w);

					double[] cnt_w_in_each_coll = new double[num_colls];
					double cnt_w_in_all_colls = 0;

					for (int u = 0; u < num_colls; u++) {
						cnt_w_in_each_coll[u] = collWordCountData[u].valueAlways(w);
						cnt_w_in_all_colls += cnt_w_in_each_coll[u];
					}

					double cnt_w_in_coll = cnt_w_in_each_coll[i];
					double cnt_sum_in_coll = cnt_sum_in_each_coll[i];

					double prob_w_in_coll = cnt_w_in_coll / cnt_sum_in_coll;
					double prob_w_in_all_colls = cnt_w_in_all_colls / cnt_sum_in_all_colls;

					double cnt_sum_in_doc = docWordCounts.sum();
					double prob_w_in_doc = (cnt_w_in_doc + dirichlet_prior * prob_w_in_coll) / (cnt_sum_in_doc + dirichlet_prior);
					prob_w_in_doc = (1 - mixture_for_all_colls) * prob_w_in_doc + mixture_for_all_colls * prob_w_in_all_colls;

					if (prob_w_in_doc == 0) {
						System.out.println();
					}

					double log_prob_w = Math.log(prob_w_in_doc);
					sum_log_probs += log_prob_w;
				}

				docPriors.set(j, sum_log_probs);
				writer.write(j + "\t" + sum_log_probs + "\n");

				// System.out.println(VectorUtils.toCounter(docWordCounts,
				// wordIndexer));
				// System.out.println(VectorUtils.toCounter(docWordProbs,
				// wordIndexer));
				// System.out.println();

				// writer.write(j + "\t" + doc_prior + "\n");
			}

			System.out.printf("\r%dth coll [%d/%d docs, %s]\n\n", i + 1, (int) num_docs_in_each_coll[i], (int) num_docs_in_each_coll[i],
					stopWatch.stop());
			writer.close();

			double[] log_probs = docPriors.values();
			double log_prob_sum = ArrayMath.sumLogProb(log_probs);

			ArrayMath.add(log_probs, -log_prob_sum, log_probs);
			ArrayMath.exp(log_probs, log_probs);

			docPriors.normalizeAfterSummation();

			docPriors.write(docPriorFileNames[i]);

		}
	}
}
