package ohs.ir.medical.dump;

import java.io.File;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.mysql.fabric.xmlrpc.base.Struct;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.medical.general.MIRPath;
import ohs.utils.Generics;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

public class ClueWebDumper extends TextDumper {

	public ClueWebDumper(String inputDir, String outputFileName) {
		super(inputDir, outputFileName);
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ClueWebDumper dh = new ClueWebDumper(MIRPath.CLUEWEB12_DIR, MIRPath.CLUEWEB12_COL_FILE);
		// dh.readVisitedDocs(MIRPath.CLEF_EHEALTH_DIR + "doc_ids.txt");
		dh.dump();

		System.out.println("process ends.");
	}

	private TextFileWriter writer;

	@Override
	public void dump() throws Exception {
		writer = new TextFileWriter(outputFileName);
		File[] files = new File(inputDirName).listFiles();

		for (File file : files) {
			if (file.isDirectory() && file.getPath().contains("ClueWeb12_")) {
				dump(file);
			}
		}

		writer.close();
	}

	private void dump(File dir) {
		List<File> files = FileUtils.getFilesUnder(dir.getPath());

		StopWatch stopWatch = StopWatch.newStopWatch();
		for (int k = 0; k < files.size(); k++) {
			File file = files.get(k);

			if (!file.getName().endsWith(".gz")) {
				continue;
			}

			List<String> lines = Generics.newArrayList();

			int cnt = 0;

			TextFileReader reader = new TextFileReader(file);
			while (reader.hasNext()) {
				String line = reader.next();

				if (line.startsWith("WARC/1.0")) {
					if (lines.size() > 0 && cnt > 1) {
						int start = 10;

						for (int i = 0; i < lines.size(); i++) {
							if (lines.get(i).startsWith("<!DOCTYPE")) {
								start = i;
								break;
							}
						}

						String content = StrUtils.join("\n", lines, start, lines.size());
						try {
							Document doc = Jsoup.parse(content);
							String t = doc.text();
							t = t.replace("\n", "\\n");

							String id = lines.get(2);
							id = id.substring(13);

							if (id.length() > 0 && t.length() > 0) {
								String[] parts = new String[] { id, t };
								parts = StrUtils.wrap(parts);

								writer.write(StrUtils.join("\t", parts) + "\n");
							}
						} catch (Exception e) {
							continue;
						}

					}

					lines.clear();
					cnt++;
				} else {
					lines.add(line);
				}
			}
			reader.close();
		}

		System.out.printf("process [%d] files under [%s], %s\n", files.size(), dir.getPath(), stopWatch.stop());
	}

}
