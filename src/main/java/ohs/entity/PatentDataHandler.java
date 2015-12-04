package ohs.entity;

import java.util.Comparator;

import ohs.entity.data.struct.BilingualText;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.types.CounterMap;

public class PatentDataHandler {

	class MyCom implements Comparator<BilingualText> {
		@Override
		public int compare(BilingualText o1, BilingualText o2) {
			return o1.getKorean().compareTo(o2.getKorean());
		}

	}

	private static boolean isNull(String s) {
		return s.equals("null") ? true : false;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		PatentDataHandler dh = new PatentDataHandler();
		dh.removeDuplications();

		System.out.println("process ends.");
	}

	public void removeDuplications() throws Exception {
		// ListMap<String, String> listMap = new ListMap<String, String>();
		CounterMap<String, String> cm = new CounterMap<String, String>();

		TextFileReader reader = new TextFileReader(ENTPath.PATENT_ORG_FILE);
		while (reader.hasNext()) {
			String line = reader.next();

			if (reader.getNumLines() == 1) {
				continue;
			}
			String[] parts = line.split("\t");
			String korLong = parts[2].trim();
			String korShort = parts[3].trim();
			String engLong = parts[4].trim();
			String engShort = parts[5].trim();
			String title = parts[6];

			// if (engLong.equals("null")) {
			// engLong = "";
			// }

			// listMap.put(korShort, engLong);

			cm.incrementCount(korShort, engLong, 1);
		}
		reader.close();

		IOUtils.write(ENTPath.PATENT_ORG_FILE_2, cm);

		// List<String> korNames = new ArrayList<String>(listMap.keySet());
		// Collections.sort(korNames);

		// TextFileWriter writer = new TextFileWriter(ENTPath.DATA_DIR + "patent_orgs_2.txt");
		//
		// for (int i = 0; i < korNames.size(); i++) {
		// String korName = korNames.get(i);
		// List<String> engNames = listMap.get(korName);
		// Iterator<String> iter = engNames.iterator();
		// while (iter.hasNext()) {
		// String engName = iter.next();
		// if (engName.equals("null")) {
		// iter.remove();
		// }
		// }
		//
		// String output = "null";
		//
		// if (engNames.size() > 0) {
		// output = StrUtils.join(" ; ", engNames);
		// }
		// writer.write(korName + "\t" + output);
		// if (i != korNames.size() - 1) {
		// writer.write("\n");
		// }
		// }
		// writer.close();

	}

}
