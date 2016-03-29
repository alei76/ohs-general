package ohs.utils;

import java.util.List;

import org.apache.commons.math.stat.descriptive.SynchronizedMultivariateSummaryStatistics;

/**
 * http://secr.tistory.com/207
 *
 */
public class KoreanUtils2 {
	/*
	 * ********************************************** 자음 모음 분리 설연수 -> ㅅㅓㄹㅇㅕㄴㅅㅜ, 바보 -> ㅂㅏㅂㅗ
	 **********************************************/
	/** 초성 - 가(ㄱ), 날(ㄴ) 닭(ㄷ) */
	public static final char[] CHO_SUNG = { 0x3131, 0x3132, 0x3134, 0x3137, 0x3138, 0x3139, 0x3141, 0x3142, 0x3143, 0x3145, 0x3146, 0x3147,
			0x3148, 0x3149, 0x314a, 0x314b, 0x314c, 0x314d, 0x314e };
	/** 중성 - 가(ㅏ), 야(ㅑ), 뺨(ㅑ) */
	public static final char[] JUNG_SUNG = { 0x314f, 0x3150, 0x3151, 0x3152, 0x3153, 0x3154, 0x3155, 0x3156, 0x3157, 0x3158, 0x3159, 0x315a,
			0x315b, 0x315c, 0x315d, 0x315e, 0x315f, 0x3160, 0x3161, 0x3162, 0x3163 };
	/** 종성 - 가(없음), 갈(ㄹ) 천(ㄴ) */
	public static final char[] JONG_SUNG = { 0x0000, 0x3131, 0x3132, 0x3133, 0x3134, 0x3135, 0x3136, 0x3137, 0x3139, 0x313a, 0x313b, 0x313c,
			0x313d, 0x313e, 0x313f, 0x3140, 0x3141, 0x3142, 0x3144, 0x3145, 0x3146, 0x3147, 0x3148, 0x314a, 0x314b, 0x314c, 0x314d,
			0x314e };

	/*
	 * ********************************************** 알파벳으로 변환 설연수 -> tjfdustn, 멍충 -> ajdcnd
	 **********************************************/
	/** 초성 - 가(ㄱ), 날(ㄴ) 닭(ㄷ) */
	public static final String[] CHO_SUNG_ENG = { "r", "R", "text", "e", "E", "f", "a", "q", "Q", "t", "T", "d", "w", "W", "c", "z", "x",
			"v", "g" };

	/** 중성 - 가(ㅏ), 야(ㅑ), 뺨(ㅑ) */
	public static final String[] JUNG_SUNG_ENG = { "k", "o", "i", "O", "j", "p", "u", "P", "h", "hk", "ho", "hl", "y", "n", "nj", "np",
			"nl", "b", "m", "ml", "l" };

	/** 종성 - 가(없음), 갈(ㄹ) 천(ㄴ) */
	public static final String[] JONG_SUNG_ENG = { "", "r", "R", "rt", "text", "sw", "sg", "e", "f", "fr", "fa", "fq", "ft", "fx", "fv",
			"fg", "a", "q", "qt", "t", "T", "d", "w", "c", "z", "x", "v", "g" };

	/** 단일 자음 - ㄱ,ㄴ,ㄷ,ㄹ... (ㄸ,ㅃ,ㅉ은 단일자음(초성)으로 쓰이지만 단일자음으론 안쓰임) */
	public static final String[] SINGLE_JAUM_ENG = { "r", "R", "rt", "text", "sw", "sg", "e", "E", "f", "fr", "fa", "fq", "ft", "fx", "fv",
			"fg", "a", "q", "Q", "qt", "t", "T", "d", "w", "W", "c", "z", "x", "v", "g" };

	public static List<Character> decomposeSyllableToPhonemes(char syllable) {
		syllable = (char) (syllable - 0xAC00);
		List<Character> ret = Generics.newArrayList();

		if (syllable >= 0 && syllable <= 11172) {
			/* A. 자음과 모음이 합쳐진 글자인경우 */

			/* A-1. 초/중/종성 분리 */
			int chosung = syllable / (21 * 28);
			int jungsung = syllable % (21 * 28) / 28;
			int jongsung = syllable % (21 * 28) % 28;

			ret.add(CHO_SUNG[chosung]);
			ret.add(JUNG_SUNG[jungsung]);

			/* 자음분리 */
			if (jongsung != 0x0000) {
				/* A-3. 종성이 존재할경우 result에 담는다 */
				ret.add(JONG_SUNG[jongsung]);
			}

		} else {
			/* B. 한글이 아니거나 자음만 있을경우 */

			/* 자음분리 */
			ret.add((char) (syllable + 0xAC00));
		} // if

		return ret;
	}

	public static char[] decomposeKoreanSyllableToEnglish(char c) {
		StringBuffer sb = new StringBuffer();

		c = (char) (c - 0xAC00);

		if (c >= 0 && c <= 11172) {
			/* A. 자음과 모음이 합쳐진 글자인경우 */

			/* A-1. 초/중/종성 분리 */
			int chosung = c / (21 * 28);
			int jungsung = c % (21 * 28) / 28;
			int jongsung = c % (21 * 28) % 28;

			/* 알파벳으로 */
			sb.append(CHO_SUNG_ENG[chosung]);
			sb.append(JUNG_SUNG_ENG[jungsung]);

			if (jongsung != 0x0000) {
				/* A-3. 종성이 존재할경우 result에 담는다 */
				sb.append(JONG_SUNG_ENG[jongsung]);
			}
		} else {
			/* B. 한글이 아니거나 자음만 있을경우 */

			/* 알파벳으로 */
			if (c >= 34097 && c <= 34126) {
				/* 단일자음인 경우 */
				int jaum = (c - 34097);
				sb.append(SINGLE_JAUM_ENG[jaum]);
			} else if (c >= 34127 && c <= 34147) {
				/* 단일모음인 경우 */
				int moum = (c - 34127);
				sb.append(JUNG_SUNG_ENG[moum]);
			} else {
				/* 알파벳인 경우 */
				sb.append((char) (c + 0xAC00));
			}
		} // if

		return getChars(sb);
	}

	public static char[] decomposeKoreanWordToPhonemes(String word) {
		List<Character> ret = Generics.newArrayList();
		for (int i = 0; i < word.length(); i++) {
			for (char c : decomposeSyllableToPhonemes(word.charAt(i))) {
				ret.add(c);
			}
		}
		return StrUtils.toChars(ret);

	}

	public static char[] decomposeKoreanWordToEnglish(String word) {
		List<Character> ret = Generics.newArrayList();
		for (int i = 0; i < word.length(); i++) {
			char[] chs = decomposeKoreanSyllableToEnglish(word.charAt(i));
			for (char ch : chs) {
				ret.add(ch);
			}
		}
		return StrUtils.toChars(ret);
	}

	private static char[] getChars(StringBuffer sb) {
		char[] ret = new char[sb.length()];

		for (int i = 0; i < sb.length(); i++) {
			ret[i] = sb.charAt(i);
		}

		return ret;
	}

	public static String getDecomposedKoreanWord(String word) {
		return getString(decomposeKoreanWordToPhonemes(word));
	}

	private static String getString(char[] chars) {
		return String.valueOf(chars);
	}

	public static final int[] HANGUL_RANGE = { 0xAC00, 0xD7A3 };

	public static final int[] JAEUM_RANGE = { 0x3131, 0x314E };

	public static final int[] OLD_JAEUM_RANGE = { 0x3165, 0x318E };

	public static final int CHAEUM = 0x3164;

	public static final int CHOSUNG_SIZE = 19;

	public static final int JUNGSUNG_SIZE = 21;

	public static final int JONGSUNG_SIZE = 28;

	public static final int[] MOEUM_RANGE = { 0x314F, 0x3163 };

	public static final int DENOM_CHOSUNG = (21 * 28);

	public static final int DENOM_JONGSUNG = (28);

	public static final int[] JAMO_BASE = { 0x1100, 0x1161, 0x11A7 };

	public static final char[] toJAMO(char c) {
		int cp = c - HANGUL_RANGE[0]; // korean 0~11,171

		int loc1 = cp / DENOM_CHOSUNG; // chosung 0~18
		cp = cp % DENOM_CHOSUNG;
		int loc2 = cp / DENOM_JONGSUNG; // jungsung 0~20
		int loc3 = cp % DENOM_CHOSUNG; // josung 0~27

		StringBuffer sb = new StringBuffer(3);
		sb.append((char) (JAMO_BASE[0] + loc1));
		sb.append((char) (JAMO_BASE[1] + loc2));
		if (loc3 != 0) {
			sb.append((char) (JAMO_BASE[2] + loc3));
		}
		return sb.toString().toCharArray();
	}

	public static char[] decompose(String word) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			int cp = word.codePointAt(i);
			int cpp = (int) c;

			if (cp >= HANGUL_RANGE[0] && cp <= HANGUL_RANGE[1]) {
				System.out.printf("한글: %c\n", c);
				for (char cc : toJAMO(c)) {
					sb.append(cc);
				}
			} else if (cp >= JAEUM_RANGE[0] && cp <= JAEUM_RANGE[0]) {
				System.out.printf("자음: %c\n", c);
				sb.append(c);
			} else if (cp >= MOEUM_RANGE[0] && cp <= MOEUM_RANGE[1]) {
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

	public static void main(String args[]) {

		for (int i = HANGUL_RANGE[0], loc = 0; i <= HANGUL_RANGE[1]; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append((char) i);
			char[] cs = sb.toString().toCharArray();
			System.out.printf("%d\t%d\t%s\n", loc++, i, sb.toString());
		}
		System.out.println();

		// for (int i = HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[0], loc = 0; i <= HANGUL_COMPATIBILITY_JAMO_JAEUM_RANGE[1]; i++) {
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

		{
			String word = "ㄱ니당ㅇㄷ";

			System.out.println((char) 2);
		}

		{
			// String word = "프로젝트ㄱㅊㅏㄴㅎ";
			String word = "ㄱㄴㄷㄷ나난같ㄴㅌㅄ가다라마";
			System.out.println("============ result ==========");
			System.out.println("단어 : " + word);
			String s = getString(decomposeKoreanWordToPhonemes(word));
			System.out.println("자음분리 : " + s);
			System.out.println("알파벳 : " + getString(decomposeKoreanWordToEnglish(word)));

			System.out.println(getString(decompose(word)));
		}
		// {
		// String word = " 히말라야여뀌 오연수 멍충아ㅏㅠkk!@#$%^&*()★"; // 분리할 단어
		// String result = ""; // 결과 저장할 변수
		// String resultEng = ""; // 알파벳으로
		//
		// System.out.println("============ result ==========");
		// System.out.println("단어 : " + word);
		// System.out.println("자음분리 : " + getString(decomposeKoreanWordToPhonemes(word)));
		// System.out.println("알파벳 : " + getString(decomposeKoreanWordToEnglish(word)));
		// }

	}
}
