package ohs.ir.news;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.makeTextDump();
		// dh.partition();
		dh.doNLP();
		// dh.doNLP2();
		System.out.println("process ends.");
	}

	public void partition() throws Exception {
		IOUtils.deleteFilesUnder(NSPath.CONTENT_DIR);

		TextFileReader reader = new TextFileReader(NSPath.NEWS_COL_TEXT_FILE);
		TextFileWriter writer = new TextFileWriter(NSPath.NEWS_META_FILE);

		List<String> labels = new ArrayList<String>();

		// id, source, published, title, media-type, content

		reader.setPrintNexts(false);

		Map<String, String> map = Generics.newHashMap();

		int num_dirs = 0;

		while (reader.hasNext()) {
			reader.print(10000);
			String line = reader.next();
			String[] parts = line.split("\t");

			if (reader.getNumLines() == 1) {
				labels.addAll(Arrays.asList(parts));
				writer.write(StrUtils.join("\t", labels, 0, labels.size() - 1) + "\n");
			} else {
				writer.write(StrUtils.join("\t", parts, 0, labels.size() - 1) + "\n");
				String date = parts[2].substring(0, 10);
				String id = parts[0];
				String fileName = String.format("%s/%s.txt", date, parts[0]);
				String content = parts[parts.length - 1].replace("\\n", "\n").replace("\\t", "\t");

				map.put(id, content);

				if (map.size() == 100) {
					write(map, ++num_dirs);
				}

			}
		}

		write(map, ++num_dirs);

		reader.printLast();
		reader.close();
	}

	private void write(Map<String, String> map, int num_dirs) throws Exception {
		for (String id : map.keySet()) {
			String content = map.get(id);
			IOUtils.write(NSPath.CONTENT_DIR + "/" + String.format("%05d/%s.txt", num_dirs, id), content);
		}
		map.clear();
	}

	// public void doNLP() throws IOException {
	// StanfordCoreNLP nlp = new StanfordCoreNLP(getProps());
	//
	// TextFileReader reader = new TextFileReader(NSPath.NEWS_COL_TEXT_FILE);
	// TextFileWriter writer = new TextFileWriter(NSPath.NEWS_COL_NLP_FILE);
	//
	// List<String> labels = new ArrayList<String>();
	//
	// reader.setPrintNexts(false);
	//
	// while (reader.hasNext()) {
	// reader.print(10);
	// String line = reader.next();
	// String[] parts = line.split("\t");
	// List<String> values = new ArrayList<String>();
	//
	// if (reader.getNumLines() > 100) {
	// break;
	// }
	//
	// if (reader.getNumLines() == 1) {
	// for (String label : parts) {
	// labels.add(label);
	// }
	//
	// labels.add("nlp");
	// writer.write(StrUtils.join("\t", labels) + "\n");
	// } else {
	// String content = parts[parts.length - 1].replace("\\n", "\n");
	//
	// Annotation anno = nlp.process(content);
	//
	// ByteArrayOutputStream os = new ByteArrayOutputStream();
	// nlp.xmlPrint(anno, os);
	//
	// // content = os.toString().replace("\r", "\\r").replace("\n",
	// // "\\n");
	//
	// // os = new ByteArrayOutputStream();
	// //
	// // nlp.prettyPrint(anno, os);
	//
	// content = os.toString().replace("\n", "\\n");
	// // content = os.toString();
	// //
	// // String content2 = os.toString();
	// //
	// // try {
	// // IOUtils.write(NSPath.DATA_DIR + "doc_2.txt", content2);
	// // } catch (Exception e) {
	// // // TODO Auto-generated catch block
	// // e.printStackTrace();
	// // }
	//
	// for (String value : parts) {
	// values.add(value);
	// }
	// values.add(content);
	// }
	//
	// if (labels.size() == values.size()) {
	// writer.write(StrUtils.join("\t", values) + "\n");
	// }
	//
	// }
	// reader.printLast();
	// reader.close();
	// writer.close();
	// }

	public void doNLP() throws Exception {

		// IOUtils.deleteFilesUnder(NSPath.TEMP_NLP_DIR);

		Set<String> visited = Generics.newHashSet();

		if (IOUtils.exists(NSPath.CONTENT_VISIT_FILE)) {
			visited = IOUtils.readSet(NSPath.CONTENT_VISIT_FILE);
		}

		TextFileWriter writer = new TextFileWriter(NSPath.CONTENT_VISIT_FILE);

		Properties prop = new Properties();
		prop.setProperty("annotators", "tokenize, quote, ssplit, pos, lemma, ner,parse, sentiment");
		prop.setProperty("parse.maxlen", "100");
		prop.setProperty("pos.maxlen", "100");
		prop.setProperty("replaceExtension", "true");
		prop.setProperty("outputFormat", "XML");

		StanfordCoreNLP nlp = new StanfordCoreNLP(prop);
		File[] dirFiles = new File(NSPath.CONTENT_DIR).listFiles();

		Arrays.sort(dirFiles);

		for (int i = 0; i < dirFiles.length; i++) {
			File dir = dirFiles[i];
			if (dir.isFile() || visited.contains(dir.getPath())) {
				continue;
			}

			writer.write(dir.getPath() + "\n");

			String outputDir = dirFiles[i].getPath().replace("content", "content_nlp");
			nlp.getProperties().setProperty("outputDirectory", outputDir);
			try {
				nlp.processFiles(IOUtils.getFilesUnder(dir), 100);
			} catch (Exception e) {

			}
		}

	}

	// public void doNLP2() throws Exception {
	//
	// Set<String> docIds = Generics.newHashSet();
	// if (new File(NSPath.NEWS_META_FILE).exists()) {
	// docIds = IOUtils.readSet(NSPath.TEMP_ID_FILE);
	// }
	//
	// TextFileWriter writer = new TextFileWriter(NSPath.TEMP_ID_FILE,
	// IOUtils.UTF_8, true);
	//
	// Properties prop = new Properties();
	// prop.setProperty("annotators", "tokenize, quote, ssplit, pos, lemma,
	// ner,parse, sentiment");
	// prop.setProperty("parse.maxlen", "100");
	// prop.setProperty("pos.maxlen", "100");
	// prop.setProperty("replaceExtension", "true");
	// prop.setProperty("outputFormat", "XML");
	//
	// StanfordCoreNLP nlp = new StanfordCoreNLP(prop);
	// File[] dirFiles = new File(NSPath.CONTENT_DIR).listFiles();
	//
	// Arrays.sort(dirFiles);
	//
	// for (int i = 0; i < dirFiles.length; i++) {
	// if (dirFiles[i].isFile()) {
	// continue;
	// }
	//
	// List<File> files = IOUtils.getFilesUnder(dirFiles[i]);
	//
	// for (File file : files) {
	// if (docIds.contains(IOUtils.removeExtension(file.getName()))) {
	// continue;
	// }
	// writer.write(IOUtils.removeExtension(file.getName()) + "\n");
	//
	// String content = IOUtils.readText(file.getPath());
	// Annotation anno = null;
	//
	// try {
	// anno = nlp.process(content);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// ByteArrayOutputStream os = new ByteArrayOutputStream();
	// nlp.xmlPrint(anno, os);
	//
	// content = os.toString();
	//
	// String outputFile = file.getCanonicalPath().replace("temp",
	// "temp_nlp").replace(".txt", ".xml");
	// IOUtils.write(outputFile, content);
	//
	// }
	//
	// }
	//
	// }

	public void makeTextDump() throws Exception {

		TextFileReader reader = new TextFileReader(NSPath.NEWS_COL_JSON_FILE);
		TextFileWriter writer = new TextFileWriter(NSPath.NEWS_COL_TEXT_FILE);
		List<String> labels = new ArrayList<String>();

		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(10000);
			String line = reader.next();

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(line);

			if (reader.getNumLines() == 1) {
				for (Object key : jsonObject.keySet()) {
					labels.add(key.toString());
				}

				writer.write(StrUtils.join("\t", labels) + "\n");
			}

			List<String> values = new ArrayList<>();

			for (String key : labels) {
				String value = jsonObject.get(key).toString();
				value = value.replace("\r", "").replace("\n", "\\n").replace("\t", "\\t").trim();
				values.add(value);
			}

			if (values.size() == labels.size()) {
				writer.write(StrUtils.join("\t", values) + "\n");
			}
		}
		reader.printLast();
		reader.close();
		writer.close();
	}

}
