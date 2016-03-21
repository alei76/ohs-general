package ohs.utils;

import java.sql.Struct;
import java.util.List;

/**
 * http://secr.tistory.com/207
 *
 */
public class KoreanUtils {
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

	public static char[] decomposeSyllableToPhonemes(char syllable) {
		syllable = (char) (syllable - 0xAC00);
		StringBuffer sb = new StringBuffer();

		if (syllable >= 0 && syllable <= 11172) {
			/* A. 자음과 모음이 합쳐진 글자인경우 */

			/* A-1. 초/중/종성 분리 */
			int chosung = syllable / (21 * 28);
			int jungsung = syllable % (21 * 28) / 28;
			int jongsung = syllable % (21 * 28) % 28;

			sb.append(CHO_SUNG[chosung]);
			sb.append(JUNG_SUNG[jungsung]);

			/* 자음분리 */
			if (jongsung != 0x0000) {
				/* A-3. 종성이 존재할경우 result에 담는다 */
				sb.append(JONG_SUNG[jongsung]);
			}

		} else {
			/* B. 한글이 아니거나 자음만 있을경우 */

			/* 자음분리 */
			sb.append((char) (syllable + 0xAC00));
		} // if

		return getChars(sb);
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

	public static void main(String args[]) {

		String word = " 히말라야여뀌  오연수 멍충아ㅏㅠkk!@#$%^&*()★"; // 분리할 단어
		String result = ""; // 결과 저장할 변수
		String resultEng = ""; // 알파벳으로

		System.out.println("============ result ==========");
		System.out.println("단어     : " + word);
		System.out.println("자음분리 : " + getString(decomposeKoreanWordToPhonemes(word)));
		System.out.println("알파벳   : " + getString(decomposeKoreanWordToEnglish(word)));
	}
}
