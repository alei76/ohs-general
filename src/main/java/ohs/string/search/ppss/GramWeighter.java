package ohs.string.search.ppss;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.types.Counter;

/**
 * A class for providing methods of computing the weights of grams.
 * 
 * The weights are used to determine the global orders
 * 
 * @author Heung-Seon Oh
 */
public class GramWeighter {

	public static Counter<String> compute(GramGenerator gramGenerator, List<StringRecord> ss) {
		List<Gram[]> allGrams = GramGenerator.generate(gramGenerator, ss);
		return computeWeightsByGramCounts(allGrams);
	}

	public static Counter<String> computeGramWeightsByIDFs(List<Gram[]> allGrams) {
		Counter<String> ret = new Counter<String>();

		Map<String, Integer> gramFirstLocs = new HashMap<String, Integer>();
		Counter<String> gramDFs = new Counter<String>();

		for (int i = 0; i < allGrams.size(); i++) {
			Gram[] grams = allGrams.get(i);
			Set<String> gramSet = new HashSet<String>();

			for (int j = 0; j < grams.length; j++) {
				Gram gram = grams[j];
				String g = gram.getString();
				ret.incrementCount(g, 1);
				gramSet.add(g);
			}

			for (String g : gramSet) {
				gramDFs.incrementCount(g, 1);
			}
		}

		for (String g : ret.keySet()) {
			// double tf = Math.log(gramWeights.getCount(g)) + 1;
			double df = gramDFs.getCount(g);
			double num_grams = allGrams.size();
			double idf = Math.log(((num_grams + 1) / df));
			ret.setCount(g, idf);
		}

		return ret;
	}

	public static Counter<String> computeTFIDFs(GramGenerator gramGenerator, List<StringRecord> ss) {
		List<Gram[]> allGrams = GramGenerator.generate(gramGenerator, ss);
		return computeTFIDFs(allGrams);

	}

	public static Counter<String> computeTFIDFs(List<Gram[]> allGrams) {
		Counter<String> ret = new Counter<String>();

		Map<String, Integer> gramFirstLocs = new HashMap<String, Integer>();
		Counter<String> gramDFs = new Counter<String>();

		for (int i = 0; i < allGrams.size(); i++) {
			Gram[] grams = allGrams.get(i);
			Set<String> gramSet = new HashSet<String>();

			for (int j = 0; j < grams.length; j++) {
				Gram gram = grams[j];
				String g = gram.getString();
				ret.incrementCount(g, 1);
				gramSet.add(g);
			}

			for (String g : gramSet) {
				gramDFs.incrementCount(g, 1);
			}
		}

		for (String g : ret.keySet()) {
			double tf = Math.log(ret.getCount(g)) + 1;
			// double tf = ret.getCount(g);
			double df = gramDFs.getCount(g);
			double num_grams = allGrams.size();
			double idf = Math.log(((num_grams + 1) / df));
			double tfidf = tf * idf;
			ret.setCount(g, idf);
		}

		return ret;
	}

	/**
	 * compute gram weights using gram counts. Grams which appear first get more weight than those appear later.
	 * 
	 * @param allGrams
	 * @return
	 */
	public static Counter<String> computeWeightsByGramCounts(List<Gram[]> allGrams) {
		Counter<String> ret = new Counter<String>();
		Map<String, Integer> gramFirstLocs = new HashMap<String, Integer>();
		int gram_cnt = 0;

		for (int i = 0; i < allGrams.size(); i++) {
			Gram[] grams = allGrams.get(i);

			for (int j = 0; j < grams.length; j++) {
				Gram gram = grams[j];
				String g = gram.getString();
				ret.incrementCount(g, 1);
				gram_cnt++;
				if (!gramFirstLocs.containsKey(g)) {
					gramFirstLocs.put(g, gram_cnt);
				}
			}
		}

		for (String g : gramFirstLocs.keySet()) {
			int id = gramFirstLocs.get(g);
			double pos_weight = 1f * id / (gram_cnt + 1);
			ret.incrementCount(g, pos_weight);
		}

		return ret;
	}

}
