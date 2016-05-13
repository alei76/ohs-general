package ohs.ml.neuralnet;

public class Word2VecParam {

	public static enum Type {
		SKIP_GRAM, CBOW
	}

	private int vec_size;

	private int context_size;

	private Type type;

	public Word2VecParam(Type type, int vec_size, int context_size) {
		this.type = type;
		this.vec_size = vec_size;
		this.context_size = context_size;
	}

	public int getContextSize() {
		return context_size;
	}

	public Type getType() {
		return type;
	}

	public int getVectorSize() {
		return vec_size;
	}

}
