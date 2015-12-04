package ohs.ir.task.medical;

import java.util.List;

import ohs.ir.task.medical.SmithWatermanScorer.ScoreMatrix;
import ohs.math.ArrayMath;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.Pair;

public class PassageGenerator {

	private Indexer<String> wordIndexer;

	private boolean makeLog = false;

	private StringBuffer logBuff;

	public Counter<Pair<Integer, Integer>> generate(List<Integer> qWords, List<Integer> dWords) {
		logBuff = new StringBuffer();

		SmithWatermanScorer swScorer = new SmithWatermanScorer();

		ScoreMatrix sm = swScorer.compute(dWords, qWords);

		double[] sw_score_sum_for_each_word = new double[dWords.size()];

		{
			double[] temp = ArrayMath.sumRows(sm.getValues());
			for (int m = 1; m < temp.length; m++) {
				sw_score_sum_for_each_word[m - 1] = temp[m];
			}
		}

		Counter<Pair<Integer, Integer>> psgScores = new Counter<Pair<Integer, Integer>>();

		for (int m = 0, p_start = 0, p_offset = 0; m < sw_score_sum_for_each_word.length; m++) {
			int w = dWords.get(m);
			double sw_score_sum_for_word = sw_score_sum_for_each_word[m];

			if (sw_score_sum_for_word == 0) {
				if (p_offset > 0) {
					Pair<Integer, Integer> psgLoc = new Pair<Integer, Integer>(p_start, p_offset);
					double psg_score = 0;

					for (int n = p_start; n < p_start + p_offset; n++) {
						int w_in_psg = dWords.get(n);
						double sw_score_sum_for_psg_word = sw_score_sum_for_each_word[n];
						psg_score += sw_score_sum_for_psg_word;
					}
					psgScores.setCount(psgLoc, psg_score);
					p_offset = 0;
				}
				p_start = m + 1;
				continue;
			}

			p_offset++;
		}

		if (makeLog) {
			StringBuffer psgBuff = new StringBuffer();

			List<Pair<Integer, Integer>> psgLocs = psgScores.getSortedKeys();

			for (int m = 0; m < psgLocs.size(); m++) {
				Pair<Integer, Integer> psgLoc = psgLocs.get(m);
				int p_start = psgLoc.getFirst();
				int p_offset = psgLoc.getSecond();
				int p_end = p_start + p_offset;
				double psg_score = psgScores.getCount(psgLoc);
				psgBuff.append(String.format("%d\t%d\t", p_start, p_offset));
				for (int n = p_start; n < p_end; n++) {
					int w = dWords.get(n);
					String word = wordIndexer.getObject(w);
					psgBuff.append(word);
					if (n != p_end - 1) {
						psgBuff.append(" ");
					}
				}

				psgBuff.append(String.format("\t%f", psg_score));

				if (m != psgLocs.size() - 1) {
					psgBuff.append("\n");
				}
			}

			logBuff.append(psgBuff.toString());
		}

		return psgScores;
	}

	public StringBuffer getLogBuff() {
		return logBuff;
	}

	public void setMakeLog(boolean makeLog) {
		this.makeLog = makeLog;
	}

}
