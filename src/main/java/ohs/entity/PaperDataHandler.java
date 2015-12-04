package ohs.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ohs.entity.data.struct.Author;
import ohs.entity.data.struct.BilingualText;
import ohs.entity.data.struct.Organization;
import ohs.entity.data.struct.Paper;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.StrUtils;

public class PaperDataHandler {

	private static boolean isNull(String s) {
		return s.equals("null") ? true : false;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		PaperDataHandler dh = new PaperDataHandler();
		// dh.process();

		// dh.extractKoreanAuthorNames();
		// dh.process2();

		// dh.selectSubsetForOrgHistory();
		//

		System.out.println("process ends.");
	}

	public void extractKoreanAuthorNames() throws Exception {
		TextFileReader reader = new TextFileReader(ENTPath.AUTHOR_META_FILE);
		reader.setPrintNexts(false);

		// TextFileWriter writer = new TextFileWriter(ENTPath.PERSON_SUBSET_FILE);

		CounterMap<String, String> cm = new CounterMap<String, String>();

		while (reader.hasNext()) {
			reader.print(1000000);

			String line = reader.next();

			// System.out.println(line);

			// if (reader.getNumLines() > 100) {
			// break;
			// }
			String[] parts = line.split("\t");
			String id = parts[0];
			String auk = parts[1];
			String aue = parts[2];
			String orgk = parts[3];
			String orge = parts[4];
			String aid = parts[9];

			String name = auk;

			cm.incrementCount(aid, auk, 1);

		}
		reader.printLast();
		reader.close();

		TextFileWriter writer = new TextFileWriter(ENTPath.AUTHOR_NAME_FILE);

		List<String> aids = cm.getInnerCountSums().getSortedKeys();

		for (int i = 0; i < aids.size(); i++) {
			String aid = aids.get(i);
			String name = cm.getCounter(aid).argMax();

			if (name.length() == 0) {
				continue;
			}

			writer.write(aid + "\t" + name);

			if (i != aids.size() - 1) {
				writer.write("\n");
			}
		}
		writer.close();

	}

	public void process() throws Exception {
		File dir = new File(ENTPath.DOMESTIC_PAPER_META_DIR);
		File[] files = dir.listFiles();

		// String[] attrs = { "CSK", "CSE", "AUK", "AUE", "EM", "TIK", "TIE", "CN1", "CN2", "SOK", "SOE", "JTK", "JTE" };

		Counter<Organization> orgCounts = new Counter<Organization>();

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			System.out.printf("process [%s]\n", file.getName());

			PaperDataFileIterator iter = new PaperDataFileIterator(file.getPath());
			while (iter.hasNext()) {
				Map<PaperAttr, String> attrValueMap = iter.next();

				Paper paper = PaperCreater.create(attrValueMap);

				if (paper == null) {
					continue;
				}

				Author[] authors = paper.getAuthors();

				for (int j = 0; j < authors.length; j++) {
					Author author = authors[j];
					Organization org = author.getOrganization();
					if (org.getName().getKorean() != null) {
						orgCounts.incrementCount(org, 1);
					}
				}

				// String output = StrUtils.join("\t", values);
				// writer.write(output + "\n");
			}
		}

		TextFileWriter writer3 = new TextFileWriter(ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE);

		List<Organization> keys = orgCounts.getSortedKeys();

		CounterMap<String, Organization> cMap = new CounterMap<String, Organization>();

		for (int i = 0; i < keys.size(); i++) {
			Organization org = keys.get(i);
			double cnt = orgCounts.getCount(org);

			String korOrg = org.getName().getKorean();
			String[] parts = korOrg.split(" ");
			String output = org.getName().toString(true) + "\t" + cnt;

			String engOrg = org.getName().getEnglish();

			if (engOrg != null) {
				String[] toks = engOrg.split(" ");

				for (int j = 0; j < toks.length - 1; j++) {
					String tok = toks[j];
					if (tok.endsWith(".")) {
						cMap.incrementCount(tok, org, 1);
					}
				}
			}

			writer3.write(output + "\n");
		}
	}

	public void process2() throws Exception {

		Map<String, String> nameMap = IOUtils.readMap(ENTPath.AUTHOR_NAME_FILE);

		TextFileReader reader = new TextFileReader(ENTPath.AUTHOR_FILE);
		reader.setPrintNexts(false);

		CounterMap<String, BilingualText> cm = new CounterMap<String, BilingualText>();
		CounterMap<String, String> cm2 = new CounterMap<String, String>();

		while (reader.hasNext()) {
			reader.print(1000000);

			String line = reader.next();

			// if (reader.getNumLines() > 10) {
			// break;
			// }
			String[] parts = line.split("\t");
			String id = parts[0];
			String auk = parts[1];
			String aue = parts[2];
			String orgk = parts[3];
			String orge = parts[4];
			String aid = parts[19];

			String name = auk;

			// if (auk.length() == 0) {
			// name = aue;
			// }

			// System.out.println(line);

			BilingualText org = new BilingualText(orgk, orge);

			cm.incrementCount(aid, org, 1);
			cm2.incrementCount(aid, name, 1);

		}
		reader.printLast();
		reader.close();

		// System.out.println(cm2.toString());

		TextFileWriter writer = new TextFileWriter(ENTPath.AUTHOR_SUBSET_FILE);

		List<String> aids = cm.getInnerCountSums().getSortedKeys();

		for (int i = 0; i < aids.size() && i < 500; i++) {
			String aid = aids.get(i);
			String name = cm2.getCounter(aid).argMax();

			Counter<BilingualText> c = cm.getCounter(aid);
			List<BilingualText> orgNames = c.getSortedKeys();

			writer.write(String.format("Author:\t%s\t%s\n", aid, name));

			for (int j = 0; j < orgNames.size(); j++) {
				BilingualText orgName = orgNames.get(j);
				double cnt = c.getCount(orgName);
				writer.write(String.format("%d\t(%s , %s)\t%d\n", j + 1, orgName.getKorean(), orgName.getEnglish(), (int) cnt));
			}
			writer.write("\n");
		}
		writer.close();

		IOUtils.write(ENTPath.AUTHOR_SUBSET_FILE_2, cm2);
	}

	public void selectSubsetForOrgHistory() throws Exception {
		List<String> lines = IOUtils.readLines(ENTPath.ORG_HISTORY_DIR + "base_orgs.txt", IOUtils.EUC_KR, Integer.MAX_VALUE);
		// TextFileWriter writer = new TextFileWriter(ENTPath.ORG_HISTORY_SUBSET_FILE_1);

		List<Integer> locs = new ArrayList<Integer>();

		for (int i = 1; i < lines.size(); i++) {
			String line = "##" + lines.get(i) + "##";
			String[] parts = line.replace("\t", "##\t").split("\t");

			if (parts.length != 13) {
				System.out.println();
			}

			for (int j = 0; j < parts.length; j++) {
				parts[j] = parts[j].replace("##", "");
			}

			String type = parts[2];
			String name = parts[3];

			boolean isSelected = true;

			if (name.startsWith("(")) {
				isSelected = false;
			}

			if (name.contains("대학") || name.contains("과학기술원") || name.contains("연구")) {

			} else {
				isSelected = false;
			}

			if (type.length() > 0 && type.equals("공기업")) {
				isSelected = true;
			}
			// System.out.println(line);

			if (!isSelected) {
				continue;
			}

			locs.add(i);
		}

		int num_persons = 6;

		int num_orgs_per_person = locs.size() / num_persons;
		int person_id = 0;

		List<String> sublines = new ArrayList<String>();

		for (int i = 0; i < locs.size(); i++) {
			if (i % num_orgs_per_person == 0) {
				person_id++;
			}

			if (person_id > num_persons) {
				person_id = num_persons;
			}

			int loc = locs.get(i);
			String line = person_id + "\t" + lines.get(loc);
			sublines.add(line);
		}

		IOUtils.write(ENTPath.ORG_HISTORY_SUBSET_FILE_1, StrUtils.join("\n", sublines));
	}
}
