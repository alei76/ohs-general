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

	public String[][] getValues(TokenAttr attr, String delim, int start, int end) {
		String[][] ret = new String[end - start][];
		for (int i = start; i < end; i++) {
			ret[i] = sents[i].getValues(attr, delim, start, end);
		}
		return ret;
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
