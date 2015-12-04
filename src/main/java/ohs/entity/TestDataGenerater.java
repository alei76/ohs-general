package ohs.entity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import ohs.entity.data.struct.BilingualText;
import ohs.entity.data.struct.Organization;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.Counter;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

public class TestDataGenerater {

	public static void generateTestData() {
		String orgFileName = ENTPath.BASE_ORG_NAME_FILE;
		String extOrgFileName = ENTPath.DOMESTIC_PAPER_ORG_NAME_FILE;
		String abbrFileName = ENTPath.COMMON_DEPT_ABBR_DICT_FILE;

		OrganizationIdentificationKernel oik = new OrganizationIdentificationKernel();
		oik.readOrganizations(orgFileName);
		oik.createNormalizer(abbrFileName);
		oik.createSearchers(null);
		// odk.createClassifiers();
		// odk.write(ENTPath.ODK_FILE);

		TextFileWriter writer = new TextFileWriter(ENTPath.ODK_TEST_DATA);

		int num_docs = 0;

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(4);

		{
			System.out.println(ENTPath.ODK_OUTPUT_PAPER_FILE);
			Counter<BilingualText> orgNames = DataReader.readBilingualTextCounter(extOrgFileName);
			int num_orgs = 0;
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();

			for (BilingualText orgName : orgNames.getSortedKeys()) {
				double cnt = orgNames.getCount(orgName);

				if (cnt < 50) {
					break;
				}

				if (++num_orgs % 100 == 0) {
					System.out.printf("\r[%d, %s]", num_orgs, stopWatch.stop());
				}

				Counter<Organization> ret = oik.identify(orgName);

				List<Organization> keys = ret.getSortedKeys();
				int num_candidates = 10;
				StringBuffer sb = new StringBuffer();
				sb.append(++num_docs);
				sb.append("\nPAPER");
				sb.append("\nINPUT:");
				sb.append(String.format("\nKorean\t%s", orgName.getKorean()));
				sb.append(String.format("\nEnglish\t%s", orgName.getEnglish()));
				sb.append("\nOUTPUT:");
				sb.append("\nNo\tID\tKorean\tEnglish\tScore\tSystem\tHuman");

				boolean hasMatch = false;

				for (int i = 0; i < keys.size() && i < num_candidates; i++) {
					Organization org = keys.get(i);
					boolean matched = false;

					if (orgName.getKorean().equals(org.getName().getKorean())) {
						matched = true;
						hasMatch = true;
					}

					double score = ret.getCount(org);
					String output = String.format("\n%d\t%d\t%s\t%s\t%s\t%s\t", i + 1, org.getId(), org.getName().getKorean(),
							org.getName().getEnglish(), nf.format(score), matched ? "1" : "");
					sb.append(output);
				}

				writer.write(sb.toString() + "\n\n");
			}
			System.out.printf("\r[%d, %s]\n", num_orgs, stopWatch.stop());
		}

		{
			System.out.println(ENTPath.PATENT_ORG_FILE_2);
			TextFileReader reader = new TextFileReader(ENTPath.PATENT_ORG_FILE_2);

			reader.setPrintNexts(false);

			while (reader.hasNext()) {
				reader.print(100);

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

				if (engNameCounts.totalCount() < 20) {
					break;
				}

				BilingualText orgName = new BilingualText(korName, engNameCounts.argMax());

				Counter<Organization> ret = oik.identify(orgName);

				List<Organization> keys = ret.getSortedKeys();
				int num_candidates = 10;
				StringBuffer sb = new StringBuffer();
				sb.append(++num_docs);
				sb.append("\nPATENT");
				sb.append("\nINPUT:");
				sb.append(String.format("\nKorean\t%s", orgName.getKorean()));
				sb.append(String.format("\nEnglish\t%s", orgName.getEnglish()));
				sb.append("\nNo\tID\tKorean\tEnglish\tScore\tSystem\tHuman");

				boolean hasMatch = false;

				for (int i = 0; i < keys.size() && i < num_candidates; i++) {
					Organization org = keys.get(i);
					double score = ret.getCount(org);
					boolean matched = false;

					if (orgName.getKorean().equals(org.getName().getKorean())) {
						matched = true;
						hasMatch = true;
					}

					String output = String.format("\n%d\t%d\t%s\t%s\t%s\t%s\t", i + 1, org.getId(), org.getName().getKorean(),
							org.getName().getEnglish(), nf.format(score), matched ? "1" : "");
					sb.append(output);
				}

				writer.write(sb.toString() + "\n\n");
			}
			reader.printLast();
		}

		writer.close();
	}

	public static void main(String[] args) {
		System.out.println("process begins.");
		refineTestData();

		System.out.println("process ends.");

	}

	public static void refineTestData() {
		List<String[]> lines = new ArrayList<String[]>();
		TextFileReader reader = new TextFileReader(ENTPath.ODK_TEST_DATA_LABELDED, IOUtils.EUC_KR);
		TextFileWriter writer = new TextFileWriter(ENTPath.DATA_DIR + "odk_test_data_labeled_compact.txt");

		while (reader.hasNext()) {
			String line = reader.next();
			line = line.replace("\"", "");
			String[] parts = DataReader.split(line);

			int len = 0;
			for (int i = 0; i < parts.length; i++) {
				len += parts[i].length();
			}

			if (len == 0) {
				String type = lines.get(1)[0];
				String korInput = lines.get(3)[1];
				String engInput = lines.get(5)[1];

				String korAns = "";
				String engAns = "";
				String ansId = "";

				for (int i = 7; i < lines.size(); i++) {
					parts = lines.get(i);
					String no = parts[0];
					String id = parts[1];
					String korOutput = parts[2];
					String engOutput = parts[3];

					engOutput = engOutput.replace("\"", "");

					double score = Double.parseDouble(parts[4]);
					double system = 0;
					double human = 0;

					if (parts[5].length() > 0) {
						system = Double.parseDouble(parts[5]);
					}

					if (parts[6].length() > 0) {
						human = Double.parseDouble(parts[6]);
					}

					if (human > 0) {
						korAns = korOutput;
						engAns = engOutput;
						ansId = id;
					}
				}

				String[] outStrs = new String[] { korInput, engInput, ansId, korAns, engAns };

				for (int i = 0; i < outStrs.length; i++) {
					if (outStrs[i].length() == 0) {
						outStrs[i] = "null";
					}
				}

				String output = StrUtils.join("\t", outStrs);
				writer.write(output + "\n");
				lines = new ArrayList<String[]>();
			} else {
				lines.add(parts);
			}

			// System.out.println(line);
		}
		reader.close();
		writer.close();
	}

}
