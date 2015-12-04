package ohs.string.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohs.math.ArrayMath;
import ohs.types.Pair;

public class Aligner {

	static enum Direction {
		WEST, NORTH, DIAGONAL, NONE
	}

	public Aligner() {

	}

	public AlignResult align(MemoMatrix mm) {
		List<Pair> path = getAlignmentPath(mm);

		String s = mm.getSource();
		String t = mm.getTarget();

		List<MatchType> mt = new ArrayList<MatchType>();

		StringBuffer sb = new StringBuffer();
		StringBuffer tb = new StringBuffer();

		int pi = -1;
		int pj = -1;

		for (int k = 0; k < path.size(); k++) {
			Pair index = path.get(k);
			int i = (int) index.getFirst();
			int j = (int) index.getSecond();

			char si = s.charAt(i);
			char tj = t.charAt(j);

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

		return new AlignResult(mm, sb.toString(), tb.toString(), mt);
	}

	private int countPrevNonZerosInSource(MemoMatrix m, int i, int j) {
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

	private int countPrevNonZerosInTarget(MemoMatrix m, int i, int j) {
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

	private List<Pair> getAlignmentPath(MemoMatrix mm) {
		List<Pair> ret = new ArrayList<Pair>();

		String s = mm.getSource();
		String t = mm.getTarget();

		int i = s.length();
		int j = t.length();

		while (i > 0 && j > 0) {
			ret.add(new Pair(i - 1, j - 1));

			// Token ti = s.getToken(i - 1);
			// Token tj = t.getToken(j - 1);
			// String wi = ti.getAttrValue(Token.WORD);
			// String wj = tj.getAttrValue(Token.WORD);
			//
			// if (wi.equals("는/e") && wj.equals("는/e")) {
			// System.out.println();
			// }
			//
			// MatchType mt = wi.equals(wj) ? MatchType.MATCH : MatchType.UNMATCH;

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
