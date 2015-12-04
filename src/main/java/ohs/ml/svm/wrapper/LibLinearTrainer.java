package ohs.ml.svm.wrapper;

import java.util.List;

import org.springframework.ui.Model;

import ohs.matrix.SparseVector;
import ohs.types.Indexer;

public class LibLinearTrainer {

	private Parameter param;

	public LibLinearTrainer() {
		Parameter param = new Parameter(SolverType.L2R_L2LOSS_SVC_DUAL, 1, Double.POSITIVE_INFINITY, 0.1);

		if (param.getEps() == Double.POSITIVE_INFINITY) {
			switch (param.getSolverType()) {
			case L2R_LR:
			case L2R_L2LOSS_SVC:
				param.setEps(0.01);
				break;
			case L2R_L2LOSS_SVR:
				param.setEps(0.001);
				break;
			case L2R_L2LOSS_SVC_DUAL:
			case L2R_L1LOSS_SVC_DUAL:
			case MCSVM_CS:
			case L2R_LR_DUAL:
				param.setEps(0.1);
				break;
			case L1R_L2LOSS_SVC:
			case L1R_LR:
				param.setEps(0.01);
				break;
			case L2R_L1LOSS_SVR_DUAL:
			case L2R_L2LOSS_SVR_DUAL:
				param.setEps(0.1);
				break;
			default:
				throw new IllegalStateException("unknown solver type: " + param.getSolverType());
			}
		}

		setParameter(param);
	}

	public LibLinearTrainer(Parameter param) {
		this.param = param;
	}

	public void setParameter(Parameter param) {
		this.param = param;
	}

	public LibLinearWrapper train(Indexer<String> labelIndexer, Indexer<String> featureIndexer, List<SparseVector> trainData) {
		Problem prob = new Problem();
		prob.l = trainData.size();
		prob.n = featureIndexer.size() + 1;
		prob.y = new double[prob.l];
		prob.x = new Feature[prob.l][];
		prob.bias = -1;

		if (prob.bias >= 0) {
			prob.n++;
		}

		for (int i = 0; i < trainData.size(); i++) {
			SparseVector x = trainData.get(i);

			Feature[] input = new Feature[prob.bias > 0 ? x.size() + 1 : x.size()];

			for (int j = 0; j < x.size(); j++) {
				int index = x.indexAtLoc(j) + 1;
				double value = x.valueAtLoc(j);

				assert index >= 0;

				input[j] = new FeatureNode(index + 1, value);
			}

			if (prob.bias >= 0) {
				input[input.length - 1] = new FeatureNode(prob.n, prob.bias);
			}

			prob.x[i] = input;
			prob.y[i] = x.label();
		}

		Model model = Linear.train(prob, param);

		return new LibLinearWrapper(model, labelIndexer, featureIndexer);
	}
}
