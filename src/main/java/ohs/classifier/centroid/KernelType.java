package ohs.classifier.centroid;

public enum KernelType {
	LINEAR(0), POLY(1), RBF(2), SIGMOD(3);

	public static KernelType parse(int id) {
		KernelType ret = LINEAR;
		if (id == 1) {
			ret = POLY;
		} else if (id == 2) {
			ret = RBF;
		} else if (id == 3) {
			ret = SIGMOD;
		}
		return ret;
	}

	private int id;

	KernelType(int id) {
		this.id = id;
	}

	public int id() {
		return id;
	}

}
