package ohs.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohs.entity.data.struct.BilingualText;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.ListMap;
import ohs.types.Pair;

public class OrganizationDetector {

	public static enum UnivComponent {
		UNIVERSITY, COLLEGE, SCHOOL, DIVISION, DEPARTMENT
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		test();

		System.out.println("process ends.");

	}

	public static void test() {
		OrganizationDetector det = new OrganizationDetector();

		TextFileWriter writer = new TextFileWriter(ENTPath.DETECT_LOG_FILE);
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

			ListMap<UnivComponent, Pair<Integer, Integer>>[] res = det.detect(orgName);

			StringBuffer sb = new StringBuffer();
			sb.append("[Input]\n");
			sb.append(String.format("%s\n", orgName));
			sb.append("[Output]\n");

			for (int i = 0; i < res.length; i++) {
				ListMap<UnivComponent, Pair<Integer, Integer>> ret = res[i];
				String s = i == 0 ? orgName.getKorean() : orgName.getEnglish();

				if (i == 0) {
					sb.append("<Korean>\n");
				} else {
					sb.append("<English>\n");
				}

				if (ret.size() > 0) {
					List<UnivComponent> labels = new ArrayList<UnivComponent>(ret.keySet());

					for (int j = 0; j < labels.size(); j++) {
						UnivComponent label = labels.get(j);

						sb.append(label);

						List<Pair<Integer, Integer>> locs = ret.get(label);

						for (int k = 0; k < locs.size(); k++) {
							Pair<Integer, Integer> loc = locs.get(k);
							sb.append("\t" + s.substring(loc.getFirst(), loc.getSecond()));
						}
						sb.append("\n");
					}
				}
			}

			System.out.println(sb.toString() + "\n");

			writer.write(sb.toString() + "\n");

		}
		reader.close();

		//
		// ListMap<UnivComponent, Pair<Integer,Integer>>[] res = det
		// .detect(new BilingualText("경북대 지리학과", "Department of Geography, Kyungpook National University"));
		//
		// ListMap<UnivComponent, Pair<Integer,Integer>>[] res = det.detect(new BilingualText("부산대학교 조선해양공학과 대학원", ""));
		//
		// ListMap<UnivComponent, Pair<Integer,Integer>>[] res = det.detect(new BilingualText("부산대학교 자연과학대학 화학과", ""));
		//
		// ListMap<UnivComponent, Pair<Integer,Integer>>[] res = det.detect(new BilingualText("한양대학교 전자전기제어계측공학과",
		// "Department of Electronic, Electrical, Control and Instrumentation Engineering, Hanyang University"));
		// ListMap<UnivComponent, Pair<Integer,Integer>>[] res = det.detect(
		// new BilingualText("한국교원대학교 가정교육과", "Department of Home Economics Education, Korea National University of Education"));
		//
		// ListMap<UnivComponent, Pair<Integer,Integer>>[] res = det
		// .detect(new BilingualText("서울대학교 산림과학부", "Forest Science Department, Seoul National University"));
	}

	private String regex1 = "(^.+대(?:학교)?\\b)+(?: ?)?(\\b.+대학\\b)?(?: ?)?(\\b.+학부\\b)?(?: ?)?(\\b.+학?과\\b)?(?: ?)?(\\b.+교실\\b)?";

	private String regex2 = "^\\b[^\\s]+연구[소원]\\b";

	private String regex3 = "(\\bDepartment of [^,]+\\b|[^,]+ Department\\b)?(?:, ?)?(\\bDivision of [^,]+\\b)?(?:, ?)?(\\bSchool of [^,]+\\b)?(?:, ?)?(\\bCollege of [^,]+\\b)?(?:, ?)?(\\b[^,]+ University(?: of [^,]+\\b)?)";

	private Pattern p1 = Pattern.compile(regex1);

	private Pattern p2 = Pattern.compile(regex2);

	private Pattern p3 = Pattern.compile(regex3, Pattern.CASE_INSENSITIVE);

	public OrganizationDetector() {

	}

	public ListMap<UnivComponent, Pair<Integer, Integer>>[] detect(BilingualText orgName) {
		String engName = orgName.getEnglish();
		String korName = orgName.getKorean();

		ListMap<UnivComponent, Pair<Integer, Integer>>[] ret = new ListMap[2];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = new ListMap<UnivComponent, Pair<Integer, Integer>>();
		}

		if (korName.length() > 0) {
			ret[0] = detectKorean(korName);
		}

		if (engName.length() > 0) {
			ret[1] = detectEnglish(engName);
		}

		return ret;
	}

	public ListMap<UnivComponent, Pair<Integer, Integer>> detectEnglish(String s) {
		Matcher m = p3.matcher(s);

		ListMap<UnivComponent, Pair<Integer, Integer>> ret = new ListMap<UnivComponent, Pair<Integer, Integer>>();

		if (m.find()) {
			for (int i = 0; i <= m.groupCount(); i++) {
				// sb.append(String.format("%d\t%s\n", i, m.group(i)));

				if (i == 0 || m.group(i) == null) {
					continue;
				}

				int start = m.start(i);
				int end = m.end(i);
				UnivComponent label = UnivComponent.values()[UnivComponent.values().length - i];

				ret.put(label, new Pair<Integer, Integer>(start, end));
			}
		}

		return ret;
	}

	public ListMap<UnivComponent, Pair<Integer, Integer>> detectKorean(String s) {
		Matcher m = p1.matcher(s);

		ListMap<UnivComponent, Pair<Integer, Integer>> ret = new ListMap<UnivComponent, Pair<Integer, Integer>>();

		if (m.find()) {
			for (int i = 0; i <= m.groupCount(); i++) {

				if (i == 0 || m.group(i) == null) {
					continue;
				}

				UnivComponent label = null;

				if (i == 1) {
					label = UnivComponent.values()[0];
				} else if (i == 2) {
					label = UnivComponent.values()[1];
				} else if (i == 3) {
					label = UnivComponent.values()[2];
				} else {
					label = UnivComponent.values()[4];
				}

				if (label == null) {
					continue;
				}

				int start = m.start(i);
				int end = m.end(i);
				ret.put(label, new Pair<Integer, Integer>(start, end));
			}

		}

		return ret;
	}

}
