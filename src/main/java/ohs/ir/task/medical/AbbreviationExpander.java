package ohs.ir.task.medical;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.StrUtils;

public class AbbreviationExpander {

	public static void main(String[] args) throws Exception {
		String[] collDirs = MIRPath.TaskDirs;

		for (int i = 0; i < collDirs.length; i++) {
			String inputFileName = collDirs[i] + "abbrs_filter.txt";
			String outputFileName = collDirs[i] + "abbrs_cm.txt";
			CounterMap<String, String> cm = readAbbreviationData(inputFileName);
			IOUtils.write(outputFileName, cm);
		}
	}

	public static CounterMap<String, String> readAbbreviationData(String fileName) throws Exception {
		CounterMap<String, String> ret = new CounterMap<String, String>();
		Analyzer analyzer = MedicalEnglishAnalyzer.getAnalyzer();

		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String shortForm = lines.get(0).split("\t")[1];
			Counter<String> c = new Counter<String>();

			for (int i = 1; i < lines.size(); i++) {
				String[] parts = lines.get(i).split("\t");
				String longForm = parts[0];

				if (longForm.toLowerCase().contains(shortForm.toLowerCase())) {
					continue;
				}

				double cnt = Double.parseDouble(parts[1]);

				List<String> words = AnalyzerUtils.getWords(longForm, analyzer);

				for (String word : words) {
					c.incrementCount(word, 1);
				}
				c.incrementAll(cnt);
			}

			ret.setCounter(shortForm, c);
		}
		reader.close();

		ret.normalize();

		// System.out.println(ret.toString());

		return ret;
	}

	private CounterMap<String, String> abbrMap;

	public AbbreviationExpander(String fileName) throws Exception {
		abbrMap = readAbbreviationData(fileName);
	}

	public Counter<String> expand(Counter<String> qLM) {
		Counter<String> ret = new Counter<String>();
		double mixture = 0.5;

		for (String word : qLM.keySet()) {
			double prob = qLM.getCount(word);
			if (abbrMap.containsKey(word)) {
				Counter<String> c = abbrMap.getCounter(word);

				double prob_for_query_word = prob * (1 - mixture);
				double prob_for_abbr_word = prob * mixture;

				for (String w : c.keySet()) {
					double prob2 = c.getCount(w);
					ret.incrementCount(w, prob_for_abbr_word * prob2);
				}

				ret.incrementCount(word, prob_for_query_word);

				// System.out.println(tok);
				// System.out.println(abbrMap.getCounter(tok));
				// System.out.println();
			} else {
				ret.incrementCount(word, prob);
			}
		}

		double sum = ret.totalCount();

		return ret;
	}

	public String expand(String searchText) {
		Counter<String> ret = new Counter<String>();
		double mixture = 0.5;

		List<String> words = StrUtils.split(searchText);
		CounterMap<String, String> cm = new CounterMap<String, String>();

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			sb.append(word);
			if (abbrMap.containsKey(word) && !cm.containsKey(word)) {
				Counter<String> c = abbrMap.getCounter(word);
				sb.append(" (");
				for (String w : c.getSortedKeys()) {
					sb.append(" " + w);
				}
				sb.append(" )");
			}
			sb.append(" ");
		}

		return sb.toString().trim();
	}
}
