package ohs.ir.medical.dump;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.mysql.fabric.xmlrpc.base.Struct;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.medical.general.MIRPath;
import ohs.utils.Generics;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

public class ClueWebDumper extends TextDumper {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ClueWebDumper dh = new ClueWebDumper(MIRPath.CLUEWEB12_DIR, MIRPath.CLUEWEB12_COL_FILE);
		// dh.readVisitedDocs(MIRPath.CLEF_EHEALTH_DIR + "doc_ids.txt");
		dh.dump();

		System.out.println("process ends.");
	}

	public ClueWebDumper(String inputDir, String outputFileName) {
		super(inputDir, outputFileName);
	}

	private Set<String> fileNames;

	private TextFileWriter fileNameWriter;

	@Override
	public void dump() throws Exception {

		fileNames = Generics.newHashSet();

		if (FileUtils.exists(MIRPath.CLUEWEB12_FILE_NAME_FILE)) {
			List<String> lines = FileUtils.readLines(MIRPath.CLUEWEB12_FILE_NAME_FILE);
			fileNames = Generics.newHashSet(lines.size());

			for (String line : lines) {
				fileNames.add(line);
			}
		}

		fileNameWriter = new TextFileWriter(MIRPath.CLUEWEB12_FILE_NAME_FILE, FileUtils.UTF_8, true);

		FileUtils.deleteFilesUnder(inputDirName.replace("2012_disk_b", "2012_disk_b_text"));

		File[] files = new File(inputDirName).listFiles();

		Arrays.sort(files);

		for (File file : files) {
			if (file.isDirectory() && file.getPath().contains("ClueWeb12_")) {
				dump(file);
			}
		}
	}

	private void dump(File dir) {
		List<File> files = FileUtils.getFilesUnder(dir.getPath());

		Collections.sort(files);

		for (int k = 0; k < files.size(); k++) {
			File file = files.get(k);

			if (!file.getName().endsWith(".gz")) {
				continue;
			}

			// if (fileNames.contains(file.getPath())) {
			// continue;
			// }

			fileNameWriter.write(file.getPath() + "\n");

			StopWatch stopWatch = StopWatch.newStopWatch();

			String outputFileName = String.format("%s/%s", file.getParent().replace("2012_disk_b", "2012_disk_b_text"),
					file.getName().replace("warc.gz", "txt"));

			TextFileWriter writer = new TextFileWriter(outputFileName);
			TextFileReader reader = new TextFileReader(file);

			List<String> lines = Generics.newArrayList();
			int cnt = 0;

			while (reader.hasNext()) {
				String line = reader.next();

				if (line.startsWith("WARC/1.0")) {
					if (lines.size() > 0 && cnt > 1) {
						int start = 10;

						for (int i = 10; i < lines.size(); i++) {
							if (lines.get(i).length() == 0) {
								start = i + 1;
								break;
							}
						}

						String id = lines.get(2).substring(13).trim();

						String content = StrUtils.join("\n", lines, start, lines.size());

						try {
							Document doc = Jsoup.parse(content);

							List<String> strs = Generics.newArrayList();
							List<String> links = Generics.newArrayList();

							goDown(doc, strs, links);

							String text = StrUtils.join("\\n", strs);

							if (id.length() > 0 && text.length() > 0) {
								String[] parts = new String[] { id, text, StrUtils.join("\\t", links) };

								writer.write(StrUtils.join("\t", StrUtils.wrap(parts)) + "\n");
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
			writer.close();

			System.out.printf("[%s, %d, %s]\n", file.getPath(), cnt, stopWatch.stop());
		}
	}

	private void goDown(Node node, List<String> strs, List<String> links) {

		List<Node> childNodes = node.childNodes();

		if (node instanceof Element) {
			Element elem = (Element) node;

			String tagName = elem.tagName();
			String type = "e";
			String url = "";

			if (tagName.equals("a")) {
				type = "a";
				url = elem.attr("href");
			} else if (tagName.equals("link")) {
				type = "link";
				url = elem.attr("href");
			} else {
				type = elem.attr("type");
				url = elem.attr("src");
			}

			type = type.trim();
			url = url.trim();

			if (url.length() > 0) {
				links.add(String.format("%s#%s", type, url));
			}

		} else {
			// System.out.println(node);
		}

		if (node instanceof TextNode) {
			TextNode tn = (TextNode) node;
			String t = tn.text().trim();

			if (t.length() > 0) {
				strs.add(t);
			}
		}

		for (int i = 0; i < childNodes.size(); i++) {
			Node child = childNodes.get(i);

			goDown(child, strs, links);

		}
	}

}
