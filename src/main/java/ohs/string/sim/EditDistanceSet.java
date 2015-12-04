package ohs.string.sim;

import java.util.HashMap;
import java.util.Map;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;

public class EditDistanceSet {

	public static int getDamerauLevenshteinDistance(String s, String t) {
		return getDamerauLevenshteinDistance(s, t, 1, 1, 1, 1);
	}

	/**
	 * http://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance#Optimal_string_alignment_distance
	 * 
	 * http://stackoverflow.com/questions/6033631/levenshtein-to-damerau-levenshtein/6035519#6035519
	 * 
	 * @param a
	 * @param b
	 * @param alphabetLength
	 * @return
	 */
	public static int getDamerauLevenshteinDistance(String a, String b, int alphabetLength) {
		final int INFINITY = a.length() + b.length();
		int[][] H = new int[a.length() + 2][b.length() + 2];
		H[0][0] = INFINITY;

		for (int i = 0; i <= a.length(); i++) {
			H[i + 1][1] = i;
			H[i + 1][0] = INFINITY;
		}
		for (int j = 0; j <= b.length(); j++) {
			H[1][j + 1] = j;
			H[0][j + 1] = INFINITY;
		}
		int[] DA = new int[alphabetLength];

		for (int i = 1; i <= a.length(); i++) {
			int DB = 0;
			for (int j = 1; j <= b.length(); j++) {
				int i1 = DA[b.charAt(j - 1)];
				int j1 = DB;
				int d = ((a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1);
				if (d == 0)
					DB = j;
				H[i + 1][j + 1] = ArrayMath.min(new int[] { H[i][j] + d, H[i + 1][j] + 1, H[i][j + 1] + 1,
						H[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1) });
			}
			DA[a.charAt(i - 1)] = i;
		}
		return H[a.length() + 1][b.length() + 1];
	}

	/**
	 * Compute the Damerau-Levenshtein distance between the specified source string and the specified target string.
	 * 
	 * https://github.com/KevinStern/software-and-algorithms/blob/master/src/main/java/blogspot/software_and_algorithms/stern_library/string
	 * /DamerauLevenshteinAlgorithm.java
	 */
	public static int getDamerauLevenshteinDistance(String s, String t, int insertCost, int deleteCost, int replaceCost, int swapCost) {
		if (s.length() == 0) {
			return t.length() * insertCost;
		}
		if (t.length() == 0) {
			return s.length() * deleteCost;
		}
		int[][] d = new int[s.length()][t.length()];
		Map<Character, Integer> sourceIndexByCharacter = new HashMap<Character, Integer>();
		if (s.charAt(0) != t.charAt(0)) {
			d[0][0] = Math.min(replaceCost, deleteCost + insertCost);
		}
		sourceIndexByCharacter.put(s.charAt(0), 0);

		for (int i = 1; i < s.length(); i++) {
			int deleteDist = d[i - 1][0] + deleteCost;
			int insertDist = (i + 1) * deleteCost + insertCost;
			int matchDist = i * deleteCost + (s.charAt(i) == t.charAt(0) ? 0 : replaceCost);
			d[i][0] = ArrayMath.min(new int[] { deleteDist, insertCost, matchDist });
		}
		for (int j = 1; j < t.length(); j++) {
			int deleteDist = (j + 1) * insertCost + deleteCost;
			int insertDist = d[0][j - 1] + insertCost;
			int matchDist = j * insertCost + (s.charAt(0) == t.charAt(j) ? 0 : replaceCost);
			d[0][j] = ArrayMath.min(new int[] { deleteDist, insertCost, matchDist });
		}
		for (int i = 1; i < s.length(); i++) {
			int maxSourceLetterMatchIndex = s.charAt(i) == t.charAt(0) ? 0 : -1;
			for (int j = 1; j < t.length(); j++) {
				Integer candidateSwapIndex = sourceIndexByCharacter.get(t.charAt(j));
				int jSwap = maxSourceLetterMatchIndex;
				int deleteDist = d[i - 1][j] + deleteCost;
				int insertDist = d[i][j - 1] + insertCost;
				int matchDist = d[i - 1][j - 1];
				if (s.charAt(i) != t.charAt(j)) {
					matchDist += replaceCost;
				} else {
					maxSourceLetterMatchIndex = j;
				}
				int swapDist;
				if (candidateSwapIndex != null && jSwap != -1) {
					int iSwap = candidateSwapIndex;
					int preSwapCost;
					if (iSwap == 0 && jSwap == 0) {
						preSwapCost = 0;
					} else {
						preSwapCost = d[Math.max(0, iSwap - 1)][Math.max(0, jSwap - 1)];
					}
					swapDist = preSwapCost + (i - iSwap - 1) * deleteCost + (j - jSwap - 1) * insertCost + swapCost;
				} else {
					swapDist = Integer.MAX_VALUE;
				}
				d[i][j] = ArrayMath.min(new int[] { deleteDist, insertDist, matchDist, swapDist });

			}
			sourceIndexByCharacter.put(s.charAt(i), i);
		}
		return d[s.length() - 1][t.length() - 1];
	}

	/**
	 * Strings.java in mallet
	 * 
	 * 
	 * @param s
	 * @param t
	 * @param normalize
	 * @return
	 */
	public static double getEditDistance(String s, String t, boolean normalize) {
		int len_s = s.length();
		int len_t = t.length();
		int d[][]; // matrix
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		if (len_s == 0)
			return 1.0;
		if (len_t == 0)
			return 1.0;

		d = new int[len_s + 1][len_t + 1];

		for (i = 0; i <= len_s; i++)
			d[i][0] = i;

		for (j = 0; j <= len_t; j++)
			d[0][j] = j;

		for (i = 1; i <= len_s; i++) {
			s_i = s.charAt(i - 1);

			for (j = 1; j <= len_t; j++) {
				t_j = t.charAt(j - 1);

				cost = (s_i == t_j) ? 0 : 1;
				int deleteDist = d[i - 1][j] + 1;
				int insertDist = d[i][j - 1] + 1;
				int substituteDist = d[i - 1][j - 1] + cost;
				d[i][j] = ArrayMath.min(new int[] { deleteDist, insertDist, substituteDist });
			}
		}

		double ret = d[len_s][len_t];

		double[][] b = new double[len_s + 1][len_t + 1];
		ArrayUtils.copy(d, b);

		System.out.println(ArrayUtils.toString(b));

		if (normalize) {
			int longer = (len_s > len_t) ? len_s : len_t;
			ret = 1 - (ret / longer);
		}
		return ret;
	}

	public static double getWeightedEditDistance(String s, String t, boolean normalize) {
		int n = s.length();
		int m = t.length();
		double d[][]; // matrix
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		double cost; // cost

		if (n == 0)
			return 1.0;
		if (m == 0)
			return 1.0;

		d = new double[n + 1][m + 1];

		double[] ws1 = new double[n + 1];
		double[] ws2 = new double[m + 1];

		for (int k = 0; k < ws1.length; k++) {
			ws1[k] = 1f / Math.log(k + 2);
		}

		for (int k = 0; k < ws2.length; k++) {
			ws2[k] = 1f / Math.log(k + 2);
		}

		for (i = 0; i <= n; i++)
			d[i][0] = ws1[i];

		for (j = 0; j <= m; j++)
			d[0][j] = ws2[j];

		for (i = 1; i <= n; i++) {
			s_i = s.charAt(i - 1);

			for (j = 1; j <= m; j++) {
				t_j = t.charAt(j - 1);

				cost = (s_i == t_j) ? 0 : (ws1[i] + ws2[j]) / 2;
				double deleteDist = d[i - 1][j] + ws1[i];
				double insertDist = d[i][j - 1] + ws2[j];
				double substituteDist = d[i - 1][j - 1] + cost;
				d[i][j] = ArrayMath.min(new double[] { deleteDist, insertDist, substituteDist });
			}
		}

		double ret = d[n][m];

		// System.out.println(ArrayUtils.toString(d));

		if (normalize) {
			double sum = (n > m) ? ArrayMath.sum(ws1) : ArrayMath.sum(ws2);
			ret = 1 - (ret / sum);
		}
		return ret;
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		String a = "CA";
		String b = "ABC";
		//
		// for (int i = 0; i < 10; i++) {
		// System.out.printf("%d\t%s\t%s\n", i, CommonFuncs.sigmoid(-i), 1f / Math.log(1 + i + 1));
		// }

		System.out.println(getEditDistance(a, b, false));
		// System.out.println(getWeightedEditDistance(a, b, false));
		// System.out.println(getDamerauLevenshteinDistance(a, b, 1000));
		// System.out.println(getDamerauLevenshteinDistance(a, b, 1, 1, 1, 1));

		// System.out.println(getEditDistance(a, b));

		System.out.println("process ends.");
	}
}
