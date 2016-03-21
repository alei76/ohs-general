package ohs.ir.medical.trec.cds_2014;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.medical.general.MIRPath;
import ohs.utils.StopWatch;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		dh.makeRawTextDump();
		System.out.println("process ends.");
	}

	public static Set<String> readDuplications() {
		Set<String> ret = new TreeSet<String>();

		{
			TextFileReader reader = new TextFileReader(new File(CDSPath.DUPLICATION_1_FILE));
			while (reader.hasNext()) {
				String line = reader.next();
				ret.add(line.trim());
			}
			reader.close();
		}

		{
			TextFileReader reader = new TextFileReader(new File(CDSPath.DUPLICATION_2_FILE));
			while (reader.hasNext()) {
				String line = reader.next();
				String[] parts = line.split(" ");
				ret.add(parts[1]);
				ret.add(parts[2]);
			}
			reader.close();
		}

		return ret;
	}

	public void makeRawTextDump() throws Exception {
		String[] fileNames = { "pmc-text-00.tar.gz", "pmc-text-01.tar.gz", "pmc-text-02.tar.gz", "pmc-text-03.tar.gz" };
		TextFileWriter writer = new TextFileWriter(MIRPath.TREC_CDS_COL_FILE);
		int num_files = 0;

		for (int i = 0; i < fileNames.length; i++) {
			String tarFileName = MIRPath.TREC_CDS_COL_DIR + fileNames[i];
			File tarFile = new File(tarFileName);
			TarArchiveInputStream is = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tarFile)));
			TarArchiveEntry entry = null;
			// read every single entry in TAR file
			while ((entry = is.getNextTarEntry()) != null) {
				// the following two lines remove the .tar.gz extension for the folder name
				// System.out.println(entry.getName());

				if (entry.isFile()) {
					num_files++;

					if (num_files % 10000 == 0) {
						System.out.println(num_files);
					}

					String fileName = entry.getName();
					StringBuffer sb = new StringBuffer();

					int c;

					while ((c = is.read()) != -1) {
						sb.append((char) c);
					}

					if (sb.length() > 0) {
						writer.write(fileName + "\t" + sb.toString() + "\n");
					}
				}
			}
			is.close();
		}
		writer.close();
		System.out.println(num_files);

	}

	public void makeTextDump() throws Exception {

		// File inputDirName = new File(CDSPath.RAW_COLLECTION_DIR, "pmc-text-00");
		// File outputFile = new File(CDSPath.DATA_DIR, "sample.txt");

		Set<String> duplicationSet = readDuplications();

		List<File> docFiles = FileUtils.getFilesUnder(CDSPath.RAW_COLLECTION_DIR);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);

		DocumentBuilder parser = dbf.newDocumentBuilder();

		parser.setEntityResolver(new EntityResolver() {

			@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				if (systemId.contains("")) {
					return new InputSource(new StringReader(""));
				}
				return null;
			}
		});

		TextFileWriter writer = new TextFileWriter(CDSPath.TEXT_COLLECTION_FILE);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int numValid = 0;
		int numMissPmcId = 0;
		int numMissTitle = 0;
		int numMissAbs = 0;
		int numMissBody = 0;
		int numDuplications = 0;

		TextFileWriter logWriter = new TextFileWriter(CDSPath.LOG_FILE);

		for (int i = 0; i < docFiles.size(); i++) {
			File docFile = docFiles.get(i);

			String filePath = docFile.getPath();

			int idx = filePath.indexOf("docs");

			filePath = filePath.substring(idx + 5);
			filePath = filePath.replace("\\", "/");

			if (duplicationSet.contains(filePath)) {
				numDuplications++;
				System.out.printf("%d Duplications\t[%text]\n", numDuplications, filePath);
				continue;
			}

			String xmlText = FileUtils.readText(docFile.getPath());
			// xmlText = xmlText.replace("archivearticle.dtd",
			// "F:/data/trec/cds/JATS-archivearticle1.dtd");

			Document xmlDoc = null;

			try {
				xmlDoc = parser.parse(new InputSource(new StringReader(xmlText)));
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			// org.w3c.dom.Document xmlDoc = parser.parse(docFile);

			String pmcId = "";
			String title = "";
			String abs = "";
			String body = "";

			NodeList nodeList = xmlDoc.getElementsByTagName("article-id");

			for (int j = 0; j < nodeList.getLength(); j++) {
				Element idElem = (Element) nodeList.item(j);
				if (idElem.getAttribute("pub-id-type").equals("pmc")) {
					pmcId = idElem.getTextContent().trim();
					break;
				}
			}

			if (pmcId.length() == 0) {
				numMissPmcId++;
				continue;
			}

			Element titleElem = (Element) xmlDoc.getElementsByTagName("article-title").item(0);
			Element absElem = (Element) xmlDoc.getElementsByTagName("abstract").item(0);
			Element bodyElem = (Element) xmlDoc.getElementsByTagName("body").item(0);

			if (titleElem != null) {
				title = titleElem.getTextContent().trim();
			}

			if (absElem != null) {
				abs = absElem.getTextContent().trim();
			}

			if (bodyElem != null) {
				StringBuffer sb = new StringBuffer();
				nodeList = bodyElem.getElementsByTagName("p");
				for (int j = 0; j < nodeList.getLength(); j++) {
					Element paraElem = (Element) nodeList.item(j);
					String text = paraElem.getTextContent().trim();
					text = text.replaceAll("[\\text]+", " ").trim();
					sb.append(text + "\n");
				}
				body = sb.toString().trim();
			}

			boolean missPmcId = false;
			boolean missTitle = false;
			boolean missAbs = false;
			boolean missBody = false;

			if (pmcId.length() == 0) {
				numMissPmcId++;
				missPmcId = true;
			}

			if (title.length() == 0) {
				numMissTitle++;
				missTitle = true;
			}

			if (abs.length() == 0) {
				numMissAbs++;
				missAbs = true;
			}

			if (body.length() == 0) {
				numMissBody++;
				missBody = true;
			}

			int numTotalMiss = numMissPmcId + numMissTitle + numMissAbs + numMissBody;

			StringBuffer sb = new StringBuffer();

			if (!missTitle) {
				sb.append(title + "\n");
			}

			if (!missAbs) {
				sb.append(abs + "\n");
			}

			if (!missBody) {
				sb.append(body);
			}

			String content = sb.toString().trim();

			if (content.length() > 0) {
				content = content.replaceAll("\n", "<NL>");
				String output = pmcId + "\t" + content;
				writer.write(output + "\n");
				numValid++;
			}

			if ((i + 1) % 1000 == 0) {
				System.out.printf("\r[%d/%d/%d, %text][%d + %d + %d + %d = %d]",

						numValid, i + 1, docFiles.size(), stopWatch.stop(), numMissPmcId, numMissTitle, numMissAbs, numMissBody,
						numTotalMiss);
			}

			if (bodyElem != null && body.length() == 0) {
				// logWriter.write(xmlText + "\n\n");
			}
		}
		writer.close();
		logWriter.close();

		System.out.printf("\r[%d/%d/%d, %text]\n", numValid, docFiles.size(), docFiles.size(), stopWatch.stop());
	}
}
