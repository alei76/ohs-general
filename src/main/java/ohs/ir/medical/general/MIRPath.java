package ohs.ir.medical.general;

public class MIRPath {

	public static final String DATA_DIR = "../../data/medical_ir/";

	// public static final String DATA_DIR = "D:/data/medical_ir/";

	public static final String STOPWORD_MALLET_FILE = DATA_DIR + "stopword_mallet.txt";

	public static final String STOPWORD_INQUERY_FILE = DATA_DIR + "stopword_inquery.txt";

	public static final String STOPWORD_MEDICAL_FILE = DATA_DIR + "stopword_medical.txt";

	public static final String EEM_LOG_FILE = DATA_DIR + "eem_log.txt";

	public static final String NEW_EEM_LOG_FILE = DATA_DIR + "new_eem_log.txt";

	public static final String PERFORMANCE_FILE = DATA_DIR + "performance.txt";

	public static final String PERFORMANCE_SUMMARY_FILE = DATA_DIR + "performance_summary.txt";

	public static final String PERFORMANCE_DETAIL_FILE = DATA_DIR + "performance_detail.txt";

	public static final String VOCAB_FILE = DATA_DIR + "vocab.txt.gz";

	/*
	 * Wikipedia
	 */

	public static final String WIKI_DIR = DATA_DIR + "wiki/";

	public static final String WIKI_COL_FILE = WIKI_DIR + "enwiki-20150304-pages-articles.txt.bz2";

	public static final String WIKI_XML_DUMP_FILE = WIKI_DIR + "enwiki-20150304-pages-articles.xml.bz2";

	public static final String WIKI_PAGE_FILE = WIKI_DIR + "wiki_page.csv.gz";

	public static final String WIKI_CATEGORY_FILE = WIKI_DIR + "wiki_category.csv.gz";

	public static final String WIKI_CATEGORYLINK_FILE = WIKI_DIR + "wiki_categorylink.csv.gz";

	public static final String WIKI_VOCAB_FILE = WIKI_DIR + "vocab.txt.gz";

	public static final String WIKI_INDEX_DIR = WIKI_DIR + "index/";

	public static final String WIKI_INDEX_SENT_DIR = WIKI_DIR + "index_sent/";

	public static final String WIKI_CATEGORY_INDEX_DIR = WIKI_DIR + "cat_index/";

	public static final String WIKI_REDIRECT_TITLE_FILE = WIKI_DIR + "redirects.txt";

	public static final String WIKI_TITLE_FILE = WIKI_DIR + "titles.txt";

	public static final String WIKI_DOC_ID_MAP_FILE = WIKI_DIR + "document_id_map.txt.gz";

	public static final String WIKI_CATEGORY_MAP_FILE = WIKI_DIR + "category_map.txt";

	public static final String WIKI_CATEGORY_COUNT_FILE = WIKI_DIR + "category_count.txt";

	public static final String WIKI_DOC_PRIOR_FILE = WIKI_DIR + "document_prior.ser";

	public static final String ICD10_DIR = WIKI_DIR + "icd-10/";

	public static final String ICD10_HTML_FILE = ICD10_DIR + "ICD-10 - Wikipedia, the free encyclopedia.html";

	public static final String ICD10_TOP_LEVEL_CHAPTER_FILE = ICD10_DIR + "icd-10-top-level.txt";

	public static final String ICD10_HIERARCHY_FILE = ICD10_DIR + "icd-10-hierarchy.txt";

	public static final String ICD10_HIERARCHY_REFINED_FILE = ICD10_DIR + "icd-10-hierarchy_refined.txt";

	public static final String ICD10_HIERARCHY_PAGE_FILE = ICD10_DIR + "icd-10-hierarchy_page.txt";

	public static final String ICD10_ESA_FILE = ICD10_DIR + "icd-10_esa.txt";

	public static final String ICD10_LOG_FILE = ICD10_DIR + "icd-10_log.txt";

	public static final String ICD10_ESA_DIR = ICD10_DIR + "esa/";

	public static final String WIKI_PROXIMITY_DIR = WIKI_DIR + "proximity/";

	/*
	 * CLEF eHealth
	 */

	public static final String CLEF_EHEALTH_DIR = DATA_DIR + "clef_ehealth/";

	public static final String CLEF_EHEALTH_COL_DIR = CLEF_EHEALTH_DIR + "collection";

	public static final String CLEF_EHEALTH_COL_FILE = CLEF_EHEALTH_DIR + "collection.txt.gz";

	public static final String CLEF_EHEALTH_SENTS_FILE = CLEF_EHEALTH_DIR + "sents.txt.gz";

	public static final String CLEF_EHEALTH_SENTS_STEM_FILE = CLEF_EHEALTH_DIR + "sents_stem.txt.gz";

	public static final String CLEF_EHEALTH_COL_FILE_2 = CLEF_EHEALTH_DIR + "collection_2.txt";

	public static final String CLEF_EHEALTH_VALID_DOC_NO_FILE = CLEF_EHEALTH_DIR + "cds-docnos.txt";

	public static final String CLEF_EHEALTH_QUERY_DIR = CLEF_EHEALTH_DIR + "query/";

	public static final String CLEF_EHEALTH_INDEX_DIR = CLEF_EHEALTH_DIR + "index/";

	public static final String CLEF_EHEALTH_INDEX_SENT_DIR = CLEF_EHEALTH_DIR + "index_sent/";

	public static final String CLEF_EHEALTH_QUERY_2013_FILE = CLEF_EHEALTH_QUERY_DIR + "2013/queries.clef2013ehealth.1-50.test.xml";

	public static final String CLEF_EHEALTH_QUERY_2014_FILE = CLEF_EHEALTH_QUERY_DIR + "2014/queries.clef2014ehealth.1-50.test.en.xml";

	public static final String CLEF_EHEALTH_RELEVANCE_JUDGE_2014_FILE = CLEF_EHEALTH_QUERY_DIR + "2014/clef2014t3.qrels.test.graded.txt";

	public static final String CLEF_EHEALTH_QUERY_2015_FILE = CLEF_EHEALTH_QUERY_DIR + "2015/clef2015.test.queries-EN.txt";

	public static final String CLEF_EHEALTH_RELEVANCE_JUDGE_2015_FILE = CLEF_EHEALTH_QUERY_DIR + "2015/qrels.clef2015.test.graded.txt";

	public static final String CLEF_EHEALTH_QUERY_2016_FILE = CLEF_EHEALTH_QUERY_DIR + "2016/queries2016.xml";

	public static final String CLEF_EHEALTH_VOCAB_FILE = CLEF_EHEALTH_DIR + "vocab.txt.gz";

	public static final String CLEF_EHEALTH_DOC_ID_MAP_FIE = CLEF_EHEALTH_DIR + "document_id_map.txt.gz";

	public static final String CLEF_EHEALTH_OUTPUT_DIR = CLEF_EHEALTH_DIR + "output/";

	public static final String CLEF_EHEALTH_OUTPUT_RESULT_DIR = CLEF_EHEALTH_OUTPUT_DIR + "result/";

	public static final String CLEF_EHEALTH_OUTPUT_LOG_DIR = CLEF_EHEALTH_OUTPUT_DIR + "log/";

	public static final String CLEF_EHEALTH_DOC_PRIOR_FILE = CLEF_EHEALTH_DIR + "document_prior.ser";

	public static final String CLEF_EHEALTH_ABBR_DIR = CLEF_EHEALTH_DIR + "abbr/";

	public static final String CLEF_EHEALTH_ABBR_FILE = CLEF_EHEALTH_ABBR_DIR + "abbrs.txt";

	public static final String CLEF_EHEALTH_OUTPUT_RESULT_2015_DIR = CLEF_EHEALTH_OUTPUT_DIR + "result-clef2015/";

	public static final String CLEF_EHEALTH_OUTPUT_RESULT_2016_DIR = CLEF_EHEALTH_OUTPUT_DIR + "result-clef2016/";

	public static final String CLEF_EHEALTH_OUTPUT_RESULT_2015_QUERY_DOC_DIR = CLEF_EHEALTH_OUTPUT_RESULT_2015_DIR + "query-doc/";

	public static final String CLEF_EHEALTH_OUTPUT_RESULT_2015_INIT_DIR = CLEF_EHEALTH_OUTPUT_RESULT_2015_DIR + "init/";

	public static final String CLEF_EHEALTH_OUTPUT_RESULT_2015_RERANK_DIR = CLEF_EHEALTH_OUTPUT_RESULT_2015_DIR + "rerank/";

	public static final String CLEF_EHEALTH_OUTPUT_RESULT_2015_QUERY_MODEL_DIR = CLEF_EHEALTH_OUTPUT_RESULT_2015_DIR + "query_model/";

	public static final String CLEF_EHEALTH_OUTPUT_RESULT_2015_LOG_DIR = CLEF_EHEALTH_OUTPUT_RESULT_2015_DIR + "log/";

	public static final String CLEF_EHEALTH_QUERY_DOC_FILE = CLEF_EHEALTH_DIR + "query_doc.txt";

	public static final String CLEF_EHEALTH_CONCEPT_QUERY_DOC_FILE = CLEF_EHEALTH_DIR + "concept_query_doc.txt";

	public static final String CLEF_EHEALTH_PROXIMITY_DIR = CLEF_EHEALTH_DIR + "proximity/";

	/*
	 * clueweb 2012
	 */

	public static final String CLUEWEB_DIR = DATA_DIR + "clueweb/";

	public static final String CLUEWEB_COL_DIR = CLUEWEB_DIR + "2012_disk_b/";

	public static final String CLUEWEB_TEXT_DIR = CLUEWEB_DIR + "2012_disk_b_text/";

	public static final String CLUEWEB_INDEX_DIR = CLUEWEB_DIR + "index";

	public static final String CLUEWEB_FILE_NAME_FILE = CLUEWEB_DIR + "clueweb12_disk_b_file_names.txt";

	public static final String CLUEWEB_DOC_ID_MAP_FIE = CLUEWEB_DIR + "document_id_map.txt.gz";

	public static final String CLUEWEB_VOCAB_FILE = CLUEWEB_DIR + "vocab.txt.gz";

	public static final String CLUEWEB_OUTPUT_DIR = CLUEWEB_DIR + "output/";

	public static final String CLUEWEB_OUTPUT_RESULT_DIR = CLUEWEB_OUTPUT_DIR + "result/";

	/*
	 * TREC CDS
	 */

	public static final String TREC_CDS_DIR = DATA_DIR + "trec_cds/";

	public static final String TREC_CDS_COL_DIR = TREC_CDS_DIR + "collection/";

	public static final String TREC_CDS_COL_FILE = TREC_CDS_DIR + "collection.txt.gz";

	public static final String TREC_CDS_COL_XML_FILE = TREC_CDS_DIR + "collection_xml.txt.gz";

	public static final String TREC_CDS_SENTS_FILE = TREC_CDS_DIR + "sents.txt.gz";

	public static final String TREC_CDS_SENTS_STEM_FILE = TREC_CDS_DIR + "sents_stem.txt.gz";

	public static final String TREC_CDS_COL_LOG_FILE = TREC_CDS_DIR + "collection_log.txt";

	public static final String TREC_CDS_QUERY_DIR = TREC_CDS_DIR + "query/";

	public static final String TREC_CDS_INDEX_DIR = TREC_CDS_DIR + "index/";

	public static final String TREC_CDS_INDEX_SENT_DIR = TREC_CDS_DIR + "index_sent/";

	public static final String TREC_CDS_DUPLICATION_FILE_1 = TREC_CDS_DIR + "collection/duplicates-1.txt";

	public static final String TREC_CDS_DUPLICATION_FILE_2 = TREC_CDS_DIR + "collection/duplicates-2.txt";

	public static final String TREC_CDS_VALID_DOC_ID_FILE = TREC_CDS_DIR + "cds-docnos.txt";

	public static final String TREC_CDS_QUERY_2014_DIR = TREC_CDS_QUERY_DIR + "2014/";

	public static final String TREC_CDS_QUERY_2015_DIR = TREC_CDS_QUERY_DIR + "2015/";

	public static final String TREC_CDS_QUERY_2014_FILE = TREC_CDS_QUERY_2014_DIR + "topics2014.xml";

	public static final String TREC_CDS_QUERY_2015_A_FILE = TREC_CDS_QUERY_2015_DIR + "topics2015A.xml";

	public static final String TREC_CDS_QUERY_2015_B_FILE = TREC_CDS_QUERY_2015_DIR + "topics2015B.xml";

	public static final String TREC_CDS_RELEVANCE_JUDGE_2014_FILE = TREC_CDS_QUERY_2014_DIR + "qrels2014.txt";

	public static final String TREC_CDS_VOCAB_FILE = TREC_CDS_DIR + "vocab.txt.gz";

	public static final String TREC_CDS_DOC_ID_MAP_FILE = TREC_CDS_DIR + "document_id_map.txt.gz";

	public static final String TREC_CDS_OUTPUT_DIR = TREC_CDS_DIR + "output/";

	public static final String TREC_CDS_OUTPUT_RESULT_DIR = TREC_CDS_OUTPUT_DIR + "result/";

	public static final String TREC_CDS_OUTPUT_RESULT_2_DIR = TREC_CDS_OUTPUT_DIR + "result-2/";

	public static final String TREC_CDS_OUTPUT_LOG_DIR = TREC_CDS_OUTPUT_DIR + "log/";

	public static final String TREC_CDS_DOC_PRIOR_FILE = TREC_CDS_DIR + "document_prior.ser";

	public static final String TREC_CDS_ABBR_DIR = TREC_CDS_DIR + "abbr/";

	public static final String TREC_CDS_ABBR_FILE = TREC_CDS_ABBR_DIR + "abbrs.txt";

	public static final String TREC_CDS_QUERY_DOC_FILE = TREC_CDS_DIR + "query_doc.txt";

	public static final String TREC_CDS_CONCEPT_QUERY_DOC_FILE = TREC_CDS_DIR + "concept_query_doc.txt";

	public static final String TREC_CDS_OUTPUT_RESULT_2015_DIR = TREC_CDS_OUTPUT_DIR + "result-2015/";

	public static final String TREC_CDS_OUTPUT_RESULT_2015_PERFORMANCE_FILE = TREC_CDS_OUTPUT_DIR + "performance_2015.txt";

	public static final String TREC_CDS_OUTPUT_RESULT_2015_PERFORMANCE_COMPACT_FILE = TREC_CDS_OUTPUT_DIR + "performance_2015_compact.txt";

	public static final String TREC_CDS_OUTPUT_RESULT_2015_QUERY_DOC_DIR = TREC_CDS_OUTPUT_RESULT_2015_DIR + "query-doc/";

	public static final String TREC_CDS_OUTPUT_RESULT_2015_INIT_DIR = TREC_CDS_OUTPUT_RESULT_2015_DIR + "init/";

	public static final String TREC_CDS_OUTPUT_RESULT_2015_RERANK_DIR = TREC_CDS_OUTPUT_RESULT_2015_DIR + "rerank/";

	public static final String TREC_CDS_OUTPUT_RESULT_2015_QUERY_MODEL_DIR = TREC_CDS_OUTPUT_RESULT_2015_DIR + "query_model/";

	public static final String TREC_CDS_OUTPUT_RESULT_2015_LOG_DIR = TREC_CDS_OUTPUT_RESULT_2015_DIR + "log/";

	public static final String TREC_CDS_PROXIMITY_DIR = TREC_CDS_DIR + "proximity/";

	/*
	 * TREC Genomics
	 */

	public static final String TREC_GENOMICS_DIR = DATA_DIR + "trec_genomics/";

	public static final String TREC_GENOMICS_COL_DIR = TREC_GENOMICS_DIR + "collection/";

	public static final String TREC_GENOMICS_COL_FILE = TREC_GENOMICS_DIR + "collection.txt.gz";

	public static final String TREC_GENOMICS_SENTS_FILE = TREC_GENOMICS_DIR + "sents.txt.gz";

	public static final String TREC_GENOMICS_SENTS_STEM_FILE = TREC_GENOMICS_DIR + "sents_stem.txt.gz";

	public static final String TREC_GENOMICS_QUERY_DIR = TREC_GENOMICS_DIR + "query/";

	public static final String TREC_GENOMICS_INDEX_DIR = TREC_GENOMICS_DIR + "index/";

	public static final String TREC_GENOMICS_INDEX_SENT_DIR = TREC_GENOMICS_DIR + "index_sent/";

	public static final String TREC_GENOMICS_QUERY_2007_DIR = TREC_GENOMICS_QUERY_DIR + "2007/";

	public static final String TREC_GENOMICS_VOCAB_FILE = TREC_GENOMICS_DIR + "vocab.txt.gz";

	public static final String TREC_GENOMICS_QUERY_2007_FILE = TREC_GENOMICS_QUERY_2007_DIR + "2007topics.txt";

	public static final String TREC_GENOMICS_RELEVANCE_JUDGE_2007_FILE = TREC_GENOMICS_QUERY_2007_DIR + "trecgen2007.all.judgments.tsv.txt";

	public static final String TREC_GENOMICS_DOC_ID_MAP_FILE = TREC_GENOMICS_DIR + "document_id_map.txt.gz";

	public static final String TREC_GENOMICS_OUTPUT_DIR = TREC_GENOMICS_DIR + "output/";

	public static final String TREC_GENOMICS_OUTPUT_RESULT_DIR = TREC_GENOMICS_OUTPUT_DIR + "result/";

	public static final String TREC_GENOMICS_OUTPUT_LOG_DIR = TREC_GENOMICS_OUTPUT_DIR + "log/";

	public static final String TREC_GENOMICS_DOC_PRIOR_FILE = TREC_GENOMICS_DIR + "document_prior.ser";

	public static final String TREC_GENOMICS_ABBR_DIR = TREC_GENOMICS_DIR + "abbr/";

	public static final String TREC_GENOMICS_ABBR_FILE = TREC_GENOMICS_ABBR_DIR + "abbrs.txt";

	public static final String TREC_GENOMICS_QUERY_DOC_FILE = TREC_GENOMICS_DIR + "query_doc.txt";

	/*
	 * OHSUMED
	 */

	public static final String OHSUMED_DIR = DATA_DIR + "ohsumed/";

	public static final String OHSUMED_COL_DIR = OHSUMED_DIR + "collection/";

	public static final String OHSUMED_COL_FILE = OHSUMED_DIR + "collection.txt.gz";

	public static final String OHSUMED_SENTS_FILE = OHSUMED_DIR + "sents.txt.gz";

	public static final String OHSUMED_SENTS_STEM_FILE = OHSUMED_DIR + "sents_stem.txt.gz";

	public static final String OHSUMED_QUERY_DIR = OHSUMED_DIR + "query/";

	public static final String OHSUMED_INDEX_DIR = OHSUMED_DIR + "index/";

	public static final String OHSUMED_INDEX_SENT_DIR = OHSUMED_DIR + "index_sent/";

	public static final String OHSUMED_QUERY_FILE = OHSUMED_QUERY_DIR + "queries.txt";

	public static final String OHSUMED_RELEVANCE_JUDGE_FILE = OHSUMED_QUERY_DIR + "judged.txt";

	public static final String OHSUMED_VOCAB_FILE = OHSUMED_DIR + "vocab.txt.gz";

	public static final String OHSUMED_DOC_ID_MAP_FILE = OHSUMED_DIR + "document_id_map.txt.gz";

	public static final String OHSUMED_OUTPUT_DIR = OHSUMED_DIR + "output/";

	public static final String OHSUMED_OUTPUT_RESULT_DIR = OHSUMED_OUTPUT_DIR + "result/";

	public static final String OHSUMED_OUTPUT_RESULT_2_DIR = OHSUMED_OUTPUT_DIR + "result-2/";

	public static final String OHSUMED_OUTPUT_LOG_DIR = OHSUMED_OUTPUT_DIR + "log/";

	public static final String OHSUMED_DOC_PRIOR_FILE = OHSUMED_DIR + "document_prior.ser";

	public static final String OHSUMED_ABBR_DIR = OHSUMED_DIR + "abbr/";

	public static final String OHSUMED_ABBR_FILE = OHSUMED_ABBR_DIR + "abbrs.txt";

	public static final String OHSUMED_QUERY_DOC_FILE = OHSUMED_DIR + "query_doc.txt";

	public static final String OHSUMED_CONCEPT_QUERY_DOC_FILE = OHSUMED_DIR + "concept_query_doc.txt";

	public static final String OHSUMED_PROXIMITY_DIR = OHSUMED_DIR + "proximity/";

	/*
	 * Common File Name Sets
	 */

	public static String[] DataDirNames = { TREC_CDS_DIR, CLEF_EHEALTH_DIR, OHSUMED_DIR, TREC_GENOMICS_DIR, CLUEWEB_COL_DIR };

	public static String[] QueryFileNames = { TREC_CDS_QUERY_2014_FILE, CLEF_EHEALTH_QUERY_2015_FILE, OHSUMED_QUERY_FILE,
			TREC_GENOMICS_QUERY_2007_FILE, CLEF_EHEALTH_QUERY_2016_FILE };

	// public static String[] IndexDirNames = { MIRPath.TREC_CDS_INDEX_DIR,
	// MIRPath.CLEF_EHEALTH_INDEX_DIR, MIRPath.OHSUMED_INDEX_DIR};

	public static final String[] IndexDirNames = { TREC_CDS_INDEX_DIR, CLEF_EHEALTH_INDEX_DIR, OHSUMED_INDEX_DIR, TREC_GENOMICS_INDEX_DIR,
			CLUEWEB_INDEX_DIR };

	public static final String[] SentIndexDirNames = { TREC_CDS_INDEX_SENT_DIR, CLEF_EHEALTH_INDEX_SENT_DIR, OHSUMED_INDEX_SENT_DIR,
			TREC_GENOMICS_INDEX_SENT_DIR };

	public static final String[] ResultDirNames = { TREC_CDS_OUTPUT_RESULT_DIR, CLEF_EHEALTH_OUTPUT_RESULT_DIR, OHSUMED_OUTPUT_RESULT_DIR,
			TREC_GENOMICS_OUTPUT_RESULT_DIR, CLEF_EHEALTH_OUTPUT_RESULT_2016_DIR };

	public static final String[] OutputDirNames = { TREC_CDS_OUTPUT_DIR, CLEF_EHEALTH_OUTPUT_DIR, OHSUMED_OUTPUT_DIR,
			TREC_GENOMICS_OUTPUT_DIR };

	public static final String[] LogDirNames = { TREC_CDS_OUTPUT_LOG_DIR, CLEF_EHEALTH_OUTPUT_LOG_DIR, OHSUMED_OUTPUT_LOG_DIR,
			TREC_GENOMICS_OUTPUT_LOG_DIR };

	public static final String[] DocPriorFileNames = { TREC_CDS_DOC_PRIOR_FILE, CLEF_EHEALTH_DOC_PRIOR_FILE, OHSUMED_DOC_PRIOR_FILE,
			TREC_GENOMICS_DOC_PRIOR_FILE };

	public static final String[] RelevanceFileNames = { TREC_CDS_RELEVANCE_JUDGE_2014_FILE, CLEF_EHEALTH_RELEVANCE_JUDGE_2015_FILE,
			OHSUMED_RELEVANCE_JUDGE_FILE, TREC_GENOMICS_RELEVANCE_JUDGE_2007_FILE };

	public static final String[] DocIdMapFileNames = { TREC_CDS_DOC_ID_MAP_FILE, CLEF_EHEALTH_DOC_ID_MAP_FIE, OHSUMED_DOC_ID_MAP_FILE,
			TREC_GENOMICS_DOC_ID_MAP_FILE, CLUEWEB_DOC_ID_MAP_FIE };

	public static final String[] AbbrFileNames = { TREC_CDS_ABBR_FILE, CLEF_EHEALTH_ABBR_FILE, OHSUMED_ABBR_FILE, TREC_GENOMICS_ABBR_FILE };

	public static final String[] CollNames = { "TREC CDS", "CLEF eHealth", "OHSUMED", "TREC GENOMICS", "WIKI" };

	public static final String[] VocFileNames = { TREC_CDS_VOCAB_FILE, CLEF_EHEALTH_VOCAB_FILE, OHSUMED_VOCAB_FILE,
			TREC_GENOMICS_VOCAB_FILE, CLUEWEB_VOCAB_FILE };

	public static final String[] QueryDocFileNames = { TREC_CDS_QUERY_DOC_FILE, CLEF_EHEALTH_QUERY_DOC_FILE, OHSUMED_QUERY_DOC_FILE,
			TREC_GENOMICS_QUERY_DOC_FILE };

	public static final String[] TaskDirs = { TREC_CDS_DIR, CLEF_EHEALTH_DIR, OHSUMED_DIR, TREC_GENOMICS_DIR };

	public static final String[] SentFileNames = { TREC_CDS_SENTS_FILE, CLEF_EHEALTH_SENTS_FILE, OHSUMED_SENTS_FILE,
			TREC_GENOMICS_SENTS_FILE };

	public static final String[] SentStemFileNames = { TREC_CDS_SENTS_STEM_FILE, CLEF_EHEALTH_SENTS_STEM_FILE, OHSUMED_SENTS_STEM_FILE,
			TREC_GENOMICS_SENTS_STEM_FILE };

}
