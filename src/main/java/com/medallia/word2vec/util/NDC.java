package com.medallia.word2vec.util;


/** Helper to create {@link org.apache.log4j.NDC} for nested diagnostic contexts */
public class NDC implements AC {
	/** Push all the contexts given and pop them when auto-closed */
	public static NDC push(String... context) {
		return new NDC(context);
	}

	private final int size;

	/** Construct an {@link AutoCloseable} {@link NDC} with the given contexts */
	private NDC(String... context) {
		for (String c : context) {
			org.apache.log4j.NDC.push("[" + c + "]");
		}
		this.size = context.length;
	}

	@Override
	public void close() {
		for (int i = 0; i < size; i++) {
			org.apache.log4j.NDC.pop();
		}
	}
}
