package ohs.ir.medical.wiki;

import java.io.ObjectInputStream;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.CommonFieldNames;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.NLPUtils;
import ohs.ir.medical.general.SearcherUtils;
import ohs.types.BidMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		DataHandler dh = new DataHandler();
		// dh.findMedicalPages();
		dh.extractMedicalPages();

		System.out.println("process ends.");
	}

	public void extractMedicalPages() throws Exception {
		IndexSearcher is = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		TextFileWriter writer = new TextFileWriter(MIRPath.WIKI_DIR + "wiki_medical_contents.txt.gz");
		TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_medical_pages.txt.gz");
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}

			// if (reader.getNumLines() == 100) {
			// break;
			// }

			String[] parts = reader.next().split("\t");
			String pageid = parts[0];
			String title = parts[1];
			String catid = parts[2];
			String cat = parts[3];

			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			builder.add(new TermQuery(new Term(CommonFieldNames.DOCUMENT_ID, pageid)), Occur.SHOULD);
			Query q = builder.build();

			TopDocs tp = is.search(q, 1);

			if (tp.scoreDocs.length == 0) {
				continue;
			}

			Document doc = is.doc(tp.scoreDocs[0].doc);

			String t = doc.get(CommonFieldNames.TITLE);
			String catStr = doc.get(CommonFieldNames.CATEGORY);
			String redStr = doc.get(CommonFieldNames.REDIRECTS);
			String content = doc.get(CommonFieldNames.CONTENT);

			String content2 = StrUtils.join("<nl>", NLPUtils.tokenize(content)).replaceAll("[\n]+", "<nl>");
			
			if (content2.length() > 0) {
				String[] out = new String[] { title, content2 };
				out = StrUtils.wrap(out);
				writer.write(StrUtils.join("\t", out) + "\n");
			}
		}
		reader.close();
		writer.close();
	}

	public void findMedicalPages() throws Exception {
		BidMap<Integer, String> idToTitle = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_titles.ser.gz");
			idToTitle = FileUtils.readIntStrBidMap(ois);
			ois.close();
		}

		BidMap<Integer, String> idToCat = null;

		{
			ObjectInputStream ois = FileUtils.openObjectInputStream(MIRPath.WIKI_DIR + "wiki_cats.ser.gz");
			List<Integer> ids = FileUtils.readIntList(ois);
			List<String> titles = FileUtils.readStrList(ois);
			List<Integer> catPages = FileUtils.readIntList(ois);
			List<Integer> catSubcats = FileUtils.readIntList(ois);
			ois.close();

			idToCat = Generics.newBidMap(ids.size());

			for (int i = 0; i < ids.size(); i++) {
				idToCat.put(ids.get(i), titles.get(i));
				// catPageCnts.setCount(ids.get(i), catPages.get(i));
			}
		}

		Set<Integer> medicalCats = Generics.newHashSet();

		{
			TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_cat_tree_top-down.txt.gz");
			while (reader.hasNext()) {
				if (reader.getNumLines() == 1) {
					continue;
				}
				String[] cats = reader.next().split("\t");

				for (String cat : cats) {
					Integer id = idToCat.getKey(cat);
					if (id != null) {
						medicalCats.add(id);
					}
				}
			}
			reader.close();
		}

		{
			TextFileWriter writer = new TextFileWriter(MIRPath.WIKI_DIR + "wiki_medical_pages.txt.gz");
			TextFileReader reader = new TextFileReader(MIRPath.WIKI_DIR + "wiki_catlinks.txt.gz");
			while (reader.hasNext()) {
				String line = reader.next();

				String[] parts = StrUtils.unwrap(line.split("\t"));

				if (parts.length != 3) {
					System.out.println(line);
					continue;
				}

				int pageid = Integer.parseInt(parts[0]);
				int parent_id = Integer.parseInt(parts[1]);
				String cl_type = parts[2];

				if (cl_type.equals("page")) {
					if (medicalCats.contains(parent_id)) {
						String title = idToTitle.getValue(pageid);
						String cat = idToCat.getValue(parent_id);
						writer.write(String.format("%d\t%s\t%d\t%s\n", pageid, title, parent_id, cat));
					}
				}
			}
			reader.close();
			writer.close();

		}
	}

}
