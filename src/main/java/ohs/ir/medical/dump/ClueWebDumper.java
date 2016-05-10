package ohs.ir.medical.dump;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Whitelist;

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

		ClueWebDumper dh = new ClueWebDumper(MIRPath.CLUEWEB_DIR, MIRPath.CLUEWEB12_COL_FILE);
		// dh.readVisitedDocs(MIRPath.CLEF_EHEALTH_DIR + "doc_ids.txt");
		dh.dump();

		System.out.println("process ends.");
	}

	private Set<String> fileNames;

	private TextFileWriter fileNameWriter;

	public ClueWebDumper(String inputDir, String outputFileName) {
		super(inputDir, outputFileName);
	}

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

		fileNameWriter = new TextFileWriter(MIRPath.CLUEWEB12_FILE_NAME_FILE, FileUtils.UTF_8, false);

		for (String fileName : new TreeSet<String>(fileNames)) {
			fileNameWriter.write(fileName + "\n");
		}

		// FileUtils.deleteFilesUnder(inputDirName.replace("2012_disk_b",
		// "2012_disk_b_text"));

		File[] files = new File(inputDirName).listFiles();

		Arrays.sort(files);

		for (File file : files) {
			if (file.isDirectory() && file.getPath().contains("ClueWeb12_")) {
				dump(file);
			}
		}
	}

	private void dump(File dir) throws Exception {
		List<File> files = FileUtils.getFilesUnder(dir.getPath());

		Collections.sort(files);

		Whitelist whitelist = Whitelist.relaxed();

		for (int k = 0; k < files.size(); k++) {
			File file = files.get(k);

			if (!file.getName().endsWith(".gz")) {
				continue;
			}

			fileNameWriter.write(file.getPath() + "\n");

			StopWatch stopWatch = StopWatch.newStopWatch();

			String outFileName1 = String.format("%s/%s",

					file.getParent().replace("2012_disk_b", "2012_disk_b_text"),

					file.getName().replace("warc", "txt"));

			if (FileUtils.exists(outFileName1)) {
				continue;
			}

			String outFileName2 = outFileName1.replace("2012_disk_b_text", "2012_disk_b_id").replace(".gz", "");

			Set<String> stopIds = Generics.newHashSet();

			if (FileUtils.exists(outFileName2)) {
				Set<String> starts = Generics.newHashSet();
				Set<String> ends = Generics.newHashSet();

				TextFileReader reader = new TextFileReader(outFileName2);

				while (reader.hasNext()) {
					String[] parts = reader.next().split("\t");
					if (parts[0].startsWith("START")) {
						starts.add(parts[1]);
					} else {
						ends.add(parts[1]);
					}
				}
				reader.close();

				for (String id : starts) {
					if (!ends.contains(id)) {
						stopIds.add(id);
					}
				}
			}

			TextFileWriter writer = new TextFileWriter(outFileName2);
			TextFileReader reader = new TextFileReader(file);

			List<String> results = Generics.newArrayList();
			List<String> lines = Generics.newArrayList();
			int cnt = 0;

			while (reader.hasNext()) {
				String line = reader.next();

				if (line.startsWith("WARC/1.0")) {
					if (lines.size() > 0 && cnt > 1) {
						int start = -1;
						String id = "";
						String uri = "";

						for (int i = 0; i < lines.size(); i++) {
							if (lines.get(i).startsWith("WARC-TREC-ID")) {
								id = lines.get(i).substring(13).trim();
							} else if (lines.get(i).startsWith("WARC-Target-URI")) {
								uri = lines.get(i).substring(16).trim();
							} else if (lines.get(i).startsWith("Content-Length")) {
								start = i + 2;
								break;
							}
						}

						for (int i = start; i < lines.size(); i++) {
							if (lines.get(i).trim().length() == 0) {
								start = i + 1;
								break;
							}
						}

						writer.write(String.format("START\t%s\n", id));

						if (!stopIds.contains(id)) {
							String text1 = StrUtils.join("\n", lines, start, lines.size()).trim();
							String text2 = Jsoup.clean(text1, whitelist);

							if (Jsoup.isValid(text2, whitelist)) {
								try {
									List<String> strs = Generics.newArrayList();
									List<String> links = Generics.newArrayList();

									Document doc = Jsoup.parse(text2);

									goDown(doc, strs, links, false, 1);

									if (id.length() > 0 && strs.size() > 0) {
										String[] parts = new String[] { id, StrUtils.join("\\n", strs), uri, StrUtils.join("\\t", links) };

										results.add(StrUtils.join("\t", StrUtils.wrap(parts)));
									}
								} catch (Exception e) {
									lines = Generics.newArrayList();
									continue;
								}
								writer.write(String.format("END\t%s\n", id));
							}
						}
					}

					lines = Generics.newArrayList();
					cnt++;
				} else {
					lines.add(line);
				}
			}
			reader.close();
			writer.close();

			FileUtils.writeStrCollection(outFileName1, results);

			results = null;
			lines = null;

			System.out.printf("[%s, %d, %s]\n", file.getPath(), cnt, stopWatch.stop());
		}
	}

	private void goDown(Node node, List<String> strs, List<String> links, boolean is_table_item, int depth) {
		if (depth >= 1000) {
			return;
		}

		List<Node> childNodes = node.childNodes();

		if (node instanceof Element) {
			Element elem = (Element) node;

			String tagName = elem.tagName();
			String type = "e";
			String url = "";

			if (tagName.equals("a")) {
				type = "a";
				url = elem.attr("abs:href");
			} else if (tagName.equals("link")) {
				type = "link";
				url = elem.attr("abs:href");
			} else {
				if (elem.hasAttr("type")) {
					type = elem.attr("type");
				}

				if (elem.hasAttr("abs:src")) {
					url = elem.attr("abs:src");
				}
			}

			if (!type.equals("e")) {
				type = StrUtils.normalizeSpaces(type);
			}

			if (url.length() > 0) {
				url = url.trim().replaceAll("[\n]+", "");
				links.add(type + ":" + url);
			}

			if (tagName.equals("table") || tagName.equals("ul")) {
				is_table_item = true;
			}
		}

		if (node instanceof TextNode) {
			TextNode tn = (TextNode) node;
			String t = tn.text();

			t = t.replaceAll("[\n]+", "\\n");
			t = StrUtils.normalizeSpaces(t);

			if (t.length() > 0) {
				if (is_table_item) {
					t = t.replaceAll("[ ]+", "_");
					t = "tbi:" + t;
				}
				strs.add(t);
			}
		}

		for (int i = 0; i < childNodes.size(); i++) {
			Node child = childNodes.get(i);

			goDown(child, strs, links, is_table_item, depth + 1);

		}
	}

}
