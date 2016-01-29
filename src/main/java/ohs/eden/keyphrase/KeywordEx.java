package ohs.eden.keyphrase;

import java.io.File;
import java.util.List;
import java.util.Set;

import ohs.io.FileUtils;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class KeywordEx {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		KeywordEx kw = new KeywordEx();
		kw.tagNews();

		System.out.println("process ends.");
	}

	public void tagNews() throws Exception {
		String baseDir = KPPath.KEYPHRASE_DIR + "keyword-ex/news/";

		Set<String> keywordSet = Generics.newHashSet();

		for (String line : FileUtils.readLines(baseDir + "4_vector_file/2015.08.txt")) {
			line = line.substring(1, line.length() - 1);
			String[] parts = line.split(",");

			for (int i = 0; i < parts.length && i < 5; i++) {
				String[] two = StrUtils.split2Two(":", parts[i]);
				keywordSet.add(two[0]);
			}
		}

		for (File file : FileUtils.getFilesUnder(baseDir + "2_pos_files/")) {
			String text = FileUtils.readText(file);
			text = text.replace("./SF", "./SF\n");

			String[] lines = text.split("\n");

			StringBuffer sb = new StringBuffer();

			int num_keywords = 0;

			for (int i = 0; i < lines.length; i++) {
				String[] toks = lines[i].split(" ");

				List<String> words = Generics.newArrayList();

				for (int j = 0; j < toks.length; j++) {
					String[] segs = toks[j].split("/");
					String word = segs[0];

					if (keywordSet.contains(word)) {
						words.add(String.format("<KW>%s</KW>", word));
						num_keywords++;
					} else {
						words.add(word);
					}
				}

				sb.append(StrUtils.join(" ", words) + "\n");
			}

			if (num_keywords > 5) {
				String outFileName = file.getPath().replace("2_pos_files", "5_tagged_files");
				FileUtils.write(outFileName, sb.toString().trim());
			}
		}

	}

}
