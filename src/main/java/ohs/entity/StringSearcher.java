package ohs.entity;

import java.io.BufferedWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.string.search.ppss.StringRecord;
import ohs.string.sim.CharacterSequence;
import ohs.string.sim.EditDistance;
import ohs.string.sim.Jaro;
import ohs.string.sim.JaroWinkler;
import ohs.string.sim.Sequence;
import ohs.string.sim.SmithWaterman;
import ohs.types.Counter;
import ohs.types.DeepMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.utils.Generics;
import ohs.utils.Generics.ListType;
import ohs.utils.Generics.MapType;
import ohs.utils.StopWatch;

/**
 * 
 * @author Heung-Seon Oh
 */
public class StringSearcher implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8740333778747553831L;

	private Map<Integer, StringRecord> srs;

	private ListMap<Integer, Integer> index;

	private Indexer<String> gramIndexer;

	private GramGenerator gg;

	private DeepMap<String, Integer, Double> cache;

	private int top_k = 100;

	private int q = 3;

	private int tau = 3;

	private int prefix_size = q * tau + 1;

	private int[] gramDFs;

	public StringSearcher() {
		this(3);
	}

	public StringSearcher(int q) {
		this.q = q;

		gg = new GramGenerator(q);
		cache = new DeepMap<String, Integer, Double>(1000, MapType.WEAK_HASH_MAP, MapType.WEAK_HASH_MAP);
	}

	private void buildGramIndexer(List<StringRecord> input) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int num_chunks = input.size() / 100;

		Counter<Integer> temp = Generics.newCounter();

		if (gramDFs.length > 0) {
			for (int i = 0; i < gramDFs.length; i++) {
				temp.incrementCount(i, gramDFs[i]);
			}
		}

		for (int i = 0; i < input.size(); i++) {

			if ((i + 1) % num_chunks == 0) {
				int progess = (int) ((i + 1f) / input.size() * 100);
				System.out.printf("\r[%d percent, %s]", progess, stopWatch.stop());
			}

			StringRecord sr = input.get(i);
			Gram[] grams = gg.generate(String.format("<%s>", sr.getString()));
			if (grams.length == 0) {
				continue;
			}
			Set<Integer> gids = Generics.newHashSet();
			for (int j = 0; j < grams.length; j++) {
				gids.add(gramIndexer.getIndex(grams[j].getString()));
			}

			for (int gid : gids) {
				temp.incrementCount(gid, 1);
			}
		}
		System.out.printf("\r[%d percent, %s]\n", 100, stopWatch.stop());
		System.out.printf("built gram indexer [%d, %s].\n", gramIndexer.size(), stopWatch.stop());

		gramDFs = new int[gramIndexer.size()];
		for (Entry<Integer, Double> e : temp.entrySet()) {
			gramDFs[e.getKey()] = e.getValue().intValue();
		}
	}

	public GramGenerator getGramGenerator() {
		return gg;
	}

	public Map<Integer, StringRecord> getStringRecords() {
		return srs;
	}

	public int getTopK() {
		return top_k;
	}

	public void index(List<StringRecord> input, boolean append) {
		System.out.printf("index [%s] records.\n", input.size());

		if (index == null && !append) {
			gramIndexer = new Indexer<String>();
			index = new ListMap<Integer, Integer>(1000, MapType.HASH_MAP, ListType.ARRAY_LIST);
			srs = Generics.newHashMap();
			gramDFs = new int[0];
		}

		buildGramIndexer(input);
		int num_records = srs.size() + input.size();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int num_chunks = input.size() / 100;

		for (int i = 0; i < input.size(); i++) {

			if ((i + 1) % num_chunks == 0) {
				int progess = (int) ((i + 1f) / input.size() * 100);
				System.out.printf("\r[%d percent, %s]", progess, stopWatch.stop());
			}

			StringRecord sr = input.get(i);
			Gram[] grams = gg.generate(String.format("<%s>", sr.getString()));
			if (grams.length == 0) {
				continue;
			}

			Counter<Integer> gWeights = Generics.newCounter();

			for (int j = 0; j < grams.length; j++) {
				int gid = gramIndexer.indexOf(grams[j].getString());
				if (gid != -1 && !gWeights.containsKey(gid)) {
					int df = gramDFs[gid];
					double idf = Math.log((num_records + 1.0) / df);
					gWeights.setCount(gid, idf);
				}
			}

			List<Integer> gids = gWeights.getSortedKeys();

			int cutoff = prefix_size;

			for (int j = 0; j < gids.size() && j < cutoff; j++) {
				index.put(gids.get(j), sr.getId());
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
				List<Integer> rids = index.get(qid, false);
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
		srs = Generics.newHashMap(size);
		for (int i = 0; i < size; i++) {
			StringRecord sr = new StringRecord();
			sr.read(ois);
			srs.put(sr.getId(), sr);
		}

		top_k = ois.readInt();
		q = ois.readInt();
		tau = ois.readInt();
		prefix_size = ois.readInt();

		gg = new GramGenerator(q);
		gramIndexer = FileUtils.readIndexer(ois);
		gramDFs = FileUtils.readIntegerArray(ois);

		int size3 = ois.readInt();
		index = new ListMap<Integer, Integer>(size3, MapType.HASH_MAP, ListType.ARRAY_LIST);

		for (int i = 0; i < size3; i++) {
			index.set(ois.readInt(), FileUtils.readIntegers(ois));
		}

		System.out.printf("read [%s] - [%s]\n", this.getClass().getName(), stopWatch.stop());
	}

	public Counter<StringRecord> search(String s) {
		Gram[] grams = gg.generate(String.format("<%s>", s));

		if (grams.length == 0) {
			return new Counter<StringRecord>();
		}

		Counter<Integer> temp = Generics.newCounter();

		for (int i = 0; i < grams.length; i++) {
			int gid = gramIndexer.indexOf(grams[i].getString());
			if (gid < 0) {
				continue;
			}
			int df = gramDFs[gid];
			double idf = Math.log((srs.size() + 1.0) / df);
			temp.setCount(gid, idf);
		}

		Counter<Integer> candis = new Counter<Integer>();

		List<Integer> gids = temp.getSortedKeys();
		for (int i = 0; i < gids.size() && i < prefix_size; i++) {
			int gid = gids.get(i);
			List<Integer> rids = index.get(gid, false);
			if (rids != null) {
				double idf = temp.getCount(gid);
				for (int rid : rids) {
					candis.incrementCount(rid, idf);
				}
			}
		}

		SmithWaterman sw = new SmithWaterman();
		EditDistance ed = new EditDistance();
		Jaro jr = new Jaro();
		JaroWinkler jw = new JaroWinkler();

		Counter<StringRecord> ret = new Counter<StringRecord>();
		List<Integer> rids = candis.getSortedKeys();

		for (int i = 0; i < rids.size() && i < top_k; i++) {
			StringRecord sr = srs.get(rids.get(i));
			double score = 0;

			if (cache.containsKeys(s, sr.getId())) {
				score = cache.get(s, sr.getId(), false);
			} else {
				Sequence ss = new CharacterSequence(s);
				Sequence tt = new CharacterSequence(sr.getString());
				double score1 = sw.getSimilarity(ss, tt);
				double score2 = ed.getSimilarity(ss, tt);
				double score3 = jr.getSimilarity(ss, tt);
				double score4 = jw.getSimilarity(ss, tt);
				score = score1 * score2;
				cache.put(s, sr.getId(), score);
			}
			ret.setCount(sr, score);
		}
		return ret;
	}

	public void setTopK(int top_k) {
		this.top_k = top_k;
	}

	public void write(ObjectOutputStream oos) throws Exception {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		oos.writeInt(srs.size());
		for (StringRecord sr : srs.values()) {
			sr.write(oos);
		}

		oos.writeInt(top_k);
		oos.writeInt(q);
		oos.writeInt(tau);
		oos.writeInt(prefix_size);

		FileUtils.writeStrings(oos, gramIndexer.getObjects());
		FileUtils.write(oos, gramDFs);

		oos.writeInt(index.size());
		Iterator<Integer> iter = index.keySet().iterator();

		while (iter.hasNext()) {
			int gid = iter.next();
			oos.writeInt(gid);
			FileUtils.writeIntegers(oos, index.get(gid));
		}
		oos.flush();

		System.out.printf("write [%s] - [%s]\n", this.getClass().getName(), stopWatch.stop());
	}

	public void write(String fileName) throws Exception {
		BufferedWriter writer = FileUtils.openBufferedWriter(fileName);
		writer.close();
	}
}
