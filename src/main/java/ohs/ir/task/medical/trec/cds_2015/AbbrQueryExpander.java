package ohs.ir.task.medical.trec.cds_2015;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;

import ohs.io.TextFileReader;
import ohs.ir.lucene.common.AnalyzerUtils;
import ohs.ir.lucene.common.MedicalEnglishAnalyzer;
import ohs.ir.task.medical.MIRPath;
import ohs.ir.task.medical.query.BaseQuery;
import ohs.ir.task.medical.query.QueryReader;
import ohs.types.Counter;

public class AbbrQueryExpander {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		AbbrQueryExpander expander = new AbbrQueryExpander(MedicalEnglishAnalyzer.getAnalyzer(), MIRPath.ABBREVIATION_FILTERED_FILE);

		List<BaseQuery> bqs = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2015_A_FILE);

		for (int i = 0; i < bqs.size(); i++) {
			BaseQuery bq = bqs.get(i);
			String q = bq.getSearchText();
			expander.expand(q);
		}

		System.out.println("process ends.");

	}

	private StrCounterMap abbrMap;

	private Analyzer analyzer;

	public AbbrQueryExpander(Analyzer analyzer, String abbrFileName) throws Exception {
		this.analyzer = analyzer;
		abbrMap = readAbbreviations(abbrFileName);

	}

	public Counter<String> expand(String q) {
		StringBuffer sb = new StringBuffer(q);
		String[] toks = q.split(" ");
		Counter<String> temp = new Counter<String>();
		for (int i = 0; i < toks.length; i++) {
			String tok = toks[i];
			temp.incrementCount(tok, 1);
		}
		temp.normalize();

		Counter<String> ret = new Counter<String>();
		double mixture = 0.5;

		for (String word : temp.keySet()) {
			double prob = temp.getCount(word);
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

	private StrCounterMap readAbbreviations(String fileName) throws Exception {
		StrCounterMap cm = new StrCounterMap();

		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String shortForm = "";

			{
				String[] parts = lines.get(0).split("\t");
				shortForm = parts[1];
			}

			Counter<String> c = new Counter<String>();

			for (int i = 1; i < lines.size(); i++) {
				String[] parts = lines.get(i).split("\t");
				String longForm = parts[0];
				List<String> words = AnalyzerUtils.getWords(longForm, analyzer);
				double cnt = Double.parseDouble(parts[1]);

				for (int j = 0; j < words.size(); j++) {
					c.incrementCount(words.get(j), cnt);
				}
			}

			cm.setCounter(shortForm, c);
		}
		reader.close();

		cm.normalize();

		return cm;
	}

}
