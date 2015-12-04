package ohs.utils;

import java.lang.Character.UnicodeBlock;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohs.math.ArrayMath;
import ohs.types.Counter;

/**
 * 
 * @author Heung-Seon Oh
 * @version 1.0
 * @date 2009. 12. 8
 * 
 */

public class StrUtils {

	public static double DamerauLevenDistance(String s, String t, boolean normalize) {
		int n = s.length();
		int m = t.length();
		int d[][]; // matrix
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		if (n == 0)
			return 1.0;
		if (m == 0)
			return 1.0;

		d = new int[n + 1][m + 1];

		for (i = 0; i <= n; i++)
			d[i][0] = i;

		for (j = 0; j <= m; j++)
			d[0][j] = j;

		for (i = 1; i <= n; i++) {
			s_i = s.charAt(i - 1);

			for (j = 1; j <= m; j++) {
				t_j = t.charAt(j - 1);

				cost = (s_i == t_j) ? 0 : 1;
				int delete = d[i - 1][j] + 1;
				int insert = d[i][j - 1] + 1;
				int substitute = d[i - 1][j - 1] + cost;
				d[i][j] = ArrayMath.min(new int[] { delete, insert, substitute });
			}
		}

		int longer = (n > m) ? n : m;
		double ret = normalize ? (double) d[n][m] / longer : (double) d[n][m];
		return ret;
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
	public static double editDistance(String s, String t, boolean normalize) {
		int n = s.length();
		int m = t.length();
		int d[][]; // matrix
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		if (n == 0)
			return 1.0;
		if (m == 0)
			return 1.0;

		d = new int[n + 1][m + 1];

		for (i = 0; i <= n; i++)
			d[i][0] = i;

		for (j = 0; j <= m; j++)
			d[0][j] = j;

		for (i = 1; i <= n; i++) {
			s_i = s.charAt(i - 1);

			for (j = 1; j <= m; j++) {
				t_j = t.charAt(j - 1);

				cost = (s_i == t_j) ? 0 : 1;
				int delete = d[i - 1][j] + 1;
				int insert = d[i][j - 1] + 1;
				int substitute = d[i - 1][j - 1] + cost;
				d[i][j] = ArrayMath.min(new int[] { delete, insert, substitute });
			}
		}

		int longer = (n > m) ? n : m;
		double ret = normalize ? (double) d[n][m] / longer : (double) d[n][m];
		return ret;
	}

	public static boolean find(String text, Pattern p) {
		return p.matcher(text).find();
	}

	public static boolean find(String text, String regex) {
		return find(text, Pattern.compile(regex));
	}

	public static String getItem(String[] toks, int index, String defaultReturn) {
		String ret = defaultReturn;
		if (index < toks.length) {
			ret = toks[index];
		}
		return ret;
	}

	public static Matcher getMatcher(String regex, String text) {
		Pattern p = getPattern(regex);
		Matcher m = p.matcher(text);
		return m;
	}

	public static List<Matcher> getMatchers(String text, Pattern p) {
		List<Matcher> ret = new ArrayList<Matcher>();
		boolean found = false;
		int loc = 0;
		do {
			Matcher m = p.matcher(text);
			found = m.find(loc);
			if (found) {
				ret.add(m);
				loc = m.end();
			}
		} while (found);
		return ret;
	}

	public static Pattern getPattern(String regex) {
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		return p;
	}

	public static String[] getTags(String text) {
		String[][] splits = splitWordsAndTags(text.split(" "));
		return splits[1];
	}

	public static String getTagsString(String text) {
		return join(" ", getTags(text));
	}

	public static String[] getWords(String text) {
		String[][] splits = splitWordsAndTags(text.split(" "));
		return splits[0];
	}

	public static String getWordsString(String text) {
		return join(" ", getWords(text));
	}

	public static String join(String glue, List<String> list) {
		return join(glue, list, 0, list.size());
	}

	public static String join(String glue, List<String> list, int start) {
		return join(glue, list, start, list.size());
	}

	public static String join(String glue, List<String> list, int start, int end) {
		StringBuffer sb = new StringBuffer();

		if (end > list.size()) {
			end = list.size();
		}

		if (start < 0) {
			start = 0;
		}

		for (int i = start; i < end; i++) {
			sb.append(list.get(i).toString() + (i == end - 1 ? "" : glue));
		}
		return sb.toString();
	}

	public static String join(String glue, String[] array) {
		return join(glue, array, 0, array.length);
	}

	public static String join(String glue, String[] array, int start) {
		return join(glue, array, start, array.length);
	}

	public static String join(String glue, String[] array, int start, int end) {
		StringBuffer sb = new StringBuffer();
		if (start < 0) {
			start = 0;
		}

		if (end > array.length) {
			end = array.length;
		}

		for (int i = start; i < end; i++) {
			sb.append((array[i] == null ? "null" : array[i]) + (i == end - 1 ? "" : glue));
		}
		return sb.toString();
	}

	public static String join(String glue, String[] array, int[] indexList) {
		List<String> list = new ArrayList<String>();
		for (int index : indexList) {
			list.add(array[index]);
		}
		return join(glue, list);
	}

	public static String[] mergeToArray(String[] words, String[] tags) {
		String[] ret = new String[words.length];

		for (int i = 0; i < words.length; i++) {
			ret[i] = words[i] + "/" + tags[i];
		}
		return ret;
	}

	public static String mergeToString(String[] words, String[] tags) {
		StringBuffer sb = new StringBuffer();
		String[] toks = mergeToArray(words, tags);
		for (int i = 0; i < toks.length; i++) {
			sb.append(toks[i]);
			if (i != toks.length - 1) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	public static Counter<String> ngrams(int ngramOrder, String[] terms) {
		Counter<String> ret = new Counter<String>();
		for (int j = 0; j < terms.length - ngramOrder + 1; j++) {
			StringBuffer sb = new StringBuffer();
			int size = 0;
			for (int k = j; k < j + ngramOrder; k++) {
				sb.append(terms[k]);
				if (k != (j + ngramOrder) - 1) {
					sb.append("_");
				}
				size++;
			}
			assert ngramOrder == size;
			String ngram = sb.toString();
			ret.incrementCount(ngram, 1);
		}
		return ret;
	}

	public static String normalizeNonRegex(String regex) {
		// if (regex.contains("\\\\")) {
		// System.out.println();
		// }

		regex = regex.replace("(", "\\(");
		regex = regex.replace(")", "\\)");
		regex = regex.replace("?", "\\?");
		regex = regex.replace("|", "\\|");
		regex = regex.replace("+", "\\+");
		regex = regex.replace("{", "\\{");
		regex = regex.replace("}", "\\}");
		regex = regex.replace("]", "\\]");
		regex = regex.replace("[", "\\]");
		regex = regex.replace(".", "\\.");
		regex = regex.replace("$", "\\$");
		regex = regex.replace("^", "\\^");
		return regex;
	}

	private static Pattern p = Pattern.compile("\\d+[\\d,\\.]*");

	public static String normalizeNumbers(String s) {
		Matcher m = p.matcher(s);

		if (m.find()) {
			StringBuffer sb = new StringBuffer();
			do {
				String g = m.group();
				g = g.replace(",", "");

				StringBuffer sb2 = new StringBuffer("<N");

				String[] toks = g.split("\\.");
				for (int j = 0; j < toks.length; j++) {
					String tok = toks[j];
					sb2.append(tok.length());

					if (j != toks.length - 1) {
						sb2.append("_");
					}
				}
				sb2.append(">");
				String r = sb2.toString();
				m.appendReplacement(sb, r);
			} while (m.find());
			m.appendTail(sb);
			s = sb.toString();
		}
		return s;
	}

	public static String normalizePunctuations(String s) {
		return normalizeSpaces(s.replaceAll("\\p{Punct}+", " "));
	}

	public static String normalizeSpaces(String text) {
		return text.replaceAll("[\\s]+", " ").trim();
	}

	public static String[] normalizeSpaces(String[] toks) {
		for (int i = 0; i < toks.length; i++) {
			toks[i] = normalizeSpaces(toks[i]);
		}
		return toks;
	}

	public static String normalizeSpecialCharacters(String text) {
		Pattern p = Pattern.compile("\\&[^\\&\\s;]+;");
		Matcher m = p.matcher(text);

		StringBuffer sb = new StringBuffer();

		while (m.find()) {
			String g = m.group();
			String r = "";

			if (g.equals("&lt;")) {
				r = "<";
			} else if (g.equals("&gt;")) {
				r = ">";
			} else if (g.equals("&apos;")) {
				r = "'";
			} else if (g.equals("&amp;")) {
				r = "&";
			} else {
				// System.out.printf("[ %s ]\n", g);
			}

			m.appendReplacement(sb, r);
		}
		m.appendTail(sb);
		return sb.toString();
	}

	public static String separateBracket(String text) {
		StringBuffer sb = new StringBuffer();
		sb.append(text.charAt(0));

		for (int i = 1; i < text.length(); i++) {
			char prevCh = text.charAt(i - 1);
			char currCh = text.charAt(i);
			if (prevCh == '(' || prevCh == ')' || prevCh == '[' || prevCh == ']' || prevCh == '{' || prevCh == '}' || prevCh == '<'
					|| prevCh == '>') {
				sb.append(currCh);
			}
		}

		return sb.toString();
	}

	public static String separateKorean(String text) {
		int[] types = new int[text.length()];

		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(ch);

			if (UnicodeBlock.HANGUL_SYLLABLES.equals(unicodeBlock)

					|| UnicodeBlock.HANGUL_COMPATIBILITY_JAMO.equals(unicodeBlock)

					|| UnicodeBlock.HANGUL_JAMO.equals(unicodeBlock)) {
				types[i] = 1;
			} else if (Character.isWhitespace(ch)) {
				types[i] = 2;
			}
		}

		StringBuffer sb = new StringBuffer();

		sb.append(text.charAt(0));

		for (int i = 1; i < text.length(); i++) {
			if ((types[i - 1] == 1 && types[i] == 0) || types[i - 1] == 0 && types[i] == 1) {
				sb.append(' ');
				sb.append(text.charAt(i));
			} else {
				sb.append(text.charAt(i));
			}
		}

		return sb.toString();
	}

	public static List<String> split(String text) {
		return split("[\\s]+", text);
	}

	public static List<String> split(String delimiter, String text) {
		List<String> ret = new ArrayList<String>();
		for (String tok : text.split(delimiter)) {
			ret.add(tok);
		}
		return ret;
	}

	public static String[][] split(String[] array, int[] indexList) {
		Set<Integer> set = new HashSet<Integer>();
		for (int index : indexList) {
			set.add(index);
		}
		List<Object> list1 = new ArrayList<Object>();
		List<Object> list2 = new ArrayList<Object>();

		for (int i = 0; i < array.length; i++) {
			if (set.contains(i)) {
				list1.add(array[i]);
			} else {
				list2.add(array[i]);
			}
		}

		String[][] ret = new String[2][];
		ret[0] = list1.toArray(new String[list1.size()]);
		ret[1] = list2.toArray(new String[list2.size()]);
		return ret;
	}

	public static String[] split2Two(String delimiter, String text) {
		String[] ret = null;
		int idx = text.lastIndexOf(delimiter);

		if (idx > -1) {
			ret = new String[2];
			ret[0] = text.substring(0, idx);
			ret[1] = text.substring(idx + 1);
		}
		return ret;
	}

	public static String[][] splitWordsAndTags(String[] toks) {
		String[][] ret = new String[2][toks.length];
		for (int i = 0; i < toks.length; i++) {
			String[] two = split2Two("/", toks[i]);
			ret[0][i] = two[0];
			ret[1][i] = two[1];
		}
		return ret;
	}

	public static String substring(String text, String startText, String endText) {
		int start = text.indexOf(startText) + startText.length();
		int end = text.indexOf(endText);
		return text.substring(start, end);
	}

	public static String[] subTokens(String[] toks, int start, int end) {
		String[] ret = new String[end - start];
		for (int i = start, loc = 0; i < end; i++, loc++) {
			ret[loc] = toks[i];
		}
		return ret;
	}

	public static String[] toArray(Collection<String> collection) {
		String[] ret = new String[collection.size()];
		int loc = 0;
		Iterator<String> iter = collection.iterator();
		while (iter.hasNext()) {
			ret[loc] = iter.next();
			loc++;
		}
		return ret;
	}

	public static String[] toArray(List<String> list) {
		return list.toArray(new String[list.size()]);
	}

	public static Character[] toCharacters(String text) {
		Character[] ret = new Character[text.length()];
		for (int i = 0; i < text.length(); i++) {
			ret[i] = new Character(text.charAt(i));
		}
		return ret;
	}

	public static List<String> toList(String[] array) {
		List<String> ret = new ArrayList<String>();
		for (String s : array) {
			ret.add(s);
		}
		return ret;
	}

	public static String toString(Object[] array, String delimiter) {
		StringBuffer sb = new StringBuffer();
		String separator = delimiter == null ? "\n" : delimiter;
		for (int i = 0; i < array.length; i++) {
			sb.append(array[i].toString() + (i == array.length - 1 ? "" : separator));
		}
		return sb.toString();
	}

	public static String toString(String delim, Counter<String> counter, NumberFormat nf) {
		if (nf == null) {
			nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(4);
			nf.setGroupingUsed(false);
		}

		StringBuffer sb = new StringBuffer();
		List<String> keys = counter.getSortedKeys();
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			double value = counter.getCount(key);
			sb.append(String.format("%s:%s", key.toString(), nf.format(value)));

			if (i != keys.size() - 1) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}

};