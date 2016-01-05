package ohs.entity;

import java.io.BufferedWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
import ohs.types.ListMap;
import ohs.utils.StopWatch;

/**
 * 
 * Implementation of
 * "A Pivotal Prefix Based Filtering Algorithm for String Similarity Search" at
 * SIGMOD'14
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

	private ListMap<Integer, Integer> index;

	private Indexer<String> gramIndexer;

	private GramGenerator gramGenerator;

	public SimpleStringSearcher() {
		this(3);
	}

	public SimpleStringSearcher(int q) {
		gramGenerator = new GramGenerator(q);
	}

	public void filter() {
		System.out.println("filter index.");
		double filter_ratio = 0.3;
		double max = -Double.MAX_VALUE;
		int num_filtered = 0;

		Iterator<Integer> iter = index.keySet().iterator();
		while (iter.hasNext()) {
			int qid = iter.next();
			List<Integer> rids = index.get(qid);

			double ratio = 1f * rids.size() / srs.size();

			if (ratio >= filter_ratio) {
				rids.clear();
				iter.remove();
				num_filtered++;
			}

			max = Math.max(ratio, max);
		}
	}

	public GramGenerator getGramGenerator() {
		return gramGenerator;
	}

	public Map<Integer, StringRecord> getStringRecords() {
		return srs;
	}

	public void index(List<StringRecord> input, boolean append) {
		System.out.printf("index [%s] records.\n", input.size());

		if (index == null && !append) {
			gramIndexer = new Indexer<String>();
			index = new ListMap<Integer, Integer>(10000, false, false);
			srs = new HashMap<Integer, StringRecord>();
		}

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int num_chunks = input.size() / 100;

		for (int i = 0; i < input.size(); i++) {

			if ((i + 1) % num_chunks == 0) {
				int progess = (int) ((i + 1f) / input.size() * 100);
				System.out.printf("\r[%d percent, %s]", progess, stopWatch.stop());
			}

			StringRecord sr = input.get(i);
			Gram[] grams = gramGenerator.generate(String.format("<%s>", sr.getString()));
			if (grams.length == 0) {
				continue;
			}

			Set<Integer> gids = new HashSet<Integer>();

			for (int j = 0; j < grams.length; j++) {
				gids.add(gramIndexer.getIndex(grams[j].getString()));
			}

			for (int gid : gids) {
				index.put(gid, sr.getId());
			}

			srs.put(sr.getId(), sr);
		}

		System.out.printf("\r[%d percent, %s]\n", 100, stopWatch.stop());

		for (int gid : index.keySet()) {
			Collections.sort(index.get(gid));
		}
	}

	public String info() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("string records:\t%d\n", srs.size()));

		{
			Counter<Integer> c = new Counter<Integer>();

			int max = -Integer.MAX_VALUE;
			int min = Integer.MAX_VALUE;
			double num_chars = 0;

			for (StringRecord sr : srs.values()) {
				c.incrementCount(sr.getString().length(), 1);
				max = Math.max(max, sr.getString().length());
				min = Math.min(min, sr.getString().length());
				num_chars += sr.getString().length();
			}
			double avg_chars = num_chars / srs.size();
			sb.append(String.format("max record length:\t%d\n", max));
			sb.append(String.format("min record length:\t%d\n", min));
			sb.append(String.format("avg record length:\t%f\n", avg_chars));
		}

		{
			int max = -Integer.MAX_VALUE;
			int min = Integer.MAX_VALUE;
			int num_records = 0;

			for (int qid : index.keySet()) {
				List<Integer> rids = index.get(qid);
				max = Math.max(max, rids.size());
				min = Math.min(min, rids.size());
				num_records += rids.size();
			}
			double avg_records = 1f * num_records / index.size();
			sb.append(String.format("q-grams:\t%d\n", index.size()));
			sb.append(String.format("max postings:\t%d\n", max));
			sb.append(String.format("min Postings:\t%d\n", min));
			sb.append(String.format("avg Postings:\t%f", avg_records));
		}
		return sb.toString();
	}

	public void read(ObjectInputStream ois) throws Exception {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int size = ois.readInt();
		srs = new HashMap<Integer, StringRecord>(size);
		for (int i = 0; i < size; i++) {
			StringRecord sr = new StringRecord();
			sr.read(ois);
			srs.put(sr.getId(), sr);
		}

		int q = ois.readInt();
		gramGenerator = new GramGenerator(q);
		gramIndexer = IOUtils.readIndexer(ois);

		int size1 = ois.readInt();
		index = new ListMap<Integer, Integer>(size1, false, false);

		for (int i = 0; i < size1; i++) {
			int gid = ois.readInt();
			int size2 = ois.readInt();
			List<Integer> rids = new ArrayList<Integer>(size2);
			for (int j = 0; j < size2; j++) {
				int rid = ois.readInt();
				rids.add(rid);
			}
			index.set(gid, rids);
		}

		System.out.printf("read string searcher in [%s]\n", stopWatch.stop());
	}

	public Counter<StringRecord> search(String s) {
		Gram[] grams = gramGenerator.generate(String.format("<%s>", s));

		if (grams.length == 0) {
			return new Counter<StringRecord>();
		}

		Counter<Integer> c = new Counter<Integer>();

		for (int i = 0; i < grams.length; i++) {
			int gid = gramIndexer.indexOf(grams[i].getString());
			if (gid < 0) {
				continue;
			}
			List<Integer> rids = index.get(gid, false);

			if (rids != null) {
				for (int rid : rids) {
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
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		oos.writeInt(srs.size());
		for (StringRecord sr : srs.values()) {
			sr.write(oos);
		}

		oos.writeInt(gramGenerator.getQ());
		IOUtils.write(oos, gramIndexer.getObjects());

		oos.writeInt(index.size());
		Iterator<Integer> iter = index.keySet().iterator();

		while (iter.hasNext()) {
			int gid = iter.next();
			oos.writeInt(gid);

			List<Integer> rids = index.get(gid);
			oos.writeInt(rids.size());
			for (int i = 0; i < rids.size(); i++) {
				oos.writeInt(rids.get(i));
			}
		}
		oos.flush();

		System.out.printf("write string searcher in [%s]\n", stopWatch.stop());
	}

	public void write(String fileName) throws Exception {
		BufferedWriter writer = IOUtils.openBufferedWriter(fileName);
		writer.close();
	}
}
