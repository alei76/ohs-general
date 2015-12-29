package ohs.ir.news;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.types.ListMap;
import ohs.types.Pair;
import ohs.types.Triple;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.makeTextDump();
		// dh.doBinning();
		dh.doNLP();
		// dh.changeFormat();
		// dh.collect();
		System.out.println("process ends.");
	}

	public void collect() throws Exception {

		ListMap<String, String> dateMap = readDateIdMap();

		Map<String, String> idDateMap = new HashMap<>();

		for (String date : dateMap.keySet()) {
			for (String id : dateMap.get(date)) {
				idDateMap.put(id, date);
			}
		}

		File[] dirs = new File(NSPath.CONTENT_NLP_CONLL_DIR).listFiles();

		TextFileWriter writer = new TextFileWriter(NSPath.NEWS_NER_FILE);

		for (int i = 0; i < dirs.length; i++) {
			List<File> files = IOUtils.getFilesUnder(dirs[i]);

			for (int j = 0; j < files.size(); j++) {
				File file = files.get(j);
				String id = IOUtils.removeExtension(file.getName());
				String date = idDateMap.get(id);

				TextFileReader reader = new TextFileReader(file);

				while (reader.hasNext()) {
					List<String> lines = reader.getNextLines();

					List<String> words = new ArrayList<>();
					List<String> poss = new ArrayList<>();
					List<String> lemmas = new ArrayList<>();
					List<String> ners = new ArrayList<>();
					List<String> sentis = new ArrayList<>();
					List<String> ssentis = new ArrayList<>();

					for (int k = 1; k < lines.size(); k++) {
						String[] parts = lines.get(k).split("\t");
						words.add(parts[1]);
						lemmas.add(parts[2]);
						poss.add(parts[3]);
						ners.add(parts[4]);
						sentis.add(parts[5]);
						ssentis.add(parts[6]);
					}

					if (ssentis.get(0).equals("Neutral")) {
						continue;
					}

					List<Pair<Integer, Integer>> nerOffets = new ArrayList<>();

					for (int k = 0; k < ners.size();) {
						String startTag = ners.get(k);

						if (!startTag.equals("O")) {
							int start = k;
							int end = k + 1;

							for (int l = k + 1; l < ners.size(); l++) {
								if (!ners.get(l).equals(startTag)) {
									end = l;
									break;
								}
							}

							k = end;

							if (!(startTag.equals("PERSON") || startTag.equals("LOCATION")
									|| startTag.equals("ORGANIZATION"))) {
								continue;
							}
							nerOffets.add(new Pair<Integer, Integer>(start, end));
						} else {
							k++;
						}
					}

					if (nerOffets.size() == 0) {
						continue;
					}

					StringBuffer sb = new StringBuffer();
					sb.append(String.format("%s\t%s\t%s", date, id, ssentis.get(0)));

					for (int k = 0; k < nerOffets.size(); k++) {
						int start = nerOffets.get(k).getFirst();
						int end = nerOffets.get(k).getSecond();
						String nerSeq = StrUtils.join(" ", words, start, end);
						String type = ners.get(start);
						sb.append(String.format("\t%s/%s", nerSeq, type));
					}
					writer.write(sb.toString() + "\n");
				}
				reader.close();
			}
		}
		writer.close();
	}

	private ListMap<String, String> readDateIdMap() throws Exception {
		ListMap<String, String> map = new ListMap<String, String>(true);
		TextFileReader reader = new TextFileReader(NSPath.NEWS_META_FILE);
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}
			String line = reader.next();
			String[] parts = line.split("\t");
			String date = parts[2].substring(0, 10);
			String id = parts[0];
			map.put(date, id);
		}
		return map;
	}

	public void changeFormat() throws Exception {
		// ListMap<String, String> map = new ListMap<String, String>();
		// for (String line : IOUtils.readLines(NSPath.NEWS_META_FILE)) {
		// String[] parts = line.split("\t");
		// String date = parts[2].substring(0, 10);
		// String id = parts[0];
		// map.put(date, id);
		// }
		//
		// List<String> dates = new ArrayList<>(map.keySet());
		// Collections.sort(dates);
		//
		// IOUtils.deleteFilesUnder(NSPath.CONTENT_NLP_CONLL_DIR);

		File[] dirs = new File(NSPath.CONTENT_NLP_DIR).listFiles();

		for (int i = 0; i < dirs.length; i++) {
			List<File> files = IOUtils.getFilesUnder(dirs[i]);

			for (int j = 0; j < files.size(); j++) {
				File file = files.get(j);
				String outputFileName = file.getCanonicalPath().replace("content_nlp", "content_nlp_conll")
						.replace(".xml", ".conll");

				try {
					IOUtils.write(outputFileName, getTextInConllFormat(file));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void doNLP() throws Exception {

		// IOUtils.deleteFilesUnder(NSPath.TEMP_NLP_DIR);

		Set<String> visited = Generics.newHashSet();

		if (IOUtils.exists(NSPath.CONTENT_VISIT_FILE)) {
			visited = IOUtils.readSet(NSPath.CONTENT_VISIT_FILE);
		}

		TextFileWriter writer = new TextFileWriter(NSPath.CONTENT_VISIT_FILE, IOUtils.UTF_8, true);

		Properties prop = new Properties();
		prop.setProperty("annotators", "tokenize, quote, ssplit, pos, lemma, ner,parse, sentiment");
		prop.setProperty("parse.maxlen", "100");
		prop.setProperty("pos.maxlen", "100");
		prop.setProperty("replaceExtension", "true");
		prop.setProperty("outputFormat", "XML");

		StanfordCoreNLP nlp = new StanfordCoreNLP(prop);
		File[] dirFiles = new File(NSPath.CONTENT_DIR, "news").listFiles();

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

	private String getTextInConllFormat(File file) throws Exception {
		InputSource is = new InputSource(new FileReader(file.getCanonicalPath()));
		org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

		String[] labels = { "word", "lemma", "POS", "NER", "sentiment" };

		StringBuffer sb2 = new StringBuffer();

		NodeList snl = document.getElementsByTagName("sentence");
		for (int i = 0; i < snl.getLength(); i++) {
			Node sn = snl.item(i);
			String sentiment = sn.getAttributes().getNamedItem("sentiment").getTextContent();
			NodeList tnl = ((org.w3c.dom.Element) sn).getElementsByTagName("token");

			StringBuffer sb = new StringBuffer();
			sb.append("Loc\tWord\tLemma\tPOS\tNER\tSent\tSSent");

			for (int j = 0; j < tnl.getLength(); j++) {
				Node tn = tnl.item(j);
				List<String> values = new ArrayList<>();
				values.add(Integer.toString(j));
				for (int k = 0; k < labels.length; k++) {
					NodeList n = ((org.w3c.dom.Element) tn).getElementsByTagName(labels[k]);

					if (n != null && n.getLength() > 0) {
						values.add(n.item(0).getTextContent());
					} else {
						values.add("XX");
					}
				}
				values.add(sentiment);
				sb.append("\n" + String.join("\t", values));
			}
			sb2.append(sb.toString() + "\n\n");
		}
		return sb2.toString().trim();
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

	private void write(String mediaType, Map<String, String> map, int num_dirs) throws Exception {
		for (String fileName : map.keySet()) {
			String content = map.get(fileName);
			String outputFileName = NSPath.DATA_DIR
					+ String.format("content/%s/%05d/%s.txt", mediaType, num_dirs, fileName);
			IOUtils.write(outputFileName, content);
		}
		map.clear();
	}

	public void doBinning() throws Exception {
		IOUtils.deleteFilesUnder(NSPath.CONTENT_DIR);

		TextFileReader reader = new TextFileReader(NSPath.NEWS_COL_TEXT_FILE);
		TextFileWriter writer = new TextFileWriter(NSPath.NEWS_META_FILE);

		List<String> labels = new ArrayList<String>();

		// id, source, published, title, media-type, content

		reader.setPrintNexts(false);

		Map<String, String> newsContents = Generics.newHashMap();
		Map<String, String> blogContents = Generics.newHashMap();

		int num_news_dirs = 0;
		int num_blog_dirs = 0;

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
				String mediaType = parts[4];
				String content = parts[parts.length - 1].replace("\\n", "\n").replace("\\t", "\t");

				if (mediaType.equals("News")) {
					newsContents.put(id, content);
				} else {
					blogContents.put(id, content);
				}

				if (newsContents.size() == 100) {
					write("news", newsContents, ++num_news_dirs);
				}

				if (blogContents.size() == 100) {
					write("blog", blogContents, ++num_blog_dirs);
				}
			}
		}

		write("news", newsContents, ++num_news_dirs);
		write("blog", newsContents, ++num_blog_dirs);

		reader.printLast();
		reader.close();
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

}
