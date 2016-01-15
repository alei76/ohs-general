package ohs.entity;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.IndexFieldName;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.medical.general.MIRPath;
import ohs.ir.medical.general.SearcherUtils;
import ohs.ir.medical.general.WordCountBox;
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

		String inputFileName = ENTPath.NAME_PERSON_FILE;
		String outputFileName = ENTPath.ENTITY_LINKER_FILE;

		if (inputFileName.contains("_orgs")) {
			outputFileName = outputFileName.replace(".ser", "_org.ser");
		} else if (inputFileName.contains("_locs")) {
			outputFileName = outputFileName.replace(".ser", "_loc.ser");
		} else if (inputFileName.contains("_pers")) {
			outputFileName = outputFileName.replace(".ser", "_per.ser");
		}

		EntityLinker el = new EntityLinker();
		el.train(inputFileName);
		el.write(outputFileName);
		el.read(outputFileName);
		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		IndexSearcher is = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		List<Entity> ents = new ArrayList<Entity>(el.getEntityMap().values());

		TextFileWriter writer = new TextFileWriter(ENTPath.EX_FILE);

		{
			String[] orgs = { "IBM", "kisti", "kaist", "seoul national", "samsung", "lg", "apple", "hyundai", "kist", "sk" };

			for (int i = 0; i < orgs.length; i++) {

				Counter<Entity> scores = el.link(orgs[i]);
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

				// Counter<Entity> scores = el.link(ent.getText(), AnalyzerUtils.getWordCounts(ent.getText(), analyzer), is);
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

	private Indexer<String> featInexer;

	private Map<Integer, SparseVector> topicWordData;

	private WeakHashMap<Integer, SparseVector> cache;

	private CounterMap<Integer, Integer> candidates;

	private boolean makeLog = false;

	private Indexer<String> wordIndexer;

	private Map<Integer, SparseVector> contexts;

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

	public StringSearcher getStringSearcher() {
		return strSearcher;
	}

	public Counter<Entity> link(String mention) throws Exception {
		return link(mention, null, null);
	}

	public Counter<Entity> link(String mention, Counter<String> features) throws Exception {
		return link(mention, features, null);
	}

	public Counter<Entity> link(String mention, Counter<String> features, IndexSearcher is) throws Exception {
		strSearcher.setMakeLog(makeLog);

		Counter<StringRecord> srs = strSearcher.search(mention.toLowerCase());

		candidates = Generics.newCounterMap();
		for (StringRecord sr : srs.keySet()) {
			int rid = sr.getId();
			candidates.incrementCount(recToEnt.get(rid), rid, srs.getCount(sr));
		}

		// for (Entry<Integer, Counter<Integer>> e : candidates.getEntrySet()) {
		// int eid = e.getKey();
		// List<Integer> rids = e.getValue().getSortedKeys();
		//
		// List<Integer> temp = Generics.newArrayList();
		//
		// for (int rid : rids) {
		// if (abbrRecIds.contains(rid)) {
		// temp.add(rid);
		// }
		// }
		// }

		SparseVector cv = null;

		if (features != null && features.size() > 0) {
			cv = VectorUtils.toSparseVector(features, featInexer);
			VectorMath.unitVector(cv);
		}

		Counter<Integer> scores = Generics.newCounter();

		for (int eid : candidates.keySet()) {
			double score = candidates.getCounter(eid).average();
			SparseVector tv = topicWordData.get(eid);

			if (cv != null) {
				double cosine = VectorMath.dotProduct(cv, tv, false);
				score *= Math.exp(cosine);
			}
			scores.setCount(eid, score);
		}

		if (is != null) {
			Indexer<String> wordIndexer = new Indexer<String>();

			cv = VectorUtils.toSparseVector(features, wordIndexer, true);
			VectorMath.unitVector(cv);

			Map<Integer, SparseVector> docWordWeights = Generics.newHashMap();

			for (int eid : scores.keySet()) {
				Counter<String> c = WordCountBox.getWordCounts(is.getIndexReader(), eid, IndexFieldName.CONTENT);
				docWordWeights.put(eid, VectorUtils.toSparseVector(c, wordIndexer, true));
			}
			SparseVector docFreqs = WordCountBox.getDocFreqs(is.getIndexReader(), IndexFieldName.CONTENT, wordIndexer);

			for (int eid : docWordWeights.keySet()) {
				SparseVector wcs = docWordWeights.get(eid);
				for (int j = 0; j < wcs.size(); j++) {
					int w = wcs.indexAtLoc(j);
					double cnt = wcs.valueAtLoc(j);
					double tf = Math.log(cnt) + 1;
					double doc_freq = docFreqs.value(w);
					double num_docs = is.getIndexReader().maxDoc();
					double idf = doc_freq == 0 ? 0 : Math.log((num_docs + 1) / doc_freq);
					double tfidf = tf * idf;
					wcs.setAtLoc(j, tfidf);
				}
				VectorMath.unitVector(wcs);
			}

			for (int eid : docWordWeights.keySet()) {
				double score = scores.getCount(eid);
				double cosine = VectorMath.dotProduct(cv, docWordWeights.get(eid));
				scores.setCount(eid, score * Math.exp(cosine));
			}
		}

		Counter<Entity> ret = new Counter<Entity>();
		for (int eid : scores.keySet()) {
			ret.setCount(ents.get(eid), scores.getCount(eid));
		}
		return ret;
	}

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int size = ois.readInt();
		ents = Generics.newHashMap(size);
		for (int i = 0; i < size; i++) {
			Entity ent = new Entity();
			ent.read(ois);
			ents.put(ent.getId(), ent);
		}

		featInexer = FileUtils.readIndexer(ois);

		recToEnt = FileUtils.readIntegers(ois);

		int size3 = ois.readInt();
		topicWordData = Generics.newHashMap(size3);

		for (int i = 0; i < size3; i++) {
			int id = ois.readInt();
			SparseVector sv = new SparseVector();
			sv.read(ois);
			topicWordData.put(id, sv);
		}

		strSearcher = new StringSearcher();
		strSearcher.read(ois);
		ois.close();

		System.out.printf("read [%s] - [%s]\n", getClass().getName(), stopWatch.stop());
	}

	public void readContexts(String contextFileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(contextFileName);
		wordIndexer = FileUtils.readIndexer(ois);
		contexts = Generics.newHashMap();
		for (SparseVector sv : SparseVector.readList(ois)) {
			contexts.put((int) sv.label(), sv);
		}

		ois.close();
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

		featInexer = Generics.newIndexer();
		topicWordData = Generics.newHashMap();

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

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

			if (catStr.equals("none")) {
				topicWordData.put(id, new SparseVector());
			} else {
				SparseVector sv = VectorUtils.toSparseVector(c, featInexer, true);
				VectorMath.unitVector(sv);
				topicWordData.put(id, sv);
			}

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

		FileUtils.write(oos, featInexer);
		FileUtils.writeIntegers(oos, recToEnt);
		// for (Entry<Integer, Integer> e : recToEnt.entrySet()) {
		// oos.writeInt(e.getKey());
		// oos.writeInt(e.getValue());
		// }

		oos.writeInt(topicWordData.size());
		for (int id : topicWordData.keySet()) {
			SparseVector sv = topicWordData.get(id);
			oos.writeInt(id);
			sv.write(oos);
		}

		strSearcher.write(oos);
		oos.close();

		System.out.printf("write [%s] - [%s]\n", getClass().getName(), stopWatch.stop());
	}

}
