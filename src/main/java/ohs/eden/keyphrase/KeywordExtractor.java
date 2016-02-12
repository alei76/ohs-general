package ohs.eden.keyphrase;

import java.util.List;

import ohs.io.TextFileReader;
import ohs.string.search.ppss.Gram;
import ohs.string.search.ppss.GramGenerator;
import ohs.types.Counter;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class KeywordExtractor {

	public static void main(String[] args) {
		System.out.printf("[%s] begins.\n", KeywordExtractor.class.getName());

		KeywordExtractor e = new KeywordExtractor();

		TextFileReader reader = new TextFileReader(KPPath.KEYWORD_EXTRACTOR_DIR + "rag_tagged.txt");
		while (reader.hasNext()) {
			List<String> lines = reader.getNextLines();
			String keywordStr = lines.get(0);
			String text = lines.get(1);
			e.extract(text);
		}
		reader.close();

		System.out.printf("ends.");
	}

	public void prepare() throws Exception {

	}

	public Counter<String> extract(String text) {
		Counter<String> ret = Generics.newCounter();
		
		GramGenerator gg = new GramGenerator(1);
		
		List<String> words = StrUtils.split(text);
		
		for(Gram g : gg.generateNGrams(words)){
			
		}
		

		return ret;
	}

}
