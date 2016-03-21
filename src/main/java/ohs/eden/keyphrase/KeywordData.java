package ohs.eden.keyphrase;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import com.google.common.base.Stopwatch;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.SetMap;
import ohs.utils.Generics;
import ohs.utils.StopWatch;

public class KeywordData {

	private Indexer<String> kwdIndexer;

	private Indexer<String> docIndxer;

	private ListMap<Integer, Integer> keywordDocs;

	private List<Integer> kwdids;

	private int[] kwd_freqs;

	private SetMap<Integer, Integer> clusters;

	private Map<Integer, String> clusterLabel;

	public ListMap<Integer, Integer> getDocIdsList() {
		return keywordDocs;
	}

	public Indexer<String> getDocIndexer() {
		return docIndxer;
	}

	public Indexer<String> getDocumentIndxer() {
		return docIndxer;
	}

	public ListMap<Integer, Integer> getKeywordDocs() {
		return keywordDocs;
	}

	public int[] getKeywordFreqs() {
		return kwd_freqs;
	}

	public Indexer<String> getKeywordIndexer() {
		return kwdIndexer;
	}

	public List<Integer> getKeywords() {
		return kwdids;
	}

	public void read(String fileName) throws Exception {
		StopWatch stopWatch = StopWatch.newStopWatch();

		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		kwdIndexer = FileUtils.readStrIndexer(ois);
		kwdids = FileUtils.readIntList(ois);
		kwd_freqs = FileUtils.readIntArray(ois);

		docIndxer = FileUtils.readStrIndexer(ois);
		keywordDocs = FileUtils.readIntListMap(ois);

		clusters = FileUtils.readIntSetMap(ois);
		clusterLabel = FileUtils.readIntStrMap(ois);

		ois.close();

		System.out.printf("read [%text] at [%text] - [%text]\n", getClass().getName(), fileName, stopWatch.stop());
	}

	public void readText(String fileName) {
		StopWatch stopWatch = StopWatch.newStopWatch();

		kwdIndexer = Generics.newIndexer();
		docIndxer = Generics.newIndexer();

		kwdids = Generics.newArrayList();
		keywordDocs = Generics.newListMap();

		clusters = Generics.newSetMap();
		clusterLabel = Generics.newHashMap();

		Counter<Integer> kwdFreqs = Generics.newCounter();

		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();

			if (line.startsWith(FileUtils.LINE_SIZE)) {
				int size = Integer.parseInt(line.split("\t")[1]);
				kwdids = Generics.newArrayList(size);
				keywordDocs = Generics.newListMap(size);
				continue;
			}

			String[] parts = line.split("\t");

			String kor = parts[0];
			String eng = parts[1];
			double kwd_freq = Double.parseDouble(parts[2]);

			String keyword = kor + "\t" + eng;

			int kwdid = kwdIndexer.getIndex(keyword);
			kwdids.add(kwdid);
			kwdFreqs.setCount(kwdid, kwd_freq);

			List<Integer> docids = Generics.newArrayList();

			for (int i = 3; i < parts.length; i++) {
				String cn = parts[i];
				docids.add(docIndxer.getIndex(cn));
			}
			keywordDocs.put(kwdid, docids);
		}
		reader.close();

		kwd_freqs = new int[kwdFreqs.size()];

		for (int i = 0; i < kwdIndexer.size(); i++) {
			kwd_freqs[i] = (int) kwdFreqs.getCount(i);
		}

		System.out.printf("read [%text] at [%text] - [%text]\n", getClass().getName(), fileName, stopWatch.stop());
	}

	public void setClusterLabel(Map<Integer, String> clusterLabel) {
		this.clusterLabel = clusterLabel;
	}

	public void setClusters(SetMap<Integer, Integer> clusters) {
		this.clusters = clusters;
	}

	public void write(String fileName) throws Exception {
		System.out.printf("write [%text] at [%text]\n", getClass().getName(), fileName);

		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);

		FileUtils.writeStrIndexer(oos, kwdIndexer);
		FileUtils.writeIntCollection(oos, kwdids);
		FileUtils.writeIntArray(oos, kwd_freqs);

		FileUtils.writeStrIndexer(oos, docIndxer);
		FileUtils.writeIntListMap(oos, keywordDocs);

		FileUtils.writeIntSetMap(oos, clusters);
		FileUtils.writeIntStrMap(oos, clusterLabel);

		oos.close();
	}
}
