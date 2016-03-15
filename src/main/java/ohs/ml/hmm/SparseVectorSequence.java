package ohs.ml.hmm;

import ohs.matrix.SparseVector;

public class SparseVectorSequence {

	private SparseVector[] svs;

	private int[] labels;

	public SparseVectorSequence(SparseVector[] svs, int[] labels) {
		this.svs = svs;
		this.labels = labels;
	}

	public SparseVector[] getVectors() {
		return svs;
	}

	public int[] getLabels() {
		return labels;
	}

	public SparseVector getVector(int i) {
		return svs[i];
	}
}
