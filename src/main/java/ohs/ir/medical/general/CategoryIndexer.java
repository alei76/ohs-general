package ohs.ir.medical.general;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.types.Counter;
import ohs.types.ListMap;
import ohs.utils.StopWatch;

/**
 * Construct an inverted index with source document collection.
 * 
 * @author Heung-Seon Oh
 * 
 */
public class CategoryIndexer {

	public static final int ramSize = 5000;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		CategoryIndexer id = new CategoryIndexer();
		// id.makeCategoryMap();
		// id.index();
		System.out.println("process ends.");
	}

	public CategoryIndexer() {

	}

	public void index() throws Exception {
		ListMap<String, Integer> catMap = readCategoryMap();

		Counter<String> counter = new Counter<String>();

		for (String cat : catMap.keySet()) {
			counter.setCount(cat, catMap.get(cat).size());
		}

		IndexWriter indexWriter = DocumentIndexer.getIndexWriter(MIRPath.WIKI_CATEGORY_INDEX_DIR);
		IndexReader indexReader = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR).getIndexReader();

		int numCats = 0;

		// List<String> cats = new ArrayList<String>(catMap.keySet());
		// Collections.sort(cats);

		List<String> cats = counter.getSortedKeys();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < cats.size(); i++) {
			String cat = cats.get(i);

			if (i % 10 == 0) {
				System.out.printf("\r[%d/%d, %text]", i, cats.size(), stopWatch.stop());
			}

			StringBuffer sb1 = new StringBuffer();
			StringBuffer sb2 = new StringBuffer();
			List<Integer> ids = catMap.get(cat);

			// if (ids.size() > 10000) {
			// System.out.println(cat);
			// continue;
			// }

			for (int indexId : ids) {
				Document doc = indexReader.document(indexId);
				String title = doc.getField(CommonFieldNames.TITLE).stringValue();
				String text = doc.getField(CommonFieldNames.CONTENT).stringValue();

				sb1.append(String.format("%text\n%text\n\n", title, text));
				sb2.append(indexId + " ");
			}

			Document doc = new Document();
			doc.add(new TextField(CommonFieldNames.DOCUMENT_ID, sb2.toString().trim(), Field.Store.YES));
			doc.add(new TextField(CommonFieldNames.TITLE, cat, Store.YES));
			doc.add(new TextField(CommonFieldNames.CONTENT, sb1.toString().trim(), Store.NO));

			indexWriter.addDocument(doc);
		}
		System.out.printf("\r[%d/%d, %text]\n", cats.size(), cats.size(), stopWatch.stop());
		indexWriter.close();
		indexReader.close();
	}

	public void makeCategoryMap() throws Exception {
		System.out.println("make category map.");
		IndexReader indexReader = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR).getIndexReader();
		TextFileWriter writer = new TextFileWriter(MIRPath.WIKI_CATEGORY_MAP_FILE);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < indexReader.maxDoc(); i++) {
			if (i % 100000 == 0) {
				System.out.printf("\r[%d/%d, %text]", i, indexReader.maxDoc(), stopWatch.stop());
			}
			Document doc = indexReader.document(i);
			String docId = doc.getField(CommonFieldNames.DOCUMENT_ID).stringValue();
			String title = doc.getField(CommonFieldNames.TITLE).stringValue();
			String text = doc.getField(CommonFieldNames.CONTENT).stringValue();
			String catText = doc.getField(CommonFieldNames.CATEGORY).stringValue().trim();

			for (String cat : catText.split("\n")) {
				cat = cat.trim();
				if (cat.length() > 0) {
					writer.write(String.format("%text\t%d\n", cat, i));
				}
			}
		}
		System.out.printf("\r[%d/%d, %text]\n", indexReader.maxDoc(), indexReader.maxDoc(), stopWatch.stop());
		writer.close();
		indexReader.close();
	}

	private ListMap<String, Integer> readCategoryMap() {
		ListMap<String, Integer> ret = new ListMap<String, Integer>();
		TextFileReader reader = new TextFileReader(MIRPath.WIKI_CATEGORY_MAP_FILE);

		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");
			String cat = parts[0].trim();
			int indexId = Integer.parseInt(parts[1]);
			ret.put(cat, indexId);
		}
		reader.close();

		ret.remove("Living people");
		ret.remove("Archived files for deletion discussions");
		ret.remove("Articles created via the Article Wizard");
		// ret.remove("Place of birth missing (living people)");
		// ret.remove("Place of birth missing");
		// ret.remove("Year of birth missing (living people)");
		// ret.remove("Year of birth unknown");
		// ret.remove("Year of birth missing");
		// ret.remove("Year of death missing");
		// ret.remove("Year of death unknown");
		ret.remove("Years");
		ret.remove("");

		Pattern p1 = Pattern.compile("([\\d]+|births|deaths|templates)");
		Pattern p2 = Pattern.compile("(Year|Place) of", Pattern.CASE_INSENSITIVE);

		Iterator<String> iter = ret.keySet().iterator();
		while (iter.hasNext()) {
			String cat = iter.next();
			Matcher m = p1.matcher(cat);
			if (p1.matcher(cat).find() || p2.matcher(cat).find()) {
				iter.remove();
			}
		}

		double numAvgDocsPerCat = 0;
		double maxDocs = 0;
		Counter<String> counter = new Counter<String>();

		for (String cat : ret.keySet()) {
			List<Integer> ids = ret.get(cat);
			Collections.sort(ids);

			numAvgDocsPerCat += ids.size();

			if (maxDocs < ids.size()) {
				maxDocs = ids.size();
			}

			counter.incrementCount(cat, ids.size());
		}

		numAvgDocsPerCat /= ret.size();
		System.out.printf("read [%d] categories.\n", ret.size());
		System.out.printf("average docs per a category [%f].\n", numAvgDocsPerCat);
		System.out.printf("max docs for a category [%f].\n", maxDocs);

		TextFileWriter writer = new TextFileWriter(MIRPath.WIKI_CATEGORY_COUNT_FILE);

		for (String cat : counter.getSortedKeys()) {
			double count = counter.getCount(cat);
			writer.write(cat + "\t" + count + "\n");
		}
		writer.close();

		return ret;
	}

}
