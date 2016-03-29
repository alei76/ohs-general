package ohs.utils;

import java.util.List;
import java.util.logging.Handler;

import org.apache.commons.math.stat.descriptive.SynchronizedMultivariateSummaryStatistics;

import ohs.math.ArrayUtils;

/**
 * http://secr.tistory.com/207
 *
 */
public class KoreanUtils {

	public static final int CHAEUM = 0x3164;

	public static final int CHOSUNG_SIZE = 19;

	public static final int JUNGSUNG_SIZE = 21;

	public static final int JONGSUNG_SIZE = 28;

	public static final int DENOM_CHOSUNG = JUNGSUNG_SIZE * JONGSUNG_SIZE;

	public static final int DENOM_JONGSUNG = JONGSUNG_SIZE;

	public static final int[] CHOSUNG_RANGE = { 0x1100, 0x1100 + CHOSUNG_SIZE };

	public static final int[] JUNGSUNG_RANGE = { 0x1161, 0x1161 + JUNGSUNG_SIZE };

	public static final int[] JONGSUNG_RANGE = { 0x11A7, 0x11A7 + JONGSUNG_SIZE };

	public static final int[] MOEUM_RANGE = { 0x314F, 0x3163 + 1 };

	public static final int[] HANGUL_RANGE = { 0xAC00, 0xD7A3 + 1 };

	public static final int[] JAEUM_RANGE = { 0x3131, 0x314E + 1 };

	public static final int[] OLD_JAEUM_RANGE = { 0x3165, 0x318E + 1 };

	public static char[] decompose(String word) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			int cp = word.codePointAt(i);
			int cpp = (int) c;

			if (cp >= HANGUL_RANGE[0] && cp < HANGUL_RANGE[1]) {
				System.out.printf("한글: %c\n", c);
				char[] chs = toJAMO(c);
				char t = fromJAMO(chs);
				for (char cc : toJAMO(c)) {
					sb.append(cc);
				}
			} else if (cp >= JAEUM_RANGE[0] && cp < JAEUM_RANGE[0]) {
				System.out.printf("자음: %c\n", c);
				sb.append(c);
			} else if (cp >= MOEUM_RANGE[0] && cp < MOEUM_RANGE[1]) {
				System.out.printf("모음: %c\n", c);
				sb.append(c);
			} else if (cp == 0x3164) {
				System.out.printf("채움코드: %c\n", c);
			} else if (cp >= 0x3165 && cp <= 0x318E) {
				System.out.printf("옛글 자모: %c\n", c);
			}
		}

		return sb.toString().toCharArray();

	}

	public static final char fromJAMO(char[] cs) {
		int c = HANGUL_RANGE[0];

		c += ((cs[0] * CHOSUNG_SIZE) + cs[1]) * JUNGSUNG_SIZE;

		char ret = (char) c;

		return ret;
	}

	public static final int fromJAMO(int[] cs) {

		int ret = 0;

		return ret;
	}

	private static String getString(char[] chars) {
		return String.valueOf(chars);
	}

	public static void main(String args[]) {
		{
			String word = "각낙닫";

			decompose(word);
		}

		// for (int i = HANGUL_RANGE[0], loc = 0; i <= HANGUL_RANGE[1]; i++) {
		// StringBuffer sb = new StringBuffer();
		// sb.append((char) i);
		// char[] cs = sb.toString().toCharArray();
		// System.out.printf("%d\t%d\t%s\n", loc++, i, sb.toString());
		// }
		// System.out.println();

		// for (int i = JAEUM_RANGE[0], loc = 0; i <= JAEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		// for (int i = CHOSUNG_RANGE[0], loc = 0; i < CHOSUNG_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }
		//
		// for (int i = JUNGSUNG_RANGE[0], loc = 0; i < JUNGSUNG_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }
		//
		// for (int i = JONGSUNG_RANGE[0], loc = 0; i < JONGSUNG_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		// for (int i = JAEUM_RANGE[0], loc = 0; i <= JAEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		// for (int i = MOEUM_RANGE[0], loc = 0; i <= MOEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// }

		// for (int i = MOEUM_RANGE[0], loc = 0; i <= MOEUM_RANGE[1]; i++) {
		// System.out.printf("%d\t%d\t%c\n", loc++, i, (char) i);
		// if (loc == 1) {
		// break;
		// }
		// }
		//

	}

	public static final char[] toJAMO(char c) {
		int[] codes = toJAMO(Character.codePointAt(new char[] { c }, 0));

		char[] ret = new char[codes.length];

		return ret;

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

	public static final int[] toJAMO(int cp) {
		cp = cp - HANGUL_RANGE[0]; // korean 0~11,171

		int loc1 = cp / DENOM_CHOSUNG; // chosung 0~18

		cp = cp % DENOM_CHOSUNG;

		int loc2 = cp / DENOM_JONGSUNG; // jungsung 0~20
		int loc3 = cp % DENOM_CHOSUNG; // josung 0~27

		int size = loc3 == 0 ? 2 : 3;
		int[] ret = new int[size];

		ret[0] = CHOSUNG_RANGE[0] + loc1; // [4352, 4371]
		ret[1] = JUNGSUNG_RANGE[0] + loc2; // [4449, 4470]
		if (ret.length == 3) {
			ret[2] = JONGSUNG_RANGE[0] + loc2; // [4519, 4547]
		}

		return ret;
	}
}
