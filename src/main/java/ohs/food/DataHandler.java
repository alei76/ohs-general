package ohs.food;

import java.io.File;
import java.util.List;

import ohs.io.FileUtils;
import ohs.types.Counter;
import ohs.utils.Generics;
import ohs.utils.StrUtils;
import ohs.utils.UnicodeUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		filter();

		System.out.println("process ends.");
	}

	public static void filter() throws Exception {
		String dirPath = "G:/data/food data/식품관련학술지_선별_텍스트/";

		List<File> files = FileUtils.getFilesUnder(dirPath);

		FileUtils.deleteFilesUnder(files.get(0).getPath().replace("식품관련학술지_선별_텍스트", "식품관련학술지_선별_텍스트_필터"));

		for (int i = 0; i < files.size(); i++) {
			String inFileName = files.get(i).getPath();
			// if (!files.get(i).getPath().contains("v31n6_781.txt")) {
			// continue;
			// }
			String text = FileUtils.readText(inFileName, "euc-kr");
			// System.out.println(text);

			Counter<Character> c = Generics.newCounter();
			Counter<String> c2 = Generics.newCounter();

			for (String word : StrUtils.split(text)) {
				for (int j = 0; j < word.length(); j++) {
					char ch = word.charAt(j);
					if (UnicodeUtils.isKorean(ch)) {
						c2.incrementCount("KOR", 1);
					} else {
						c2.incrementCount("ELSE", 1);
					}
					c.incrementCount(word.charAt(j), 1);
				}
			}

			// System.out.println("--------------------");
			// System.out.println(inFileName);
			// System.out.println("--------------------");
			// System.out.println(c.toString());
			// System.out.println("--------------------");

			// List<Character> keys = c.getSortedKeys();
			// boolean toFilter = false;
			// for (int j = 0; j < 5 && j < keys.size(); j++) {
			//
			// if (keys.get(j) == '?') {
			// toFilter = true;
			// break;
			// }
			// }

			double kor_cnt = c2.getCount("KOR") + 1;
			double other_cnt = c2.getCount("ELSE") + 1;

			double ratio = kor_cnt / other_cnt;

			if (ratio > 1.5) {
				String outFileName = inFileName.replace("식품관련학술지_선별_텍스트", "식품관련학술지_선별_텍스트_필터");
				FileUtils.write(outFileName, text);
			}
		}
	}

}
