package ohs.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ohs.entity.data.struct.BilingualText;
import ohs.entity.data.struct.Organization;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.types.Counter;

public class DataReader {

	public static void main(String[] args) {
		System.out.println("process begins.");

		// List<Organization> orgs = readOrganizations(ENTPath.BASE_ORG_NAME_FILE);
		List<Organization> orgs2 = readOrganizationHistories(ENTPath.BASE_ORG_HISTORY_FILE);

		System.out.println("process ends.");
	}

	public static Organization parse(List<String> lines) {
		Map<String, Organization> map = new HashMap<String, Organization>();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			if (line.startsWith("ID\t")) {
				continue;
			}

			String[] parts = split(line);

			// try {
			int id = -1;
			String country = parts[1];
			String type = parts[2];

			String korName = parts[3].trim();
			String engName = parts[4].trim();
			BilingualText orgName = new BilingualText(korName, engName);

			String korAbbr = parts[5];
			String engAbbr = parts[6];

			String korHistory = parts[7];
			String engHistory = parts[8];
			String year = parts[9].trim();
			String homepage = parts[10];
			String desc = parts[11];

			Organization org = map.get(korName);

			if (org == null) {
				org = new Organization(id, null, orgName);
				org.getKoreanVariants().add(korAbbr);
				org.getEnglishVariants().add(engAbbr);
				org.setHomepage(homepage);
				map.put(korName, org);
			} else {
				if (org.getYear() > 0) {
					System.out.println(org);
					System.out.println();
				}
			}

			if (korHistory.length() == 0) {

				org.setYear(Integer.parseInt(year));
			} else {
				String[] events = korHistory.split(";");
				String[] eventYears = year.split(";");

				assert (events.length == eventYears.length);

				for (int j = 0; j < events.length; j++) {
					String event = events[j];
					String eventYear = eventYears[j];

					Map<Integer, Character> ops = new TreeMap<Integer, Character>();

					for (int k = 0; k < event.length(); k++) {
						char ch = event.charAt(k);
						if (ch == '-' || ch == '+') {
							ops.put(k, ch);
						}
					}

					String[] memberNames = event.split("[+-]");

					for (int k = 0; k < memberNames.length; k++) {
						String memberName = memberNames[k];

						if (memberName.length() == 0) {
							continue;
						}

						Organization member = map.get(memberName);

						if (member == null) {
							member = new Organization(-1, null, new BilingualText(memberName, ""));
						} else {
							// System.out.println();
						}

						if (org.equals(member)) {
							continue;
						}

						org.getHistory().add(member);
					}

					org.setYear(Integer.parseInt(eventYear));
				}
			}
		}

		List<Organization> orgs = new ArrayList<Organization>();

		{

			List<Organization> temp = new ArrayList<Organization>(map.values());

			Counter<Integer> c = new Counter<Integer>();

			for (int i = 0; i < temp.size(); i++) {
				c.setCount(i, temp.get(i).getYear());
			}

			List<Integer> locs = c.getSortedKeys(true);

			for (int i = 0; i < locs.size(); i++) {
				int loc = locs.get(i);
				orgs.add(temp.get(loc));
			}
		}

		// for (int i = 0; i < orgs.size(); i++) {
		// System.out.println(orgs.get(i));
		// System.out.println();
		// }
		//
		// System.out.println("------------------------------");

		return null;
	}

	public static List<BilingualText> readBaseOrgNames(String fileName) {
		List<BilingualText> ret = new ArrayList<BilingualText>();
		List<Organization> orgs = readOrganizations(fileName);
		for (Organization org : orgs) {
			ret.add(org.getName());
		}
		return ret;
	}

	public static Counter<BilingualText> readBilingualTextCounter(String fileName) {
		System.out.printf("read [%s].\n", fileName);
		Counter<BilingualText> ret = new Counter<BilingualText>();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			String korOrg = parts[0].equals("null") ? "" : parts[0];
			String engOrg = parts[1].equals("null") ? "" : parts[1];
			double cnt = Double.parseDouble(parts[2]);

			BilingualText orgName = new BilingualText(korOrg, engOrg);

			ret.incrementCount(orgName, cnt);
		}
		reader.close();
		return ret;
	}

	public static List<Organization> readOrganizationHistories(String fileName) {
		System.out.printf("read [%s].\n", fileName);
		List<Organization> ret = new ArrayList<Organization>();

		// List<Organization> lines = new ArrayList<Organization>();

		Counter<String> c = new Counter<String>();

		TextFileReader reader = new TextFileReader(fileName, IOUtils.EUC_KR);
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();

			if (reader.getNumLines() == 1) {
				continue;
			}

			parse(lines);

			// } catch (Exception e) {
			// System.out.println(StrUtils.join("\t", parts));
			// }

		}
		reader.close();

		System.out.printf("num orgs:\t%d\n", c.size());

		return ret;
	}

	public static List<Organization> readOrganizations(String fileName) {
		System.out.printf("read [%s].\n", fileName);
		List<Organization> ret = new ArrayList<Organization>();
		TextFileReader reader = new TextFileReader(fileName, IOUtils.EUC_KR);
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}

			String line = reader.next();
			String[] parts = split(line);

			int id = Integer.parseInt(parts[0]);
			String country = parts[1];
			String type = parts[2];

			String korName = parts[3].trim();
			String engName = parts[4].trim();
			BilingualText orgName = new BilingualText(korName, engName);

			String korVariants = parts[5];

			String korAbbrs = parts[6];
			String engAbbrs = parts[7];
			String year = parts[8];
			String homepage = parts[9];

			Organization org = new Organization(id, null, orgName);
			org.setHomepage(homepage);

			if (korAbbrs.length() > 0) {
				String[] abbrs = korAbbrs.split(",");
				for (String abbr : abbrs) {
					org.getKoreanVariants().add(abbr.trim());
				}
			}

			if (engAbbrs.length() > 0) {
				String[] abbrs = engAbbrs.split(",");
				for (String abbr : abbrs) {
					org.getEnglishVariants().add(abbr.trim());
				}
			}

			ret.add(org);
		}
		reader.close();
		return ret;
	}

	public static String[] split(String line) {
		String[] ret = line.replace("\t", "\t_").split("\t");
		for (int j = 0; j < ret.length; j++) {
			if (ret[j].startsWith("_")) {
				ret[j] = ret[j].substring(1);
			}

			if (ret[j].equals("empty")) {
				ret[j] = "";
			}
			ret[j] = ret[j].trim();
		}
		return ret;
	}

}
