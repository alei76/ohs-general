package ohs.ir.news;

public class NSPath {

	public static final String DATA_DIR = "../../data/news_ir/";

	public static final String NEWS_COL_JSON_FILE = DATA_DIR + "signalmedia-1m.jsonl.gz";
	public static final String NEWS_COL_TEXT_FILE = DATA_DIR + "signalmedia-1m.csv.gz";
	public static final String NEWS_META_FILE = DATA_DIR + "meta.txt";
	public static final String NEWS_COL_NLP_FILE = DATA_DIR + "signalmedia-1m_nlp.csv.gz";
	public static final String NEWS_WORD_COUNT_TEXT_FILE = DATA_DIR + "doc_word_cnts.csv.gz";
	public static final String CONTENT_DIR = NSPath.DATA_DIR + "content";
	public static final String CONTENT_NLP_DIR = NSPath.DATA_DIR + "content_nlp";
	public static final String CONTENT_NLP_CONLL_DIR = NSPath.DATA_DIR + "content_nlp_conll";
	public static final String CONTENT_VISIT_FILE = NSPath.DATA_DIR + "content_visit.txt";
	
	public static final String NEWS_NER_FILE = NSPath.DATA_DIR + "ners.txt";

}
