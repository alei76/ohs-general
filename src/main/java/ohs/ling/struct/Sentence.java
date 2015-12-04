package ohs.ling.struct;

import java.util.ArrayList;

public class Sentence extends ArrayList<Token> {

	public String toString() {
		return toString(true);
	}

	public String toString(boolean vertical) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < size(); i++) {
			Token t = get(i);
			sb.append(i + "\t" + t.toString());
			if (i != size() - 1) {
				sb.append(vertical ? "\n" : " ");
			}
		}
		return sb.toString();
	}

}
