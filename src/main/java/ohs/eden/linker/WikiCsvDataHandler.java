package ohs.eden.linker;

import ohs.io.TextFileReader;

public class WikiCsvDataHandler {

	public static void main(String[] args) {
		System.out.printf("[%s] begins.\n", WikiCsvDataHandler.class.getName());

		WikiCsvDataHandler d = new WikiCsvDataHandler();
		d.encodeRedirects();

		System.out.printf("[%s] ends.\n", WikiCsvDataHandler.class.getName());
	}

	public void encodeRedirects() {
		TextFileReader reader = new TextFileReader(ELPath.WIKI_DIR + "wiki_title.csv");
		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");
		}
	}

	private String[] normalize(String[] parts) {
		String[] ret = new String[parts.length];

		for (int i = 0; i < parts.length; i++) {
			String s = parts[i];
			s = s.substring(1, s.length() - 1);
			ret[i] = s;
		}

		return ret;
	}

}
