package ohs.entity;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Struct;
import java.util.ArrayList;
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
		EntityLinker el = new EntityLinker();

		// if (FileUtils.exists(ENTPath.ENTITY_LINKER_FILE)) {
		// el.read(ENTPath.ENTITY_LINKER_FILE);
		// } else {
		el.train(ENTPath.NAME_ORGANIZATION_FILE);
		el.write(ENTPath.ENTITY_LINKER_FILE);
		el.read(ENTPath.ENTITY_LINKER_FILE);
		// el.setTopK(20);

		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		IndexSearcher is = SearcherUtils.getIndexSearcher(MIRPath.WIKI_INDEX_DIR);

		List<Entity> ents = new ArrayList<Entity>(el.getEntities().values());

		TextFileWriter writer = new TextFileWriter(ENTPath.EX_FILE);

		{
			String[] orgs = { "IBM", "kisti", "kaist", "seoul national", "samsung", "lg", "apple", "hyundai" };

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
			for (int i = 0; i < ents.size() && i < 50; i++) {
				Entity ent = ents.get(i);
				// Counter<Entity> scores = el.link(ent.getText(), AnalyzerUtils.getWordCounts(ent.getText(), analyzer), is);
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
		}

		writer.close();

		System.out.println("process ends.");
	}

	private StringSearcher searcher;

	private Map<Integer, Entity> ents;

	private List<Integer> recToEntIdMap;

	private Indexer<String> featInexer;

	private Map<Integer, SparseVector> topicWordData;

	private WeakHashMap<Integer, SparseVector> cache;

	private Set<Integer> abbrRecIds;

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

	public Map<Integer, Entity> getEntities() {
		return ents;
	}

	public Counter<Entity> link(String mention) throws Exception {
		return link(mention, null, null);
	}

	public Counter<Entity> link(String mention, Counter<String> features) throws Exception {
		return link(mention, features, null);
	}

	public Counter<Entity> link(String mention, Counter<String> features, IndexSearcher is) throws Exception {
		Counter<StringRecord> srs = searcher.search(mention.toLowerCase());

		CounterMap<Integer, Integer> cm = Generics.newCounterMap();
		for (StringRecord sr : srs.keySet()) {
			int rid = sr.getId();
			cm.incrementCount(recToEntIdMap.get(rid), rid, srs.getCount(sr));
		}

		// for (Entry<Integer, Counter<Integer>> e : cm.getEntrySet()) {
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

		for (int eid : cm.keySet()) {
			double score = cm.getCounter(eid).average();
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

		recToEntIdMap = FileUtils.readIntegers(ois);

		int size3 = ois.readInt();
		topicWordData = Generics.newHashMap(size3);

		for (int i = 0; i < size3; i++) {
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

	public void setTopK(int top_k) {
		searcher.setTopK(top_k);
	}

	public void train(String dataFileName) throws Exception {
		List<StringRecord> srs = Generics.newArrayList();
		recToEntIdMap = Generics.newArrayList();
		ents = Generics.newHashMap();

		featInexer = Generics.newIndexer();
		topicWordData = Generics.newHashMap();
		abbrRecIds = Generics.newHashSet();

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
			String variantStr = parts[4];

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

			// recToEntIdMap.put(sr.getId(), ent.getId());

			recToEntIdMap.add(ent.getId());

			// String abbr = getAbbreviation(name);
			//
			// if (abbr != null) {
			// sr = new StringRecord(srs.size(), abbr.toLowerCase());
			// srs.add(sr);
			//
			// recToEntIdMap.add(ent.getId());
			// abbrRecIds.add(sr.getId());
			// }

			if (!variantStr.equals("none")) {
				for (String var : variantStr.split("\\|")) {
					var = StrUtils.normalizeSpaces(var);
					sr = new StringRecord(srs.size(), var.toLowerCase());
					srs.add(sr);

					recToEntIdMap.add(ent.getId());

					// abbr = getAbbreviation(var);
					//
					// if (abbr != null) {
					// sr = new StringRecord(srs.size(), abbr.toLowerCase());
					// srs.add(sr);
					//
					// recToEntIdMap.add(ent.getId());
					// abbrRecIds.add(sr.getId());
					// }
				}
			}
		}

		if (recToEntIdMap.size() != srs.size()) {
			System.out.println("wrong records!!");
			System.exit(0);
		}

		// TermWeighting.computeTFIDFs(topicWordData);

		System.out.printf("read [%d] records from [%d] entities at [%s].\n", srs.size(), ents.size(), dataFileName);
		searcher = new StringSearcher(3);
		searcher.index(srs, false);
		System.out.println(searcher.info() + "\n");

		// searcher.filter();
		// System.out.println(searcher.info() + "\n");
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
		FileUtils.writeIntegers(oos, recToEntIdMap);
		// for (Entry<Integer, Integer> e : recToEntIdMap.entrySet()) {
		// oos.writeInt(e.getKey());
		// oos.writeInt(e.getValue());
		// }

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
