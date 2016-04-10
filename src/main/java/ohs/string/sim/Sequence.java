package ohs.string.sim;

import java.io.Serializable;

public class Sequence<E> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1207132892305932857L;

	private E[] values;

	public Sequence(E[] values) {
		this.values = values;
	}

	public E get(int i) {
		return values[i];
	}

	public E[] values() {
		return values;
	}

	public int length() {
		return values.length;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			sb.append(String.format("%d:\t%s", i, values[i].toString()));
			if (i != values.length) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

}
