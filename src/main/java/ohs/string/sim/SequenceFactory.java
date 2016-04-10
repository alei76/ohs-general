package ohs.string.sim;

import java.util.List;

import ohs.utils.StrUtils;

public class SequenceFactory {
	public static Sequence<Character> newCharSequence(char[] s) {
		return new Sequence<Character>(StrUtils.asCharacters(s));
	}

	public static Sequence<Character> newCharSequence(String s) {
		return newCharSequence(s.toCharArray());
	}

	public static Sequence<Character>[] newCharSequences(char[] s, char[] t) {
		Sequence<Character>[] ret = new Sequence[2];
		ret[0] = newCharSequence(s);
		ret[1] = newCharSequence(t);
		return ret;
	}

	public static Sequence<Character>[] newCharSequences(String s, String t) {
		return newCharSequences(s.toCharArray(), t.toCharArray());
	}

	public static Sequence<String> newStringSequence(String[] s) {
		return new Sequence<String>(s);
	}

	public static Sequence<String> newStrSequence(List<String> s) {
		return new Sequence<String>(StrUtils.asArray(s));
	}

	public static Sequence<String>[] newStrSequences(List<String> s, List<String> t) {
		return newStrSequences(StrUtils.asArray(s), StrUtils.asArray(t));
	}

	public static Sequence<String>[] newStrSequences(String[] s, String[] t) {
		Sequence<String>[] ret = new Sequence[2];
		ret[0] = newStringSequence(s);
		ret[1] = newStringSequence(t);
		return ret;
	}
}
