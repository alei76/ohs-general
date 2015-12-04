package ohs.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohs.entity.data.struct.BilingualText;
import ohs.entity.data.struct.Organization;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.types.Counter;
import ohs.types.ListMap;

public class OrgDataMerger {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		OrgDataMerger m = new OrgDataMerger();
		m.merge();

		System.out.println("process ends.");
	}

	public void merge() throws Exception {
		List<Organization> orgs = DataReader.readOrganizations(ENTPath.BASE_ORG_NAME_FILE);

		Map<String, Organization> orgMap = new HashMap<String, Organization>();
		Counter<String> orgCounts = new Counter<String>();

		for (Organization org : orgs) {
			orgMap.put(org.getName().getKorean(), org);
			orgCounts.incrementCount(org.getName().getKorean(), 1);
		}

		System.out.println(orgCounts.toString());

		ListMap<String, String[]> newOrgMap = new ListMap<String, String[]>();

		TextFileReader reader = new TextFileReader(ENTPath.BASE_ORG_HISTORY_FILE, IOUtils.EUC_KR);
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();

			if (reader.getNumLines() == 1) {
				continue;
			}

			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);

				if (line.startsWith("ID\t")) {
					continue;
				}

				String[] parts = DataReader.split(line);

				// try {
				int id = -1;
				String country = parts[1];
				String type = parts[2];

				String korName = parts[3].trim();
				String engName = parts[4].trim();

				String regex = "\\([^\\(\\)]+\\)";
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(korName);

				StringBuffer sb = new StringBuffer();

				while (m.find()) {
					// System.out.println(m.group());
					m.appendReplacement(sb, "");
				}
				m.appendTail(sb);

				korName = sb.toString().trim();

				BilingualText orgName = new BilingualText(korName, engName);

				String korAbbr = parts[5];
				String engAbbr = parts[6];

				String korHistory = parts[7];
				String engHistory = parts[8];
				String year = parts[9].trim();
				String homepage = parts[10];
				String desc = parts[11];

				if (year.contains(";")) {
					System.out.println();
				}

				Organization org = orgMap.get(korName);

				if (org == null) {
					org = new Organization(id, null, orgName);
					org.getKoreanVariants().add(korAbbr);
					org.getEnglishVariants().add(engAbbr);
					org.setHomepage(homepage);

					// System.out.println(org);
					newOrgMap.put(korName, parts);
				} else {

				}

			}
		}
		reader.close();

		List<String> korNames = new ArrayList<String>(newOrgMap.keySet());
		Collections.sort(korNames);

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < korNames.size(); i++) {
			String korName = korNames.get(i);
			List<String[]> list = newOrgMap.get(korName);
			if (list.size() > 1) {
				sb.append(korName + "\n");
				for (int j = 0; j < list.size(); j++) {
					String[] parts = list.get(j);
					String country = parts[1];
					String type = parts[2];

					String engName = parts[4].trim();
					BilingualText orgName = new BilingualText(korName, engName);

					String korAbbr = parts[5];
					String engAbbr = parts[6];

					String korHistory = parts[7];
					String engHistory = parts[8];
					String year = parts[9].trim();
					String homepage = parts[10];
					String desc = parts[11];
					String output = i + "\t" + country + "\t" + type + "\t" + korName + "\t" + engName + "\t" + "" + "\t" + korAbbr + "\t"
							+ engAbbr + "\t" + year + "\t" + homepage + "\t" + desc;
					sb.append(output + "\n");
				}
				sb.append("\n");
			} else {
				for (int j = 0; j < list.size(); j++) {
					String[] parts = list.get(j);
					String country = parts[1];
					String type = parts[2];

					String engName = parts[4].trim();
					BilingualText orgName = new BilingualText(korName, engName);

					String korAbbr = parts[5];
					String engAbbr = parts[6];

					String korHistory = parts[7];
					String engHistory = parts[8];
					String year = parts[9].trim();
					String homepage = parts[10];
					String desc = parts[11];
					String output = i + "\t" + country + "\t" + type + "\t" + korName + "\t" + engName + "\t" + "" + "\t" + korAbbr + "\t"
							+ engAbbr + "\t" + year + "\t" + homepage + "\t" + desc;
					sb.append(output + "\n");
				}
			}
		}

		IOUtils.write(ENTPath.DATA_DIR + "temp.txt", sb.toString().trim());

	}
}
