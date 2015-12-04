package ohs.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohs.entity.data.struct.BilingualText;
import ohs.io.TextFileReader;
import ohs.utils.StrUtils;

public class OrganizationNormalizer {
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		test();

		System.out.println("process ends.");
	}

	public static void test() {
		OrganizationNormalizer n = new OrganizationNormalizer(ENTPath.COMMON_DEPT_ABBR_DICT_FILE);

		TextFileReader reader = new TextFileReader(ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");

			String korOrg = parts[0].equals("null") ? null : parts[0];
			String engOrg = parts[1].equals("null") ? null : parts[1];

			BilingualText orgName = new BilingualText(korOrg, engOrg);
			double cnt = Double.parseDouble(parts[2]);

			// if (count < 50 || count > 100) {
			// continue;
			// }

			if (orgName.getKorean() == null || orgName.getEnglish() == null) {
				continue;
			}

			BilingualText orgName2 = n.normalize(orgName);

			if (!orgName.equals(orgName2)) {
				StringBuffer sb = new StringBuffer();
				sb.append("[Input]\n");
				sb.append(String.format("%s\n", orgName));
				sb.append("[Output]\n");
				sb.append(String.format("%s\n", orgName2));
				System.out.println(sb.toString());
			}

		}
		reader.close();

	}

	private Map<String, String> engAbbrs;

	private Pattern p1;

	private Pattern p2 = Pattern.compile("Co\\.?\\,? ?Ltd\\.?", Pattern.CASE_INSENSITIVE);

	public OrganizationNormalizer(String fileName) {
		read(fileName);

		StringBuffer sb = new StringBuffer();
		sb.append("(");

		List<String> shortForms = new ArrayList<String>(engAbbrs.keySet());

		for (int i = 0; i < shortForms.size(); i++) {
			String shortForm = shortForms.get(i);
			shortForm = shortForm.replace(".", "\\.");
			// shortForm = shortForm.replace("'", "\\'");
			sb.append(shortForm);
			if (i != shortForms.size() - 1) {
				sb.append("|");
			}
		}
		sb.append(")");

		p1 = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
	}

	public BilingualText normalize(BilingualText orgName) {
		String korName = orgName.getKorean();
		String engName = orgName.getEnglish();

		if (korName != null) {
			korName = normalizeKorean(korName);
		}

		if (engName != null) {
			engName = normalizeEnglish(engName);
		}
		BilingualText ret = new BilingualText(korName, engName);
		return ret;
	}

	public String normalizeEnglish(String s) {
		// s = s.replace("&", " and ");
		// s = s.replace("R and D", "R&D");
		s = s.replaceAll("[\\.]+", ".");
		s = s.replaceAll(",+", ",");

		StringBuffer sb = new StringBuffer();
		Matcher m = p1.matcher(s);

		while (m.find()) {
			String g = m.group();
			String r = engAbbrs.get(g.toLowerCase());
			m.appendReplacement(sb, r);
		}
		m.appendTail(sb);
		s = sb.toString();
		s = StrUtils.normalizeSpaces(s);

		sb = new StringBuffer();
		m = p2.matcher(s);

		while (m.find()) {
			String g = m.group();
			String r = "Co., Ltd.";
			m.appendReplacement(sb, r);
		}
		m.appendTail(sb);
		s = sb.toString();
		s = StrUtils.normalizeSpaces(s);

		return s;
	}

	public String normalizeKorean(String s) {
		StringBuffer sb = new StringBuffer();
		return s;
	}

	public void read(String fileName) {
		System.out.printf("read abbreviation patterns from [%s].\n", fileName);

		engAbbrs = new TreeMap<String, String>();

		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();
			String[] parts = line.split("\t");
			String shortForm = parts[0];
			String longForm = parts[1];
			engAbbrs.put(shortForm.toLowerCase(), longForm);
		}
		reader.close();
	}
}
