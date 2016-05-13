package ohs.ml.neuralnet;

import java.util.List;

import ohs.io.FileUtils;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.NLPUtils;
import ohs.math.ArrayMath;
import ohs.ml.neuralnet.Word2VecParam.Type;
import ohs.types.Indexer;
import ohs.types.IntegerArrayList;
import ohs.utils.Generics;

public class Word2Vec {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String text = FileUtils.readText(MIRPath.WIKI_DIR + "wiki_cancer.txt");

		Indexer<String> wordIndexer = Generics.newIndexer();
		List<String> sents = NLPUtils.tokenize(text);

		Word2VecParam param = new Word2VecParam(Type.SKIP_GRAM, 10, 5);

		Word2Vec word2Vec = new Word2Vec(param);
		// word2Vec.train(wordIndexer, data);

		System.out.println("process ends.");
	}

	private Word2VecParam param;

	private Indexer<String> wordIndexer;

	private double[][] inWordVecs;

	private double[][] outWordVecs;

	private double[][] grad;

	private int voc_size;

	private int num_sents;

	public Word2Vec(Word2VecParam param) {
		this.param = param;
	}

	public void train(Indexer<String> wordIndexer, List<IntegerArrayList> sents) {
		this.wordIndexer = wordIndexer;
		this.num_sents = sents.size();
		this.voc_size = wordIndexer.size();

		inWordVecs = new double[voc_size][param.getVectorSize()];
		outWordVecs = new double[voc_size][param.getVectorSize()];

		ArrayMath.random(0, 1, inWordVecs);

		ArrayMath.add(inWordVecs, -0.5, inWordVecs);

	}

}
