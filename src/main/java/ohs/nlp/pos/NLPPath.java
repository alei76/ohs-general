package ohs.nlp.pos;

public class NLPPath {

	public static final String DATA_DIR = "../../data/knlp/";

	public static final String SEJONG_DIR = DATA_DIR + "sejong/";

	public static final String SEJONG_POS_DATA_FILE = SEJONG_DIR + "현대문어_형태분석_말뭉치.zip";

	public static final String JOSA_DICT_FILE = SEJONG_DIR + "사전/조사_기초.txt";

	public static final String EOMI_DICT_FILE = SEJONG_DIR + "사전/조사_기초.txt";

	public static final String POS_TAG_SET_FILE = DATA_DIR + "pos_set.txt";

	public static final String POS_HMM_MODEL_FILE = DATA_DIR + "pos_hmm.ser.gz";

	public static final String POS_DATA_FILE = DATA_DIR + "pos_data.txt.gz";

	public static final String WORD_POS_CNT_ILE = DATA_DIR + "word_pos_cnt.txt";

	public static final String WORD_POS_TRANS_CNT_ILE = DATA_DIR + "word_pos_trans_cnt.txt";

	public static final String WORD_CNT_ILE = DATA_DIR + "word_cnt.txt";

	public static final String DICT_ANALYZED_FILE = DATA_DIR + "dict_anal.txt.gz";

	public static final String DICT_SUFFIX_FILE = DATA_DIR + "dict_suffixes.txt";

	public static final String DICT_WORD_FILE = DATA_DIR + "dict_words.txt";

	public static final String DICT_SYSTEM_FILE = DATA_DIR + "dict_sys.txt.gz";

	public static final String DICT_SER_FILE = DATA_DIR + "dict_trie.ser.gz";

	public static final String CONNECTION_RULE_FILE = DATA_DIR + "conn_trie.ser.gz";

}
