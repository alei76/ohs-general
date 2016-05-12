package ohs.ml.svm.wrapper;

import java.util.List;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
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

	public LibLinearWrapper train(Indexer<String> labelIndexer, Indexer<String> featIndexer, List<SparseVector> xs) {
		Problem prob = new Problem();
		prob.l = xs.size();
		prob.n = featIndexer.size() + 1;
		prob.y = new double[prob.l];
		prob.x = new Feature[prob.l][];
		prob.bias = -1;

		if (prob.bias >= 0) {
			prob.n++;
		}

		for (int i = 0; i < xs.size(); i++) {
			SparseVector x = xs.get(i);
			prob.x[i] = LibLinearWrapper.toFeatureNodes(x, prob.n, prob.bias);
			prob.y[i] = x.label();
		}

		Model model = Linear.train(prob, param);

		return new LibLinearWrapper(model, labelIndexer, featIndexer);
	}

}
