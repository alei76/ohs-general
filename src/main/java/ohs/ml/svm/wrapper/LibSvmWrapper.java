package ohs.ml.svm.wrapper;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import ohs.io.IOUtils;
import ohs.math.ArrayUtils;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;
import ohs.types.Indexer;

public class LibSvmWrapper implements Serializable {

	private static final long serialVersionUID = -3273222430839071709L;

	public static LibSvmWrapper read(String fileName) throws Exception {
		System.out.printf("read [%s]\n", fileName);

		ObjectInputStream ois = IOUtils.openObjectInputStream(fileName);
		Indexer<String> labelIndexer = IOUtils.readIndexer(ois);
		Indexer<String> featIndexer = IOUtils.readIndexer(ois);

		svm_model model = new svm_model();
		svm_parameter param = new svm_parameter();
		model.param = param;

		param.svm_type = ois.readInt();
		param.kernel_type = ois.readInt();
		param.degree = ois.readInt();
		param.gamma = ois.readDouble();
		param.coef0 = ois.readDouble();

		param.cache_size = ois.readDouble();
		param.eps = ois.readDouble();
		param.C = ois.readDouble();
		param.nr_weight = ois.readInt();

		int size = ois.readInt();
		if (size == 0) {
		} else {
			param.weight_label = new int[size];
			for (int i = 0; i < param.weight_label.length; i++) {
				param.weight_label[i] = ois.readInt();
			}
		}

		size = ois.readInt();
		if (size == 0) {
		} else {
			param.weight = new double[size];
			for (int i = 0; i < param.weight.length; i++) {
				param.weight[i] = ois.readDouble();
			}
		}

		param.nu = ois.readDouble();
		param.p = ois.readDouble();
		param.shrinking = ois.readInt();
		param.probability = ois.readInt();

		model.nr_class = ois.readInt();
		model.l = ois.readInt();

		size = ois.readInt();
		if (size == 0) {
		} else {
			svm_node[][] SV = new svm_node[size][];

			for (int i = 0; i < SV.length; i++) {
				int size2 = ois.readInt();
				svm_node[] nodes = new svm_node[size2];
				for (int j = 0; j < nodes.length; j++) {
					svm_node node = new svm_node();
					node.index = ois.readInt();
					node.value = ois.readDouble();
					nodes[j] = node;
				}
				SV[i] = nodes;
			}
			model.SV = SV;
		}

		size = ois.readInt();
		if (size == 0) {
		} else {
			double[][] sv_coef = new double[size][];

			for (int i = 0; i < sv_coef.length; i++) {
				int size2 = ois.readInt();
				double[] coef = new double[size2];
				for (int j = 0; j < coef.length; j++) {
					coef[j] = ois.readDouble();
				}
				sv_coef[i] = coef;
			}
			model.sv_coef = sv_coef;
		}

		size = ois.readInt();
		if (size == 0) {
		} else {
			double[] rho = new double[size];
			for (int i = 0; i < rho.length; i++) {
				rho[i] = ois.readDouble();
			}
			model.rho = rho;
		}

		size = ois.readInt();
		if (size == 0) {
		} else {
			double[] probA = new double[size];
			for (int i = 0; i < probA.length; i++) {
				probA[i] = ois.readDouble();
			}
			model.probA = probA;
		}

		size = ois.readInt();
		if (size == 0) {
		} else {
			double[] probB = new double[size];
			for (int i = 0; i < probB.length; i++) {
				probB[i] = ois.readDouble();
			}
			model.probB = probB;
		}

		size = ois.readInt();
		if (size == 0) {
		} else {
			int[] label = new int[size];
			for (int i = 0; i < label.length; i++) {
				label[i] = ois.readInt();
			}
			model.label = label;
		}

		size = ois.readInt();
		if (size == 0) {
		} else {
			int[] nSV = new int[size];
			for (int i = 0; i < size; i++) {
				nSV[i] = ois.readInt();
			}
			model.nSV = nSV;
		}

		ois.close();

		return new LibSvmWrapper(model, labelIndexer, featIndexer);
	}

	public static void sort(svm_node nodes[]) {
		for (int i = 0; i < nodes.length; i++) {
			for (int j = 1; j < nodes.length - i; j++)
				if (nodes[j - 1].index > nodes[j].index) {
					svm_node temp = nodes[j];
					nodes[j] = nodes[j - 1];
					nodes[j - 1] = temp;
				}
		}
	}

	private svm_model model;

	private Indexer<String> labelIndexer;

	private Indexer<String> termIndexer;

	public LibSvmWrapper(svm_model model, Indexer<String> topicIndexer, Indexer<String> termIndexer) {
		this.model = model;
		this.labelIndexer = topicIndexer;
		this.termIndexer = termIndexer;
	}

	public String evalute(List<SparseVector> testData) {
		SparseVector label_correct = new SparseVector(ArrayUtils.copy(model.label));

		SparseVector label_answer = label_correct.copy();
		SparseVector label_predict = label_correct.copy();

		for (int i = 0; i < testData.size(); i++) {
			SparseVector query = testData.get(i);
			SparseVector label_score = score(query);
			int predictId = label_score.argMax();
			int answerId = query.label();

			if (predictId == answerId) {
				label_correct.increment(answerId, 1);
			}

			label_answer.increment(answerId, 1);
			label_predict.increment(predictId, 1);
		}

		return TopicEval.evalute(null, label_answer, label_predict, label_correct);
	}

	public Counter<String> score(Counter<String> input) {
		SparseVector vector = VectorUtils.toSparseVector(input, termIndexer);
		return VectorUtils.toCounter(score(vector), labelIndexer);
	}

	public SparseVector score(SparseVector query) {
		svm_node input[] = new svm_node[query.size()];

		for (int i = 0; i < query.size(); i++) {
			int index = query.indexAtLoc(i) + 1;
			double value = query.valueAtLoc(i);

			assert index > 0;

			input[i] = new svm_node();
			input[i].index = index;
			input[i].value = value;
		}

		int nr_class = svm.svm_get_nr_class(model);
		int labels[] = new int[nr_class];
		svm.svm_get_labels(model, labels);
		double prob_estimates[] = new double[nr_class];
		double output;

		if (svm.svm_check_probability_model(model) == 0) {
			output = svm.svm_predict(model, input);
		} else {
			output = svm.svm_predict_probability(model, input, prob_estimates);
		}

		SparseVector ret = new SparseVector(labels, prob_estimates, -1);
		return ret;
	}

	public void write(String fileName) throws Exception {
		System.out.printf("write to [%s].\n", fileName);
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(fileName);

		IOUtils.write(oos, labelIndexer);
		IOUtils.write(oos, termIndexer);

		oos.writeInt(model.param.svm_type);
		oos.writeInt(model.param.kernel_type);
		oos.writeInt(model.param.degree);
		oos.writeDouble(model.param.gamma);
		oos.writeDouble(model.param.coef0);

		oos.writeDouble(model.param.cache_size);
		oos.writeDouble(model.param.eps);
		oos.writeDouble(model.param.C);
		oos.writeInt(model.param.nr_weight);

		int size = model.param.weight_label.length;
		oos.writeInt(size);

		if (size > 0) {
			int[] weight_label = model.param.weight_label;
			for (int i = 0; i < weight_label.length; i++) {
				oos.writeInt(weight_label[i]);
			}
		}

		size = model.param.weight.length;
		oos.writeInt(size);

		if (size > 0) {
			double[] weight = model.param.weight;
			for (int i = 0; i < weight.length; i++) {
				oos.writeDouble(weight[i]);
			}
		}

		oos.writeDouble(model.param.nu);
		oos.writeDouble(model.param.p);
		oos.writeInt(model.param.shrinking);
		oos.writeInt(model.param.probability);

		oos.writeInt(model.nr_class);
		oos.writeInt(model.l);

		size = model.SV.length;
		oos.writeInt(size);

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				svm_node[] nodes = model.SV[i];
				oos.writeInt(nodes.length);
				for (int j = 0; j < nodes.length; j++) {
					svm_node node = nodes[j];
					oos.writeInt(node.index);
					oos.writeDouble(node.value);
				}
			}
		}

		size = model.sv_coef.length;
		oos.writeInt(size);

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				double[] coef = model.sv_coef[i];
				oos.writeInt(coef.length);
				for (int j = 0; j < coef.length; j++) {
					oos.writeDouble(coef[j]);
				}
			}
		}

		size = model.rho.length;
		oos.writeInt(size);

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				oos.writeDouble(model.rho[i]);
			}
		}

		size = model.probA.length;
		oos.writeInt(size);

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				oos.writeDouble(model.probA[i]);
			}
		}

		size = model.probB.length;
		oos.writeInt(size);

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				oos.writeDouble(model.probB[i]);
			}
		}

		size = model.label.length;
		oos.writeInt(size);

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				oos.writeInt(model.label[i]);
			}
		}

		size = model.nSV.length;
		oos.writeInt(size);

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				oos.writeInt(model.nSV[i]);
			}
		}

		oos.close();
	}
}
