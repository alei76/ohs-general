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
import ohs.string.sim.Jaccard;
import ohs.string.sim.Jaro;
import ohs.string.sim.Sequence;
import ohs.string.sim.SimScorer;
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

	private DeepMap<String, Integer, Double[]> cache;

	private int top_k = 100;

	private int q = 3;

	private int tau = 3;

	private int prefix_size = q * tau + 1;

	private int[] gramDFs;

	private List<SimScorer> simScorers;

	private Map<Integer, Double[]> simScores;

	private Counter<Integer> candidates;

	private boolean makeLog = false;

	private StringBuffer logBuff;

	public StringSearcher() {
		this(3);
	}

	public StringSearcher(int q) {
		this.q = q;

		gg = new GramGenerator(q);
		cache = new DeepMap<String, Integer, Double[]>(1000, MapType.WEAK_HASH_MAP, MapType.WEAK_HASH_MAP);

		simScorers = Generics.newArrayList();

		simScorers.add(new EditDistance());
		simScorers.add(new SmithWaterman());
		simScorers.add(new Jaro());
		simScorers.add(new Jaccard());
		simScores = Generics.newHashMap();

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

	public StringBuffer getLogBuffer() {
		return logBuff;
	}

	public List<SimScorer> getSimScorers() {
		return simScorers;
	}

	public Map<Integer, Double[]> getSimScores() {
		return simScores;
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

		Counter<Integer> gidfs = Generics.newCounter();

		for (int i = 0; i < grams.length; i++) {
			int gid = gramIndexer.indexOf(grams[i].getString());
			if (gid < 0) {
				continue;
			}
			int df = gramDFs[gid];
			double idf = Math.log((srs.size() + 1.0) / df);
			gidfs.setCount(gid, idf);
		}

		candidates = Generics.newCounter();

		List<Integer> gids = gidfs.getSortedKeys();

		for (int i = 0; i < gids.size() && i < prefix_size; i++) {
			int gid = gids.get(i);
			List<Integer> rids = index.get(gid, false);
			if (rids != null) {
				double idf = gidfs.getCount(gid);
				for (int rid : rids) {
					candidates.incrementCount(rid, idf);
				}
			}
		}

		logBuff = new StringBuffer();

		if (makeLog) {
			logBuff.append(String.format("Input:\t%s", s));
		}

		Counter<StringRecord> ret = new Counter<StringRecord>();
		simScores = Generics.newHashMap(top_k);

		List<Integer> rids = candidates.getSortedKeys();

		for (int i = 0; i < rids.size() && i < top_k; i++) {
			int rid = rids.get(i);
			StringRecord sr = srs.get(rid);
			double idf_sum = candidates.getCount(rid);

			Sequence ss = new CharacterSequence(s);
			Sequence tt = new CharacterSequence(sr.getString());

			Double[] scores = cache.get(s, sr.getId(), false);

			if (scores == null) {
				scores = new Double[simScorers.size()];
				for (int j = 0; j < simScorers.size(); j++) {
					scores[j] = simScorers.get(j).getSimilarity(ss, tt);
				}
				cache.put(s, sr.getId(), scores);
			}

			simScores.put(sr.getId(), scores);

			double sim_score_sum = 0;
			for (int j = 0; j < scores.length; j++) {
				sim_score_sum += scores[j].doubleValue();
			}
			double avg_sim_score = sim_score_sum / scores.length;
			double score = idf_sum * avg_sim_score;

			if (makeLog) {
				logBuff.append("\n" + sr.getId() + "\t" + sr.getString());
				for (int j = 0; j < scores.length; j++) {
					logBuff.append(String.format("\t%f", scores[j]));
				}
				logBuff.append(String.format("\t%f", avg_sim_score));
				logBuff.append(String.format("\t%f", idf_sum));
				logBuff.append(String.format("\t%f", score));
			}

			ret.setCount(sr, score);
		}
		return ret;
	}

	public void setMakeLog(boolean makeLog) {
		this.makeLog = makeLog;
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
