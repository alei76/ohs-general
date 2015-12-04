package ohs.entity;

import java.util.Map;

import ohs.entity.data.struct.Author;
import ohs.entity.data.struct.BilingualText;
import ohs.entity.data.struct.ControlNumber;
import ohs.entity.data.struct.Organization;
import ohs.entity.data.struct.Paper;
import ohs.entity.data.struct.PaperSource;

public class PaperCreater {

	public static Paper create(Map<PaperAttr, String> attrValueMap) {
		ControlNumber cn = new ControlNumber(attrValueMap.get(PaperAttr.CN1), attrValueMap.get(PaperAttr.CN2));
		BilingualText abs = new BilingualText(attrValueMap.get(PaperAttr.ABK), attrValueMap.get(PaperAttr.ABE));
		BilingualText title = new BilingualText(attrValueMap.get(PaperAttr.TIK), attrValueMap.get(PaperAttr.TIE));
		BilingualText sourceTitle = new BilingualText(attrValueMap.get(PaperAttr.JTK), attrValueMap.get(PaperAttr.JTE));
		String year = attrValueMap.get(PaperAttr.PY);
		PaperSource source = new PaperSource(sourceTitle, year);

		String korNameStr = attrValueMap.get(PaperAttr.AUK);
		String engNameStr = attrValueMap.get(PaperAttr.AUE);

		String emailStr = attrValueMap.get(PaperAttr.EM);

		String korOrgStr = attrValueMap.get(PaperAttr.CSK);
		String engOrgStr = attrValueMap.get(PaperAttr.CSE);

		String[] korNames = splits(korNameStr);
		String[] engNames = splits(engNameStr);

		String[] korOrgs = splits(korOrgStr);
		String[] engOrgs = splits(engOrgStr);

		String[] emails = splits(emailStr);

		int num_kor_names = korNames.length;
		int num_eng_names = engNames.length;
		int num_kor_orgs = korOrgs.length;
		int num_eng_orgs = engOrgs.length;
		int num_emails = emails.length;

		boolean isValid = true;

		if (num_kor_names > 0 && num_kor_orgs > 0 && num_kor_names == num_kor_orgs) {
			if (num_emails > 0) {
				if (num_kor_names == num_emails) {

				} else {
					isValid = false;
				}
			}

			if (num_eng_names > 0) {
				if (num_kor_names == num_eng_names) {

				} else {
					isValid = false;
				}
			}

			if (num_eng_orgs > 0) {
				if (num_kor_names == num_eng_orgs) {

				} else {
					isValid = false;
				}
			}

		} else {
			isValid = false;
		}

		if (isValid) {
			// System.out.printf("##Valid\n", num_kor_names, korNameStr);
			// System.out.printf("KOR:\t%d\t%s\n", num_kor_names, korNameStr);
			// System.out.printf("ENG:\t%d\t%s\n", num_eng_names, engNameStr);
			// System.out.printf("KOR:\t%d\t%s\n", num_kor_orgs, korOrgStr);
			// System.out.printf("ENG:\t%d\t%s\n", num_eng_orgs, engOrgStr);
			// System.out.printf("EML:\t%d\t%s\n", num_emails, emailStr);
			// System.out.println();
		} else {
			// System.out.printf("@@Invalid\n", num_kor_names, korNameStr);
			// System.out.printf("KOR:\t%d\t%s\n", num_kor_names, korNameStr);
			// System.out.printf("ENG:\t%d\t%s\n", num_eng_names, engNameStr);
			// System.out.printf("KOR:\t%d\t%s\n", num_kor_orgs, korOrgStr);
			// System.out.printf("EML:\t%d\t%s\n", num_emails, emailStr);
			// System.out.println();
		}

		Paper ret = null;

		if (isValid) {
			Author[] authors = new Author[num_kor_names];

			for (int i = 0; i < num_kor_names; i++) {
				String korName = korNames[i];
				String korOrg = korOrgs[i];

				String engName = num_eng_names > 0 ? engNames[i] : null;
				String engOrg = num_eng_orgs > 0 ? engOrgs[i] : null;
				String email = num_emails > 0 ? emails[i] : null;

				BilingualText authorName = new BilingualText(korName, engName);
				BilingualText orgName = new BilingualText(korOrg, engOrg);

				Organization org = new Organization(0, null, orgName);
				Author author = new Author(0, null, authorName, org, email);
				authors[i] = author;
			}

			ret = new Paper(0, null, cn, authors, title, abs, null, source);

			// System.out.println(ret.toString());
			// System.out.println();
		}
		return ret;
	}

	public static String[] splits(String s) {
		String[] ret = new String[0];
		if (s != null) {
			ret = s.split(";");
		}

		for (int i = 0; i < ret.length; i++) {
			ret[i] = ret[i].trim();
			ret[i] = ret[i].replace("\n", " ");
			ret[i] = ret[i].replace("\t", " ");

			if (ret[i].length() == 0) {
				ret[i] = null;
			}
		}

		return ret;
	}
}
