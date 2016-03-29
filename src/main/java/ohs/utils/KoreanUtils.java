package ohs.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohs.io.TextFileWriter;
import ohs.math.ArrayUtils;
import ohs.nlp.pos.NLPPath;

/**
 * http://secr.tistory.com/207
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

	public static int[] codepoints(char[] cs) {
		int[] ret = new int[cs.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = Character.codePointAt(cs, i);
		}
		return ret;
	}

	public static char[] compose(char[] cs) {
		char ret = '1';

		int[] cps = codepoints(cs);

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < cps.length; i++) {
			char c = cs[i];
			int cp = cps[i];

			if (isInRange(HANGUL_SYLLABLES_RANGE, cp)) {
				sb.append(c);
			} else if (isInRange(HANGUL_JAMO_CHOSUNG_RANGE, cp)) {
				int end = i;
				if (i + 1 < cps.length) {
					if (isInRange(HANGUL_JAMO_JUNGSUNG_RANGE, cps[i + 1])) {
						end = i + 1;
					}
				}

				if (i + 2 < cps.length) {
					if (isInRange(HANGUL_JAMO_JONGSUNG_RANGE, cps[i + 2])) {
						end = i + 2;
					}
				}
				int[] subcps = new int[end - i + 1];
				for (int j = 0; j < subcps.length; j++) {
					subcps[j] = cps[i + j];
				}
				char res = fromAnalyzedJAMO(subcps);
				sb.append(res);
				i = end;
			} else if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cp)) {
				int end = i;
				if (i + 1 < cps.length) {
					if (isInRange(HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE, cps[i + 1])) {
						end = i + 1;
					}
				}

				if (i + 2 < cps.length) {
					if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cps[i + 2])) {
						end = i + 2;
					}
				}

				char[] subcs = new char[end - i + 1];
				for (int j = 0; j < subcs.length; j++) {
					subcs[j] = cs[i + j];
				}
				char res = fromNotAnalyzedJAMO(subcs);
				sb.append(res);
				i = end;
			} else {
				sb.append(c);
			}
		}

		return sb.toString().toCharArray();
	}

	public static char[] decompose(String word) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			int cp = word.codePointAt(i);

			if (isInRange(HANGUL_SYLLABLES_RANGE, cp)) {
				System.out.printf("한글: %c\n", c);
				for (char cc : toJAMO(c)) {
					sb.append(cc);
				}
			} else if (isInRange(HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE, cp)) {
				System.out.printf("자음: %c\n", c);
				sb.append(c);
			} else if (isInRange(HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE, cp)) {
				System.out.printf("모음: %c\n", c);
				sb.append(c);
			} else if (cp == CHAEUM) {
				System.out.printf("채움코드: %c\n", c);
			} else if (isInRange(OLD_JAEUM_RANGE, cp)) {
				System.out.printf("옛글 자모: %c\n", c);
			} else {

			}
		}
		return sb.toString().toCharArray();

	}

	public static final char fromAnalyzedJAMO(char[] cs) {
		int[] cps = codepoints(cs);
		return fromAnalyzedJAMO(cps);
	}

	public static final char fromAnalyzedJAMO(int[] cps) {
		int[] locs = new int[cps.length];
		locs[0] = cps[0] - HANGUL_JAMO_CHOSUNG_RANGE[0]; // [4352, 4371]
		locs[1] = cps[1] - HANGUL_JAMO_JUNGSUNG_RANGE[0]; // [4449, 4470]
		if (locs.length == 3) {
			locs[2] = cps[2] - HANGUL_JAMO_JONGSUNG_RANGE[0]; // [4519, 4547]
		} else {
			locs[2] = HANGUL_JAMO_JONGSUNG_RANGE[0];
		}
		int cp = locs[0] * DENOM_CHOSUNG + locs[1] * DENOM_JONGSUNG + locs[2];
		cp += HANGUL_SYLLABLES_RANGE[0];

		char ret = (char) cp;
		return ret;
	}

	public static char fromNotAnalyzedJAMO(char[] cs) {
		int[] cps = new int[cs.length];

		int size = hjMaps.length;
		char c = cs[0];
		Map<Character, Integer> map = hjMaps[0];
		Integer res = hjMaps[0].get(c);

		cps[0] = hjMaps[0].get(cs[0]);
		cps[1] = hjMaps[1].get(cs[1]);
		cps[2] = hjMaps[2].get(cs[2]);

		return '1';
	}

	public static char fromNotAnalyzedJAMO(int[] cps) {
		int[] locs = new int[cps.length];
		locs[0] = cps[0] - HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0];
		locs[1] = cps[1] - HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[0];
		locs[2] = cps[2] - HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0];

		locs[0] = HANGUL_JAMO_CHOSUNG_RANGE[0] + locs[0]; // [4352, 4371]
		locs[1] = HANGUL_JAMO_JUNGSUNG_RANGE[0] + locs[1]; // [4449, 4470]
		if (locs.length == 3) {
			locs[2] = HANGUL_JAMO_JONGSUNG_RANGE[0] + locs[2]; // [4519, 4547]
		}

		char[] cs = new char[locs.length];

		for (int i = 0; i < cs.length; i++) {
			cs[i] = (char) locs[i];
		}

		return '1';
	}

	private static String getString(char[] chars) {
		return String.valueOf(chars);
	}

	public static boolean isInRange(int[] range, int cp) {
		if (cp >= range[0] && cp < range[1]) {
			return true;
		} else {
			return false;
		}
	}

	public static void main(String args[]) {
		{
			writeMaps();
		}

		// {
		// String word = "ㄱㅣㄷ각낙닫ㄱㄴㄷㄹㅁ";
		//
		// char[] chs = decompose(word);
		//
		// fromAnalyzedJAMO(chs);
		//
		// // compose(chs);
		//
		// String s = String.valueOf(chs);
		//
		// System.out.println(StrUtils.toString(s.toCharArray()));
		// }

		// {
		// String word2 = "ㄱㅏㄱ각낙닫";
		//
		// compose(word2.toCharArray());
		// }

		// for (int i = HANGUL_SYLLABLES_RANGE[0], loc = 0; i <= HANGUL_SYLLABLES_RANGE[1]; i++) {
		// StringBuffer sb = new StringBuffer();
		// sb.append((char) i);
		// System.out.printf("%d\t%d\t%s\n", loc++, i, sb.toString());
		// }
		// System.out.println();

		// for (int i = HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0], loc = 0; i <= HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		// for (int i = HANGUL_JAMO_CHOSUNG_RANGE[0], loc = 0; i < HANGUL_JAMO_CHOSUNG_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }
		//
		// for (int i = HANGUL_JAMO_JUNGSUNG_RANGE[0], loc = 0; i < HANGUL_JAMO_JUNGSUNG_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }
		//
		// for (int i = HANGUL_JAMO_JONGSUNG_RANGE[0], loc = 0; i < HANGUL_JAMO_JONGSUNG_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		// for (int i = HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0], loc = 0; i <= HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		// for (int i = HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[0], loc = 0; i <= HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		// for (int i = HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[0], loc = 0; i <= HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// if (loc == 1) {
		// break;
		// }
		// }
		//
	}

	public static Map<Character, Integer> hcjMap = Generics.newHashMap();

	public static Map<Character, Integer>[] hjMaps = new HashMap[3];

	static {

		for (int i = HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0]; i <= HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[1]; i++) {
			hcjMap.put((char) i, i);
		}

		for (int i = HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[0]; i <= HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[1]; i++) {
			hcjMap.put((char) i, i);
		}

		hjMaps = new HashMap[3];
		for (int i = 0; i < hjMaps.length; i++) {
			hjMaps[i] = Generics.newHashMap();
		}

		for (int i = HANGUL_JAMO_CHOSUNG_RANGE[0]; i < HANGUL_JAMO_CHOSUNG_RANGE[1]; i++) {
			hjMaps[0].put((char) i, i);
		}

		for (int i = HANGUL_JAMO_JUNGSUNG_RANGE[0]; i < HANGUL_JAMO_JUNGSUNG_RANGE[1]; i++) {
			hjMaps[1].put((char) i, i);
		}

		for (int i = HANGUL_JAMO_JONGSUNG_RANGE[0]; i < HANGUL_JAMO_JONGSUNG_RANGE[1]; i++) {
			hjMaps[2].put((char) i, i);
		}

	}

	public static void writeMaps() {

		// {
		//
		//// int[] HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE = { 0x3131, 0x314E };
		//
		// TextFileWriter writer = new TextFileWriter(NLPPath.DATA_DIR + "자음-1.txt");
		// for (int i = HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0], loc = 0; i <= HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[1]; i++) {
		// // System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// writer.write(String.format("%d\t%d\t%c\n", loc++, i, (char) i));
		// }
		// writer.close();
		// }

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

		// for (int i = HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0], loc = 0; i <= HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		// for (int i = HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[0], loc = 0; i <= HANGUL_COMPATIBILITY_JAMO_MOEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		System.out.println();
	}

	public static final char[] toJAMO(char c) {
		int[] cps = toJAMO(Character.codePointAt(new char[] { c }, 0));
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < cps.length; i++) {
			sb.append(Character.toChars(cps[i]));
		}
		return sb.toString().toCharArray();

	}

	public static final int[] toJAMO(int cp) {
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

	public static final int[] toJAMOs(int[] cps) {
		List<Integer> list = Generics.newArrayList();
		for (int cp : cps) {
			for (int res : toJAMO(cp)) {
				list.add(res);
			}
		}
		return ArrayUtils.copy(list);
	}
}
