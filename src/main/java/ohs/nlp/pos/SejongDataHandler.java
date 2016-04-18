package ohs.nlp.pos;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import edu.stanford.nlp.ling.Word;
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
import ohs.utils.StrUtils;
import ohs.utils.UnicodeUtils;

public class SejongDataHandler {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		SejongDataHandler sdh = new SejongDataHandler();
		// sdh.extractPosData();
		// sdh.extractCounts();
		// sdh.buildAnalyzedDict();
		// sdh.buildSystemDict();

		// sdh.buildTrie();
		// sdh.build();

		System.out.println("process ends.");
	}

	public static void write(ObjectOutputStream oos, Node<Character> node) throws Exception {
		for (Node<Character> child : node.getChildren().values()) {
			write(oos, child);

			char key = child.getKey();
			int cnt = child.getCount();
			int depth = child.getDepth();
			int id = child.getID();

			oos.writeChar(key);
			oos.writeInt(cnt);
			oos.writeInt(depth);
			oos.writeInt(id);
		}
	}

	public void build() throws Exception {
		CounterMap<String, String> cm = Generics.newCounterMap();

		SejongReader reader = new SejongReader(NLPPath.POS_DATA_FILE);
		while (reader.hasNext()) {
			KDocument doc = reader.next();

			for (KSentence sent : doc.getSentences()) {
				System.out.println(sent.toString());
				String[] words = sent.getValues(TokenAttr.WORD);
				System.out.println(StrUtils.join(" ", words));
				System.out.println();

			}
		}
		reader.close();

		// List<String> words = Generics.newArrayList(cm.keySet());
		// Collections.sort(words);
		//
		// for (int i = 0; i < words.size(); i++) {
		// String word = words.get(i);
		// Counter<String> c = cm.getCounter(word);
		//
		// List<String> res = Generics.newArrayList();
		// res.add(word);
		// res.addAll(c.keySet());
		// words.set(i, String.join("\t", res));
		// }

	}

	public void buildAnalyzedDict() throws Exception {
		CounterMap<String, String> cm = Generics.newCounterMap();

		SejongReader reader = new SejongReader(NLPPath.POS_DATA_FILE);
		while (reader.hasNext()) {
			KDocument doc = reader.next();

			for (KSentence sent : doc.getSentences()) {
				for (MultiToken mt : sent.toMultiTokens()) {
					String s = StrUtils.join(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, mt.getSubValues(TokenAttr.WORD),
							mt.getSubValues(TokenAttr.POS));
					cm.incrementCount(mt.getValue(TokenAttr.WORD), s, 1);
				}
			}
		}
		reader.close();

		List<String> eojeols = Generics.newArrayList(cm.keySet());
		Collections.sort(eojeols);

		for (int i = 0; i < eojeols.size(); i++) {
			String eojeol = eojeols.get(i);
			Counter<String> morphemes = cm.getCounter(eojeol);

			List<String> res = Generics.newArrayList();
			res.add(eojeol);
			res.addAll(morphemes.keySet());
			eojeols.set(i, String.join("\t", res));
		}

		FileUtils.writeStrCollection(NLPPath.DICT_ANALYZED_FILE, eojeols);
	}

	public void buildSystemDict() throws Exception {
		CounterMap<String, String> cm = Generics.newCounterMap();

		SejongReader reader = new SejongReader(NLPPath.POS_DATA_FILE);
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

		List<String> res = Generics.newArrayList();

		List<String> keys1 = Generics.newArrayList(cm.keySet());
		Collections.sort(keys1);

		for (int i = 0; i < keys1.size(); i++) {
			String key = keys1.get(i);
			List<String> keys2 = Generics.newArrayList(cm.keySetOfCounter(key));
			Collections.sort(keys2);

			for (int j = 0; j < keys2.size(); j++) {
				res.add(key + "\t" + keys2.get(j));
			}
		}

		FileUtils.writeStrCollection(NLPPath.DICT_SYSTEM_FILE, res);
	}

	public void buildTrie() throws Exception {
		CounterMap<String, String> cm = FileUtils.readStrCounterMap(NLPPath.WORD_POS_CNT_ILE);
		Trie<Character> trie = new Trie<Character>();

		List<String> keys = cm.getOutKeyCountSums().getSortedKeys();

		for (int i = 0; i < keys.size(); i++) {
			String word = keys.get(i);
			Counter<String> posCnts = cm.getCounter(word);

			StringBuffer sb = new StringBuffer();

			for (int j = 0; j < word.length(); j++) {
				char in = word.charAt(j);
				sb.append(UnicodeUtils.decomposeToJamo(in));
			}

			Node<Character> node = trie.insert(StrUtils.asCharacters(sb.toString().toCharArray()));

			Counter<String> c = (Counter<String>) node.getData();

			if (c == null) {
				c = Generics.newCounter();
				node.setData(c);
			}

			c.incrementAll(posCnts);

			// String str = UnicodeUtils.decomposeToJamo(word);

			// System.out.println(str);

			// System.out.println(s);
		}

		System.out.println(trie.toString());

		trie.trimToSize();

		System.out.println();
	}

	public void extractCounts() throws Exception {
		CounterMap<String, String> cm = Generics.newCounterMap();
		CounterMap<String, String> cm2 = Generics.newCounterMap();
		CounterMap<String, String> cm3 = Generics.newCounterMap();

		SejongReader reader = new SejongReader(NLPPath.POS_DATA_FILE);
		while (reader.hasNext()) {
			KDocument doc = reader.next();

			for (KSentence sent : doc.getSentences()) {
				for (MultiToken mt : sent.toMultiTokens()) {

					if (mt.size() > 0) {
						Token t = mt.getToken(0);
						String word = t.getValue(TokenAttr.WORD);
						String pos = t.getValue(TokenAttr.POS);
						cm2.incrementCount(word, pos, 1);
					}

					for (int i = 1; i < mt.size(); i++) {
						Token t1 = mt.getToken(i - 1);
						String word1 = t1.getValue(TokenAttr.WORD);
						String pos1 = t1.getValue(TokenAttr.POS);

						Token t2 = mt.getToken(i);
						String word2 = t2.getValue(TokenAttr.WORD);
						String pos2 = t2.getValue(TokenAttr.POS);

						cm2.incrementCount(word2, pos2, 1);
						cm3.incrementCount(pos1, pos2, 1);
					}
				}
			}
		}
		reader.close();
		FileUtils.writeStrCounterMap(NLPPath.WORD_POS_CNT_ILE, cm2);
		FileUtils.writeStrCounterMap(NLPPath.WORD_POS_TRANS_CNT_ILE, cm3);
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
								subparts[j] = word + Token.DELIM_TOKEN + pos;
							}

							parts[2] = StrUtils.join(MultiToken.DELIM_MULTI_TOKEN, subparts);

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
