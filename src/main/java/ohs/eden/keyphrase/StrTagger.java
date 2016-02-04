package ohs.eden.keyphrase;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.taskdefs.optional.native2ascii.KaffeNative2Ascii;

import ohs.ling.struct.Span;
import ohs.utils.Generics;

public class StrTagger {

	public static String tag(String text, String taget, String tagName) throws Exception {
		List<String> targets = Generics.newArrayList();
		targets.add(taget);
		return tag(text, targets, tagName);
	}

	public static String tag(String text, Collection<String> targets, String tagName) throws Exception {
		StringBuffer sb = new StringBuffer();

		Pattern p = Pattern.compile(String.format("(%s)", String.join("|", targets)), Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(text);

		while (m.find()) {
			String g = m.group();
			m.appendReplacement(sb, String.format("<%s>%s</%s>", tagName, g, tagName));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	public static void main(String[] args) throws Exception {
		String text = "이것은 <KWD>키워드</KWD> 태깅 <KWD>테스트</KWD>이다. ";
		// String text = "이것은 키워드 태깅 테스트이다. ";

		// text = tag(text, "키워드", "KWD");
		System.out.println(extract(text, "KWD"));
	}

	public static List<Span> extract(String text, String tagName) throws Exception {
		Set<String> tagNames = Generics.newHashSet();
		tagNames.add(tagName);
		return extract(text, tagNames);
	}

	public static List<Span> extract(String text) throws Exception {
		Set<String> tagNames = null;
		return extract(text, tagNames);
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
				sb.append(i == k ? String.format("^(%d)", i) : " ");
			}

		}
		return sb.toString();

	}

	public static List<Span> extract(String text, Set<String> tagNames) throws Exception {
		List<Span> ret = Generics.newArrayList();

		int start = 0;

		for (int i = 0; i < text.length();) {
			System.out.println(markAt(text, i, false));
			
			if (text.charAt(i) == '<') {
				if ((i + 1) < text.length() && text.charAt(i + 1) == '/') {
					StringBuffer sb = new StringBuffer();
					for (int j = i + 2; j < text.length(); j++) {
						if (text.charAt(j) == '>') {
							break;
						}
						sb.append(text.charAt(j));
					}

					if (tagNames.contains(sb.toString())) {
						String value = text.substring(start, i);
						int len = ret.size() * (sb.toString().length() + 5);

						int start_at_raw_text = start - len - (sb.length() + 2);
						ret.add(new Span(start_at_raw_text, value));
						System.out.println(value);
					}
					i += (sb.length() + 2);

					System.out.println(markAt(text, i, false));
				} else {
					StringBuffer sb = new StringBuffer();
					int last_j = 0;
					for (int j = i + 1; j < text.length(); j++) {
						System.out.println(markAt(text, j, false));
						if (text.charAt(j) == '>') {
							last_j = j;
							break;
						}
						sb.append(text.charAt(j));
					}

					if (tagNames.contains(sb.toString())) {
						start = last_j + 1;
					}
					i = last_j + 1;

					System.out.println(markAt(text, i, false));

				}
			} else {
				i++;
			}
		}

		return ret;
	}
}
