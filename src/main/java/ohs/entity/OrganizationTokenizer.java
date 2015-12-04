package ohs.entity;

import java.util.List;

import ohs.entity.data.struct.BilingualText;
import ohs.io.TextFileReader;
import ohs.math.ArrayMath;
import ohs.math.LA;
import ohs.types.Counter;
import ohs.types.CounterMap;

public class OrganizationTokenizer {

	private static final String SINGLE_SPACE = " ";

	public static void main(String[] args) {
		System.out.println("process begins.");

		Counter<BilingualText> orgCounts = readOrganizations();

		OrganizationTokenizer lm = train(orgCounts);

		for (BilingualText orgName : orgCounts.keySet()) {
			double count = orgCounts.getCount(orgName);
			if (count < 10 || orgName.getKorean().equals("empty")) {
				continue;
			}

			String res = lm.tokenize(orgName.getKorean());

			if (res.length() != orgName.getKorean().length()) {
				System.out.printf("%s => %s\n", orgName.getKorean(), res);
			}
		}

		System.out.println("process ends.");

	}

	public static Counter<BilingualText> readOrganizations() {
		Counter<BilingualText> ret = new Counter<BilingualText>();
		TextFileReader reader = new TextFileReader(ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			BilingualText orgName = new BilingualText(parts[0], parts[1]);
			ret.setCount(orgName, Double.parseDouble(parts[2]));
		}
		reader.close();
		return ret;
	}

	public static OrganizationTokenizer train(Counter<BilingualText> orgCounts) {
		int max_ngram_size = 5;

		CounterMap<String, String>[] ngramData = new CounterMap[max_ngram_size - 1];
		for (int i = 0; i < ngramData.length; i++) {
			ngramData[i] = new CounterMap<String, String>();
		}

		double[] mixture_weights = new double[ngramData.length];
		for (int i = 0; i < mixture_weights.length; i++) {
			mixture_weights[i] = (i + 1) * (i + 1);
		}

		ArrayMath.normalize(mixture_weights);

		List<BilingualText> keys = orgCounts.getSortedKeys();
		double min_count_of_org = 10;

		for (int i = 0; i < keys.size(); i++) {
			BilingualText orgName = keys.get(i);
			double count = orgCounts.getCount(orgName);

			if (count < min_count_of_org || orgName.getKorean().equals("empty")) {
				continue;
			}

			String s = orgName.getKorean();
			s = String.format("^%s$", s);

			int len = s.length();

			for (int j = 0; j < len; j++) {
				String ch = String.valueOf(s.charAt(j));

				for (int k = 1; k < max_ngram_size; k++) {
					int start = j - k;

					if (start >= 0) {
						String prevCh = s.substring(start, j);
						ngramData[k - 1].incrementCount(prevCh, ch, count);
					}
				}
			}
		}
		return new OrganizationTokenizer(ngramData, max_ngram_size, mixture_weights);
	}

	private CounterMap<String, String>[] ngramData;

	private int max_ngram_size;

	private double[] mixture_weights;

	public OrganizationTokenizer(CounterMap<String, String>[] ngramData, int max_ngram_size, double[] mixture_weights) {
		this.ngramData = ngramData;
		this.max_ngram_size = max_ngram_size;
		this.mixture_weights = mixture_weights;
	}

	public void test() {
		CounterMap<Integer[], Integer> counterMap = new CounterMap();
		Integer[] key = new Integer[] { 1, 2 };
		Integer key2 = 3;

		counterMap.incrementCount(key, key2, 1);
		counterMap.incrementCount(key, key2, 1);
		counterMap.incrementCount(key, key2, 1);

		System.out.println(counterMap);
	}

	public String tokenize(String s) {
		String ret = String.format("^%s$", s);

		StringBuffer sb = new StringBuffer();

		// if (s.equals("충남대 교육학과")) {
		// System.out.println();
		// }

		for (int i = 1; i < ret.length() - 1;) {
			String ch = String.valueOf(ret.charAt(i));

			String[] prevChs = new String[max_ngram_size - 1];

			for (int j = 1; j < max_ngram_size; j++) {
				int start = i - j;

				if (start >= 0) {
					String prevCh = ret.substring(start, i);
					prevChs[j - 1] = prevCh;
				}
			}

			double[] probs1 = new double[prevChs.length];
			double[] probs2 = new double[prevChs.length];

			for (int j = 0; j < prevChs.length; j++) {
				probs1[j] = ngramData[j].getCounter(prevChs[j]).getProbability(SINGLE_SPACE);
				probs2[j] = ngramData[j].getCounter(prevChs[j]).getProbability(ch);
			}

			double score1 = LA.dotProduct(mixture_weights, probs1);
			double score2 = LA.dotProduct(mixture_weights, probs2);
			double ratio = 1;

			if (score1 > 0 && score2 > 0) {
				ratio = score1 / score2;
			}

			boolean addSpace = false;
			boolean deleteSpace = true;

			if (ch.equals(SINGLE_SPACE)) {
				if (score1 < score2) {
					// ch = "";
					deleteSpace = true;
				}
			} else {
				if (score1 > score2) {
					addSpace = true;
				}
			}

			if (addSpace) {
				sb.append(SINGLE_SPACE);
			}

			sb.append(ch);

			i++;

		}

		ret = sb.toString();
		return ret;
	}
}
