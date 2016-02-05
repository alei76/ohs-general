package ohs.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Document {

	private Sentence[] sents;

	public Document() {

	}

	public Document(Sentence[] sents) {
		this.sents = sents;
	}

	public Sentence get(int i) {
		return sents[i];
	}

	public void read(ObjectInputStream ois) throws Exception {
		sents = new Sentence[ois.readInt()];
		for (int i = 0; i < sents.length; i++) {
			sents[i].read(ois);
		}
	}

	public int size() {
		return sents.length;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(sents.length);
		for (int i = 0; i < sents.length; i++) {
			sents[i].write(oos);
		}
	}
}
