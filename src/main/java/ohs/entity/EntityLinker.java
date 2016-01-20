package ohs.entity;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.apache.lucene.analysis.Analyzer;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.string.search.ppss.StringRecord;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.Generics;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

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

		String inputFileName = ENTPath.TITLE_FILE;
		String entityLinkerFileName = ENTPath.ENTITY_LINKER_FILE;
		String entityContextFileName = ENTPath.ENTITY_CONTEXT_FILE;

		if (inputFileName.contains("_org")) {
			entityLinkerFileName = entityLinkerFileName.replace(".ser", "_org.ser");
			entityContextFileName = entityContextFileName.replace(".ser", "_org.ser");
		} else if (inputFileName.contains("_loc")) {
			entityLinkerFileName = entityLinkerFileName.replace(".ser", "_loc.ser");
			entityContextFileName = entityContextFileName.replace(".ser", "_loc.ser");
		} else if (inputFileName.contains("_per")) {
			entityLinkerFileName = entityLinkerFileName.replace(".ser", "_per.ser");
			entityContextFileName = entityContextFileName.replace(".ser", "_per.ser");
		} else if (inputFileName.contains("titles")) {
			entityLinkerFileName = entityLinkerFileName.replace(".ser", "_title.ser");
			entityContextFileName = entityContextFileName.replace(".ser", "_title.ser");
		}

		EntityLinker el = new EntityLinker();
		// el.train(inputFileName);
		// el.write(entityLinkerFileName);
		el.read(entityLinkerFileName);

		if (FileUtils.exists(entityContextFileName)) {
			EntityContexts entContexts = new EntityContexts();
			entContexts.read(entityContextFileName);
			el.setEntityContexts(entContexts);
		}

		el.setAnalyzer(MedicalEnglishAnalyzer.newAnalyzer());

		List<Entity> ents = new ArrayList<Entity>(el.getEntityMap().values());

		TextFileWriter writer = new TextFileWriter(ENTPath.EX_FILE);

		{
			String[] orgs = { "IBM", "kisti", "kaist", "seoul national", "samsung", "lg", "apple", "hyundai", "kist",
					"sk" };

			String contextStr = "company organization";

			for (int i = 0; i < orgs.length; i++) {

				Counter<Entity> scores = el.link(orgs[i], contextStr);
				scores.keepTopNKeys(10);

				writer.write("====== input ======" + "\n");
				writer.write(orgs[i] + "\n");
				writer.write("====== candidates ======" + "\n");
				for (Entity e : scores.getSortedKeys()) {
					writer.write(e.toString() + "\t" + scores.getCount(e) + "\n");
				}
				writer.write("\n\n");
			}
		}

		{
			for (int i = 0; i < ents.size() && i < 100; i++) {
				Entity ent = ents.get(i);

				List<String> words = StrUtils.split(ent.getText());

				Collections.shuffle(words);

				String s = StrUtils.join(" ", words);

				// Counter<Entity> scores = el.link(ent.getText(),
				// AnalyzerUtils.getWordCounts(ent.getText(), analyzer), is);
				Counter<Entity> scores = el.link(s);
				scores.keepTopNKeys(10);

				writer.write("====== input ======" + "\n");
				writer.write(ent.toString() + "\n");
				writer.write(s + "\n");
				writer.write("====== candidates ======" + "\n");
				for (Entity e : scores.getSortedKeys()) {
					writer.write(e.toString() + "\t" + scores.getCount(e) + "\n");
				}
				writer.write("\n\n");
			}
		}

		writer.close();

		System.out.println("process ends.");
	}

	private StringSearcher strSearcher;

	private Map<Integer, Entity> ents;

	private List<Integer> recToEnt;

	private WeakHashMap<Integer, SparseVector> cache;

	private CounterMap<Integer, Integer> candidates;

	private boolean makeLog = false;

	private EntityContexts entContexts;

	private Analyzer analyzer;

	private StringBuffer logBuff;

	public EntityLinker() {
		cache = Generics.newWeakHashMap(10000);

	}

	public String getAbbreviation(String name) {
		String[] words = name.split(" ");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			if (Character.isUpperCase(word.charAt(0))) {
				sb.append(word.charAt(0));
			}
		}

		String ret = null;

		if (words.length == sb.length() && sb.length() > 2) {
			ret = sb.toString();
		}
		return ret;
	}

	public CounterMap<Integer, Integer> getCandidates() {
		return candidates;
	}

	public Map<Integer, Entity> getEntityMap() {
		return ents;
	}

	public StringBuffer getLogBuffer() {
		return logBuff;
	}

	public StringSearcher getStringSearcher() {
		return strSearcher;
	}

	public Counter<Entity> link(String mention) throws Exception {
		return link(mention, new Counter<String>());
	}

	public Counter<Entity> link(String mention, Counter<String> contextWordCounts) throws Exception {
		strSearcher.setMakeLog(makeLog);

		Counter<StringRecord> srs = strSearcher.search(mention.toLowerCase());

		candidates = Generics.newCounterMap();
		for (StringRecord sr : srs.keySet()) {
			int rid = sr.getId();
			candidates.incrementCount(recToEnt.get(rid), rid, srs.getCount(sr));
		}

		Counter<Integer> scores = Generics.newCounter();

		for (int eid : candidates.keySet()) {
			double score = candidates.getCounter(eid).average();
			scores.setCount(eid, score);
		}

		if (entContexts != null && contextWordCounts != null && contextWordCounts.size() > 0) {

			Indexer<String> wordIndexer = entContexts.getWordIndexer();
			SparseVector isv = VectorUtils.toSparseVector(contextWordCounts, wordIndexer);
			VectorMath.unitVector(isv);

			for (Entry<Integer, Double> e : scores.entrySet()) {
				int eid = e.getKey();
				double score = e.getValue();
				SparseVector esv = entContexts.getContextVectors().get(eid);
				double cosine = 0;

				if (isv.size() > 0 && esv != null) {
					cosine = VectorMath.cosine(isv, esv, false);
				}
				scores.setCount(eid, score * Math.exp(cosine));
			}
		}

		Counter<Entity> ret = new Counter<Entity>();
		for (int eid : scores.keySet()) {
			ret.setCount(ents.get(eid), scores.getCount(eid));
		}
		return ret;
	}

	public Counter<Entity> link(String mention, String context) throws Exception {
		Counter<String> contextWordCounts = AnalyzerUtils.getWordCounts(context, analyzer);
		return link(mention, contextWordCounts);
	}

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		analyzer = MedicalEnglishAnalyzer.newAnalyzer();

		int size = ois.readInt();
		ents = Generics.newHashMap(size);

		for (int i = 0; i < size; i++) {
			Entity ent = new Entity();
			ent.read(ois);
			ents.put(ent.getId(), ent);
		}

		recToEnt = FileUtils.readIntegers(ois);

		strSearcher = new StringSearcher();
		strSearcher.read(ois);
		ois.close();

		System.out.printf("read [%s] - [%s]\n", getClass().getName(), stopWatch.stop());
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public void setEntityContexts(EntityContexts entContexts) {
		this.entContexts = entContexts;
	}

	public void setMakeLog(boolean makeLog) {
		this.makeLog = makeLog;
	}

	public void setTopK(int top_k) {
		strSearcher.setTopK(top_k);
	}

	public void train(String dataFileName) throws Exception {
		List<StringRecord> srs = Generics.newArrayList();
		recToEnt = Generics.newArrayList();
		ents = Generics.newHashMap();

		Analyzer analyzer = MedicalEnglishAnalyzer.newAnalyzer();

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
			String variants = parts[4];

			name = StrUtils.normalizeSpaces(name);

			Counter<String> c = AnalyzerUtils.getWordCounts(catStr, analyzer);

			// if (catStr.equals("none")) {
			// topicWordData.put(id, new SparseVector());
			// } else {
			// SparseVector sv = VectorUtils.toSparseVector(c, featInexer,
			// true);
			// VectorMath.unitVector(sv);
			// topicWordData.put(id, sv);
			// }

			Entity ent = new Entity(id, name, topic);
			ents.put(ent.getId(), ent);

			StringRecord sr = new StringRecord(srs.size(), name.toLowerCase());
			srs.add(sr);

			// recToEnt.put(sr.getId(), ent.getId());

			recToEnt.add(ent.getId());

			// String abbr = getAbbreviation(name);
			//
			// if (abbr != null) {
			// sr = new StringRecord(srs.size(), abbr.toLowerCase());
			// srs.add(sr);
			//
			// recToEnt.add(ent.getId());
			// abbrRecIds.add(sr.getId());
			// }

			if (!variants.equals("none")) {
				for (String var : variants.split("\\|")) {
					var = StrUtils.normalizeSpaces(var);
					sr = new StringRecord(srs.size(), var.toLowerCase());
					srs.add(sr);

					recToEnt.add(ent.getId());

					// abbr = getAbbreviation(var);
					//
					// if (abbr != null) {
					// sr = new StringRecord(srs.size(), abbr.toLowerCase());
					// srs.add(sr);
					//
					// recToEnt.add(ent.getId());
					// abbrRecIds.add(sr.getId());
					// }
				}
			}
		}

		if (recToEnt.size() != srs.size()) {
			System.out.println("wrong records!!");
			System.exit(0);
		}

		// TermWeighting.computeTFIDFs(topicWordData);

		System.out.printf("read [%d] records from [%d] entities at [%s].\n", srs.size(), ents.size(), dataFileName);
		strSearcher = new StringSearcher(3);
		strSearcher.index(srs, false);
		System.out.println(strSearcher.info() + "\n");

		// strSearcher.filter();
		// System.out.println(strSearcher.info() + "\n");
	}

	public void write(String fileName) throws Exception {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		oos.writeInt(ents.size());
		for (Entity ent : ents.values()) {
			ent.write(oos);
		}

		FileUtils.writeIntegers(oos, recToEnt);

		strSearcher.write(oos);
		oos.close();

		System.out.printf("write [%s] - [%s]\n", getClass().getName(), stopWatch.stop());
	}

}
