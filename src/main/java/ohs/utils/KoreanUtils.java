package ohs.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KoreanUtils {

	public static final int HANGUL_UNICODE_START = 0xAC00;

	public static final int HANGUL_UNICODE_END = 0xD7AF;

	public static Pattern namePattern = Pattern.compile("^[\\x{ac00}-\\x{d7af}\\s\\p{Punct}]+$");
	public static char composeUnicodesToKoreanChar(int[] unicodes) throws IllegalArgumentException {
		if (unicodes.length != 3)
			throw new IllegalArgumentException();
		unicodes[0] -= 0x1100;
		unicodes[1] -= 0x1161;
		unicodes[2] -= 0x11a8;
		char ret = (char) ((((unicodes[0] * 588) + unicodes[1] * 28) + unicodes[2]) + 44032);
		return ret;
	}

	/**
	 * http://quadflask.tistory.com/289
	 * 
	 * @param c
	 * @return
	 */
	public static int[] decomposeKoreanCharToUnicodes(char c) {
		int[] ret = new int[3];
		int a = c - 44032;
		ret[0] = 0x1100 + ((a / 28) / 21);
		ret[1] = 0x1161 + ((a / 21) % 21);
		ret[2] = 0x11a8 + (a % 28);
		return ret;
	}

	/**
	 * 
	 * 
	 * http://ash84.tistory.com/771
	 * 
	 * @param text
	 * @return
	 */
	public static boolean isHangul(String text) {
		Matcher m = namePattern.matcher(text);
		return m.find();
	}

	public static boolean isKorean(char c) {
		boolean ret = false;
		if ((HANGUL_UNICODE_START <= c) && (c <= HANGUL_UNICODE_END)) {
			ret = true;
		}
		return ret;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
