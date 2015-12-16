package ohs.entity;

import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohs.entity.data.struct.Organization;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.string.search.ppss.GramOrderer;
import ohs.string.search.ppss.PivotalPrefixStringSearcher;
import ohs.string.search.ppss.StringRecord;
import ohs.types.Counter;
import ohs.types.CounterMap;

/**
 * @author Heung-Seon Oh
 * 
 * 
 * 
 */
public class EntityLinker implements Serializable {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		EntityLinker el = new EntityLinker();
		el.createSearchers(ENTPath.PERSON_NAME_FILE);
		System.out.println("process ends.");
	}

	private PivotalPrefixStringSearcher searcher;

	private TextFileWriter logWriter = new TextFileWriter(ENTPath.ODK_LOG_FILE);

	private List<StringRecord> srs;

	private Map<Integer, Entity> ents;

	private Map<Integer, Integer> recToEntId;

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
		srs = new ArrayList<StringRecord>();
		recToEntId = new HashMap<Integer, Integer>();
		ents = new HashMap<Integer, Entity>();

		int q = 2;
		int tau = 3;

		TextFileReader reader = new TextFileReader(dataFileName);
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}

			String[] parts = line.split("\t");

			Entity ent = null;
			for (int i = 0; i < parts.length; i++) {
				StringRecord sr = new StringRecord(srs.size(), parts[0]);
				if (i == 0) {
					ent = new Entity(ents.size(), parts[0], null);
					ents.put(ent.getId(), ent);
				}
				srs.add(sr);
				recToEntId.put(sr.getId(), ent.getId());
			}
		}

		System.out.printf("read [%d] records from [%d] entities at [%s].\n", srs.size(), ents.size(), dataFileName);

		GramOrderer gramOrderer = new GramOrderer();

		searcher = new PivotalPrefixStringSearcher(q, tau, true);
		searcher.setGramSorter(gramOrderer);
		searcher.index(srs);
	}

	public Counter<Entity> link(String name) {

		Counter<StringRecord> searchScore = searcher.search(name);

		CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();
		Counter<Entity> ret = new Counter<Entity>();

		for (StringRecord sr : searchScore.keySet()) {
			double score = searchScore.getCount(sr);
			int rid = sr.getId();
			int eid = recToEntId.get(rid);
			cm.incrementCount(eid, rid, score);
		}

		for (int eid : cm.keySet()) {
			Counter<Integer> c = cm.getCounter(eid);
			ret.setCount(ents.get(eid), c.max());
		}

		logWriter.write(name + "\n");
		logWriter.write(searchScore.toString());
		logWriter.write("\n\n");

		// logWriter.write(orgScores2.toString() + "\n\n");

		return ret;
	}

	public void write(String fileName) throws Exception {
		BufferedWriter writer = IOUtils.openBufferedWriter(fileName);
		searcher.write(writer);
		writer.close();
	}

}
