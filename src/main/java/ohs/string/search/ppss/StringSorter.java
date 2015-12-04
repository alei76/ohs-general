package ohs.string.search.ppss;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Sorting strings according to the lengths in ascending.
 * 
 * @author Heung-Seon Oh
 */
public class StringSorter implements Serializable {

	public static class LengthComparator implements Comparator<StringRecord> {
		@Override
		public int compare(StringRecord o1, StringRecord o2) {
			return o1.getString().length() - o2.getString().length();
		}
	}

	public static void sortByLength(List<StringRecord> ss) {
		Collections.sort(ss, new LengthComparator());
	}
}
