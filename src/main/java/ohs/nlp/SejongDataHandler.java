package ohs.nlp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.utils.Generics;

public class SejongDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		SejongDataHandler sdh = new SejongDataHandler();
		sdh.extractPosData();

		System.out.println("process ends.");
	}

	public void extractPosData() throws Exception {

		String inputFileName = NLPPath.POS_RAW_DATA_FILE;

		String outputFileName = NLPPath.POS_DATA_FILE;

		TextFileWriter writer = new TextFileWriter(outputFileName);

		ZipInputStream zio = new ZipInputStream(new FileInputStream(inputFileName));
		BufferedReader br = new BufferedReader(new InputStreamReader(zio, "utf-16"));
		ZipEntry ze = null;

		while ((ze = zio.getNextEntry()) != null) {
			if (ze.isDirectory()) {
				continue;
			}

			String name = FileUtils.removeExtension(ze.getName());
			String startTag = name + "-";

			StringBuffer sb = new StringBuffer();
			String line = "";

			List<String> lines = Generics.newArrayList();

			while ((line = br.readLine()) != null) {

				if (line.startsWith(startTag)) {
					lines.add(line);
				} else {
					if (lines.size() > 0) {
						sb.append(String.join("\n", lines));
						sb.append("\n\n");

						lines.clear();
						lines = Generics.newArrayList();
					}
				}
			}

			writer.write(sb.toString().trim() + "\n\n");
		}
		br.close();
		writer.close();

		// writer.close();

	}

}
