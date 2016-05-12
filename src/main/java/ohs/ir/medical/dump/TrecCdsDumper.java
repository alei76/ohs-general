package ohs.ir.medical.dump;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
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
import ohs.utils.StrUtils;

public class TrecCdsDumper extends TextDumper {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		TrecCdsDumper dh = new TrecCdsDumper(MIRPath.TREC_CDS_COL_DIR, MIRPath.TREC_CDS_COL_FILE);
		dh.readValidDocIDs(MIRPath.TREC_CDS_VALID_DOC_ID_FILE);
		dh.setIntermediateFileName(MIRPath.TREC_CDS_COL_XML_FILE);
		dh.dump();

		// dh.makeSingleFile();
		// dh.extractText();
		System.out.println("process ends.");
	}

	private Set<String> validDocIds;

	private String xmlFileName;

	public TrecCdsDumper(String inputDir, String outputFileName) {
		super(inputDir, outputFileName);
	}

	@Override
	public void dump() throws Exception {
		System.out.printf("dump from [%s]\n", inputDirName);

		makeSingleFile();
		extractText();
	}

	public void extractText() throws Exception {
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

		TextFileReader reader = new TextFileReader(xmlFileName);
		TextFileWriter writer = new TextFileWriter(outputFileName);

		reader.setPrintNexts(false);

		int numValid = 0;
		int num_miss_pmcid = 0;
		int nuim_miss_title = 0;
		int num_miss_abstracts = 0;
		int num_miss_bodies = 0;
		int numDocs = 0;

		while (reader.hasNext()) {
			reader.printProgress();
			String line = reader.next();

			// if (reader.getNumLines() < 37573) {
			// continue;
			// }

			String[] parts = line.split("\t");
			String fileName = parts[0];
			String xmlText = parts[1];

			// System.out.println(line);

			fileName = fileName.split("/")[2];

			String docId = FileUtils.removeExtension(fileName);

			if (validDocIds != null && !validDocIds.contains(docId)) {
				continue;
			}

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

			for (int k = 0; k < nodeList.getLength(); k++) {
				Element idElem = (Element) nodeList.item(k);
				if (idElem.getAttribute("pub-id-type").equals("pmc")) {
					pmcId = idElem.getTextContent().trim();
					break;
				}
			}

			if (pmcId.length() == 0 || !validDocIds.contains(pmcId)) {
				num_miss_pmcid++;
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
				for (int k = 0; k < nodeList.getLength(); k++) {
					Element paraElem = (Element) nodeList.item(k);
					String text = paraElem.getTextContent().trim();
					text = text.replaceAll("[\\text]+", " ").trim();
					sb.append(text + "\n");
				}
				body = sb.toString().trim().replace("\n", "\\n");
			}

			boolean missPmcId = false;
			boolean missTitle = false;
			boolean missAbs = false;
			boolean missBody = false;

			if (pmcId.length() == 0) {
				num_miss_pmcid++;
				missPmcId = true;
				pmcId = "null";
			}

			if (title.length() == 0) {
				nuim_miss_title++;
				missTitle = true;
				title = "null";
			}

			if (abs.length() == 0) {
				num_miss_abstracts++;
				missAbs = true;

				abs = "null";
			}

			if (body.length() == 0) {
				num_miss_bodies++;
				missBody = true;

				body = "null";
			}

			int num_miss = num_miss_pmcid + nuim_miss_title + num_miss_abstracts + num_miss_bodies;

			if (pmcId.length() > 0) {
				String output = StrUtils.join("\t", new String[] { pmcId, title, abs, body });
				writer.write(output + "\n");
				numValid++;
			}
		}
		reader.printProgress();
		reader.close();

		writer.close();

	}

	public void makeSingleFile() throws Exception {

		TextFileWriter writer = new TextFileWriter(xmlFileName);
		File[] files = new File(inputDirName).listFiles();
		int num_docs_in_coll = 0;

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0, kk = 0; i < files.length; i++) {
			File file = files[i];

			if (!file.getName().endsWith(".tar.gz")) {
				continue;
			}

			TarArchiveInputStream tis = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(file)));
			TarArchiveEntry tae = null;

			int num_docs_in_file = 0;
			// read every single entry in TAR file
			while ((tae = tis.getNextTarEntry()) != null) {
				// the following two lines remove the .tar.gz extension for the folder name
				// System.out.println(entry.getName());

				// if (num_docs_in_coll == 40000) {
				// break;
				// }

				if (tae.isDirectory()) {
					continue;
				}

				if (num_docs_in_coll % 5000 == 0) {
					System.out.printf("read [%d] docs so far in time [%s].\n", num_docs_in_coll, stopWatch.stop());
				}

				num_docs_in_file++;
				num_docs_in_coll++;

				String fileName = tae.getName();
				StringBuffer sb = new StringBuffer();

				int c;

				while ((c = tis.read()) != -1) {
					sb.append((char) c);
				}

				if (sb.length() > 0) {
					String outoput = fileName + "\t" + sb.toString().trim().replace("\t", " ").replace("\n", "<NL>");
					writer.write(outoput + "\n");
				}
			}
			tis.close();

			System.out.printf("read [%d] docs from [%s] in time [%s]\n", num_docs_in_file, file.getName(), stopWatch.stop());
		}
		writer.close();

		System.out.printf("read [%d] docs from [%s] in time [%s]\n", num_docs_in_coll, xmlFileName, stopWatch.stop());
	}

	public void readValidDocIDs(String fileName) {
		validDocIds = new TreeSet<String>();
		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			String line = reader.next();
			validDocIds.add(line.trim());
		}
		reader.close();
	}

	public void setIntermediateFileName(String xmlFileName) {
		this.xmlFileName = xmlFileName;
	}
}
