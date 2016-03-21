package ohs.string.partition;

import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.Gram.Type;

/**
 * @author Heung-Seon Oh
 * 
 *         This class provides a method for partitioning a text string into a set of segments.
 * 
 */
public class StringPartitioner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("process begins.");
		String entity = "vanateshe";
		String entity2 = "surajit_chaudri";
		String entity3 = "일자산 해맞이 광장";
		StringPartitioner p = new StringPartitioner(3, false);

		String[] entities = { "surajit chaudri", "일자산 해맞이 광장", "vankatesh" };
		for (String ent : entities) {
			System.out.println(ent);
			for (Gram gram : p.partition(ent)) {
				System.out.println(gram.toString());
			}
			System.out.println();
		}
		// System.out.println(StrUtils.join("|", p.partition(entity)));
		// System.out.println();
		// System.out.println(StrUtils.join("|", p.partition(entity2)));
		// System.out.println();
		// System.out.println(StrUtils.join("|", p.partition(entity3)));
		System.out.println();

		System.out.println("process ends.");

	}

	/**
	 * 
	 */
	private int min_edit_dist;

	/**
	 * a text is not segmented if its length is lower than the minimum.
	 */
	private boolean useDynamicLen;

	public StringPartitioner(int min_edit_dist, boolean useDynamicLen) {
		this.min_edit_dist = min_edit_dist;
		this.useDynamicLen = useDynamicLen;
	}

	/**
	 * Partition an entity into a set of segments
	 * 
	 * 
	 * @param text
	 * @return
	 */
	public Gram[] partition(String s) {
		Gram[] ret = null;
		ret = partitionHere(s);
		return ret;
	}

	private Gram[] partitionHere(String s) {
		int len = s.length();
		int tau = min_edit_dist;
		int expect_len_for_segment = 2;

		if (useDynamicLen) {
			double a = 1f * len / expect_len_for_segment;
			double b = 1f * len % expect_len_for_segment;
			tau = (int) Math.floor(a);
			if (b == 0) {
				tau = tau - 1;
			}
		}

		int num_segments = tau + 1;
		int k = len - (int) Math.floor(len / num_segments) * num_segments;
		int num_segments_in_front = tau + 1 - k;
		int num_segments_in_back = num_segments - num_segments_in_front;

		int len_for_segments_in_front = (int) Math.floor(1f * len / num_segments);
		int len_for_segments_in_back = (int) Math.ceil(1f * len / num_segments);
		Gram[] ret = new Gram[num_segments];

		for (int i = 0, start = 0; i < num_segments; i++) {
			int segment_len = i < num_segments_in_front ? len_for_segments_in_front : len_for_segments_in_back;
			int end = start + segment_len;
			ret[i] = new Gram(s.substring(start, end), start, Type.NONE);
			start += segment_len;
		}
		return ret;
	}

}
