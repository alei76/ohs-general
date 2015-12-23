package ohs.entity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.entity.data.struct.BilingualText;
import ohs.entity.org.DataReader;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.Gram.Type;
import ohs.string.search.ppss.GramGenerator;
import ohs.string.search.ppss.GramInvertedIndex;
import ohs.string.search.ppss.GramOrderer;
import ohs.string.search.ppss.GramPostingEntry;
import ohs.string.search.ppss.GramPostings;
import ohs.string.search.ppss.GramUtils;
import ohs.string.search.ppss.GramWeighter;
import ohs.string.search.ppss.OptimalPivotSelector;
import ohs.string.search.ppss.PivotSelector;
import ohs.string.search.ppss.RandomPivotSelector;
import ohs.string.search.ppss.StringRecord;
import ohs.string.search.ppss.StringSorter;
import ohs.string.search.ppss.StringVerifier;
import ohs.string.sim.SmithWaterman;
import ohs.types.Counter;
import ohs.types.DeepMap;
import ohs.types.ListMap;
import ohs.types.Pair;
import ohs.utils.StrUtils;

/**
 * 
 * Implementation of "A Pivotal Prefix Based Filtering Algorithm for String Similarity Search" at SIGMOD'14
 * 
 * 
 * @author Heung-Seon Oh
 */
public class CopyOfSimplePivotalPrefixStringSearcher implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8740333778747553831L;

	/**
	 * @param args
	 * @throws Exception
	 */
	// public static void main(String[] args) throws Exception {
	// System.out.println("process begins.");
	// test1();
	// // test2();
	// // test3();
	// System.out.println("process ends.");
	// }

	public static void test2() {
		String[] strs = { "imyouteca", "ubuntucom", "utubbecou", "youtbecom", "yoytubeca" };
		String s = "yotubecom";

		List<StringRecord> strings = new ArrayList<StringRecord>();

		for (int i = 0; i < strs.length; i++) {
			strings.add(new StringRecord(i, strs[i]));
		}

		GramOrderer gramOrderer = new GramOrderer();

		int q = 2;
		int tau = 2;

		CopyOfSimplePivotalPrefixStringSearcher ppss = new CopyOfSimplePivotalPrefixStringSearcher(q, tau, true);
		ppss.setGramSorter(gramOrderer);
		ppss.index(strings);
		Counter<StringRecord> res = ppss.search(s);

		System.out.println(res.toString());
	}

	private int q;

	private int tau;

	private int prefix_size;

	private int pivot_size;

	private List<StringRecord> srs;

	private List<Gram[]> allGrams;

	private GramInvertedIndex L;

	private Map<String, Integer> gramOrders;

	private GramGenerator gg;

	private PivotSelector pivotSelector;

	private StringVerifier stringVerifier;

	private GramOrderer gramOrderer;

	private Counter<Character> chWeights;

	public CopyOfSimplePivotalPrefixStringSearcher() {
		this(2, 2, false);
	}

	public CopyOfSimplePivotalPrefixStringSearcher(int q, int tau, boolean useOptimalPivotSelector) {
		this.q = q;
		this.tau = tau;

		pivot_size = tau + 1;

		prefix_size = q * tau + 1;

		this.gramOrderer = new GramOrderer();

		gg = new GramGenerator(q);

		stringVerifier = new StringVerifier(q, tau);

		pivotSelector = useOptimalPivotSelector

				? new OptimalPivotSelector(q, prefix_size, pivot_size) : new RandomPivotSelector(q, prefix_size, pivot_size);
	}

	private void computeCharacterWeights() {
		chWeights = new Counter<Character>();

		for (int i = 0; i < srs.size(); i++) {
			String s = srs.get(i).getString();

			Set<Character> chs = new HashSet<Character>();

			for (int j = 0; j < s.length(); j++) {
				chs.add(s.charAt(j));
			}

			for (char ch : chs) {
				chWeights.incrementCount(ch, 1);
			}
		}

		for (char ch : chWeights.keySet()) {
			double df = chWeights.getCount(ch);
			double num_docs = srs.size();
			double idf = Math.log((num_docs + 1) / df);
			chWeights.setCount(ch, idf);
		}
	}

	public List<Gram[]> getAllGrams() {
		return allGrams;
	}

	public GramGenerator getGramGenerator() {
		return gg;
	}

	public GramOrderer getGramSorter() {
		return gramOrderer;
	}

	private Set<String> getIntersection(Set<String> a, Set<String> b) {
		Set<String> ret = new HashSet<String>();

		Set<String> large = null;
		Set<String> small = null;

		if (a.size() > b.size()) {
			large = a;
			small = b;
		} else {
			large = b;
			small = a;
		}
		for (String s : small) {
			if (large.contains(s)) {
				ret.add(s);
			}
		}
		return ret;
	}

	private int getLastLoc(List<Integer> list) {
		return list.size() > 0 ? list.get(list.size() - 1) : -1;
	}

	private Pair<String, Integer> getLastPrefix(Gram[] grams, ListMap<Type, Integer> groups) {
		List<Integer> prefixLocs = groups.get(Type.PREFIX);
		int last_loc = prefixLocs.get(prefixLocs.size() - 1);
		Gram gram = grams[last_loc];
		Integer order = gramOrders.get(gram.getString());

		if (order == null) {
			order = -1;
		}
		return new Pair<String, Integer>(gram.getString(), order);
	}

	public int getPivotSize() {
		return pivot_size;
	}

	public int getPrefixSize() {
		return prefix_size;
	}

	public int getQ() {
		return q;
	}

	private int[] getSearchRange(int len, Type indexType, GramPostings postings) {
		int start = 0;
		int end = postings.size();

		DeepMap<Type, Integer, Integer> typeLenLocMap = postings.getTypeLengthLocs();
		Map<Integer, Integer> M = typeLenLocMap.get(indexType);

		int start_key = len - tau;
		int end_key = len + tau + 1;

		Integer t_start = M.get(start_key);
		Integer t_end = M.get(end_key);

		if (t_start == null) {
			start_key = len - tau + 1;
			t_start = M.get(start_key);
		}

		if (t_start == null) {
			t_start = M.get(len);
		}

		if (t_end == null) {
			end_key = len + tau + 2;
			t_end = M.get(end_key);
		}

		if (t_end == null) {
			t_end = M.get(len + 1);
		}

		if (t_start != null) {
			start = t_start.intValue();
		}

		if (t_end != null) {
			end = t_end.intValue();
		}

		return new int[] { start, end };
	}

	public List<StringRecord> getStringRecords() {
		return srs;
	}

	private Set<String> getStringSet(Gram[] grams, List<Integer> locs) {
		Set<String> ret = new HashSet<String>();
		for (int i = 0; i < locs.size(); i++) {
			ret.add(grams[locs.get(i)].getString());
		}
		return ret;
	}

	public StringVerifier getStringVerifier() {
		return stringVerifier;
	}

	public int getTau() {
		return tau;
	}

	public void index(List<StringRecord> input) {
		System.out.printf("index [%s] records.\n", input.size());

		StringSorter.sortByLength(input);

		System.out.println("sorted records by length.");

		srs = new ArrayList<StringRecord>();
		allGrams = new ArrayList<Gram[]>();

		for (int i = 0; i < input.size(); i++) {
			StringRecord sr = input.get(i);
			String s = sr.getString();
			Gram[] grams = gg.generate(s.toLowerCase());

			if (grams.length == 0) {
				continue;
			}

			allGrams.add(grams);
			srs.add(sr);
		}

		System.out.printf("generated [%d] q-grams.\n", allGrams.size());

		if (gramOrderer.getGramWeights() == null) {
			gramOrderer.setGramWeights(GramWeighter.computeWeightsByGramCounts(allGrams));
		}

		System.out.printf("determined [%d] q-grams orders.\n", gramOrderer.getGramWeights().size());

		gramOrderer.determineGramOrders();
		gramOrders = gramOrderer.getGramOrders();

		if (pivotSelector instanceof OptimalPivotSelector) {
			((OptimalPivotSelector) pivotSelector).setGramWeights(gramOrderer.getGramWeights());
		}

		System.out.println("make an inverted index.");

		L = new GramInvertedIndex();

		for (int i = 0; i < allGrams.size(); i++) {
			String s = srs.get(i).getString();
			Gram[] grams = allGrams.get(i);

			gramOrderer.order(grams);
			pivotSelector.select(grams);

			int len = s.length();

			for (int j = 0; j < grams.length; j++) {
				Gram gram = grams[j];
				String g = gram.getString();
				int start = gram.getStart();
				Type type = gram.getType();

				if (type == Type.SUFFIX) {
					continue;
				}

				GramPostings gp = L.get(g, true);
				gp.getEntries().add(new GramPostingEntry(i, start, type));

				DeepMap<Type, Integer, Integer> M = gp.getTypeLengthLocs();

				if (!M.containsKeys(Type.PREFIX, len)) {
					M.put(Type.PREFIX, len, gp.getEntries().size() - 1);
				}

				if (type == Type.PIVOT) {
					if (!M.containsKeys(Type.PIVOT, len)) {
						M.put(Type.PIVOT, len, gp.getEntries().size() - 1);
					}
				}
			}
		}

		computeCharacterWeights();
	}

	public void read(BufferedReader reader) throws Exception {
		{
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("## PPSS")) {
					break;
				}
			}
		}

		int num_read = GramUtils.getNumLinesToRead(reader);

		for (int i = 0; i < num_read; i++) {
			String line = reader.readLine();
			String[] parts = line.split("\t");

			if (i == 0) {
				q = Integer.parseInt(parts[1]);
				prefix_size = q * tau + 1;
			} else if (i == 1) {
				tau = Integer.parseInt(parts[1]);
				pivot_size = tau + 1;
			} else if (i == 2) {
				pivotSelector = Boolean.parseBoolean(parts[1]) ?

				new OptimalPivotSelector(q, prefix_size, pivot_size) : new RandomPivotSelector(q, pivot_size, prefix_size);
			}
		}

		stringVerifier = new StringVerifier(q, tau);
		gg = new GramGenerator(q);

		srs = new ArrayList<StringRecord>();

		allGrams = new ArrayList<Gram[]>();

		num_read = GramUtils.getNumLinesToRead(reader);

		for (int i = 0; i < num_read; i++) {
			String line = reader.readLine();
			String[] parts = line.split("\t");
			int id = Integer.parseInt(parts[1]);
			String s = parts[2];
			srs.add(new StringRecord(id, s));
			allGrams.add(gg.generate(s));
		}

		L = new GramInvertedIndex();
		L.read(reader);

		gramOrderer = new GramOrderer();
		Counter<String> gramWeights = new Counter<String>();

		num_read = GramUtils.getNumLinesToRead(reader);

		for (int i = 0; i < num_read; i++) {
			String line = reader.readLine();
			String[] parts = line.split("\t");
			gramWeights.setCount(parts[0], Double.parseDouble(parts[1]));
		}

		gramOrderer.setGramWeights(gramWeights);
		gramOrderer.determineGramOrders();
	}

	public void read(String fileName) throws Exception {
		BufferedReader reader = IOUtils.openBufferedReader(fileName);
		read(reader);
		reader.close();
	}

	public Counter<StringRecord> search(String s) {
		Gram[] sGrams = gg.generate(s.toLowerCase());

		if (sGrams.length == 0) {
			return new Counter<StringRecord>();
		}

		gramOrderer.order(sGrams);
		pivotSelector.select(sGrams);

		ListMap<Type, Integer> typeLocs = GramUtils.groupGramsByTypes(sGrams, true);
		Set<String> prefixesInS = getStringSet(sGrams, typeLocs.get(Type.PREFIX));
		Set<String> pivotsInS = getStringSet(sGrams, typeLocs.get(Type.PIVOT));
		Pair<String, Integer> lastPrefixInS = getLastPrefix(sGrams, typeLocs);

		if (lastPrefixInS.getSecond() < 0) {
			return new Counter<StringRecord>();
		}

		Set<Integer> C = new HashSet<Integer>();

		Type[] searchTypes = new Type[] { Type.PIVOT, Type.PREFIX };
		Type[] indexTypes = new Type[] { Type.PREFIX, Type.PIVOT };

		int len = s.length();

		for (int i = 0; i < searchTypes.length; i++) {
			Type searchType = searchTypes[i];
			Type indexType = indexTypes[i];

			for (int loc : typeLocs.get(searchType)) {
				Gram gram = sGrams[loc];
				GramPostings gp = L.get(gram.getString(), false);

				if (gp == null) {
					continue;
				}

				int[] range = getSearchRange(len, indexType, gp);
				int start = range[0];
				int end = range[1];

				// System.out.println(gram.getString() + " -> " + postings.toString());

				for (int j = start; j < gp.size() && j < end; j++) {
					GramPostingEntry entry = gp.getEntries().get(j);
					if (searchType == Type.PREFIX) {
						if (indexType != Type.PIVOT) {
							continue;
						}
					}

					int rid = entry.getId();
					int p = entry.getStart();

					Gram[] rGrams = allGrams.get(rid);
					ListMap<Type, Integer> groupsInR = GramUtils.groupGramsByTypes(rGrams, true);
					Set<String> prefixesInR = getStringSet(rGrams, groupsInR.get(Type.PREFIX));
					Set<String> pivotsInR = getStringSet(rGrams, groupsInR.get(Type.PIVOT));
					Pair<String, Integer> lastPrefixInR = getLastPrefix(rGrams, groupsInR);

					if (lastPrefixInR.getSecond() < 0) {
						continue;
					}

					/*
					 * Lemma 2. If srs r and s are similar, we have
					 * 
					 * If last(pre(r)) > last(pre(s)), piv(s) ∩ pre(r) != phi ; If last(pre(r)) <= last(pre(s)), piv(r) ∩ pre(s) != phi;
					 */

					// if (Math.abs(p - gram.getStart()) > tau) {
					// continue;
					// }

					// if (Math.abs(p - gram.getStart()) > tau
					//
					// || (lastPrefixInS.getSecond() > lastPrefixInR.getSecond()
					// && getIntersection(pivotsInR, prefixesInS).size() == 0)
					//
					// || (lastPrefixInR.getSecond() > lastPrefixInS.getSecond()
					// && getIntersection(pivotsInS, prefixesInR).size() == 0)) {
					// continue;
					// }

					C.add(rid);
				}
			}
		}

		Counter<StringRecord> A = new Counter<StringRecord>();

		SmithWaterman sw = new SmithWaterman();
		// sw.setChWeight(chWeights);

		for (int loc : C) {
			String r = srs.get(loc).getString();

			if (r.equals("Don Mattingly")) {
				System.out.println();
			}

			// if (!stringVerifier.verify(s, sGrams, r)) {
			// continue;
			// }

			double swScore = sw.getNormalizedScore(s, r);

			// double long_len = Math.max(s.length(), r.length());
			// double sim = 1 - (ed / long_len);

			// double edit_dist = stringVerifier.getEditDistance();
			A.incrementCount(srs.get(loc), swScore);
		}

		return A;
	}

	public void setGramSorter(GramOrderer gramOrderer) {
		this.gramOrderer = gramOrderer;
	}

	public void setPivotSelector(PivotSelector pivotSelector) {
		this.pivotSelector = pivotSelector;
	}

	public void write(BufferedWriter writer) throws Exception {
		writer.write(String.format("## PPSS\n"));
		writer.write(String.format("## Basic Parameters\t%d\n", 3));
		writer.write(String.format("q\t%d\n", q));
		writer.write(String.format("tau\t%d\n", tau));
		writer.write(String.format("useOptimalPivotSelector\t%s\n", pivotSelector instanceof OptimalPivotSelector ? true : false));
		writer.write("\n");
		writer.write(String.format("## Strings\t%d\n", srs.size()));

		for (int i = 0; i < srs.size(); i++) {
			StringRecord sr = srs.get(i);
			writer.write(String.format("%d\t%d\t%s", i, sr.getId(), sr.getString()));
			if (i != srs.size() - 1) {
				writer.write("\n");
			}
			writer.flush();
		}
		writer.write("\n\n");

		L.write(writer);

		writer.write("\n\n");

		Counter<String> gramWeights = gramOrderer.getGramWeights();

		writer.write(String.format("## Gram Weights\t%d\n", gramWeights.size()));

		List<String> grams = gramWeights.getSortedKeys();

		for (int i = 0; i < grams.size(); i++) {
			String g = grams.get(i);
			double weight = gramWeights.getCount(g);
			writer.write(String.format("%s\t%s", g, Double.toString(weight)));
			if (i != grams.size() - 1) {
				writer.write("\n");
			}
			writer.flush();
		}

	}

	public void write(String fileName) throws Exception {
		BufferedWriter writer = IOUtils.openBufferedWriter(fileName);
		write(writer);
		writer.close();
	}

	public void writeObject(String fileName) throws Exception {
		ObjectOutputStream oos = IOUtils.openObjectOutputStream(fileName);
		oos.writeObject(this);
		oos.close();
	}
}
