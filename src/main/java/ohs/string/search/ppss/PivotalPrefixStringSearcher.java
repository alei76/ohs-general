package ohs.string.search.ppss;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.eden.org.DataReader;
import ohs.eden.org.ENTPath;
import ohs.eden.org.data.struct.BilingualText;
import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.string.search.ppss.Gram.Type;
import ohs.string.sim.SequenceFactory;
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
public class PivotalPrefixStringSearcher implements Serializable {

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

	public static void test1() throws Exception {

		List<BilingualText> orgNames = DataReader.readBaseOrgNames(ENTPath.BASE_ORG_NAME_FILE);
		Counter<BilingualText> extOrgCounts = DataReader.readBilingualTextCounter(ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE);

		List<StringRecord> strings = new ArrayList<StringRecord>();
		List<StringRecord> strings2 = new ArrayList<StringRecord>();

		for (int i = 0; i < orgNames.size(); i++) {
			strings.add(new StringRecord(i, orgNames.get(i).getKorean()));
			strings2.add(new StringRecord(i, orgNames.get(i).getKorean()));
		}

		GramOrderer gramOrderer = new GramOrderer();

		// {
		// List<StringRecord> srs = new ArrayList<StringRecord>();
		//
		// for (BilingualText orgName : externalOrgCounts.keySet()) {
		// String korName = orgName.getKorean();
		// if (korName.length() == 0) {
		// continue;
		// }
		// srs.add(new StringRecord(srs.size(), korName));
		// }
		//
		// Counter<String> gramWeights1 = GramWeighter.compute(new
		// GramGenerator(2), srs);
		// System.out.println(gramWeights1);
		// gramOrderer.setGramWeights(gramWeights1);
		// }

		int q = 2;
		int tau = 3;

		PivotalPrefixStringSearcher ppss = new PivotalPrefixStringSearcher(q, tau, true);
		ppss.setGramSorter(gramOrderer);
		ppss.index(strings);
		// ppss.write(ENTPath.PPSS_INDEX_FILE);
		// ppss.writeObject(ENTPath.PPSS_OBJECT_FILE);

		{

			TextFileWriter writer = new TextFileWriter(ENTPath.PPSS_RESULT_FILE);
			List<BilingualText> testOrgs = extOrgCounts.getSortedKeys();

			for (int i = 0; i < testOrgs.size(); i++) {
				BilingualText name = testOrgs.get(i);
				double cnt = extOrgCounts.getCount(name);

				System.out.println(name + "\n");
				Counter<StringRecord> res = ppss.search(name.getKorean());
				StringBuffer sb = new StringBuffer();
				sb.append("[Input]:\n");
				sb.append(name.toString() + "\n");
				sb.append(String.format("[Output]:\n"));

				List<StringRecord> orgs = res.getSortedKeys();

				for (int j = 0; j < orgs.size() && j < 10; j++) {
					StringRecord sr = orgs.get(j);
					double score = res.getCount(sr);
					sb.append(String.format("%d, %d, %s, %s\n", j + 1, sr.getId(), sr.getString(), score + ""));
				}

				System.out.println(sb.toString());

				writer.write(sb.toString());
			}
			writer.close();
		}
	}

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

		PivotalPrefixStringSearcher ppss = new PivotalPrefixStringSearcher(q, tau, true);
		ppss.setGramSorter(gramOrderer);
		ppss.index(strings);
		Counter<StringRecord> res = ppss.search(s);

		System.out.println(res.toString());
	}

	public static void test3() throws Exception {
		List<BilingualText> orgNames = DataReader.readBaseOrgNames(ENTPath.BASE_ORG_NAME_FILE);
		// Counter<BilingualText> externalOrgCounts =
		// DataReader.readBilingualTextCounter(ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE);

		List<StringRecord> srs = new ArrayList<StringRecord>();
		List<StringRecord> srs2 = new ArrayList<StringRecord>();

		for (int i = 0; i < orgNames.size(); i++) {
			srs.add(new StringRecord(i, orgNames.get(i).getKorean()));
			srs2.add(new StringRecord(i, orgNames.get(i).getKorean()));
		}

		// GramOrderer gramOrderer = new GramOrderer();

		// {
		// List<StringRecord> srs = new ArrayList<StringRecord>();
		//
		// for (BilingualText orgName : externalOrgCounts.keySet()) {
		// String korName = orgName.getKorean();
		// if (korName.length() == 0) {
		// continue;
		// }
		// srs.add(new StringRecord(srs.size(), korName));
		// }
		//
		// Counter<String> gramWeights1 = GramWeighter.compute(new
		// GramGenerator(2), srs);
		// System.out.println(gramWeights1);
		// gramOrderer.setGramWeights(gramWeights1);
		// }

		int q = 2;
		int tau = 2;

		PivotalPrefixStringSearcher ppss = new PivotalPrefixStringSearcher(q, tau, true);
		// ppss.setGramSorter(gramOrderer);
		ppss.index(srs);
		// ppss.write(ENTPath.PPSS_INDEX_FILE);
		// ppss.writeObject(ENTPath.PPSS_OBJECT_FILE);

		// {
		// TextFileWriter writer = new TextFileWriter(ENTPath.DATA_DIR +
		// "ppss_res.txt");
		// for (int i = 0; i < strings2.size(); i++) {
		// String str = strings2.get(i);
		// System.out.println(str);
		//
		// // if (!str.contains("경남양돈산업클러스터사업단")) {
		// // continue;
		// // }
		// Counter<String> res = ext.search(str);
		// writer.write(String.format("Input:\t%s\n", str));
		// writer.write(String.format("Output:\t%s\n\n",
		// res.toStringSortedByValues(false, false, res.size())));
		// }
		//
		// writer.close();
		// }

		TextFileReader reader = new TextFileReader(ENTPath.PATENT_ORG_FILE_2);
		TextFileWriter writer = new TextFileWriter(ENTPath.PPSS_RESULT_FILE);

		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");

			String korName = null;
			Counter<String> engNameCounts = new Counter<String>();

			for (int i = 0; i < parts.length; i++) {
				String[] two = StrUtils.split2Two(":", parts[i]);
				String name = two[0];
				double cnt = Double.parseDouble(two[1]);
				if (i == 0) {
					korName = name;
				} else {
					engNameCounts.incrementCount(name, cnt);
				}
			}

			BilingualText orgName = new BilingualText(korName, engNameCounts.argMax());

			Counter<StringRecord> ret = ppss.search(korName);

			List<StringRecord> keys = ret.getSortedKeys();
			int num_candidates = 10;
			StringBuffer sb = new StringBuffer(line);
			for (int i = 0; i < keys.size() && i < num_candidates; i++) {
				StringRecord sr = keys.get(i);
				double score = ret.getCount(sr);
				sb.append(String.format("\n%d\t%s\t%f", i + 1, sr, score));
			}

			writer.write(sb.toString() + "\n\n");
		}
		writer.close();
	}

	private int q;

	private int tau;

	private int prefix_size;

	private int pivot_size;

	private List<StringRecord> srs;

	private List<Gram[]> allGrams;

	private GramInvertedIndex L;

	private Map<String, Integer> gramOrders;

	private GramGenerator gramGenerator;

	private PivotSelector pivotSelector;

	private StringVerifier stringVerifier;

	private GramOrderer gramOrderer;

	private Counter<Character> chWeights;

	public PivotalPrefixStringSearcher() {
		this(2, 2, false);
	}

	public PivotalPrefixStringSearcher(int q, int tau, boolean useOptimalPivotSelector) {
		this.q = q;
		this.tau = tau;

		pivot_size = tau + 1;

		prefix_size = q * tau + 1;

		this.gramOrderer = new GramOrderer();

		gramGenerator = new GramGenerator(q);

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

	private int[] determineSearchRange(int len, Type indexType, GramPostings postings) {
		int[] ret = new int[] { 0, postings.getEntries().size() };
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

		return ret;
	}

	public List<Gram[]> getAllGrams() {
		return allGrams;
	}

	public GramGenerator getGramGenerator() {
		return gramGenerator;
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
			Gram[] grams = gramGenerator.generateQGrams(s.toLowerCase());

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
		gramGenerator = new GramGenerator(q);

		srs = new ArrayList<StringRecord>();

		allGrams = new ArrayList<Gram[]>();

		num_read = GramUtils.getNumLinesToRead(reader);

		for (int i = 0; i < num_read; i++) {
			String line = reader.readLine();
			String[] parts = line.split("\t");
			int id = Integer.parseInt(parts[1]);
			String s = parts[2];
			srs.add(new StringRecord(id, s));
			allGrams.add(gramGenerator.generateQGrams(s));
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
		BufferedReader reader = FileUtils.openBufferedReader(fileName);
		read(reader);
		reader.close();
	}

	public Counter<StringRecord> search(String s) {
		Gram[] sGrams = gramGenerator.generateQGrams(s.toLowerCase());

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
				String g = gram.getString();
				GramPostings gp = L.get(g, false);

				if (gp == null) {
					continue;
				}

				int[] range = determineSearchRange(len, indexType, gp);

				int start = range[0];
				int end = range[1];

				// System.out.println(gram.getString() + " -> " +
				// postings.toString());

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
					 * Lemma 2. If srs r and text are similar, we have
					 * 
					 * If last(pre(r)) > last(pre(text)), piv(text) ∩ pre(r) != phi ; If last(pre(r)) <= last(pre(text)), piv(r) ∩ pre(text)
					 * != phi;
					 */

					if (Math.abs(p - gram.getStart()) > tau

							|| (lastPrefixInS.getSecond() > lastPrefixInR.getSecond()
									&& getIntersection(pivotsInR, prefixesInS).size() == 0)

							|| (lastPrefixInR.getSecond() > lastPrefixInS.getSecond()
									&& getIntersection(pivotsInS, prefixesInR).size() == 0)) {
						continue;
					}

					C.add(rid);
				}
			}
		}

		Counter<StringRecord> A = new Counter<StringRecord>();

		SmithWaterman<Character> sw = new SmithWaterman<Character>();
		// sw.setChWeight(chWeights);

		for (int loc : C) {
			String r = srs.get(loc).getString();

			if (r.equals("Don Mattingly")) {
				System.out.println();
			}

			// if (!stringVerifier.verify(text, sGrams, r)) {
			// continue;
			// }

			double swScore = sw.getSimilarity(SequenceFactory.newCharSequences(s, r));

			// double long_len = Math.max(text.length(), r.length());
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
		BufferedWriter writer = FileUtils.openBufferedWriter(fileName);
		write(writer);
		writer.close();
	}

	public void writeObject(String fileName) throws Exception {
		ObjectOutputStream oos = FileUtils.openObjectOutputStream(fileName);
		oos.writeObject(this);
		oos.close();
	}
}
