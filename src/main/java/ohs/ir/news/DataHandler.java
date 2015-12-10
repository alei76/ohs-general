package ohs.ir.news;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.utils.StrUtils;

public class DataHandler {
	private static Properties getProps() {
		Properties ret = new Properties();
		ret.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner,parse, sentiment");
		ret.setProperty("parse.maxlen", "100");
		// ret.setProperty("outputDirectory",
		// "/data2/ohs/data/news_ir/temp_nlp");
		// ret.setProperty("annotators", "tokenize, ssplit, pos");
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.makeTextDump();
		// dh.partition();
		dh.doNLP();
		System.out.println("process ends.");
	}

	public void partition() throws Exception {
		IOUtils.deleteFilesUnder(NSPath.TEMP_DIR);

		TextFileReader reader = new TextFileReader(NSPath.NEWS_COL_TEXT_FILE);

		List<String> labels = new ArrayList<String>();

		// id, source, published, title, media-type, content

		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(10000);
			String line = reader.next();
			String[] parts = line.split("\t");

			if (reader.getNumLines() == 1) {
				labels.addAll(Arrays.asList(parts));
			} else {
				String date = parts[2].substring(0, 10);
				String fileName = String.format("%s/%s.txt", date, parts[0]);
				// String fileName = StrUtils.join("=", parts, 0, parts.length -
				// 2);
				// String filePath = String.format("%s/%s.txt", date, fileName);
				String content = parts[parts.length - 1].replace("\\n", "\n").replace("\\t", "\t");
				IOUtils.write(NSPath.TEMP_DIR + "/" + fileName, content);
			}

		}
		reader.printLast();
		reader.close();
	}

	public void countWords() throws IOException {
		TextFileReader reader = new TextFileReader(NSPath.NEWS_COL_TEXT_FILE);
		TextFileWriter writer = new TextFileWriter(NSPath.NEWS_WORD_COUNT_TEXT_FILE);
		List<String> labels = new ArrayList<>();

		reader.setPrintNexts(false);

		while (reader.hasNext()) {
			reader.print(100000);
			String line = reader.next();
			if (reader.getNumLines() == 1) {
				labels.addAll(Arrays.asList(line.split("\t")));
				labels.add("word-cnts");
				writer.write(StrUtils.join("\t", labels) + "\n");
			} else {
				String[] values = line.split("\t");
				String content = values[0];
				// Counter<String> c = getCounter(content);
				// String s = c.toString(c.size());
				// String output = line + "\t" + s.substring(1, s.length() - 1);
				// if (labels.size() == output.split("\t").length) {
				// writer.write(output + "\n");
				// }
			}

			if (line.split("\t").length != 6) {
				System.out.println(line);
			}
		}
		reader.printLast();
		reader.close();
		writer.close();
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

	public void doNLP() throws IOException {

		IOUtils.deleteFilesUnder(NSPath.TEMP_NLP_DIR);

		StanfordCoreNLP nlp = new StanfordCoreNLP(getProps());
		File[] dirFiles = new File(NSPath.TEMP_DIR).listFiles();

		for (int i = 0; i < dirFiles.length; i++) {
			if (dirFiles[i].isFile()) {
				continue;
			}

			List<File> files = IOUtils.getFilesUnder(dirFiles[i]);

			String outputDir = dirFiles[i].getPath();
			outputDir = outputDir.replace("temp", "temp_nlp");

			Properties prop = nlp.getProperties();
			prop.setProperty("outputDirectory", outputDir);
			prop.setProperty("replaceExtension", "true");
			nlp.processFiles(files, 100);
		}

	}

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
