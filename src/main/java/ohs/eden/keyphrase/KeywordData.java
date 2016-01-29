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

	private Indexer<String> keywordIndexer;

	private Indexer<String> docIndxer;

	private List<List<Integer>> docIdsList;

	private List<Integer> kids;

	private int[] keyword_freqs;

	public List<List<Integer>> getDocIdsList() {
		return docIdsList;
	}

	public Indexer<String> getDocIndxer() {
		return docIndxer;
	}

	public int[] getKeywordFreqs() {
		return keyword_freqs;
	}

	public List<Integer> getKeywordIds() {
		return kids;
	}

	public Indexer<String> getKeywordIndexer() {
		return keywordIndexer;
	}

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		keywordIndexer = FileUtils.readStrIndexer(ois);
		// docIndxer = FileUtils.readIndexer(ois);
		kids = FileUtils.readIntList(ois);
		keyword_freqs = FileUtils.readIntArray(ois);

		// int size = ois.readInt();
		// docIdsList = Generics.newArrayList(size);
		// for (int i = 0; i < size; i++) {
		// docIdsList.add(FileUtils.readIntegers(ois));
		// }
		ois.close();
	}

	public void readFromText(String fileName) {
		keywordIndexer = Generics.newIndexer();
		docIndxer = Generics.newIndexer();

		kids = Generics.newArrayList();
		docIdsList = Generics.newArrayList();
		Counter<Integer> kwFreqs = Generics.newCounter();

		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();

			if (line.startsWith(FileUtils.LINE_SIZE)) {
				int size = Integer.parseInt(line.split("\t")[1]);
				kids = Generics.newArrayList(size);
				docIdsList = Generics.newArrayList(size);
				continue;
			}

			String[] parts = line.split("\t");

			String kor = parts[0];
			String eng = parts[1];
			double kw_freq = Double.parseDouble(parts[2]);

			String keyword = kor + "\t" + eng;

			int kid = keywordIndexer.getIndex(keyword);
			kids.add(kid);
			kwFreqs.setCount(kid, kw_freq);

			// List<Integer> dids = Generics.newArrayList();
			//
			// for (int i = 3; i < parts.length; i++) {
			// String cn = parts[3];
			// dids.add(docIndxer.getIndex(cn));
			// }
			// docIdsList.add(dids);
		}
		reader.close();

		keyword_freqs = new int[kwFreqs.size()];

		for (int i = 0; i < keywordIndexer.size(); i++) {
			keyword_freqs[i] = (int) kwFreqs.getCount(i);
		}
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);

		FileUtils.writeStrIndexer(oos, keywordIndexer);
		// FileUtils.write(oos, docIndxer);
		FileUtils.writeIntCollection(oos, kids);
		FileUtils.writeIntArray(oos, keyword_freqs);

		// oos.writeInt(docIdsList.size());
		//
		// for (List<Integer> docIds : docIdsList) {
		// FileUtils.writeIntegers(oos, docIds);
		// }
		oos.close();
	}
}
