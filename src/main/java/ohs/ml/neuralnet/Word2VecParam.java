package ohs.ml.neuralnet;

public class Word2VecParam {

	public static enum Type {
		SKIP_GRAM, CBOW
	}

	private int vec_size;

	private Type type;

	public Word2VecParam(int vec_size, Type type) {
		this.vec_size = vec_size;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public int getVectorSize() {
		return vec_size;
	}

}
