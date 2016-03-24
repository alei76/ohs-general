package ohs.nlp.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import ohs.io.FileUtils;

public class KDocumentCollection extends ArrayList<KDocument> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8797495521485834143L;

	public KSentence[] getSentences() {
		List<KSentence> ret = new ArrayList<KSentence>();
		for (KDocument doc : this) {
			for (KSentence sent : doc.getSentences()) {
				ret.add(sent);
			}
		}
		return ret.toArray(new KSentence[ret.size()]);
	}

	public Token[] getTokens() {
		List<Token> ret = new ArrayList<>();
		for (KDocument doc : this) {
			for (KSentence sent : doc.getSentences()) {
				for (Token t : sent.getTokens()) {
					ret.add(t);
				}
			}
		}
		return ret.toArray(new Token[ret.size()]);
	}

	public void read(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();

		for (int i = 0; i < size; i++) {
			KDocument doc = new KDocument();
			doc.read(ois);
			add(doc);
		}
	}

	public void read(String fileName) throws Exception {
		ObjectInputStream ois = FileUtils.openObjectInputStream(fileName);
		read(ois);
		ois.close();
	}

	public int sizeOfTokens() {
		int ret = 0;
		for (int i = 0; i < size(); i++) {
			ret += get(i).sizeOfTokens();
		}
		return ret;
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(this.size());

		for (KDocument doc : this) {
			doc.write(oos);
		}
	}

	public void write(String fileName) throws Exception {
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		write(oos);
		oos.close();
	}
}
