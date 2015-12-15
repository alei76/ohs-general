package ohs.entity;

import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ohs.classifier.centroid.CentroidClassifier;
import ohs.entity.data.struct.BilingualText;
import ohs.entity.data.struct.Organization;
import ohs.entity.org.OrganizationDetector.UnivComponent;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.string.search.ppss.GramOrderer;
import ohs.string.search.ppss.GramWeighter;
import ohs.string.search.ppss.PivotalPrefixStringSearcher;
import ohs.string.search.ppss.StringRecord;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.types.Pair;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

/**
 * @author Heung-Seon Oh
 * 
 * 
 * 
 */
public class EntityLinker implements Serializable {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		System.out.println("process ends.");
	}

	private PivotalPrefixStringSearcher[] searchers = new PivotalPrefixStringSearcher[2];

	private TextFileWriter logWriter = new TextFileWriter(ENTPath.ODK_LOG_FILE);

	private List<Organization> orgs;

	private Map<Integer, Organization> orgMap;

	private Map<Integer, Integer>[] recordToOrgMaps = new Map[2];

	public EntityLinker() {

	}

	/**
	 * Create two pivotal prefix searchers for English and Korean. If extOrgFileName is given, global gram orders are determined based on
	 * external organization names.
	 * 
	 * @param extOrgFileName
	 * 
	 *            Contains external organization names. They are used to compute global gram orders employed in Searchers.
	 */
	public void createSearchers(String extOrgFileName) {
		Counter<BilingualText> c = null;

		if (extOrgFileName != null) {
			c = DataReader.readBilingualTextCounter(extOrgFileName);
		}

		for (int i = 0, rid = 0; i < searchers.length; i++) {
			List<StringRecord> srs = new ArrayList<StringRecord>();
			Map<Integer, Integer> recordToOrgMap = new HashMap<Integer, Integer>();
			recordToOrgMaps[i] = recordToOrgMap;

			int q = 2;
			int tau = 3;

			if (i == 1) {
				q = 4;
			}

			for (int j = 0; j < orgs.size(); j++) {
				Organization org = orgs.get(j);

				String name = null;
				Set<String> variants = null;

				if (i == 0) {
					name = org.getName().getKorean();
					variants = org.getKoreanVariants();
				} else {
					name = org.getName().getEnglish();
					name = name.toLowerCase();

					variants = new HashSet<String>();

					for (String v : org.getEnglishVariants()) {
						variants.add(v.toLowerCase());
					}
				}

				if (name.length() == 0 || name.length() < q) {
					continue;
				}

				srs.add(new StringRecord(rid, name));
				recordToOrgMap.put(rid, org.getId());
				rid++;

				for (String variant : variants) {
					if (variant.length() == 0 || variant.length() < q) {
						continue;
					}

					srs.add(new StringRecord(rid, variant));
					recordToOrgMap.put(rid, org.getId());
					rid++;
				}
			}

			GramOrderer gramOrderer = new GramOrderer();

			if (c != null) {
				Counter<BilingualText> extOrgCounts = new Counter<BilingualText>();

				for (BilingualText orgName : c.keySet()) {
					double cnt = c.getCount(orgName);
					extOrgCounts.setCount(orgName, cnt);
				}

				List<StringRecord> ss = new ArrayList<StringRecord>();

				for (BilingualText orgName : extOrgCounts.keySet()) {
					String name = (i == 0) ? orgName.getKorean() : orgName.getEnglish();
					if (name.length() == 0 || name.length() < q) {
						continue;
					}
					ss.add(new StringRecord(ss.size(), name));
				}

				Counter<String> gramWeights = GramWeighter.compute(new GramGenerator(q), ss);
				gramOrderer.setGramWeights(gramWeights);
			}

			PivotalPrefixStringSearcher searcher = new PivotalPrefixStringSearcher(q, tau, true);
			searcher.setGramSorter(gramOrderer);
			searcher.index(srs);

			searchers[i] = searcher;

		}
	}

	public Counter<Organization> link(BilingualText orgName) {
		String[] names = new String[] { orgName.getKorean(), orgName.getEnglish() };
		Counter<StringRecord>[] searchScoreData = new Counter[2];

		for (int i = 0; i < searchScoreData.length; i++) {
			searchScoreData[i] = new Counter<StringRecord>();
		}

		for (int i = 0; i < names.length; i++) {
			String name = names[i];

			if (name.length() == 0) {
				continue;
			}

			String subName = name;

			if (i == 1) {
				subName = subName.toLowerCase();
			}

			PivotalPrefixStringSearcher searcher = searchers[i];

			Counter<StringRecord> searchScore = searcher.search(subName);

			{
				Map<Integer, Integer> recordToOrgMap = recordToOrgMaps[i];
				CounterMap<Integer, Integer> cm = new CounterMap<Integer, Integer>();

				Counter<StringRecord> tempScores = new Counter<StringRecord>();

				for (StringRecord sr : searchScore.keySet()) {
					double score = searchScore.getCount(sr);
					int rid = sr.getId();
					int oid = recordToOrgMap.get(rid);
					cm.incrementCount(oid, rid, score);
				}

				for (int oid : cm.keySet()) {
					Counter<Integer> c = cm.getCounter(oid);
					Organization org = orgMap.get(oid);
					tempScores.setCount(new StringRecord(oid, org.getName().getKorean()), c.max());
				}

				searchScore = tempScores;
				// System.out.println(cm);

			}
			searchScoreData[i] = searchScore;
		}

		logWriter.write(orgName.toString() + "\n");

		for (int i = 0; i < searchScoreData.length; i++) {
			logWriter.write(searchScoreData[i].toString());
			if (i != searchScoreData.length - 1) {
				logWriter.write("\n");
			}
		}

		logWriter.write("\n\n");

		Counter<Organization> ret = new Counter<Organization>();
		// Counter<StringRecord> c = new Counter<StringRecord>();

		for (int i = 0; i < searchScoreData.length; i++) {
			Counter<StringRecord> searchScores = searchScoreData[i];
			for (StringRecord sr : searchScores.keySet()) {
				double score = searchScores.getCount(sr);
				Organization org = orgMap.get(sr.getId());
				ret.incrementCount(org, score);
				// c.incrementCount(sr, 1);
			}
		}

		for (Organization org : ret.keySet()) {
			double score = ret.getCount(org);
			// double cnt = c.getCount(sr);
			// score /= cnt;
			ret.setCount(org, score / 2);
		}

		// logWriter.write(orgScores2.toString() + "\n\n");

		return ret;
	}

	/**
	 * Read organization names. The input of ODK is mapped to the a set of organization names.
	 * 
	 * @param orgFileName
	 */
	public void readOrganizations(String orgFileName) {
		orgs = DataReader.readOrganizations(orgFileName);
		orgMap = new HashMap<Integer, Organization>();

		for (int i = 0; i < orgs.size(); i++) {
			Organization org = orgs.get(i);
			orgMap.put(org.getId(), org);
		}
	}

	public void write(String fileName) throws Exception {
		BufferedWriter writer = IOUtils.openBufferedWriter(fileName);

		for (int i = 0; i < searchers.length; i++) {
			searchers[i].write(writer);
			if (i != searchers.length - 1) {
				writer.write("\n\n");
			}
		}
		writer.close();
	}

}
