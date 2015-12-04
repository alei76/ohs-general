package com.medallia.word2vec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import com.medallia.word2vec.Searcher.Match;
import com.medallia.word2vec.Searcher.UnknownWordException;
import com.medallia.word2vec.Word2VecTrainerBuilder.TrainingProgressListener;
import com.medallia.word2vec.neuralnetwork.NeuralNetworkType;
import com.medallia.word2vec.util.AutoLog;
import com.medallia.word2vec.util.Format;
import com.medallia.word2vec.util.Strings;

import ohs.io.TextFileReader;
import ohs.math.ArrayUtils;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.Vocab;

/** Example usages of {@link Word2VecModel} */
public class Word2VecExamples {
	private static final Log LOG = AutoLog.getLog();

	public static Properties getDefaultProp() throws IOException {
		Properties prop = new Properties();
		prop.setProperty("input_file", "../../data/medical_ir/ohsumed/sents_stem.txt.gz");
		prop.setProperty("output_file", "../../data/medical_ir/ohsumed/word2vec_model_stem.ser.gz");
		prop.setProperty("network_type", "cbow");
		prop.setProperty("threads", "1");
		prop.setProperty("min_freq", "5");
		prop.setProperty("use_hierarchical_sofmax", "false");
		prop.setProperty("window_size", "8");
		prop.setProperty("layer_size", "200");
		prop.setProperty("negative_samples", "25");
		prop.setProperty("iterations", "5");
		prop.setProperty("down_sample_rate", "1e-4");
		prop.setProperty("num_train_sents", "10000");
		prop.setProperty("train_mode", "true");
		return prop;
	}

	private static void interact(Searcher searcher) throws IOException, UnknownWordException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			while (true) {
				System.out.print("Enter word or sentence (EXIT to break): ");
				String word = br.readLine();
				if (word.equals("EXIT")) {
					break;
				}
				List<Match> matches = searcher.getMatches(word, 20);
				System.out.println(Strings.joinObjects("\n", matches));
			}
		}
	}

	/**
	 * Runs the example
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		boolean isTrainMode = true;
		Properties prop = getDefaultProp();

		File propFile = new File("word2vec.prop");

		if (propFile.exists()) {
			FileInputStream fis = new FileInputStream(propFile);
			prop.load(fis);
			fis.close();
		} else {
			FileOutputStream fos = new FileOutputStream(propFile);
			prop.store(fos, "word2vec prop");
			fos.close();
		}

		if (Boolean.parseBoolean(prop.getProperty("train_mode"))) {
			Word2VecExamples e = new Word2VecExamples(prop);
			e.process();
		} else {
			Word2VecModel model = Word2VecModel.fromSerFile(prop.getProperty("output_file"));

			interact(model.forSearch());
		}

		System.out.println("process ends.");
	}

	/** Example using Skip-Gram model */
	// public static void skipGram() throws IOException, TException, InterruptedException, UnknownWordException {
	// List<Integer[]> sents = new ArrayList<Integer[]>();
	// Vocab vocab = new Vocab();
	//
	// readSentences(MIRPath.OHSUMED_SENTS_FILE, vocab, sents);
	//
	// Word2VecModel model = Word2VecModel.trainer().setMinVocabFrequency(100).useNumThreads(20).setWindowSize(7)
	// .type(NeuralNetworkType.SKIP_GRAM).useHierarchicalSoftmax().setLayerSize(300).useNegativeSamples(0)
	// .setDownSamplingRate(1e-3).setNumIterations(5).setListener(new TrainingProgressListener() {
	// @Override
	// public void update(Stage stage, double progress) {
	// System.out.println(String.format("%s is %.2f%% complete", Format.formatEnum(stage), progress * 100));
	// }
	// }).train(vocab, sents);
	//
	// try (ProfilingTimer timer = ProfilingTimer.create(LOG, "Writing output to file")) {
	// FileUtils.writeStringToFile(new File("300layer.20threads.5iter.model"), ThriftUtils.serializeJson(model.toThrift()));
	// }
	//
	// interact(model.forSearch());
	// }

	private Properties prop;

	public Word2VecExamples(Properties prop) {
		this.prop = prop;
	}

	/**
	 * Trains a model and allows user to find similar words demo-word.sh example from the open source C implementation
	 */
	public void process() throws Exception {
		// File f = new File("text8");
		// if (!f.exists())
		// throw new IllegalStateException("Please download and unzip the text8 example from http://mattmahoney.net/dc/text8.zip");

		String inFileName = prop.getProperty("input_file");
		String outFileName = prop.getProperty("output_file");
		int min_freq = Integer.parseInt(prop.getProperty("min_freq"));
		int num_threads = Integer.parseInt(prop.getProperty("threads"));
		int window_size = Integer.parseInt(prop.getProperty("window_size"));
		int layer_size = Integer.parseInt(prop.getProperty("layer_size"));
		int negative_samples = Integer.parseInt(prop.getProperty("negative_samples"));
		int iterations = Integer.parseInt(prop.getProperty("iterations"));
		double down_sample_rate = Double.parseDouble(prop.getProperty("down_sample_rate"));
		boolean use_hierarchical_softmax = Boolean.parseBoolean(prop.getProperty("use_hierarchical_sofmax"));
		NeuralNetworkType type = NeuralNetworkType.CBOW;

		if (prop.getProperty("network_type").equals("skip_gram")) {
			type = NeuralNetworkType.SKIP_GRAM;
			use_hierarchical_softmax = true;
		}

		int num_train_sents = Integer.MAX_VALUE;

		if (!prop.getProperty("num_train_sents").equals("all")) {
			num_train_sents = Integer.parseInt(prop.getProperty("num_train_sents"));
		}

		int[][] sents = null;
		Vocab vocab = null;

		{
			int num_valid_sents = 0;

			{
				System.out.printf("read [%s]\n", inFileName);
				Counter<String> wordCounts = new Counter<String>();
				TextFileReader reader = new TextFileReader(inFileName);
				reader.setPrintNexts(false);

				while (reader.hasNext()) {
					reader.print(500000);

					String[] parts = reader.next().split("\t");

					if (parts.length != 3) {
						continue;
					}

					if (num_train_sents == num_valid_sents) {
						break;
					}

					num_valid_sents++;

					String[] words = parts[2].split("[\\s]+");

					for (String word : words) {
						wordCounts.incrementCount(word, 1);
					}
				}
				reader.printLast();
				reader.close();

				wordCounts.pruneKeysBelowThreshold(min_freq);

				Indexer<String> wordIndexer = new Indexer<String>();
				List<String> words = wordCounts.getSortedKeys();
				double[] cnts = new double[words.size()];

				for (int i = 0; i < words.size(); i++) {
					String word = words.get(i);
					int cnt = (int) wordCounts.getCount(word);

					wordIndexer.add(word);
					cnts[i] = cnt;
				}

				vocab = new Vocab(wordIndexer, cnts);
			}

			{

				sents = new int[num_valid_sents][];

				TextFileReader reader = new TextFileReader(inFileName);
				reader.setPrintNexts(false);

				int loc = 0;

				while (reader.hasNext()) {
					reader.print(500000);

					String[] parts = reader.next().split("\t");

					if (parts.length != 3) {
						continue;
					}

					if (loc == num_train_sents) {
						break;
					}

					String[] words = parts[2].split("[\\s]+");
					List<Integer> ws = new ArrayList<Integer>();

					for (int i = 0; i < words.length; i++) {
						String word = words[i];
						int w = vocab.getWordIndexer().indexOf(word);
						if (w < 0) {
							continue;
						}
						ws.add(w);
					}

					int[] ws2 = new int[ws.size()];
					ArrayUtils.copy(ws, ws2);
					sents[loc++] = ws2;
				}
				reader.printLast();
				reader.close();
			}
		}

		Word2VecTrainerBuilder builder = Word2VecModel.trainer();

		builder.setMinVocabFrequency(min_freq).useNumThreads(num_threads).

		setWindowSize(window_size).type(type).setLayerSize(layer_size).

		useNegativeSamples(negative_samples).

		setDownSamplingRate(down_sample_rate).setNumIterations(iterations).

		setListener(new TrainingProgressListener() {

			@Override
			public void update(Stage stage, double progress) {
				System.out.println(String.format("%s is %.2f%% complete", Format.formatEnum(stage), progress * 100));
			}
		});

		if (use_hierarchical_softmax) {
			builder.useHierarchicalSoftmax();
		}

		Word2VecModel model = builder.train(vocab, sents);

		// Writes model to a thrift file
		// try (ProfilingTimer timer = ProfilingTimer.create(LOG, "Writing output to file")) {
		// FileUtils.writeStringToFile(new File("text8.model"), ThriftUtils.serializeJson(model.toThrift()));
		// }

		// Alternatively, you can write the model to a bin file that's compatible with the C
		// implementation.
		// try (final OutputStream os = Files.newOutputStream(Paths.get("text8.bin"))) {
		// model.toBinFile(os);
		// }

		model.toSerFile(outFileName);

		// interact(model.forSearch());
	}
}
