package ohs.ling.types;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Sentence {

	private Token[] tokens;

	public Sentence() {

	}

	public Sentence(Token[] tokens) {
		this.tokens = tokens;
	}

	public Token get(int i) {
		return tokens[i];
	}

	public void read(ObjectInputStream ois) throws Exception {
		tokens = new Token[ois.readInt()];
		for (int i = 0; i < tokens.length; i++) {
			tokens[i].read(ois);
		}
	}

	public int size() {
		return tokens.length;
	}

	@Override
	public String toString() {
		return toString(true);
	}

	public String toString(boolean vertical) {
		StringBuffer sb = new StringBuffer("Loc");

		for (int i = 0; i < TokenAttr.values().length; i++) {
			sb.append(String.format("\t%s", TokenAttr.values()[i]));
		}

		for (int i = 0; i < tokens.length; i++) {
			Token t = tokens[i];
			sb.append(String.format("\n%d", i + 1));
			for (int j = 0; j < TokenAttr.values().length; j++) {
				sb.append(String.format("\t%s", t.getValue(TokenAttr.values()[j])));
			}
		}
		return sb.toString();
	}

	public void write(ObjectOutputStream oos) throws Exception {
		oos.writeInt(tokens.length);
		for (int i = 0; i < tokens.length; i++) {
			tokens[i].write(oos);
		}
	}

}
