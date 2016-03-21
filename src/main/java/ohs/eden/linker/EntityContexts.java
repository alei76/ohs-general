package ohs.eden.linker;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import ohs.io.FileUtils;
import ohs.math.VectorMath;
import ohs.matrix.SparseVector;
import ohs.types.Indexer;
import ohs.utils.Generics;
import ohs.utils.StopWatch;

public class EntityContexts {
	private Indexer<String> wordIndexer;

	private Map<Integer, SparseVector> contVecs;

	public EntityContexts() {
		wordIndexer = new Indexer<String>();
		contVecs = Generics.newHashMap();
	}

	public EntityContexts(Indexer<String> wordIndexer, Map<Integer, SparseVector> contVecs) {
		this.wordIndexer = wordIndexer;
		this.contVecs = contVecs;
	}

	public Map<Integer, SparseVector> getContextVectors() {
		return contVecs;
	}

	public Indexer<String> getWordIndexer() {
		return wordIndexer;
	}

	public void read(ObjectInputStream ois) throws Exception {
		StopWatch stopWatch = StopWatch.newStopWatch();
		stopWatch.start();

		wordIndexer = FileUtils.readStrIndexer(ois);
		int size = ois.readInt();
		for (int i = 0; i < size; i++) {
			SparseVector sv = new SparseVector();
			sv.read(ois);
			contVecs.put(sv.label(), sv);

			VectorMath.unitVector(sv);
		}
		System.out.printf("read [%text] - [%text]\n", this.getClass().getName(), stopWatch.stop());
	}

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		read(ois);
		ois.close();
	}

	public void setContextVectors(Map<Integer, SparseVector> conVecs) {
		this.contVecs = conVecs;
	}

	public void setWordIndexer(Indexer<String> wordIndexer) {
		this.wordIndexer = wordIndexer;
	}

	public void write(ObjectOutputStream oos) throws Exception {
		StopWatch stopWatch = StopWatch.newStopWatch();
		stopWatch.start();

		FileUtils.writeStrIndexer(oos, wordIndexer);
		oos.writeInt(contVecs.size());
		for (SparseVector sv : contVecs.values()) {
			sv.write(oos);
		}

		System.out.printf("write [%text] - [%text]\n", this.getClass().getName(), stopWatch.stop());
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		write(oos);
		oos.close();
	}
}
