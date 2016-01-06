package ohs.entity;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.string.search.ppss.StringRecord;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.Generics;
import ohs.utils.StopWatch;

/**
 * @author Heung-Seon Oh
 * 
 * 
 * 
 */
public class EntityLinker implements Serializable {

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
		EntityLinker el = new EntityLinker();

		// if (IOUtils.exists(ENTPath.ENTITY_LINKER_FILE)) {
		// el.read(ENTPath.ENTITY_LINKER_FILE);
		// } else {
		// el.createSearcher(ENTPath.NAME_PERSON_FILE);
		// el.write(ENTPath.ENTITY_LINKER_FILE);
		// }

		el.read(ENTPath.ENTITY_LINKER_FILE);

		List<Entity> ents = new ArrayList<Entity>(el.getEntities().values());

		TextFileWriter writer = new TextFileWriter(ENTPath.EX_FILE);

		for (int i = 0; i < ents.size() && i < 20; i++) {
			Entity ent = ents.get(i);
			Counter<Entity> scores = el.link(ent.getText());
			scores.keepTopNKeys(10);

			writer.write("====== input ======" + "\n");
			writer.write(ent.toString() + "\n");
			writer.write("====== candidates ======" + "\n");
			for (Entity e : scores.getSortedKeys()) {
				writer.write(e.toString() + "\t" + scores.getCount(e) + "\n");
			}
			writer.write("\n\n");
		}

		writer.close();

		// Counter<String> features = new Counter<String>();
		// features.setCount("baseball", 1);
		// features.setCount("dodgers", 1);

		// Counter<Entity> scores1 = el.link("cancer");
		// Counter<Entity> scores1 = el.link("Mattingly", features);
		// Counter<Entity> scores2 = el.link("Mattingly",
		// SearcherUtils.getIndexSearcher("../../data/medical_ir/wiki/index"));

		// System.out.println(scores.toStringSortedByValues(true, true,
		// scores.size()));

		// TextFileReader reader = new
		// TextFileReader("../../data/news_ir/ners.txt");
		// while (reader.hasNext()) {
		// String line = reader.next();
		//
		// }

		System.out.println("process ends.");
	}

	private StringSearcher searcher;

	private Map<Integer, Entity> ents;

	private Map<Integer, Integer> recToEntIdMap;

	private Indexer<String> featInexer;

	private Map<Integer, SparseVector> topicWordData;

	public EntityLinker() {

	}

	public void createSearcher(String dataFileName) {
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

			// if (reader.getNumLines() > 10000) {
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
		searcher = new StringSearcher(3);
		searcher.index(srs, false);
		System.out.println(searcher.info() + "\n");

		searcher.filter();
		System.out.println(searcher.info() + "\n");
	}

	public Map<Integer, Entity> getEntities() {
		return ents;
	}

	private Counter<String> getWordCounts(IndexReader ir, int docid, String field) throws Exception {
		Terms termVector = ir.getTermVector(docid, field);

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

	public Counter<Entity> link(String mention) {
		return link(mention, null, null);
	}

	public Counter<Entity> link(String mention, Counter<String> features) {
		return link(mention, features, null);
	}

	public Counter<Entity> link(String mention, Counter<String> features, IndexSearcher is) {
		Counter<StringRecord> candidates = searcher.search(mention.toLowerCase());

		CounterMap<Integer, Integer> cm = Generics.newCounterMap();
		Counter<Entity> ret = Generics.newCounter();

		for (StringRecord sr : candidates.keySet()) {
			int rid = sr.getId();
			cm.incrementCount(recToEntIdMap.get(rid), rid, candidates.getCount(sr));
		}

		SparseVector cv = null;

		if (features != null && features.size() > 0) {
			cv = VectorUtils.toSparseVector(features, featInexer);
			VectorMath.unitVector(cv);
		}

		for (int eid : cm.keySet()) {
			double score = cm.getCounter(eid).max();
			SparseVector tv = topicWordData.get(eid);

			if (cv != null) {
				double cosine = VectorMath.cosine(cv, tv, false);
				score *= Math.exp(cosine);
			}

			// Counter<String> wordCounts = new Counter<String>();
			//
			// try {
			// wordCounts = getWordCounts(is.getIndexReader(), eid, IndexFieldName.CONTENT);
			// } catch (Exception e) {
			// e.printStackTrace();
			// }

			ret.setCount(ents.get(eid), score);
		}
		return ret;
	}

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = IOUtils.openObjectInputStream(fileName);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

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

		searcher = new StringSearcher();
		searcher.read(ois);
		ois.close();

		System.out.printf("read [%s] - [%s]\n", getClass().getName(), stopWatch.stop());
	}

	public void write(String fileName) throws Exception {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

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

		System.out.printf("write [%s] - [%s]\n", getClass().getName(), stopWatch.stop());
	}

}