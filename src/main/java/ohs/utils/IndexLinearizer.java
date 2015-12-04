package ohs.utils;

public class IndexLinearizer {

	private int numLabels;

	private int numFeatures;

	public IndexLinearizer(int numClasses, int numFeatures) {
		super();
		this.numLabels = numClasses;
		this.numFeatures = numFeatures;
	}

	public int getFeatureIndex(int linearIndex) {
		return linearIndex / numLabels;
	}

	public int getLabelIndex(int linearIndex) {
		return linearIndex % numLabels;
	}

	public int getLinearIndex(int labelId, int featureId) {
		return labelId + featureId * numLabels;
	}

	public int size() {
		return numLabels * numFeatures;
	}

}
