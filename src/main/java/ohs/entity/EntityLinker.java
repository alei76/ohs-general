package ohs.entity;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

		if (IOUtils.exists(ENTPath.ENTITY_LINKER_FILE)) {
			el.read(ENTPath.ENTITY_LINKER_FILE);
		} else {
			el.createSearchers(ENTPath.NAME_PERSON_FILE);
			el.write(ENTPath.ENTITY_LINKER_FILE);
		}

		Counter<String> contextWords = new Counter<String>();
		contextWords.setCount("baseball", 1);
		contextWords.setCount("dodgers", 1);

		Counter<Entity> scores = el.link("Mattingly", contextWords);

		// System.out.println(scores.toStringSortedByValues(true, true, scores.size()));

		TextFileWriter writer = new TextFileWriter(ENTPath.EX_FILE);
		for (Entity e : scores.getSortedKeys()) {
			writer.write(e.toString() + "\t" + scores.getCount(e) + "\n");
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

	private Indexer<String> wordInexer;

	private List<SparseVector> topicWordData;

	public EntityLinker() {

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

		wordInexer = new Indexer<String>();
		topicWordData = new ArrayList<SparseVector>();

		TextFileReader reader = new TextFileReader(dataFileName);
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			// if (reader.getNumLines() > 100000) {
			// break;
			// }

			String[] parts = line.split("\t");
			String name = parts[0];
			String topic = parts[1];
			String catStr = parts[2];
			String variantStr = parts[3];

			topicWordData.add(VectorUtils.toSparseVector(VectorUtils.toCounter(catStr), wordInexer, true));

			Entity ent = new Entity(ents.size(), name, topic);
			ents.put(ent.getId(), ent);

			StringRecord sr = new StringRecord(srs.size(), name);
			srs.add(sr);

			recToEntIdMap.put(sr.getId(), ent.getId());

			if (!variantStr.equals("none")) {
				String[] variants = variantStr.split("\\|");
				for (int i = 0; i < variants.length; i++) {
					sr = new StringRecord(srs.size(), variants[i]);
					srs.add(sr);
					recToEntIdMap.put(sr.getId(), ent.getId());
				}
			}
		}

		// TermWeighting.computeTFIDFs(topicWordData);

		System.out.printf("read [%d] records from [%d] entities at [%s].\n", srs.size(), ents.size(), dataFileName);

		searcher = new SimpleStringSearcher(2);
		searcher.index(srs, false);
	}

	public Counter<Entity> link(String name, Counter<String> contextWords) {
		Counter<StringRecord> searchScore = searcher.search(name);

		CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();
		Counter<Entity> ret = new Counter<Entity>();

		for (StringRecord sr : searchScore.keySet()) {
			int rid = sr.getId();
			cm.incrementCount(recToEntIdMap.get(rid), rid, searchScore.getCount(sr));
		}

		SparseVector cv = VectorUtils.toSparseVector(contextWords, wordInexer);
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

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = IOUtils.openObjectInputStream(fileName);

		{
			ents = new HashMap<Integer, Entity>();
			int size = ois.readInt();

			for (int i = 0; i < size; i++) {
				Entity ent = new Entity();
				ent.read(ois);
				ents.put(ent.getId(), ent);
			}
		}

		wordInexer = IOUtils.readIndexer(ois);
		recToEntIdMap = IOUtils.readIntegerMap(ois);
		topicWordData = SparseVector.readList(ois);
		searcher = new SimpleStringSearcher();
		searcher.read(ois);
		ois.close();
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(fileName);

		{
			oos.writeInt(ents.size());
			for (Entity ent : ents.values()) {
				ent.write(oos);
			}
		}

		IOUtils.write(oos, wordInexer);
		IOUtils.write(oos, recToEntIdMap);
		SparseVector.write(oos, topicWordData);
		searcher.write(oos);
		oos.close();
	}

}
