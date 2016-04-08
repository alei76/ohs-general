package ohs.string.sim;

import java.io.Serializable;

public interface Sequence<E> extends Serializable {

	public E get(int i);

	public int length();

}
