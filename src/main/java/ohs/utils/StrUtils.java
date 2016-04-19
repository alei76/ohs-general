package ohs.utils;

import java.lang.Character.UnicodeBlock;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohs.math.ArrayMath;
import ohs.nlp.ling.types.TextSpan;
import ohs.types.Counter;

/**
 * 
 * @author Heung-Seon Oh
 * @version 1.0
 * @date 2009. 12. 8
 * 
 */

public class StrUtils {

	public static final String LINE_SEP = System.getProperty("line.separator");

	private static Pattern p = Pattern.compile("\\d+[\\d,\\.]*");

	public static int absLengthDiff(String a, String b) {
		return Math.abs(lengthDiff(a, b));
	}

	public static String[] asArray(Collection<String> a) {
		return a.toArray(new String[a.size()]);
	}

	public static String[] asArray(String... a) {
		String[] ret = new String[a.length];
		int loc = 0;
		for (String b : a) {
			ret[loc++] = b;
		}
		return ret;
	}

	public static Character[] asCharacters(char[] a) {
		Character[] ret = new Character[a.length];
		for (int i = 0; i < a.length; i++) {
			ret[i] = new Character(a[i]);
		}
		return ret;
	}

	public static Character[] asCharacters(String s) {
		return asCharacters(s.toCharArray());
	}

	public static char[] asChars(Character[] a) {
		char[] ret = new char[a.length];
		for (int i = 0; i < a.length; i++) {
			ret[i] = a[i].charValue();
		}
		return ret;
	}

	public static char[] asChars(List<Character> a) {
		char[] ret = new char[a.size()];
		for (int i = 0; i < a.size(); i++) {
			ret[i] = a.get(i).charValue();
		}
		return ret;
	}

	public static List<String> asList(String[] s) {
		return Arrays.asList(s);
	}

	public static String[] copy(String[] a) {
		String[] b = new String[a.length];
		copy(a, b);
		return b;
	}

	public static void copy(String[] a, String[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i];
		}
	}

	/**
	 * Strings.java in mallet
	 * 
	 * 
	 * @param text
	 * @param t
	 * @param normalize
	 * @return
	 */
	public static double editDistance(String s, String t, boolean normalize) {
		int n = s.length();
		int m = t.length();
		int d[][]; // matrix
		int i; // iterates through text
		int j; // iterates through t
		char s_i; // ith character of text
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

	public static List<TextSpan> extract(String text) throws Exception {
		Set<String> tagNames = null;
		return extract(text, tagNames, false);
	}

	public static List<TextSpan> extract(String t, Set<String> tagNames, boolean get_start_at_plain) throws Exception {
		List<TextSpan> ret = Generics.newArrayList();

		int start_at_tagged = -1;
		int ext_len = 0;

		int START_TAG_EXT_LEN = 2;
		int END_TAG_EXT_LEN = 3;

		char OPEN_CHAR = '<';
		char CLOSE_CHAR = '>';

		for (int i = 0; i < t.length();) {
			if (t.charAt(i) == OPEN_CHAR) {
				if ((i + 1) < t.length() && t.charAt(i + 1) == '/') {
					StringBuffer sb = new StringBuffer();
					for (int j = i + 2; j < t.length(); j++) {
						if (t.charAt(j) == CLOSE_CHAR) {
							break;
						}
						sb.append(t.charAt(j));
					}

					int tag_len = sb.length();

					if (tagNames.contains(sb.toString())) {
						String value = t.substring(start_at_tagged, i);
						int start_at_plain = i - ext_len - tag_len;

						ret.add(new TextSpan(get_start_at_plain ? start_at_plain : start_at_tagged, value));
						// System.out.println(value);

						ext_len += (tag_len + END_TAG_EXT_LEN);
						i += (tag_len + 2);
					} else {
						i++;
					}
				} else {
					StringBuffer sb = new StringBuffer();
					int last_j = 0;
					for (int j = i + 1; j < t.length(); j++) {
						// System.out.println(markAt(t, j, false));
						if (t.charAt(j) == CLOSE_CHAR) {
							last_j = j;
							break;
						}
						sb.append(t.charAt(j));
					}

					if (tagNames.contains(sb.toString())) {
						start_at_tagged = last_j + 1;
						int tag_len = sb.length();
						ext_len += (tag_len + START_TAG_EXT_LEN);
						i = last_j + 1;
					} else {
						i++;
					}
				}
			} else {
				i++;
			}
		}

		return ret;
	}

	public static List<TextSpan> extract(String text, String tagName) throws Exception {
		Set<String> tagNames = Generics.newHashSet();
		tagNames.add(tagName);
		return extract(text, tagNames, false);
	}

	public static List<TextSpan> extract(String text, String tagName, boolean get_plain_start) throws Exception {
		Set<String> tagNames = Generics.newHashSet();
		tagNames.add(tagName);
		return extract(text, tagNames, get_plain_start);
	}

	public static boolean find(String text, Pattern p) {
		return p.matcher(text).find();
	}

	public static boolean find(String text, String regex) {
		return find(text, Pattern.compile(regex));
	}

	public static Matcher getMatcher(String text, String regex) {
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

	public static void init(String[] s) {
		for (int i = 0; i < s.length; i++) {
			s[i] = "";
		}
	}

	public static boolean isLowercase(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isLowerCase(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isUppercase(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isUpperCase(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static String join(String glue, Iterable<String> a) {
		return join(glue, a, 0, Integer.MAX_VALUE);
	}

	public static String join(String glue, Iterable<String> a, int start, int end) {
		StringBuffer sb = new StringBuffer();

		if (a instanceof List) {
			List<String> b = (List<String>) a;
			for (int i = start; i < end; i++) {
				sb.append(b.get(i));
				if (i != end - 1) {
					sb.append(glue);
				}
			}
		} else {
			Iterator<String> iter = a.iterator();
			int loc = 0;

			while (iter.hasNext()) {
				if (loc == end) {
					break;
				} else if (loc >= start && loc < end) {
					if (loc > start) {
						sb.append(glue);
					}
					sb.append(iter.next());
				}
				loc++;
			}
		}

		return sb.toString();
	}

	// public static String join(String glue, Collection<String> c, int start, int end) {
	// return join(glue, c, start, end);
	// }

	public static String join(String inGlue, String midGlue, String outGlue, String[][][] a) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < a.length; i++) {
			sb.append(join(inGlue, midGlue, a[i]));
			if (i != a.length - 1) {
				sb.append(outGlue);
			}
		}
		return sb.toString();
	}

	public static String join(String inGlue, String outGlue, String[] a, String[] b) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < a.length; i++) {
			sb.append(String.format("%s%s%s", a[i], inGlue, b[i]));
			if (i != a.length - 1) {
				sb.append(outGlue);
			}
		}
		return sb.toString();
	}

	public static String join(String inGlue, String outGlue, String[][] a) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < a.length; i++) {
			sb.append(join(inGlue, a[i]));
			if (i != a.length - 1) {
				sb.append(outGlue);
			}
		}
		return sb.toString();
	}

	public static String join(String glue, String[] a) {
		return join(glue, a, 0, a.length);
	}

	public static String join(String glue, String[] a, int start) {
		return join(glue, a, start, a.length);
	}

	public static String join(String glue, String[] a, int start, int end) {
		StringBuffer sb = new StringBuffer();
		if (start < 0) {
			start = 0;
		}

		if (end > a.length) {
			end = a.length;
		}

		for (int i = start; i < end; i++) {
			sb.append(a[i]);
			if (i != end - 1) {
				sb.append(glue);
			}
		}
		return sb.toString();
	}

	public static String join(String glue, String[] a, int[] is) {
		List<String> b = Generics.newArrayList();
		for (int i : is) {
			b.add(a[i]);
		}
		return join(glue, b, 0, b.size());
	}

	public static String[] join(String glue, String[] a, String[] b) {
		String[] c = new String[a.length];
		join(glue, a, b, c);
		return c;
	}

	public static void join(String glue, String[] a, String[] b, String[] c) {
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] + glue + b[i];
		}
	}

	public static int lengthDiff(String a, String b) {
		return a.length() - b.length();
	}

	public static void main(String[] args) throws Exception {
		String text = ">>><><><KWD>태깅</><KWD>태깅</KWD>";
		// String text = ">>><><>태깅</>태깅";

		// text = tag(text, "키워드", "KWD");

		List<TextSpan> textSpans = extract(text, "KWD", false);

		for (TextSpan span : textSpans) {
			System.out.println(span + " -> " + text.substring(span.getStart(), span.getEnd()));
		}

		// System.out.println(textSpans);
	}

	public static String markAt(String s, int i, boolean vertical) {
		StringBuffer sb = new StringBuffer();
		if (vertical) {
			for (int k = 0; k < s.length(); k++) {
				sb.append(String.format("%d:\t%c\t%s", k, s.charAt(k), i == k ? "#" : ""));
				if (k != s.length() - 1) {
					sb.append("\n");
				}
			}
		} else {

			for (int k = 0; k < s.length(); k++) {
				sb.append(s.charAt(k));
			}
			sb.append("\n");
			for (int k = 0; k < s.length(); k++) {
				if (k > i) {
					break;
				}
				sb.append(i == k ? String.format("^(%c at %d)", s.charAt(i), i) : "#");
			}
		}
		return sb.toString();
	}

	public static String[] newStrings(int size) {
		return new String[size];
	}

	public static Counter<String> ngrams(int ngram_order, List<String> words) {
		Counter<String> ret = new Counter<String>();
		for (int j = 0; j < words.size() - ngram_order + 1; j++) {
			StringBuffer sb = new StringBuffer();
			int size = 0;
			for (int k = j; k < j + ngram_order; k++) {
				sb.append(words.get(k));
				if (k != (j + ngram_order) - 1) {
					sb.append("_");
				}
				size++;
			}
			assert ngram_order == size;
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

	public static String normalizeSpaces(String s) {
		return s.replaceAll("[\\s]+", " ").trim();
	}

	public static String[] normalizeSpaces(String[] s) {
		for (int i = 0; i < s.length; i++) {
			s[i] = normalizeSpaces(s[i]);
		}
		return s;
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

	public static String[] replace(String[] s, String target, String replacement) {
		String[] ret = new String[s.length];
		for (int i = 0; i < s.length; i++) {
			if (s[i] == null || s[i].toString().equals(target)) {
				ret[i] = replacement;
			} else {
				ret[i] = s[i].toString();
			}
		}
		return ret;
	}

	public static String separateBrackets(String s) {
		StringBuffer sb = new StringBuffer();
		sb.append(s.charAt(0));

		for (int i = 1; i < s.length(); i++) {
			char prevCh = s.charAt(i - 1);
			char currCh = s.charAt(i);
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

	public static List<String> split(String s) {
		return split("[\\s]+", s);
	}

	public static List<String> split(String delim, String s) {
		ArrayList<String> ret = new ArrayList<String>();
		for (String tok : s.split(delim)) {
			if (tok.length() > 0) {
				ret.add(tok);
			}
		}
		ret.trimToSize();
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

	public static String[] split2Two(String delim, String s) {
		String[] ret = null;
		int idx = s.lastIndexOf(delim);

		if (idx > -1) {
			ret = new String[2];
			ret[0] = s.substring(0, idx);
			ret[1] = s.substring(idx + 1);
		}
		return ret;
	}

	public static List<String> splitPunctuations(String s) {
		return split("[\\s\\p{Punct}]+", s);
	}

	public static String substring(String text, String startText, String endText) {
		int start = text.indexOf(startText) + startText.length();
		int end = text.indexOf(endText);
		return text.substring(start, end);
	}

	public static String[] subTokens(String[] toks, int start, int end) {
		String[] ret = new String[end - start];
		for (int i = start, j = 0; i < toks.length && i < end; i++) {
			ret[j] = toks[i];
		}
		return ret;
	}

	public static String tag(String t, Collection<String> targets, String tagName) throws Exception {
		StringBuffer sb = new StringBuffer();

		Pattern p = Pattern.compile(String.format("(%s)", String.join("|", targets)), Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(t);

		while (m.find()) {
			String g = m.group();
			m.appendReplacement(sb, String.format("<%s>%s</%s>", tagName, g, tagName));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	public static String tag(String t, String taget, String tagName) throws Exception {
		List<String> targets = Generics.newArrayList();
		targets.add(taget);
		return tag(t, targets, tagName);
	}

	public static String toString(char[] chs) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < chs.length; i++) {
			sb.append(String.format("%d\t%c", i, chs[i]));
			if (i != chs.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public static String toString(Object[] array, String delim) {
		StringBuffer sb = new StringBuffer();
		String separator = delim == null ? "\n" : delim;
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

	public static String[] trim(String[] a) {
		String[] b = new String[a.length];
		trim(a, b);
		return b;
	}

	public static void trim(String[] a, String[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i].trim();
		}
	}

	public static String unwrap(String a) {
		return unwrap(a, "\"", "\"");
	}

	public static String unwrap(String a, String open, String close) {
		return a.substring(open.length(), a.length() - close.length());
	}

	public static String[] unwrap(String[] a) {
		String[] b = new String[a.length];
		unwrap(a, "\"", "\"", b);
		return b;
	}

	public static String[] unwrap(String[] a, String open, String close, String[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = unwrap(a[i], open, close);
		}
		return b;
	}

	public static String wrap(String a) {
		return wrap(a, "\"", "\"");
	}

	public static String wrap(String a, String open, String close) {
		return String.format("%s%s%s", open, a, close);
	}

	public static String[] wrap(String[] a) {
		String[] b = new String[a.length];
		wrap(a, "\"", "\"", b);
		return b;
	}

	public static void wrap(String[] a, String open, String close, String[] b) {
		for (int i = 0; i < a.length; i++) {
			b[i] = wrap(a[i], open, close);
		}
	}

};