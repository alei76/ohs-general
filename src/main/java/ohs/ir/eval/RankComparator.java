package ohs.ir.eval;

import ohs.matrix.SparseVector;

public class RankComparator {
	public static String compareRankings(SparseVector docScores1, SparseVector docScores2, SparseVector docRels) {
		StringBuffer sb = new StringBuffer();

		sb.append(String.format("Relevant docs:\t%d", docRels.size()));
		sb.append(String.format("\nRelevant docs at top-%d:\t%d", docScores1.size(), getRelevant(docScores1, docRels, docScores1.size())));
		sb.append(String.format("\nRelevant docs at top-20:"));
		sb.append(String.format("\nRanking-1:\t%d", getRelevant(docScores1, docRels, 20)));
		sb.append(String.format("\nRanking-2:\t%d", getRelevant(docScores2, docRels, 20)));
		sb.append("\nDocID\tRelevance\tRank1\tRank2\tChange\tEffect");

		SparseVector ranking1 = docScores1.ranking();
		SparseVector ranking2 = docScores2.ranking();

		docScores1.sortByValue();
		docScores2.sortByValue();

		int num_poss = 0;
		int num_negs = 0;

		for (int i = 0; i < docScores2.size() && i < 20; i++) {
			int docId = docScores2.indexAtLoc(i);
			int rank2 = i + 1;
			int rank1 = (int) ranking1.valueAlways(docId);
			int change = rank1 - rank2;
			double relevance = docRels.valueAlways(docId);

			String effect = "";

			if (relevance > 0) {
				if (change > 0) {
					effect = "POS";
					num_poss++;
				} else if (change < 0) {
					effect = "NEG";
					num_negs++;
				}
			}

			sb.append(String.format("\n%d\t%d\t%d\t%d\t%d\t%s", docId, (int) relevance, rank1, rank2, change, effect));
		}

		sb.append(String.format("\nPOSs:\t%d", num_poss));
		sb.append(String.format("\nNEGs:\t%d", num_negs));
		sb.append("\nRanks of relevant docs:");
		sb.append("\nRanking-1:");

		for (int i = 0; i < docScores1.size(); i++) {
			int docId = docScores1.indexAtLoc(i);
			int rank = i + 1;
			if (docRels.valueAlways(docId) > 0) {
				sb.append(String.format("\t%d", rank));
			}
		}

		sb.append("\nRanking-2:");

		for (int i = 0; i < docScores2.size(); i++) {
			int docId = docScores2.indexAtLoc(i);
			int rank = i + 1;
			if (docRels.valueAlways(docId) > 0) {
				sb.append(String.format("\t%d", rank));
			}
		}
		sb.append("\n");

		docScores1.sortByIndex();
		docScores2.sortByIndex();

		return sb.toString();
	}

	public static int getRelevant(SparseVector docScores, SparseVector docRels, int n) {
		int ret = 0;

		docScores.sortByValue();

		for (int i = 0; i < n && i < docScores.size(); i++) {
			int docId = docScores.indexAtLoc(i);
			double rel = docRels.valueAlways(docId);
			if (rel > 0) {
				ret++;
			}
		}
		docScores.sortByIndex();
		return ret;
	}
}
