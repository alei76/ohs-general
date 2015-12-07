package ohs.ir.news;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.medical.general.NLPUtils;
import ohs.types.Counter;
import ohs.utils.StrUtils;

public class DataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		DataHandler dh = new DataHandler();
		// dh.tokenize();
		dh.countWords();
		System.out.println("process ends.");
	}

	private Counter<String> getCounter(String s) {
		Counter<String> ret = new Counter<String>();
		for (String sent : s.split("\\\\n")) {
			for (String word : sent.split(" ")) {
				ret.incrementCount(word.toLowerCase(), 1);
			}
		}
		return ret;
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

	public void tokenize() throws Exception {
		TextFileReader reader = new TextFileReader(NSPath.NEWS_COL_JSON_FILE);
		TextFileWriter writer = new TextFileWriter(NSPath.NEWS_COL_TEXT_FILE);
		List<String> labels = new ArrayList<String>();

		while (reader.hasNext()) {
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

				if (values.size() > 0 && values.get(0).equals("33b6c3c7-7a3c-450e-9293-04652bf6441a")) {
					System.out.println();
				}

				if (value.trim().length() == 0) {
					continue;
				}

				if (key.equals("content") || key.equals("title")) {
					value = StrUtils.join("\\n", NLPUtils.tokenize(value));
				} else {
					value = StrUtils.normalizeSpaces(value);
				}
				values.add(value);
			}

			if (values.size() == labels.size()) {
				writer.write(StrUtils.join("\t", values) + "\n");
			}
		}
		reader.close();
		writer.close();

	}

}
