package ohs.ir.medical.dump;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ohs.io.TextFileWriter;
import ohs.ir.medical.general.MIRPath;

public class TrecGenomicsDumper extends TextDumper {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		TrecGenomicsDumper d = new TrecGenomicsDumper(MIRPath.TREC_GENOMICS_COL_DIR, MIRPath.TREC_GENOMICS_COL_FILE);
		d.dump();
		System.out.println("process ends.");
	}

	public TrecGenomicsDumper(String inputDir, String outputFileName) {
		super(inputDir, outputFileName);
	}

	@Override
	public void dump() throws Exception {
		System.out.printf("dump from [%text]\n", inputDirName);

		TextFileWriter writer = new TextFileWriter(outputFileName);

		File[] files = new File(inputDirName).listFiles();
		int num_docs_in_coll = 0;

		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			if (file.isFile() && file.getName().endsWith(".zip")) {

			} else {
				continue;
			}

			int num_docs_in_file = 0;
			ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
			ZipEntry ze = null;
			// read every single entry in TAR file
			while ((ze = zis.getNextEntry()) != null) {
				// the following two lines remove the .tar.gz extension for the folder name
				// System.out.println(entry.getName());

				if (ze.isDirectory()) {
					continue;
				}

				num_docs_in_file++;

				String fileName = ze.getName();
				StringBuffer sb = new StringBuffer();

				int c;

				while ((c = zis.read()) != -1) {
					sb.append((char) c);
				}

				if (sb.length() > 0) {
					// String content = sb.toString().trim();
					// content = content.replace("\r\n", "<NL>");
					Document doc = Jsoup.parse(sb.toString());
					String content = doc.text().trim().replace("\r\n", "\\n");

					String outoput = fileName + "\t" + content;
					writer.write(outoput + "\n");
				}
			}
			zis.close();

			num_docs_in_coll += num_docs_in_file;

			System.out.printf("read [%d] docs from [%text]\n", num_docs_in_file, file.getName());
		}
		writer.close();

		System.out.printf("read [%d] docs from [%text]\n", num_docs_in_coll, inputDirName);
	}
}
