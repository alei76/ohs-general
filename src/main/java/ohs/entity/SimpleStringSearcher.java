package ohs.entity;

import java.io.BufferedWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.io.IOUtils;
import ohs.io.TextFileWriter;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.string.search.ppss.StringRecord;
import ohs.string.sim.SmithWaterman;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.SetMap;

/**
 * 
 * Implementation of "A Pivotal Prefix Based Filtering Algorithm for String Similarity Search" at SIGMOD'14
 * 
 * 
 * @author Heung-Seon Oh
 */
public class SimpleStringSearcher implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8740333778747553831L;

	private Map<Integer, StringRecord> srs;

	private SetMap<Integer, Integer> index;

	private Indexer<String> gramIndexer;

	private GramGenerator gg;

	private TextFileWriter logWriter;

	public SimpleStringSearcher() {
		this(3);
	}

	public SimpleStringSearcher(int q) {
		gg = new GramGenerator(q);
	}

	public GramGenerator getGramGenerator() {
		return gg;
	}

	public Map<Integer, StringRecord> getStringRecords() {
		return srs;
	}

	public void index(List<StringRecord> input, boolean append) {
		System.out.printf("index [%s] records.\n", input.size());

		if (index == null && !append) {
			gramIndexer = new Indexer<String>();
			index = new SetMap<Integer, Integer>();
			srs = new HashMap<Integer, StringRecord>();
		}

		for (int i = 0; i < input.size(); i++) {
			StringRecord sr = input.get(i);
			Gram[] grams = gg.generate(String.format("<%s>", sr.getString()));
			if (grams.length == 0) {
				continue;
			}
			for (int j = 0; j < grams.length; j++) {
				index.put(gramIndexer.getIndex(grams[j].getString()), sr.getId());
			}
			srs.put(sr.getId(), sr);
		}
	}

	public String info() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("Records:\t%d\n", srs.size()));

		{
			Counter<Integer> c = new Counter<Integer>();

			int max = -Integer.MAX_VALUE;
			int min = Integer.MAX_VALUE;
			double num_chars = 0;

			for (StringRecord sr : srs.values()) {
				c.incrementCount(sr.getString().length(), 1);

				if (sr.getString().length() > max) {
					max = sr.getString().length();
				}

				if (sr.getString().length() < min) {
					min = sr.getString().length();
				}
				num_chars += sr.getString().length();
			}
			double avg_chars = num_chars / srs.size();
			sb.append(String.format("Max Length:\t%d\n", max));
			sb.append(String.format("Min Length:\t%d\n", min));
			sb.append(String.format("Avg Length:\t%f\n", avg_chars));
		}

		{
			int max = -Integer.MAX_VALUE;
			int min = Integer.MAX_VALUE;
			double num_records = 0;

			for (int qid : index.keySet()) {
				Set<Integer> rids = index.get(qid);

				if (rids.size() > max) {
					max = rids.size();
				}

				if (rids.size() < min) {
					min = rids.size();
				}
				num_records += rids.size();
			}
			double avg_records = num_records / index.size();
			sb.append(String.format("Q-grams:\t%d\n", index.size()));
			sb.append(String.format("Max Postings:\t%d\n", max));
			sb.append(String.format("Min Postings:\t%d\n", min));
			sb.append(String.format("Avg Postings:\t%f", avg_records));
		}
		return sb.toString();
	}

	public void read(ObjectInputStream ois) throws Exception {
		{
			int size = ois.readInt();
			srs = new HashMap<Integer, StringRecord>(size);
			for (int i = 0; i < size; i++) {
				StringRecord sr = new StringRecord();
				sr.read(ois);
				srs.put(sr.getId(), sr);
			}
		}

		int q = ois.readInt();
		gg = new GramGenerator(q);
		gramIndexer = IOUtils.readIndexer(ois);

		int size1 = ois.readInt();
		index = new SetMap<Integer, Integer>(false, false, size1);

		for (int i = 0; i < size1; i++) {
			int gid = ois.readInt();
			int size2 = ois.readInt();
			Set<Integer> rids = new HashSet<Integer>(size2);
			for (int j = 0; j < size2; j++) {
				int rid = ois.readInt();
				rids.add(rid);
			}
			index.set(gid, rids);
		}
	}

	public Counter<StringRecord> search(String s) {
		Gram[] grams = gg.generate(String.format("<%s>", s));

		if (grams.length == 0) {
			return new Counter<StringRecord>();
		}

		Counter<Integer> c = new Counter<Integer>();

		for (int i = 0; i < grams.length; i++) {
			int gid = gramIndexer.indexOf(grams[i].getString());
			if (gid < 0) {
				continue;
			}
			Set<Integer> postings = index.get(gid, false);

			if (postings != null) {
				for (int rid : postings) {
					c.incrementCount(rid, 1);
				}
			}
		}

		Counter<StringRecord> ret = new Counter<StringRecord>();
		SmithWaterman sw = new SmithWaterman();

		for (int rid : c.keySet()) {
			StringRecord sr = srs.get(rid);
			ret.setCount(sr, sw.getNormalizedScore(s, sr.getString()));
		}
		return ret;
	}

	public void write(ObjectOutputStream oos) throws Exception {
		{
			oos.writeInt(srs.size());
			for (StringRecord sr : srs.values()) {
				sr.write(oos);
			}
		}

		oos.writeInt(gg.getQ());
		IOUtils.write(oos, gramIndexer);

		{
			oos.writeInt(index.size());
			Iterator<Integer> iter1 = index.keySet().iterator();

			while (iter1.hasNext()) {
				int gid = iter1.next();
				Set<Integer> rids = index.get(gid);

				oos.writeInt(gid);
				oos.writeInt(rids.size());

				Iterator<Integer> iter2 = rids.iterator();
				while (iter2.hasNext()) {
					oos.writeInt(iter2.next());
				}
			}
		}
		oos.flush();
	}

	public void write(String fileName) throws Exception {
		BufferedWriter writer = IOUtils.openBufferedWriter(fileName);
		writer.close();
	}
}
