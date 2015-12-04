package ohs.ir.task.medical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohs.ir.task.medical.SmithWatermanScorer.ScoreMatrix;
import ohs.math.ArrayMath;
import ohs.string.sim.MatchType;
import ohs.types.Pair;

public class SmithWatermanAligner {

	static enum Direction {
		WEST, NORTH, DIAGONAL, NONE
	}

	public SmithWatermanAligner() {

	}

	public void align(ScoreMatrix sm) {
		List<Pair<Integer, Integer>> path = getAlignmentPath(sm);

		List<Integer> s = sm.getSource();
		List<Integer> t = sm.getTarget();

		List<MatchType> mt = new ArrayList<MatchType>();

		StringBuffer sb = new StringBuffer();
		StringBuffer tb = new StringBuffer();

		int pi = -1;
		int pj = -1;

		for (int k = 0; k < path.size(); k++) {
			Pair<Integer, Integer> index = path.get(k);
			int i = index.getFirst();
			int j = index.getSecond();

			int si = s.get(i);
			int tj = t.get(j);

			// System.out.printf("[%d, %d = %s, %s]\n", i, j, wi, wj);

			MatchType mi = null;

			if (si == tj) {
				mi = MatchType.MATCH;
			} else {
				mi = MatchType.UNMATCH;
			}

			mt.add(mi);

			if (i == pi) {
				si = '#';
			}

			if (j == pj) {
				tj = '#';
			}

			sb.append(si);
			tb.append(tj);

			pi = i;
			pj = j;
		}

	}

	private int countPrevNonZerosInSource(ScoreMatrix m, int i, int j) {
		int ret = 0;
		for (int k = i - 1; k >= 0; k--) {
			double prevNorthScore = m.get(k, j);
			if (prevNorthScore == 0) {
				break;
			}
			ret++;
		}
		return ret;
	}

	private int countPrevNonZerosInTarget(ScoreMatrix m, int i, int j) {
		int ret = 0;
		for (int k = j - 1; k >= 0; k--) {
			double prevWestScore = m.get(i, k);
			if (prevWestScore == 0) {
				break;
			}
			ret++;
		}
		return ret;
	}

	private List<Pair<Integer, Integer>> getAlignmentPath(ScoreMatrix mm) {
		List<Pair<Integer, Integer>> ret = new ArrayList<Pair<Integer, Integer>>();

		List<Integer> s = mm.getSource();
		List<Integer> t = mm.getTarget();

		int i = s.size();
		int j = t.size();

		while (i > 0 && j > 0) {
			ret.add(new Pair<Integer, Integer>(i - 1, j - 1));

			double score = mm.get(i, j);
			double west_score = mm.get(i, j - 1);
			double north_score = mm.get(i - 1, j);
			double diagonal_score = mm.get(i - 1, j - 1);
			double maxScore = ArrayMath.max(new double[] { north_score, west_score, diagonal_score });

			Direction from = Direction.NONE;

			if (maxScore == diagonal_score) {
				if (diagonal_score == 0) {
					int ct = countPrevNonZerosInTarget(mm, i, j);
					int cs = countPrevNonZerosInSource(mm, i, j);

					if (ct < cs) {
						from = Direction.WEST;
					} else {
						from = Direction.NORTH;
					}
				} else {
					from = Direction.DIAGONAL;
				}
			} else {
				if (west_score > north_score) {
					from = Direction.WEST;
				} else if (west_score < north_score) {
					from = Direction.NORTH;
				} else if (west_score == north_score) {
					int ct = countPrevNonZerosInTarget(mm, i, j);
					int cs = countPrevNonZerosInSource(mm, i, j);

					if (ct == cs) {
						from = Direction.DIAGONAL;
					} else if (ct < cs) {
						from = Direction.WEST;
					} else {
						from = Direction.NORTH;
					}
				}
			}

			if (from == Direction.DIAGONAL) {
				i--;
				j--;
			} else if (from == Direction.NORTH) {
				i--;
			} else if (from == Direction.WEST) {
				j--;
			}
		}

		Collections.reverse(ret);
		return ret;
	}

}
