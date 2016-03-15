package ohs.ml.hmm;

import java.util.List;

import org.apache.xmlbeans.xml.stream.StartPrefixMapping;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;

public class HMM {

	public static void main(String[] args) {
		System.out.println("process begins.");

		System.out.println("process ends.");
	}

	private double[] start_probs;

	private double[][] trans_probs;

	private double[][] emission_probs;

	private int num_states;

	private int voc_size;

	private double[][] temp_trans_probs;

	public HMM(int num_states, int voc_size) {
		this.num_states = num_states;
		this.voc_size = voc_size;

		trans_probs = new double[num_states][num_states];
		emission_probs = new double[num_states][voc_size];
		start_probs = new double[num_states];

		temp_trans_probs = new double[num_states][num_states];

		ArrayMath.random(0, 1, trans_probs);
		ArrayMath.random(0, 1, emission_probs);
		ArrayMath.random(0, 1, start_probs);

		ArrayMath.normalizeRows(trans_probs);
		ArrayMath.normalizeRows(emission_probs);
	}

	public void train(List<SparseVectorSequence> xs, int iter) {

		for (SparseVectorSequence x : xs) {
			for (int label : x.getLabels()) {
				start_probs[label]++;
			}
		}
		ArrayMath.normalize(start_probs);

		for (int i = 0; i < iter; i++) {
			train(xs);
		}
	}

	public void train(List<SparseVectorSequence> xs) {
		for (int i = 0; i < xs.size(); i++) {
			SparseVectorSequence x = xs.get(i);

		}
	}

	public void forward(int[] obs) {
		double[][] fwd = new double[num_states][obs.length];

		for (int i = 0; i < num_states; i++) {
			int o = obs[i];
			fwd[i][0] = start_probs[i] * emission_probs[i][o];
		}

		for (int t = 1; t < obs.length; t++) {
			for (int j = 0; j < num_states; j++) {
				for (int i = 0; i < num_states; i++) {
					fwd[j][t] += fwd[i][t - 1] * trans_probs[i][j];
					fwd[j][t] *= emission_probs[j][obs[t]];
				}
			}
		}

		// /* initialization (time 0) */
		// for (int i = 0; i < numStates; i++)
		// fwd[i][0] = pi[i] * b[i][o[0]];
		//
		// /* induction */
		// for (int t = 0; t <= T - 2; t++) {
		// for (int j = 0; j < numStates; j++) {
		// fwd[j][t + 1] = 0;
		// for (int i = 0; i < numStates; i++)
		// fwd[j][t + 1] += (fwd[i][t] * a[i][j]);
		// fwd[j][t + 1] *= b[j][o[t + 1]];
		// }
		// }
	}

}
