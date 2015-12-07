package ohs.ir.medical.clef.ehealth_2014;

public class EHPath {
	public static final String DATA_DIR = "../../data/clef/";

	public static final String TASK_THREE_DIR = DATA_DIR + "task-3/";

	public static final String TASK_TWO_DIR = DATA_DIR + "task-2/";

	public static final String TASK_THREE_DIST_DIR = DATA_DIR + "task-3/dist/";

	public static final String RAW_COLLECTION_DIR = TASK_THREE_DIST_DIR + "collection/unzipped";

	public static final String COLLECTION_FILE = TASK_THREE_DIR + "collection.txt";

	public static final String COLLECTION_SENTENCE_FILE = TASK_THREE_DIR + "collection_sent.txt";

	public static final String ABBREVIATION_FILE = TASK_THREE_DIR + "abbreviation.txt";

	public static final String ABBREVIATION_FILTERED_FILE = TASK_THREE_DIR + "abbreviation_filter.txt";

	public static final String ABBREVIATION_GROUPED_FILE = TASK_THREE_DIR + "abbreviation_group.txt";

	public static final String INDEX_DIR = TASK_THREE_DIR + "index";

	public static final String STOPWORD_FILE = DATA_DIR + "stopword_mallet.txt";

	public static final String QUERY_DIR = TASK_THREE_DIST_DIR + "query/";

	public static final String QUERY_2013_DIR = QUERY_DIR + "2013/";

	public static final String QUERY_2014_DIR = QUERY_DIR + "2014/";

	public static final String QUERY_2014_TRAIN_FILE = QUERY_2014_DIR + "queries.clef2014ehealth.1-5.train.en.xml";

	public static final String QUERY_2014_TRAIN_RELEVANCE_FILE = QUERY_2014_DIR + "clef2014t3.qrels.training.graded.txt";

	public static final String QUERY_2014_TEST_FILE = QUERY_2014_DIR + "queries.clef2014ehealth.1-50.test.en.xml";

	public static final String DISCHARGE_DIR = TASK_TWO_DIR
			+ "dist/2014ShAReCLEFeHealthTasks2_training_10Jan2014/2014ShAReCLEFeHealthTasks2_training_10Jan2014/2014ShAReCLEFeHealthTask2_training_corpus/";

	public static final String QUERY_2013_TRAIN_FILE = QUERY_2013_DIR + "queries.clef2013ehealth.1-5.train.xml";

	public static final String QUERY_2013_TRAIN_RELEVANCE_FILE = QUERY_2013_DIR + "qrels.clef2013ehealth.1-5-train.bin.txt";

	public static final String QUERY_2013_TEST_FILE = QUERY_2013_DIR + "queries.clef2013ehealth.1-50.test.xml";

	public static final String QUERY_2013_TEST_RELEVANCE_FILE = QUERY_2013_DIR + "qrels.clef2013ehealth.1-50-SmithWatermanScorer.graded.final.txt";

	public static final String OUTPUT_DIR = TASK_THREE_DIR + "output/";

	public static final String OUTPUT_BASIC_DIR = OUTPUT_DIR + "basic";

	public static final String OUTPUT_RERANKING_DIR = OUTPUT_DIR + "reranking";

}
