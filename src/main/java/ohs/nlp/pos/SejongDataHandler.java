package ohs.nlp.pos;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.tree.trie.hash.Node;
import ohs.tree.trie.hash.Trie;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.utils.Generics;
import ohs.utils.KoreanUtils;
import ohs.utils.StrUtils;

public class SejongDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		SejongDataHandler sdh = new SejongDataHandler();
		// sdh.extractPosData();
		// sdh.extractCounts();
		// sdh.buildSystemDict();
		// sdh.buildAnalyzedDict();
		sdh.buildTrie();

		System.out.println("process ends.");
	}

	public void buildTrie() throws Exception {
		List<String> lines = FileUtils.readLines(NLPPath.DICT_ANALYZED_FILE);
		Trie<Character> trie = new Trie<Character>();

		for (int i = 0; i < lines.size(); i++) {
			String[] parts = lines.get(i).split("\t");

			char[] chs = KoreanUtils.decomposeKoreanWordToPhonemes(parts[0]);
			String s = String.valueOf(chs);

			Node<Character> node = trie.insert(StrUtils.toCharacters(chs));
			node.setData("");

			// System.out.println(s);
		}

		System.out.println(trie.toString());

		trie.trimToSize();

		System.out.println();
	}

	public void buildAnalyzedDict() throws Exception {
		CounterMap<String, String> cm = Generics.newCounterMap();

		SejongReader reader = new SejongReader(NLPPath.POS_DATA_FILE, NLPPath.POS_TAG_SET_FILE);
		while (reader.hasNext()) {
			KDocument doc = reader.next();

			for (KSentence sent : doc.getSentences()) {
				for (Token tok : sent.getTokens()) {
					MultiToken mt = (MultiToken) tok;
					cm.incrementCount(mt.getText(), mt.joinValues(), 1);
				}
			}
		}
		reader.close();

		List<String> words = Generics.newArrayList(cm.keySet());
		Collections.sort(words);

		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			Counter<String> c = cm.getCounter(word);

			List<String> res = Generics.newArrayList();
			res.add(word);
			res.addAll(c.keySet());
			words.set(i, String.join("\t", res));
		}

		FileUtils.writeStrCollection(NLPPath.DICT_ANALYZED_FILE, words);
	}

	public void buildSystemDict() throws Exception {
		CounterMap<String, String> cm = Generics.newCounterMap();

		SejongReader reader = new SejongReader(NLPPath.POS_DATA_FILE, NLPPath.POS_TAG_SET_FILE);
		while (reader.hasNext()) {
			KDocument doc = reader.next();

			for (KSentence sent : doc.getSentences()) {
				for (Token tok : sent.getTokens()) {
					MultiToken mt = (MultiToken) tok;

					for (Token t : mt.getTokens()) {
						cm.incrementCount(t.getValue(TokenAttr.WORD), t.getValue(TokenAttr.POS), 1);
					}
				}
			}
		}
		reader.close();

		List<String> dict = Generics.newArrayList();

		for (String word : cm.keySet()) {
			Counter<String> c = cm.getCounter(word);

			if (c.size() == 1) {
				dict.add(word + "\t" + c.argMax());
			}
		}

		Collections.sort(dict);

		FileUtils.writeStrCollection(NLPPath.DICT_SYSTEM_FILE, dict);
	}

	public void extractCounts() throws Exception {

		Set<String> posSet = Generics.newHashSet();

		for (String line : FileUtils.readLines(NLPPath.POS_TAG_SET_FILE)) {
			String[] parts = line.split("\t");
			posSet.add(parts[0]);
		}

		CounterMap<String, String> cm = Generics.newCounterMap();
		CounterMap<String, String> cm2 = Generics.newCounterMap();

		SejongReader reader = new SejongReader(NLPPath.POS_DATA_FILE, NLPPath.POS_TAG_SET_FILE);
		while (reader.hasNext()) {
			KDocument doc = reader.next();

			for (KSentence sent : doc.getSentences()) {
				for (Token tok : sent.getTokens()) {
					MultiToken mt = (MultiToken) tok;

					for (Token t : mt.getTokens()) {
						String word = t.getValue(TokenAttr.WORD);
						String pos = t.getValue(TokenAttr.POS);
						cm2.incrementCount(word, pos, 1);
					}
				}
			}
		}
		reader.close();
		FileUtils.writeStrCounterMap(NLPPath.WORD_POS_CNT_ILE, cm);
		FileUtils.writeStrCounterMap(NLPPath.WORD_CNT_ILE, cm2);
	}

	public void extractPosData() throws Exception {
		TextFileWriter writer = new TextFileWriter(NLPPath.POS_DATA_FILE);
		ZipInputStream zio = new ZipInputStream(new FileInputStream(NLPPath.SEJONG_POS_DATA_FILE));
		BufferedReader br = new BufferedReader(new InputStreamReader(zio, "utf-16"));
		ZipEntry ze = null;

		while ((ze = zio.getNextEntry()) != null) {
			if (ze.isDirectory()) {
				continue;
			}

			String name = FileUtils.removeExtension(ze.getName());
			String startTag = name + "-";

			StringBuffer sb = new StringBuffer();
			String line = "";

			boolean isDoc = false;

			while ((line = br.readLine()) != null) {

				if (line.startsWith("<body>")) {
					isDoc = true;
					continue;
				} else if (line.startsWith("</body>")) {
					isDoc = false;

					String docText = sb.toString().trim();
					sb = new StringBuffer();

					String[] lines = docText.split("\n");

					List<String> inputs = Generics.newArrayList();
					List<String> outputs = Generics.newArrayList();

					for (int i = 0; i < lines.length; i++) {
						if (lines[i].startsWith(startTag)) {
							String[] parts = lines[i].split("\t");

							String[] subparts = parts[2].split(" \\+ ");

							for (int j = 0; j < subparts.length; j++) {
								String[] two = StrUtils.split2Two("/", subparts[j]);
								String word = two[0];
								String pos = two[1];
								subparts[j] = word + " / " + pos;
							}

							parts[2] = StrUtils.join(" + ", subparts);

							String input = StrUtils.join("\t", parts, 1, 3);
							inputs.add(input);
						} else {
							if (inputs.size() > 0) {
								String output = StrUtils.join("\n", inputs);
								outputs.add(output);
							}
							inputs.clear();
						}
					}

					StringBuffer res = new StringBuffer();
					res.append(String.format("<doc id=%s>\n", name));
					res.append(StrUtils.join("\n\n", outputs));
					res.append("\n</doc>");
					writer.write(res.toString() + "\n\n");
				}

				if (isDoc) {
					sb.append(line + "\n");
				}
			}
		}
		br.close();
		writer.close();
	}

}
