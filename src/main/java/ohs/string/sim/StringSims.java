package ohs.string.sim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ohs.math.ArrayMath;
import ohs.math.ArrayUtils;
import ohs.math.VectorMath;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.types.Counter;

public class StringSims {

	private static String commonChars(String s, String t, int halflen) {
		StringBuilder common = new StringBuilder();
		StringBuilder copy = new StringBuilder(t);
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			boolean foundIt = false;
			for (int j = Math.max(0, i - halflen); !foundIt && j < Math.min(i + halflen, t.length()); j++) {
				if (copy.charAt(j) == ch) {
					foundIt = true;
					common.append(ch);
					copy.setCharAt(j, '*');
				}
			}
		}
		return common.toString();
	}

	private static int commonPrefixLength(int maxLength, String common1, String common2) {
		int n = Math.min(maxLength, Math.min(common1.length(), common2.length()));
		for (int i = 0; i < n; i++) {
			if (common1.charAt(i) != common2.charAt(i))
				return i;
		}
		return n; // first n characters are the same
	}

	public static double CosineSimilarity(String s, String t) {
		SparseVector sv = VectorUtils.toSparseVector(getCharacterCounts(s));
		SparseVector tv = VectorUtils.toSparseVector(getCharacterCounts(t));
		return VectorMath.cosine(sv, tv, false);
	}

	public static int DamerauLevenshteinDistance(String s, String t) {
		return DamerauLevenshteinDistance(s, t, 1, 1, 1, 1);
	}

	/**
	 * http://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance# Optimal_string_alignment_distance
	 * 
	 * http://stackoverflow.com/questions/6033631/levenshtein-to-damerau- levenshtein/6035519#6035519
	 * 
	 * @param a
	 * @param b
	 * @param alphabetLength
	 * @return
	 */
	public static int DamerauLevenshteinDistance(String a, String b, int alphabetLength) {
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
				H[i + 1][j + 1] = ArrayMath
						.min(new int[] { H[i][j] + d, H[i + 1][j] + 1, H[i][j + 1] + 1, H[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1) });
			}
			DA[a.charAt(i - 1)] = i;
		}
		return H[a.length() + 1][b.length() + 1];
	}

	/**
	 * Compute the Damerau-Levenshtein distance between the specified source string and the specified target string.
	 * 
	 * https://github.com/KevinStern/software-and-algorithms/blob/master/src/
	 * main/java/blogspot/software_and_algorithms/stern_library/string /DamerauLevenshteinAlgorithm.java
	 */
	public static int DamerauLevenshteinDistance(String s, String t, int insert_cost, int delete_cost, int replace_cost, int swap_cost) {
		if (s.length() == 0) {
			return t.length() * insert_cost;
		}
		if (t.length() == 0) {
			return s.length() * delete_cost;
		}
		int[][] d = new int[s.length()][t.length()];
		Map<Character, Integer> sourceIndexByCharacter = new HashMap<Character, Integer>();
		if (s.charAt(0) != t.charAt(0)) {
			d[0][0] = Math.min(replace_cost, delete_cost + insert_cost);
		}
		sourceIndexByCharacter.put(s.charAt(0), 0);

		int delete_dist = 0;
		int insert_dist = 0;
		int match_dist = 0;
		int jSwap = 0;

		for (int i = 1; i < s.length(); i++) {
			delete_dist = d[i - 1][0] + delete_cost;
			insert_dist = (i + 1) * delete_cost + insert_cost;
			match_dist = i * delete_cost + (s.charAt(i) == t.charAt(0) ? 0 : replace_cost);
			d[i][0] = ArrayMath.min(new int[] { delete_dist, insert_cost, match_dist });
		}

		for (int j = 1; j < t.length(); j++) {
			delete_dist = (j + 1) * insert_cost + delete_cost;
			insert_dist = d[0][j - 1] + insert_cost;
			match_dist = j * insert_cost + (s.charAt(0) == t.charAt(j) ? 0 : replace_cost);
			d[0][j] = ArrayMath.min(new int[] { delete_dist, insert_cost, match_dist });
		}

		for (int i = 1; i < s.length(); i++) {
			int maxSourceLetterMatchIndex = s.charAt(i) == t.charAt(0) ? 0 : -1;
			for (int j = 1; j < t.length(); j++) {
				Integer candidateSwapIndex = sourceIndexByCharacter.get(t.charAt(j));
				jSwap = maxSourceLetterMatchIndex;
				delete_dist = d[i - 1][j] + delete_cost;
				insert_dist = d[i][j - 1] + insert_cost;
				match_dist = d[i - 1][j - 1];
				if (s.charAt(i) != t.charAt(j)) {
					match_dist += replace_cost;
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
					swapDist = preSwapCost + (i - iSwap - 1) * delete_cost + (j - jSwap - 1) * insert_cost + swap_cost;
				} else {
					swapDist = Integer.MAX_VALUE;
				}
				d[i][j] = ArrayMath.min(new int[] { delete_dist, insert_dist, match_dist, swapDist });

			}
			sourceIndexByCharacter.put(s.charAt(i), i);
		}
		return d[s.length() - 1][t.length() - 1];
	}

	public static double DiceSimilarity(String s, String t) {
		SparseVector sv = VectorUtils.toSparseVector(getCharacterCounts(s));
		SparseVector tv = VectorUtils.toSparseVector(getCharacterCounts(t));
		double sim = 2 * VectorMath.dotProduct(sv, tv);
		double norm = VectorMath.dotProduct(sv, sv) + VectorMath.dotProduct(tv, tv);
		return sim / norm;
	}

	/**
	 * Strings.java in mallet
	 * 
	 * 
	 * @param s
	 * @param t
	 * @return
	 */
	public static double editDistance(String s, String t) {
		int len_s = s.length();
		int len_t = t.length();
		int d[][]; // matrix
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t

		if (len_s == 0)
			return len_t;
		if (len_t == 0)
			return len_s;

		d = new int[len_s + 1][len_t + 1];

		for (i = 0; i <= len_s; i++)
			d[i][0] = i;

		for (j = 0; j <= len_t; j++)
			d[0][j] = j;

		int cost = 0;
		int delete_dist = 0;
		int insert_dist = 0;
		int replace_dist = 0;

		for (i = 1; i <= len_s; i++) {
			s_i = s.charAt(i - 1);

			for (j = 1; j <= len_t; j++) {
				t_j = t.charAt(j - 1);

				cost = (s_i == t_j) ? 0 : 1;
				delete_dist = d[i - 1][j] + 1;
				insert_dist = d[i][j - 1] + 1;
				replace_dist = d[i - 1][j - 1] + cost;
				d[i][j] = ArrayMath.min(new int[] { delete_dist, insert_dist, replace_dist });
			}
		}
		double ret = d[len_s][len_t];
		return ret;
	}

	public static double EuclideanDistance(String s, String t) {
		SparseVector sv = VectorUtils.toSparseVector(getCharacterCounts(s));
		SparseVector tv = VectorUtils.toSparseVector(getCharacterCounts(t));
		return VectorMath.euclideanDistance(sv, tv, false);
	}

	private static Counter<Integer> getCharacterCounts(String s) {
		Counter<Integer> ret = new Counter<Integer>();
		for (int i = 0; i < s.length(); i++) {
			ret.incrementCount((int) s.charAt(i), 1);
		}
		return ret;
	}

	private static int halfLengthOfShorter(String s, String t) {
		return Math.min(s.length(), t.length()) / 2 + 1;
	}

	public static double JaccardSimilarity(String s, String t) {
		SparseVector sv = VectorUtils.toSparseVector(getCharacterCounts(s));
		SparseVector tv = VectorUtils.toSparseVector(getCharacterCounts(t));
		double sim = VectorMath.dotProduct(sv, tv);
		double norm = VectorMath.dotProduct(sv, sv) + VectorMath.dotProduct(tv, tv) - sim;
		return sim / norm;
	}

	public static double JaroDistance(String s, String t) {
		int halflen = halfLengthOfShorter(s, t);
		String common1 = commonChars(s, t, halflen);
		String common2 = commonChars(t, s, halflen);
		if (common1.length() != common2.length())
			return 0;
		if (common1.length() == 0 || common2.length() == 0)
			return 0;
		int transpositions = transpositions(common1, common2);
		double dist = (common1.length() / ((double) s.length()) + common2.length() / ((double) t.length())
				+ (common1.length() - transpositions) / ((double) common1.length())) / 3.0;
		return dist;
	}

	public static double JaroWinklerDistance(String s, String t) {
		double dist = JaroDistance(s, t);
		if (dist < 0 || dist > 1)
			throw new IllegalArgumentException("innerDistance should produce scores between 0 and 1");
		int prefLength = commonPrefixLength(4, s, t);
		dist = dist + prefLength * 0.1 * (1 - dist);
		return dist;
	}

	public static void main(String[] args) {
		System.out.println("process begins.");
		{
			String a = "";
			String b = "ABC";
			//
			// for (int i = 0; i < 10; i++) {
			// System.out.printf("%d\t%s\t%s\n", i, CommonFuncs.sigmoid(-i), 1f
			// / Math.log(1 + i + 1));
			// }

			// System.out.println(editDistance(a, b));
			// System.out.println(getWeightedEditDistance(a, b, false));
			// System.out.println(getDamerauLevenshteinDistance(a, b, 1000));
			// System.out.println(getDamerauLevenshteinDistance(a, b, 1, 1, 1,
			// 1));
		}

		{
			// String s = "BBCDG";
			// String t = "AAABCDE";

			String s = "ABC";
			String t = "ABCC";
			System.out.println(editDistance(s, t));
			System.out.println(SmithWatermanSimilarity(s, t));
			System.out.println(NeedlemanWunschSimilarity(s, t));
			System.out.println(DiceSimilarity(s, t));

			System.out.println(JaccardSimilarity(s, t));
			System.out.println(CosineSimilarity(s, t));
			System.out.println(weightedEditDistance(s, t));
			System.out.println(DamerauLevenshteinDistance(s, t, 1000));
			System.out.println(DamerauLevenshteinDistance(s, t, 1, 1, 1, 1));
			System.out.println(smtpSimilarity(s, t));
		}

		// System.out.println(getEditDistance(a, b));

		System.out.println("process ends.");
	}

	public static double MongeElkan(String s, String t) {
		return 0;
	}

	public static double NeedlemanWunschSimilarity(String s, String t) {
		int len_s = s.length();
		int len_t = t.length();
		int d[][]; // matrix
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t

		if (len_s == 0)
			return 0;
		if (len_t == 0)
			return 0;

		d = new int[len_s + 1][len_t + 1];

		int cost = 0;
		int match_cost = 1;
		int unmatch_cost = -1;
		int gap_cost = -1;

		for (i = 0; i <= len_s; i++)
			d[i][0] = gap_cost * i;
		for (j = 0; j <= len_t; j++)
			d[0][j] = gap_cost * j;

		int delete_score = 0;
		int insert_score = 0;
		int replace_score = 0;

		for (i = 1; i <= len_s; i++) {
			s_i = s.charAt(i - 1);

			for (j = 1; j <= len_t; j++) {
				t_j = t.charAt(j - 1);

				cost = (s_i == t_j) ? match_cost : unmatch_cost;
				delete_score = d[i - 1][j] + gap_cost;
				insert_score = d[i][j - 1] + gap_cost;
				replace_score = d[i - 1][j - 1] + cost;
				d[i][j] = ArrayMath.max(new int[] { delete_score, insert_score, replace_score });
			}
		}
		double ret = d[len_s][len_t];

		double norm = Math.min(len_s, len_t) * match_cost;

		return ret;
	}

	public static double SmithWatermanSimilarity(String s, String t) {
		int len_s = s.length();
		int len_t = t.length();
		int d[][]; // matrix
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t

		if (len_s == 0)
			return 0;
		if (len_t == 0)
			return 0;

		d = new int[len_s + 1][len_t + 1];

		int gap_cost = -1;
		int match_cost = 2;
		int unmatch_cost = -1;

		int max_i = 0;
		int max_j = 0;
		int max = -Integer.MAX_VALUE;

		int delete_dist = 0;
		int insert_dist = 0;
		int replace_dist = 0;
		int cost = 0;
		int max_at = 0;

		for (i = 1; i <= len_s; i++) {
			s_i = s.charAt(i - 1);

			for (j = 1; j <= len_t; j++) {
				t_j = t.charAt(j - 1);

				cost = (s_i == t_j) ? match_cost : unmatch_cost;
				delete_dist = d[i - 1][j] + gap_cost;
				insert_dist = d[i][j - 1] + gap_cost;
				replace_dist = d[i - 1][j - 1] + cost;
				max_at = ArrayMath.max(new int[] { 0, delete_dist, insert_dist, replace_dist });
				d[i][j] = max_at;
				if (max_at > max) {
					max_i = i;
					max_j = j;
					max = max_at;
				}
			}
		}
		double sim = d[max_i][max_j];
		double norm = Math.min(len_s, len_t) * match_cost;

		return sim;
	}

	private static double smtpF(SparseVector s, SparseVector t, double lambda) {
		Set<Integer> ws = new HashSet<Integer>();
		for (SparseVector v : new SparseVector[] { s, t }) {
			for (int i : v.indexes()) {
				ws.add(i);
			}
		}

		double sigma = 2;
		double sim = 0;
		double norm = 0;

		for (int j : ws) {
			double v1 = s.valueAlways(j);
			double v2 = t.valueAlways(j);

			double ns = 0;

			if (v1 * v2 > 0) {
				ns = 0.5 * (1 + Math.exp(-Math.pow(((v1 - v2) / sigma), 2)));
			} else if (v1 == 0 && v2 == 0) {
			} else {
				ns = -lambda;
			}

			double nu = 0;
			if (v1 == 0 && v2 == 0) {
			} else {
				nu = 1;
			}
			sim += ns;
			norm += nu;
		}

		double ret = 0;

		if (norm != 0) {
			ret = sim / norm;
		}
		return ret;
	}

	/**
	 * Similarity Measure for Text Processing
	 *
	 * 1. Lin, Y.-S., Jiang, J.-Y., Lee, S.-J.: A Similarity Measure for Text Classification and Clustering. IEEE Transactions on Knowledge
	 * and Data Engineering. 26, 1575–1590 (2014).
	 * 
	 * 
	 * @param s
	 * @param t
	 * @return
	 */
	public static double smtpSimilarity(String s, String t) {
		SparseVector sv = VectorUtils.toSparseVector(getCharacterCounts(s));
		SparseVector tv = VectorUtils.toSparseVector(getCharacterCounts(t));
		double lambda = 1;
		double ret = (smtpF(sv, tv, lambda) + lambda) / (1 + lambda);
		return ret;
	}

	private static int transpositions(String common1, String common2) {
		int transpositions = 0;
		for (int i = 0; i < common1.length(); i++) {
			if (common1.charAt(i) != common2.charAt(i))
				transpositions++;
		}
		transpositions /= 2;
		return transpositions;
	}

	public static double weightedEditDistance(String s, String t) {
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

		// if (normalize) {
		// double sum = (n > m) ? ArrayMath.sum(ws1) : ArrayMath.sum(ws2);
		// ret = 1 - (ret / sum);
		// }
		return ret;
	}

}
