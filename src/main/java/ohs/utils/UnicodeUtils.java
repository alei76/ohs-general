package ohs.utils;

import java.util.Map;

import ohs.io.TextFileWriter;
import ohs.math.ArrayUtils;
import ohs.nlp.pos.NLPPath;

/**
 * http://www.elex.pe.kr/entry/유니코드에서의-한글
 *
 */
public class UnicodeUtils {

	public static final int[] HANGUL_SYLLABLES_RANGE = { 0xAC00, 0xD7A3 + 1 };

	public static final int[] HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE = { 0x314F, 0x3163 + 1 };

	public static final int[] HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE = { 0x3131, 0x314E + 1 };

	public static final int[] OLD_JAEUM_RANGE = { 0x3165, 0x318E + 1 };

	public static final int CHAEUM = 0x3164;

	public static final int CHOSUNG_SIZE = 19;

	public static final int JUNGSUNG_SIZE = 21;

	public static final int JONGSUNG_SIZE = 28;

	public static final int[] HANGUL_JAMO_CHOSUNG_RANGE = { 0x1100, 0x1100 + CHOSUNG_SIZE };

	public static final int[] HANGUL_JAMO_JUNGSUNG_RANGE = { 0x1161, 0x1161 + JUNGSUNG_SIZE };

	public static final int[] HANGUL_JAMO_JONGSUNG_RANGE = { 0x11A7, 0x11A7 + JONGSUNG_SIZE };

	public static final int DENOM_CHOSUNG = JUNGSUNG_SIZE * JONGSUNG_SIZE;

	public static final int DENOM_JONGSUNG = JONGSUNG_SIZE;

	private static final Map<Integer, Integer[]> m;

	static {
		int size1 = HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[1] - HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0];
		int size2 = HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[1] - HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[0];
		int size = size1 + size2;

		m = Generics.newHashMap(size);
		for (int i = 0; i < size; i++) {
			m.put(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0] + i, new Integer[3]);
		}

		int i = 0;
		int j = 1;
		int k = 2;
		int jaeum_start = HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0];
		int moeum_start = HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[0];
		int chosung_start = HANGUL_JAMO_CHOSUNG_RANGE[0];
		int jungsung_start = HANGUL_JAMO_JUNGSUNG_RANGE[0];
		int jongsung_start = HANGUL_JAMO_JONGSUNG_RANGE[0];

		/*
		 * ㄱ
		 */

		m.get(jaeum_start)[i] = chosung_start;
		m.get(jaeum_start)[k] = jongsung_start + 1;

		/*
		 * ㄲ
		 */

		m.get(jaeum_start + 1)[i] = chosung_start + 1;
		m.get(jaeum_start + 1)[k] = jongsung_start + 2;

		/*
		 * ㄳ
		 */
		m.get(jaeum_start + 2)[k] = jongsung_start + 3;

		/*
		 * ㄴ
		 */
		m.get(jaeum_start + 3)[i] = chosung_start + 2;
		m.get(jaeum_start + 3)[k] = jongsung_start + 4;

		/*
		 * ㄵ
		 */
		m.get(jaeum_start + 4)[k] = jongsung_start + 5;

		/*
		 * ㄶ
		 */
		m.get(jaeum_start + 5)[k] = jongsung_start + 6;

		/*
		 * ㄷ
		 */
		m.get(jaeum_start + 6)[i] = chosung_start + 3;
		m.get(jaeum_start + 6)[k] = jongsung_start + 7;

		/*
		 * ㄸ
		 */
		m.get(jaeum_start + 7)[i] = chosung_start + 4;

		/*
		 * ㄹ
		 */
		m.get(jaeum_start + 8)[i] = chosung_start + 5;
		m.get(jaeum_start + 8)[k] = jongsung_start + 8;

		/*
		 * ㄺ,ㄻ,ㄼ,ㄽ,ㄾ,ㄿ,ㅀ
		 */
		m.get(jaeum_start + 9)[k] = jongsung_start + 9;
		m.get(jaeum_start + 10)[k] = jongsung_start + 10;
		m.get(jaeum_start + 11)[k] = jongsung_start + 11;
		m.get(jaeum_start + 12)[k] = jongsung_start + 12;
		m.get(jaeum_start + 13)[k] = jongsung_start + 13;
		m.get(jaeum_start + 14)[k] = jongsung_start + 14;
		m.get(jaeum_start + 15)[k] = jongsung_start + 15;

		/*
		 * ㅁ
		 */
		m.get(jaeum_start + 16)[i] = chosung_start + 6;
		m.get(jaeum_start + 16)[k] = jongsung_start + 16;

		/*
		 * ㅂ
		 */
		m.get(jaeum_start + 17)[i] = chosung_start + 7;
		m.get(jaeum_start + 17)[k] = jongsung_start + 17;

		/*
		 * ㅂㅂ
		 */
		m.get(jaeum_start + 18)[i] = chosung_start + 8;

		/*
		 * ㅄ
		 */
		m.get(jaeum_start + 19)[k] = jongsung_start + 18;

		/*
		 * ㅅ
		 */
		m.get(jaeum_start + 20)[i] = chosung_start + 9;
		m.get(jaeum_start + 20)[k] = jongsung_start + 19;

		/*
		 * ㅅㅅ
		 */
		m.get(jaeum_start + 21)[i] = chosung_start + 10;
		m.get(jaeum_start + 21)[k] = jongsung_start + 20;

		/*
		 * ㅇ
		 */
		m.get(jaeum_start + 22)[i] = chosung_start + 11;
		m.get(jaeum_start + 22)[k] = jongsung_start + 21;

		/*
		 * ㅈ
		 */
		m.get(jaeum_start + 23)[i] = chosung_start + 12;
		m.get(jaeum_start + 23)[k] = jongsung_start + 22;

		/*
		 * ㅈㅈ
		 */
		m.get(jaeum_start + 24)[i] = chosung_start + 13;

		/*
		 * ㅊ
		 */
		m.get(jaeum_start + 25)[i] = chosung_start + 14;
		m.get(jaeum_start + 25)[k] = jongsung_start + 23;

		/*
		 * ㅋ
		 */
		m.get(jaeum_start + 26)[i] = chosung_start + 15;
		m.get(jaeum_start + 26)[k] = jongsung_start + 24;

		/*
		 * ㅌ
		 */
		m.get(jaeum_start + 27)[i] = chosung_start + 16;
		m.get(jaeum_start + 27)[k] = jongsung_start + 25;

		/*
		 * ㅍ
		 */
		m.get(jaeum_start + 28)[i] = chosung_start + 17;
		m.get(jaeum_start + 28)[k] = jongsung_start + 26;

		/*
		 * ㅎ
		 */
		m.get(jaeum_start + 29)[i] = chosung_start + 18;
		m.get(jaeum_start + 29)[k] = jongsung_start + 27;

		/*
		 * ㅏ,ㅐ,ㅑ,ㅒ,ㅓ,ㅔ,ㅕ,ㅖ,ㅐ,ㅗ,ㅘ,ㅙ,ㅚ,ㅛ,ㅜ,ㅝ,ㅞ,ㅟ,ㅠ,ㅟ,ㅣ
		 */

		for (int ss = HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[0]; ss < HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[1]; ss++) {
			m.get(ss)[j] = jungsung_start++;
		}
	}

	public static final int[] ENGLISH_LOWER_RANGE = { 'a', 'z' + 1 };

	public static final int[] ENGLISH_UPPER_RANGE = { 'A', 'Z' + 1 };

	public static final int[] NUMBER_RANGE = { '0', '9' + 1 };

	public static String composeJamo(String word) {
		StringBuffer sb = new StringBuffer();
		int len = word.length();

		int[] cps = getCodePoints(word.toCharArray());

		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			int cp = cps[i];

			if (isInRange(HANGUL_SYLLABLES_RANGE, cp)) {
				int end = i;
				int offset = i + 1;

				if (offset < len) {
					if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cps[offset])) {
						end = offset;
					}
				}

				int dist = end - i;

				boolean success = false;

				if (dist == 1) {
					int[] tmp_cps = toJamo(cp);
					if (tmp_cps[2] == 0) {
						Integer cc = m.get(cps[end])[2];

						if (cc != null) {
							tmp_cps[2] = cc.intValue();

							int rcp = fromAnalyzedJAMO(tmp_cps);

							if (isInRange(HANGUL_SYLLABLES_RANGE, rcp)) {
								char res = (char) rcp;
								sb.append(res);
								i = end;

								success = true;
							}
						}
					}
				}

				if (!success) {
					sb.append(c);
				}
			} else if (isInRange(HANGUL_JAMO_CHOSUNG_RANGE, cp)) {
				int end = i;
				int offset = i + 1;

				if (offset < len) {
					if (isInRange(HANGUL_JAMO_JUNGSUNG_RANGE, cps[offset])) {
						end = offset;
						offset++;
						if (offset < len) {
							if (isInRange(HANGUL_JAMO_JONGSUNG_RANGE, cps[offset])) {
								end = offset;
							}
						}
					}
				}

				int dist = end - i;

				boolean success = false;

				if (dist > 1) {
					int[] subcps = new int[3];

					ArrayUtils.copySubarray(cps, i, end + 1, subcps);

					int rcp = fromAnalyzedJAMO(subcps);

					if (isInRange(HANGUL_SYLLABLES_RANGE, rcp)) {
						char res = (char) rcp;
						sb.append(res);
						i = end;

						success = true;
					}

				}

				if (!success) {
					sb.append(c);
				}
			} else if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cp)) {
				int end = i;
				int offset = i + 1;

				if (offset < len) {
					if (isInRange(HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE, cps[offset])) {
						end = offset;
						offset++;
						if (offset < len) {
							if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cps[offset])) {
								end = offset;
							}
						}
					}
				}

				int dist = end - i;

				boolean success = false;

				if (dist > 1) {
					int[] subcps = new int[3];

					ArrayUtils.copySubarray(cps, i, end + 1, subcps);

					subcps = mapCJJCodes(subcps);

					int rcp = fromAnalyzedJAMO(subcps);

					if (isInRange(HANGUL_SYLLABLES_RANGE, rcp)) {
						char res = (char) rcp;
						sb.append(res);
						i = end;
						success = true;
					}
				}

				if (!success) {
					sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	public static char[] decomposeToJamo(char c) {
		char[] ret = new char[] { c };
		int cp = (int) c;

		if (isInRange(HANGUL_SYLLABLES_RANGE, cp)) {
			// System.out.printf("한글: %c\n", c);
			ret = toJamo(c);
		} else if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cp)) {
			// System.out.printf("자음: %c\n", c);
		} else if (isInRange(HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE, cp)) {
			// System.out.printf("모음: %c\n", c);
		} else if (cp == CHAEUM) {
			// System.out.printf("채움코드: %c\n", c);
		} else if (isInRange(OLD_JAEUM_RANGE, cp)) {
			// System.out.printf("옛글 자모: %c\n", c);
		} else {

		}

		return ret;
	}

	public static String decomposeToJamo(String word) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < word.length(); i++) {
			for (char c : decomposeToJamo(word.charAt(i))) {
				sb.append(c);
			}

			// if (isInRange(HANGUL_SYLLABLES_RANGE, cp)) {
			// // System.out.printf("한글: %c\n", c);
			// for (char cc : toJamo(c)) {
			// sb.append(cc);
			// }
			// } else if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cp)) {
			// // System.out.printf("자음: %c\n", c);
			// sb.append(c);
			// } else if (isInRange(HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE, cp)) {
			// // System.out.printf("모음: %c\n", c);
			// sb.append(c);
			// } else if (cp == CHAEUM) {
			// // System.out.printf("채움코드: %c\n", c);
			// sb.append(c);
			// } else if (isInRange(OLD_JAEUM_RANGE, cp)) {
			// // System.out.printf("옛글 자모: %c\n", c);
			// } else {
			// sb.append(c);
			// }
		}
		return sb.toString();
	}

	public static int fromAnalyzedJAMO(int[] cps) {
		if (cps.length != 3) {
			throw new IndexOutOfBoundsException();
		}

		int[] locs = new int[3];
		locs[0] = cps[0] - HANGUL_JAMO_CHOSUNG_RANGE[0]; // [4352, 4371]
		locs[1] = cps[1] - HANGUL_JAMO_JUNGSUNG_RANGE[0]; // [4449, 4470]

		if (cps[2] != 0) {
			locs[2] = cps[2] - HANGUL_JAMO_JONGSUNG_RANGE[0]; // [4519, 4547]
		}

		int cp = locs[0] * DENOM_CHOSUNG + locs[1] * DENOM_JONGSUNG + locs[2];
		cp += HANGUL_SYLLABLES_RANGE[0];

		return cp;
	}

	public static int[] getCodePoints(char[] cs) {
		int[] ret = new int[cs.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = Character.codePointAt(cs, i);
		}
		return ret;
	}

	public static boolean isEnglish(char c) {
		int i = c;
		return isInRange(ENGLISH_LOWER_RANGE, i) || isInRange(ENGLISH_UPPER_RANGE, i);
	}

	public static boolean isInRange(int[] range, int cp) {
		return Conditions.isInArrayRange(range[0], range[1], cp);
	}

	public static boolean isKorean(char c) {

		return isInRange(HANGUL_SYLLABLES_RANGE, c)

				|| isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, c)

				|| isInRange(HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE, c)

				|| isInRange(HANGUL_JAMO_CHOSUNG_RANGE, c)

				|| isInRange(HANGUL_JAMO_JUNGSUNG_RANGE, c)

				|| isInRange(HANGUL_JAMO_JONGSUNG_RANGE, c)

		;
	}

	public static boolean isKorean(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!isKorean(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isNumber(char c) {
		return isInRange(NUMBER_RANGE, c);
	}

	public static void main(String args[]) {
		// {
		// writeMaps();
		// }

		{
			String[] words = { "가ㄴ장ㄱㅏㄴ장" };

			for (int i = 0; i < words.length; i++) {
				String word = words[i];
				String word2 = composeJamo(word);
				System.out.println(word2);
			}
		}

	}

	public static int[] mapCJJCodes(int[] cps) {
		if (cps.length != 3) {
			throw new IllegalArgumentException();
		}
		int[] ret = new int[cps.length];
		for (int i = 0; i < cps.length; i++) {
			int cp = cps[i];

			Integer[] triple = m.get(cp);

			if (triple == null) {
				throw new IllegalArgumentException();
			}
			ret[i] = triple[i];
		}
		return ret;
	}

	public static final char[] toJamo(char c) {
		int[] cps = toJamo((int) c);
		char[] ret = new char[3];
		for (int i = 0; i < cps.length; i++) {
			if (cps[i] != 0) {
				ret[i] = (char) cps[i];
			}
		}
		return ret;
	}

	public static final int[] toJamo(int cp) {
		cp = cp - HANGUL_SYLLABLES_RANGE[0]; // korean 0~11,171

		int[] locs = new int[3];

		locs[0] = cp / DENOM_CHOSUNG; // chosung 0~18
		cp = cp % DENOM_CHOSUNG;
		locs[1] = cp / DENOM_JONGSUNG; // jungsung 0~20
		locs[2] = cp % DENOM_JONGSUNG; // josung 0~27

		locs[0] += HANGUL_JAMO_CHOSUNG_RANGE[0]; // [4352, 4371]
		locs[1] += HANGUL_JAMO_JUNGSUNG_RANGE[0]; // [4449, 4470]

		if (locs[2] != 0) {
			locs[2] += HANGUL_JAMO_JONGSUNG_RANGE[0]; // [4519, 4547]
		}

		return locs;
	}

	public static void writeMaps() {

		{

			TextFileWriter writer = new TextFileWriter(NLPPath.DATA_DIR + "자음.txt");
			for (int i = HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0], loc = 0; i < HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[1]; i++) {
				writer.write(String.format("%d\t%d\t%c\n", loc++, i, (char) i));
			}
			writer.close();
		}

		{
			TextFileWriter writer = new TextFileWriter(NLPPath.DATA_DIR + "모음.txt");
			for (int i = HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[0], loc = 0; i < HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[1]; i++) {
				writer.write(String.format("%d\t%d\t%c\n", loc++, i, (char) i));
			}
			writer.close();
		}

		{
			TextFileWriter writer = new TextFileWriter(NLPPath.DATA_DIR + "초성.txt");
			for (int i = HANGUL_JAMO_CHOSUNG_RANGE[0], loc = 0; i < HANGUL_JAMO_CHOSUNG_RANGE[1]; i++) {
				// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
				writer.write(String.format("%d\t%d\t%c\n", loc++, i, (char) i));
			}
			writer.close();
		}

		{
			TextFileWriter writer = new TextFileWriter(NLPPath.DATA_DIR + "중성.txt");
			for (int i = HANGUL_JAMO_JUNGSUNG_RANGE[0], loc = 0; i < HANGUL_JAMO_JUNGSUNG_RANGE[1]; i++) {
				writer.write(String.format("%d\t%d\t%c\n", loc++, i, (char) i));
			}
			writer.close();

		}

		{
			TextFileWriter writer = new TextFileWriter(NLPPath.DATA_DIR + "종성.txt");
			for (int i = HANGUL_JAMO_JONGSUNG_RANGE[0], loc = 0; i < HANGUL_JAMO_JONGSUNG_RANGE[1]; i++) {
				writer.write(String.format("%d\t%d\t%c\n", loc++, i, (char) i));
			}
			writer.close();
		}

		System.out.println();
	}

}
