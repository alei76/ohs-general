package ohs.ir.medical.dump;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.medical.general.MIRPath;
import ohs.utils.StrUtils;

public class ClefEHealthDumper extends TextDumper {

	public static Set<String> getStopFileExtensions() {
		Set<String> ret = new HashSet<String>();
		ret.add("doc");
		ret.add("docx");
		ret.add("pdf");
		ret.add("swf");
		ret.add("ppt");
		ret.add("pptx");
		ret.add("png");
		ret.add("flv");
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		ClefEHealthDumper dh = new ClefEHealthDumper(MIRPath.CLEF_EHEALTH_COL_DIR, MIRPath.CLEF_EHEALTH_COL_FILE);
		// dh.readVisitedDocs(MIRPath.CLEF_EHEALTH_DIR + "doc_ids.txt");
		dh.dump();

		System.out.println("process ends.");
	}

	private Set<String> docIdSet;

	public ClefEHealthDumper(String inputDir, String outputFileName) {
		super(inputDir, outputFileName);
	}

	@Override
	public void dump() throws Exception {
		System.out.printf("dump from [%s]\n", inputDirName);

		TextFileWriter writer = new TextFileWriter(outputFileName);

		int num_docs_in_coll = 0;

		Set<String> stopExpSet = getStopFileExtensions();

		File[] files = new File(inputDirName).listFiles();

		for (int i = 0, num_files = 0; i < files.length; i++) {
			File file = files[i];

			if (file.isDirectory()) {
				continue;
			}

			ZipInputStream zio = new ZipInputStream(new FileInputStream(file));
			BufferedReader br = new BufferedReader(new InputStreamReader(zio));
			ZipEntry ze = null;

			while ((ze = zio.getNextEntry()) != null) {
				if (ze.isDirectory()) {
					continue;
				}

				List<String> lines = new ArrayList<String>();
				int num_docs_in_file = 0;
				String line = null;

				while ((line = br.readLine()) != null) {
					if (line.equals("")) {
						continue;
					}

					lines.add(line);

					if (line.startsWith("#EOR")) {
						String uid = lines.get(0);
						String date = lines.get(1);
						String url = lines.get(2);
						String html = StrUtils.join("\n", lines, 4, lines.size() - 1);

						if (!uid.startsWith("#UID") || !date.startsWith("#DATE") || !url.startsWith("#URL")
								|| !lines.get(3).startsWith("#CONTENT")) {

							lines = new ArrayList<String>();
							continue;
						}

						uid = uid.substring(5);
						date = date.substring(6);
						url = url.substring(5);

						if (docIdSet != null && docIdSet.contains(uid)) {
							lines = new ArrayList<String>();
							continue;
						}

						Pattern p = Pattern.compile("\\.([a-z]+)$");
						Matcher m = p.matcher(url);

						if (m.find()) {
							String exp = m.group(1).toLowerCase();

							if (stopExpSet.contains(exp)) {
								lines = new ArrayList<String>();
								continue;
							}
						}

						Document doc = Jsoup.parse(html);
						String content = doc.text();
						String output = String.format("%s\t%s\t%s\t%s", uid, date, url, content.replaceAll("\n", "<NL>"));

						writer.write(output + "\n");

						lines = new ArrayList<String>();
						num_docs_in_file++;
						num_docs_in_coll++;
					}
				}

				System.out.printf("read [%d] docs from [%s]\n", num_docs_in_file, ze.getName());
			}

			br.close();
		}

		writer.close();

		System.out.printf("read [%d] docs from [%s]\n", num_docs_in_coll, inputDirName);
	}

	public void readVisitedDocs(String fileName) {
		docIdSet = new HashSet<String>();
		File file = new File(fileName);
		if (file.exists()) {
			TextFileReader reader = new TextFileReader(file);
			while (reader.hasNext()) {
				docIdSet.add(reader.next());
			}
			reader.close();
		}
	}

}
