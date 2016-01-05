package ohs.entity;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.string.search.ppss.StringRecord;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;

/**
 * @author Heung-Seon Oh
 * 
 * 
 * 
 */
public class SimpleEntityLinker implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7199650129494305577L;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		SimpleEntityLinker el = new SimpleEntityLinker();

		// if (IOUtils.exists(ENTPath.ENTITY_LINKER_FILE)) {
		// el.read(ENTPath.ENTITY_LINKER_FILE);
		// } else {
//		el.createSearchers(ENTPath.TITLE_FILE);
//		el.write(ENTPath.ENTITY_LINKER_FILE);
		// }

		el.read(ENTPath.ENTITY_LINKER_FILE);

		Counter<String> features = new Counter<String>();
		features.setCount("baseball", 1);
		features.setCount("dodgers", 1);

		Counter<Entity> scores1 = el.link("Mattingly", features);
		// Counter<Entity> scores2 = el.link("Mattingly", SearcherUtils.getIndexSearcher("../../data/medical_ir/wiki/index"));

		// System.out.println(scores.toStringSortedByValues(true, true, scores.size()));

		TextFileWriter writer = new TextFileWriter(ENTPath.EX_FILE);
		for (Entity e : scores1.getSortedKeys()) {
			writer.write(e.toString() + "\t" + scores1.getCount(e) + "\n");
		}
		writer.close();

		// TextFileReader reader = new TextFileReader("../../data/news_ir/ners.txt");
		// while (reader.hasNext()) {
		// String line = reader.next();
		//
		// }

		System.out.println("process ends.");
	}

	private SimpleStringSearcher searcher;

	private Map<Integer, Entity> ents;

	private Map<Integer, Integer> recToEntIdMap;

	private Indexer<String> featInexer;

	private Map<Integer, SparseVector> topicWordData;

	public SimpleEntityLinker() {

	}

	/**
	 * Create two pivotal prefix searchers for English and Korean. If extOrgFileName is given, global gram orders are determined based on
	 * external organization names.
	 * 
	 * @param dataFileName
	 * 
	 *            Contains external organization names. They are used to compute global gram orders employed in Searchers.
	 */
	public void createSearchers(String dataFileName) {
		List<StringRecord> srs = new ArrayList<StringRecord>();
		recToEntIdMap = new HashMap<Integer, Integer>();
		ents = new HashMap<Integer, Entity>();

		featInexer = new Indexer<String>();
		topicWordData = new HashMap<Integer, SparseVector>();

		TextFileReader reader = new TextFileReader(dataFileName);
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			// if (reader.getNumLines() > 1000) {
			// break;
			// }

			String[] parts = line.split("\t");
			int id = Integer.parseInt(parts[0]);
			String name = parts[1];
			String topic = parts[2];
			String catStr = parts[3];
			String variantStr = parts[4];

			if (catStr.equals("none")) {
				topicWordData.put(id, new SparseVector());
			} else {
				topicWordData.put(id, VectorUtils.toSparseVector(VectorUtils.toCounter(catStr), featInexer, true));
			}

			Entity ent = new Entity(id, name, topic);
			ents.put(ent.getId(), ent);

			StringRecord sr = new StringRecord(srs.size(), name.toLowerCase());
			srs.add(sr);

			recToEntIdMap.put(sr.getId(), ent.getId());

			if (!variantStr.equals("none")) {
				String[] variants = variantStr.split("\\|");
				for (int i = 0; i < variants.length; i++) {
					sr = new StringRecord(srs.size(), variants[i].toLowerCase());
					srs.add(sr);
					recToEntIdMap.put(sr.getId(), ent.getId());
				}
			}
		}

		// TermWeighting.computeTFIDFs(topicWordData);

		System.out.printf("read [%d] records from [%d] entities at [%s].\n", srs.size(), ents.size(), dataFileName);

		searcher = new SimpleStringSearcher(3);
		searcher.index(srs, false);
		System.out.println(searcher.info());
	}

	private Counter<String> getWordCounts(IndexReader ir, int docId, String field) throws Exception {
		Terms termVector = ir.getTermVector(docId, field);

		if (termVector == null) {
			return new Counter<String>();
		}

		TermsEnum termsEnum = null;
		termsEnum = termVector.iterator();

		BytesRef bytesRef = null;
		PostingsEnum postingsEnum = null;
		Counter<String> ret = new Counter<String>();

		while ((bytesRef = termsEnum.next()) != null) {
			postingsEnum = termsEnum.postings(postingsEnum, PostingsEnum.ALL);

			if (postingsEnum.nextDoc() != 0) {
				throw new AssertionError();
			}

			String word = bytesRef.utf8ToString();
			// if (word.startsWith("<N") && word.endsWith(">")) {
			// continue;
			// }
			if (word.contains("<N")) {
				continue;
			}

			int freq = postingsEnum.freq();
			ret.incrementCount(word, freq);

			// for (int k = 0; k < freq; k++) {
			// final int position = postingsEnum.nextPosition();
			// locWords.put(position, w);
			// }
		}
		return ret;
	}

	public Counter<Entity> link(String mention, Counter<String> features) {
		Counter<StringRecord> candidates = searcher.search(mention.toLowerCase());

		CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();
		Counter<Entity> ret = new Counter<Entity>();

		for (StringRecord sr : candidates.keySet()) {
			int rid = sr.getId();
			cm.incrementCount(recToEntIdMap.get(rid), rid, candidates.getCount(sr));
		}

		SparseVector cv = VectorUtils.toSparseVector(features, featInexer);
		VectorMath.unitVector(cv);

		for (int eid : cm.keySet()) {
			double sw_score = cm.getCounter(eid).max();
			SparseVector tv = topicWordData.get(eid);
			double cosine = VectorMath.cosine(cv, tv, false);
			double new_score = sw_score * Math.exp(cosine);
			ret.setCount(ents.get(eid), new_score);
		}
		return ret;
	}

	public Counter<Entity> link(String mention, IndexSearcher is) throws Exception {
		Counter<StringRecord> candidates = searcher.search(mention.toLowerCase());

		CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();
		Counter<Entity> ret = new Counter<Entity>();

		for (StringRecord sr : candidates.keySet()) {
			int rid = sr.getId();
			cm.incrementCount(recToEntIdMap.get(rid), rid, candidates.getCount(sr));
		}

		for (int eid : cm.keySet()) {
			Document doc = is.doc(eid);
			String content = doc.get(IndexFieldName.CONTENT);
			double sw_score = cm.getCounter(eid).max();
			SparseVector tv = topicWordData.get(eid);
			ret.setCount(ents.get(eid), sw_score);
		}
		return ret;
	}

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = IOUtils.openObjectInputStream(fileName);

		int size = ois.readInt();
		ents = new HashMap<Integer, Entity>(size);
		for (int i = 0; i < size; i++) {
			Entity ent = new Entity();
			ent.read(ois);
			ents.put(ent.getId(), ent);
		}

		featInexer = IOUtils.readIndexer(ois);
		recToEntIdMap = IOUtils.readIntegerMap(ois);

		int size2 = ois.readInt();
		topicWordData = new HashMap<Integer, SparseVector>(size2);

		for (int i = 0; i < size2; i++) {
			int id = ois.readInt();
			SparseVector sv = new SparseVector();
			sv.read(ois);
			topicWordData.put(id, sv);
		}

		searcher = new SimpleStringSearcher();
		searcher.read(ois);
		ois.close();
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(fileName);
		oos.writeInt(ents.size());
		for (Entity ent : ents.values()) {
			ent.write(oos);
		}

		IOUtils.write(oos, featInexer);
		IOUtils.write(oos, recToEntIdMap);

		oos.writeInt(topicWordData.size());
		for (int id : topicWordData.keySet()) {
			SparseVector sv = topicWordData.get(id);
			oos.writeInt(id);
			sv.write(oos);
		}

		searcher.write(oos);
		oos.close();
	}

}
