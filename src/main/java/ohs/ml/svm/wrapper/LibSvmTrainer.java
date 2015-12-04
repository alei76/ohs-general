package ohs.ml.svm.wrapper;

import java.util.Iterator;
import java.util.List;

import libsvm.svm;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;

public class LibSvmTrainer {

	public static svm_parameter getDefaultParameters() {
		svm_parameter param = new svm_parameter();
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.RBF;
		param.degree = 3;
		param.gamma = 0.0D;
		param.coef0 = 0.0D;
		param.nu = 0.5D;
		param.cache_size = 100D;
		param.C = 1.0D;
		param.eps = 0.001D;
		param.p = 0.10000000000000001D;
		param.shrinking = 0;
		param.probability = 1;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
		return param;
	}

	private svm_parameter param;

	private Indexer<String> featureIndexer;

	private Indexer<String> labelIndexer;

	public LibSvmTrainer() {
		setParameter(getDefaultParameters());
	}

	public LibSvmTrainer(svm_parameter param) {
		this.param = param;
	}

	public void setParameter(svm_parameter param) {
		this.param = param;
	}

	public LibSvmWrapper train(Indexer<String> labelIndexer, Indexer<String> featureIndexer, List<SparseVector> trainData) {
		svm_problem problem = new svm_problem();
		problem.l = trainData.size();
		problem.x = new svm_node[problem.l][];
		problem.y = new double[problem.l];

		int max_index = featureIndexer.size();

		for (int i = 0; i < trainData.size(); i++) {
			SparseVector sv = trainData.get(i);
			svm_node input[] = new svm_node[sv.size()];
			for (int j = 0; j < sv.size(); j++) {
				int index = sv.indexAtLoc(j) + 1; // add if feat index
														// starts
														// with 0.

				assert index > 0;

				double value = sv.valueAtLoc(j);

				input[j] = new svm_node();
				input[j].index = index;
				input[j].value = value;

				if (index > max_index) {
					max_index = index;
				}
			}

			problem.x[i] = input;
			problem.y[i] = sv.label();
		}

		// int max_index = termIndexer.size();
		if (param.gamma == 0.0D && max_index > 0) {
			param.gamma = 1f / max_index;
		}
		String error_msg = svm.svm_check_parameter(problem, param);

		if (error_msg != null) {
			System.err.print((new StringBuilder("Error: ")).append(error_msg).append("\n").toString());
			System.exit(1);
		}

		libsvm.svm_model model = svm.svm_train(problem, param);
		return new LibSvmWrapper(model, labelIndexer, featureIndexer);
	}

	public LibSvmWrapper train(List<Counter<String>> trainData, List<String> topics) {
		svm_problem problem = new svm_problem();
		problem.l = trainData.size();
		problem.x = new svm_node[problem.l][];
		problem.y = new double[problem.l];
		labelIndexer = new Indexer<String>();
		featureIndexer = new Indexer<String>();
		for (int i = 0; i < trainData.size(); i++) {
			Counter<String> data = trainData.get(i);
			svm_node input[] = new svm_node[data.size()];
			int loc = 0;
			for (Iterator<String> iterator = data.keySet().iterator(); iterator.hasNext();) {
				String term = (String) iterator.next();
				double count = data.getCount(term);
				int termInd = featureIndexer.getIndex(term) + 1;
				input[loc] = new svm_node();
				input[loc].index = termInd;
				input[loc].value = count;
				loc++;
			}

			LibSvmWrapper.sort(input);
			String topic = (String) topics.get(i);
			int topicId = labelIndexer.getIndex(topic);
			problem.x[i] = input;
			problem.y[i] = topicId;
		}

		int max_index = featureIndexer.size();
		if (param.gamma == 0.0D && max_index > 0)
			param.gamma = 1.0D / (double) max_index;
		String error_msg = svm.svm_check_parameter(problem, param);
		if (error_msg != null) {
			System.err.print((new StringBuilder("Error: ")).append(error_msg).append("\n").toString());
			System.exit(1);
		}
		libsvm.svm_model model = svm.svm_train(problem, param);
		return new LibSvmWrapper(model, labelIndexer, featureIndexer);
	}
}
