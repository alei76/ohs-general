package ohs.eden.keyphrase;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.utils.Generics;

public class KeywordData {

	private Indexer<String> kwdIndexer;

	private Indexer<String> docIndxer;

	private List<List<Integer>> docIdsList;

	private List<Integer> kwdids;

	private int[] kwd_freqs;

	public List<List<Integer>> getDocIdsList() {
		return docIdsList;
	}

	public Indexer<String> getDocumentIndxer() {
		return docIndxer;
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
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		kwdIndexer = FileUtils.readStrIndexer(ois);
		// docIndxer = FileUtils.readIndexer(ois);
		kwdids = FileUtils.readIntList(ois);
		kwd_freqs = FileUtils.readIntArray(ois);

		// int size = ois.readInt();
		// docIdsList = Generics.newArrayList(size);
		// for (int i = 0; i < size; i++) {
		// docIdsList.add(FileUtils.readIntegers(ois));
		// }
		ois.close();
	}

	public void readFromText(String fileName) {
		kwdIndexer = Generics.newIndexer();
		docIndxer = Generics.newIndexer();

		kwdids = Generics.newArrayList();
		docIdsList = Generics.newArrayList();
		Counter<Integer> kwdFreqs = Generics.newCounter();

		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();

			if (line.startsWith(FileUtils.LINE_SIZE)) {
				int size = Integer.parseInt(line.split("\t")[1]);
				kwdids = Generics.newArrayList(size);
				docIdsList = Generics.newArrayList(size);
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

			// List<Integer> dids = Generics.newArrayList();
			//
			// for (int i = 3; i < parts.length; i++) {
			// String cn = parts[3];
			// dids.add(docIndxer.getIndex(cn));
			// }
			// docIdsList.add(dids);
		}
		reader.close();

		kwd_freqs = new int[kwdFreqs.size()];

		for (int i = 0; i < kwdIndexer.size(); i++) {
			kwd_freqs[i] = (int) kwdFreqs.getCount(i);
		}
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);

		FileUtils.writeStrIndexer(oos, kwdIndexer);
		// FileUtils.write(oos, docIndxer);
		FileUtils.writeIntCollection(oos, kwdids);
		FileUtils.writeIntArray(oos, kwd_freqs);

		// oos.writeInt(docIdsList.size());
		//
		// for (List<Integer> docIds : docIdsList) {
		// FileUtils.writeIntegers(oos, docIds);
		// }
		oos.close();
	}
}
