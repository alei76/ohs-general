package ohs.utils;

import java.util.Map;

import ohs.io.TextFileWriter;
import ohs.nlp.pos.NLPPath;

/**
 * http://www.elex.pe.kr/entry/유니코드에서의-한글
 *
 */
public class KoreanUtils {

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

	// public static int[] codepoints(char[] cs) {
	// int[] ret = new int[cs.length];
	// for (int i = 0; i < ret.length; i++) {
	// ret[i] = Character.codePointAt(cs, i);
	// }
	// return ret;
	// }

	public static String composeJamo(String word) {
		StringBuffer sb = new StringBuffer();
		int len = word.length();

		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			int cp = word.codePointAt(i);

			if (isInRange(HANGUL_SYLLABLES_RANGE, cp)) {
				sb.append(c);
			} else if (isInRange(HANGUL_JAMO_CHOSUNG_RANGE, cp)) {
				int end = i;
				if (i + 1 < len) {
					if (isInRange(HANGUL_JAMO_JUNGSUNG_RANGE, word.codePointAt(i + 1))) {
						end = i + 1;
						if (i + 2 < len) {
							if (isInRange(HANGUL_JAMO_JONGSUNG_RANGE, word.codePointAt(i + 2))) {
								end = i + 2;
							}
						}
					}
				}

				int dist = end - i;

				if (dist > 1) {
					int[] subcps = new int[3];
					for (int j = i, loc = 0; j < end + 1; j++, loc++) {
						subcps[loc] = word.codePointAt(j);
					}
					char res = fromAnalyzedJAMO(subcps);
					sb.append(res);
					i = end;
				} else {
					sb.append(c);
				}
			} else if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cp)) {
				int end = i;
				if (i + 1 < len) {
					if (isInRange(HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE, word.codePointAt(i + 1))) {
						end = i + 1;
						if (i + 2 < len) {
							if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, word.codePointAt(i + 2))) {
								end = i + 2;
							}
						}
					}

				}

				int dist = end - i;

				if (dist > 1) {
					int[] subcps = new int[3];
					for (int j = i, loc = 0; j < end + 1; j++, loc++) {
						subcps[loc] = word.codePointAt(j);
					}
					subcps = mapCJJCodes(subcps);

					char res = fromAnalyzedJAMO(subcps);
					sb.append(res);
					i = end;
				} else {
					sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	public static String decomposeToJamo(String word) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			int cp = word.codePointAt(i);

			if (isInRange(HANGUL_SYLLABLES_RANGE, cp)) {
				// System.out.printf("한글: %c\n", c);
				for (char cc : toJamo(c)) {
					sb.append(cc);
				}
			} else if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cp)) {
				// System.out.printf("자음: %c\n", c);
				sb.append(c);
			} else if (isInRange(HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE, cp)) {
				// System.out.printf("모음: %c\n", c);
				sb.append(c);
			} else if (cp == CHAEUM) {
				// System.out.printf("채움코드: %c\n", c);
				sb.append(c);
			} else if (isInRange(OLD_JAEUM_RANGE, cp)) {
				// System.out.printf("옛글 자모: %c\n", c);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();

	}

	public static final char fromAnalyzedJAMO(int[] cps) {
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

		char ret = (char) cp;
		return ret;
	}

	public static boolean isInRange(int[] range, int cp) {
		if (cp >= range[0] && cp < range[1]) {
			return true;
		} else {
			return false;
		}
	}

	public static void main(String args[]) {
		// {
		// writeMaps();
		// }

		{
			String word = "가ㄱㅣㄷ각낙닫ㄱㄴㄷㄹㅁ";

			String word2 = decomposeToJamo(word);

			System.out.println(word2);

			String word3 = composeJamo(word2);

			System.out.println(word3);

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
		int[] cps = toJamo(Character.codePointAt(new char[] { c }, 0));
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < cps.length; i++) {
			sb.append(Character.toChars(cps[i]));
		}
		return sb.toString().toCharArray();
	}

	public static final int[] toJamo(int cp) {
		cp = cp - HANGUL_SYLLABLES_RANGE[0]; // korean 0~11,171

		int loc1 = cp / DENOM_CHOSUNG; // chosung 0~18
		cp = cp % DENOM_CHOSUNG;
		int loc2 = cp / DENOM_JONGSUNG; // jungsung 0~20
		int loc3 = cp % DENOM_CHOSUNG; // josung 0~27

		int size = loc3 == 0 ? 2 : 3;
		int[] locs = new int[size];

		locs[0] = HANGUL_JAMO_CHOSUNG_RANGE[0] + loc1; // [4352, 4371]
		locs[1] = HANGUL_JAMO_JUNGSUNG_RANGE[0] + loc2; // [4449, 4470]
		if (locs.length == 3) {
			locs[2] = HANGUL_JAMO_JONGSUNG_RANGE[0] + loc3; // [4519, 4547]
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
