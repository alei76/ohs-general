package ohs.string.sim;

import java.io.Serializable;

import ohs.types.Counter;
import ohs.utils.Generics;

public interface Sequence extends Serializable {

	public String get(int i);

	public default Counter<String> getTokenCounts() {
		Counter<String> ret = Generics.newCounter();
		for (int i = 0; i < length(); i++) {
			ret.incrementCount(get(i), 1);
		}
		return ret;
	}

	public int length();

}
