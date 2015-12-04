package ohs.ir.task.medical.trec.cds_2014;

public class CDSPath {

	public static final String DATA_DIR = "../../data/trec/cds/";

	public static final String RAW_COLLECTION_DIR = DATA_DIR + "dist/collection/docs/";

	public static final String TEXT_COLLECTION_FILE = DATA_DIR + "collection.txt";

	public static final String DUPLICATION_1_FILE = DATA_DIR + "dist/duplicates-1.txt";

	public static final String DUPLICATION_2_FILE = DATA_DIR + "dist/duplicates-2.txt";

	public static final String LOG_FILE = DATA_DIR + "log.txt";

	public static final String TEST_QUERY_FILE = DATA_DIR + "dist/topics.xml";

	public static final String INDEX_DIR = DATA_DIR + "index/";

	public static final String OUTPUT_DIR = DATA_DIR + "output/";

	public static final String QC_DATA_DIR = DATA_DIR + "qc/";

	public static final String OUTPUT_RERANKING_DIR = OUTPUT_DIR + "reranking/";

	public static final String OUTPUT_CORRELATION_FILE = OUTPUT_DIR + "correlation.txt";

	public static final String OUTPUT_INITIAL_SEARCH_RESULT_FILE = OUTPUT_RERANKING_DIR + "temp_results-init.txt";

	public static final String OUTPUT_QC_RERANKED_SEARCH_RESULT_FILE = OUTPUT_RERANKING_DIR + "temp_results-init+prf+qc.txt";

	public static final String OUTPUT_PRF_RERANKED_SEARCH_RESULT_FILE = OUTPUT_RERANKING_DIR + "temp_results-init+prf.txt";

	public static final String OUTPUT_ESA_RERANKED_SEARCH_RESULT_FILE = OUTPUT_RERANKING_DIR + "temp_results-init+prf+esa.txt";

	public static final String OUTPUT_DOCUMENT_ID_MAP_FILE = OUTPUT_RERANKING_DIR + "document_id_map.txt";

	public static final String OUTPUT_DOC_QUERY_MATRIX_FILE = OUTPUT_DIR + "doc_query_matrix.txt";

	public static final String OUTPUT_DOC_TYPE_FILE = OUTPUT_DIR + "doc_type.txt";

	public static final String ABBREVIATION_FILE = DATA_DIR + "abbreviation.txt";

	public static final String ABBREVIATION_FILTERED_FILE = DATA_DIR + "abbreviation_filter.txt";

	public static final String ABBREVIATION_GROUPED_FILE = DATA_DIR + "abbreviation_group.txt";

	public static final String ABBREVIATION_MODEL_FILE = DATA_DIR + "abbreviation_mode.ser";

	public static final String VOCABULARY_DIR = DATA_DIR + "vocabulary/";

	public static final String WORD_COUNT_FILE = VOCABULARY_DIR + "word_count.ser";

	public static final String WORD_DOC_FREQ_FILE = VOCABULARY_DIR + "word_doc-freq.ser";

	public static final String WORD_INDEXER_FILE = VOCABULARY_DIR + "word_indexer.txt";

	public static final String ICD10_DIR = DATA_DIR + "icd-10/";

	public static final String ICD10_HTML_FILE = ICD10_DIR + "ICD-10 - Wikipedia, the free encyclopedia.htm";

	public static final String ICD10_TOP_LEVEL_CHAPTER_FILE = ICD10_DIR + "icd-10-top-level.txt";

	public static final String ICD10_HIERARCHY_FILE = ICD10_DIR + "icd-10-hierarchy.txt";

	public static final String ICD10_REFINED_HIERARCHY_FILE = ICD10_DIR + "icd-10-hierarchy_refined.txt";

	public static final String ICD10_REFINED_HIERARCHY_PAGE_ATTACHED_FILE = ICD10_DIR + "icd-10-hierarchy_page-attached.txt";

	public static final String ICD10_LOG_FILE = ICD10_DIR + "icd-10_log.txt";

	public static final String ICD10_ESA_DIR = ICD10_DIR + "esa/";

	public static final String ICD10_WORD_CONCEPT_MAP_FILE = ICD10_ESA_DIR + "icd-10_word-concept.ser";

	public static final String ICD10_CONCEPT_CATEGORY_MAP_FILE = ICD10_ESA_DIR + "icd-10_concept-category.ser";

	public static final String ICD10_CONCEPT_INDEXER_FILE = ICD10_ESA_DIR + "icd-10_concept_indexer.txt";

	public static final String ICD10_CATEGORY_INDEXER_FILE = ICD10_ESA_DIR + "icd-10_category_indexer.txt";

	public static final String QUERY_CLASSIFIER_DIR = DATA_DIR + "query_classifier/";

	public static final String QUERY_CLASSIFIER_TYPE_INDEXER_FILE = QUERY_CLASSIFIER_DIR + "query_type_indexer.txt";

	public static final String QUERY_CLASSIFIER_MODEL_FILE = QUERY_CLASSIFIER_DIR + "query_classifier_model.txt";

	public static final String QUERY_CLASSIFIER_TRAIN_DATA_FILE = QUERY_CLASSIFIER_DIR + "query_classifier_model.txt";

}
