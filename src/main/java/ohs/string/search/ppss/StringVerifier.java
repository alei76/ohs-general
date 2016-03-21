package ohs.string.search.ppss;

import java.io.Serializable;

import ohs.math.ArrayMath;
import ohs.string.search.ppss.Gram.Type;

/**
 * A class of verifying grams outlined at Algorithm 4.
 * 
 * @author Heung-Seon Oh
 */
public class StringVerifier implements Serializable {

	public static double computeSubstringEditDistance(String g, String sub_r) {
		int n = g.length();
		int m = sub_r.length();
		int ed[][]; // matrix
		int i; // iterates through text
		int j; // iterates through t
		char s_i; // ith character of text
		char t_j; // jth character of t
		int cost; // cost

		ed = new int[n + 1][m + 1];

		for (i = 0; i <= n; i++) {
			ed[i][0] = i;
		}

		// for (j = 0; j <= m; j++) {
		// ed[0][j] = j;
		// }

		for (i = 1; i <= n; i++) {
			s_i = g.charAt(i - 1);
			for (j = 1; j <= m; j++) {
				t_j = sub_r.charAt(j - 1);
				cost = (s_i == t_j) ? 0 : 1;
				ed[i][j] = ArrayMath.min(new int[] { ed[i - 1][j] + 1, ed[i][j - 1] + 1, ed[i - 1][j - 1] + cost });
			}
		}

		int min = Integer.MAX_VALUE;

		for (i = 1; i <= n; i++) {
			if (ed[i][m] < min) {
				min = ed[i][m];
			}
		}

		int ret = min;

		// System.out.println(show(g, sub_r, ed));
		// System.out.printf("min:\t%d\n\n", min);

		return ret;
	}

	public static void main(String[] args) {
		{
			String g = "om";
			String sub_r = "beca";

			System.out.println(computeSubstringEditDistance(g, sub_r));
			System.out.println();

			// System.out.println(computeSubstringEditDistance(sub_r, g));
			// System.out.println();
		}

		{
			String g = "ot";
			String sub_r = "yoytu";
			System.out.println(computeSubstringEditDistance(g, sub_r));
			System.out.println();
		}

		// {
		// String g = "yotubecom";
		// String sub_r = "yoytubeca";
		// System.out.println(computeSubstringEditDistance(g, sub_r));
		// }
	}

	public static String show(String s, String t, int[][] d) {
		StringBuffer sb = new StringBuffer();
		sb.append("@");

		for (int i = 0; i < d[0].length; i++) {
			if (i == 0) {
				sb.append(" #");
			} else {
				sb.append(" " + t.charAt(i - 1));
			}
		}

		for (int i = 0; i < d.length; i++) {
			sb.append("\n");
			if (i == 0) {
				sb.append("#");
			} else {
				sb.append(s.charAt(i - 1));
			}

			for (int j = 0; j < d[i].length; j++) {
				sb.append(" " + d[i][j]);
			}
		}

		return sb.toString();
	}

	private int q;

	private int tau;

	private double[][] M;

	private double num_errors;

	private double ed;

	public StringVerifier(int q, int tau) {
		this.q = q;
		this.tau = tau;
	}

	public double computeEditDistance(String s, String r) {
		int n = s.length();
		int m = r.length();
		int ed[][]; // matrix
		int i; // iterates through text
		int j; // iterates through t
		char s_i; // ith character of text
		char t_j; // jth character of t
		int cost; // cost

		int min_i = 1;
		int min_j = 1;

		if (n == 0)
			return 1.0;
		if (m == 0)
			return 1.0;

		ed = new int[n + 1][m + 1];

		for (i = 0; i <= n; i++)
			ed[i][0] = i;

		for (j = 0; j <= m; j++)
			ed[0][j] = j;

		for (i = 1; i <= n; i++) {
			s_i = s.charAt(i - 1);
			for (j = 1; j <= m; j++) {
				t_j = r.charAt(j - 1);
				cost = (s_i == t_j) ? 0 : 1;
				ed[i][j] = ArrayMath.min(new int[] { ed[i - 1][j] + 1, ed[i][j - 1] + 1, ed[i - 1][j - 1] + cost });
			}
		}

		int ret = ed[n][m];

		// System.out.println(show(text, t, ed) + "\n\n");

		return ret;
	}

	public double getEditDistance() {
		return ed;
	}

	public double getNumErrors() {
		return num_errors;
	}

	public boolean verify(String s, Gram[] grams, String r) {
		double num_errors = 0;
		double ed = 0;

		// System.out.println("--------------------------------");

		boolean[] visited = new boolean[r.length()];

		for (int i = 0; i < grams.length; i++) {
			Gram gram = grams[i];
			if (gram.getType() != Type.PIVOT) {
				continue;
			}

			String g = gram.getString();

			int start = gram.getStart() - tau;
			// int end = gram.getStart() + q - 1 + tau;
			int end = gram.getStart() + q + tau;

			if (start < 0) {
				start = 0;
			}

			if (end > r.length()) {
				end = r.length();
			}

			if (start >= end) {
				continue;
			}

			boolean isVisited = false;

			for (int j = start; j < end; j++) {
				if (visited[j]) {
					isVisited = true;
					break;
				}
			}

			if (isVisited) {
				continue;
			}

			for (int j = start; j < end; j++) {
				visited[j] = true;
			}

			String sub_r = r.substring(start, end);
			double sed = computeSubstringEditDistance(g, sub_r);
			num_errors += sed;

			if (num_errors > tau) {
				return false;
			}
		}

		ed = computeEditDistance(s, r);

		if (ed <= tau) {
			return true;
		} else {
			return false;
		}
	}
}
