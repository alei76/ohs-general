package ohs.nlp.ling.types;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SpanComparator implements Comparator<TextSpan> {
	public static void sort(List<TextSpan> textSpans) {
		Collections.sort(textSpans, new SpanComparator());
	}

	@Override
	public int compare(TextSpan o1, TextSpan o2) {
		return o1.getStart() > o2.getStart() ? 1 : -1;
	}
}
