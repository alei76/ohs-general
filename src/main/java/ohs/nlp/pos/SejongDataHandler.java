package ohs.nlp.pos;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ohs.io.FileUtils;
import ohs.io.TextFileWriter;
import ohs.nlp.ling.types.KDocument;
import ohs.nlp.ling.types.KSentence;
import ohs.nlp.ling.types.MultiToken;
import ohs.nlp.ling.types.Token;
import ohs.nlp.ling.types.TokenAttr;
import ohs.types.CounterMap;
import ohs.types.Triple;
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
		sdh.buildDicts();

		System.out.println("process ends.");
	}

	public void buildDicts() throws Exception {
		CounterMap<String, String> cm1 = Generics.newCounterMap();
		CounterMap<String, String> cm2 = Generics.newCounterMap();
		CounterMap<String, String> cm3 = Generics.newCounterMap();

		SejongReader reader = new SejongReader(NLPPath.POS_DATA_FILE);
		while (reader.hasNext()) {
			KDocument doc = reader.next();

			for (KSentence sent : doc.getSentences()) {
				for (MultiToken mt : sent.toMultiTokens()) {
					String ot = mt.getValue(TokenAttr.WORD);
					String[] words = mt.getSubValues(TokenAttr.WORD);
					String[] poss = mt.getSubValues(TokenAttr.POS);

					// cm3.incrementCount(ot, StrUtils.join(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, words, poss), 1);

					if (ot.equals(StrUtils.join("", words))) {
						for (int i = 0; i < words.length; i++) {
							// cm2.incrementCount(words[i], poss[i], 1);
						}
					} else {
						Map<Integer, Triple<Integer, Integer, String>> ts = Generics.newHashMap();

						StringBuffer sb = new StringBuffer(ot);

						for (int i = 0; i < words.length; i++) {
							String word = words[i];
							String pos = poss[i];

							if (pos.startsWith("N") || pos.startsWith("S")) {
								int start = sb.indexOf(word);

								if (start > -1) {
									int end = start + word.length();
									ts.put(i, Generics.newTriple(start, end, sb.substring(start, end)));

									for (int j = start; j < end; j++) {
										sb.setCharAt(j, '#');
									}
									words[i] = pos;
								}
							}
						}

						ot = sb.toString();

						if (ot.matches("^[#]+$")) {
							continue;
						}

						int end = -1;

						for (int i = words.length - 1; i >= 0; i--) {
							String w1 = words[i];
							String p1 = poss[i];

							if (p1.startsWith("S") || w1.equals(p1)) {

							} else {
								end = i;
								break;
							}
						}

						if (end == -1) {
							// String s = StrUtils.join(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, words, poss);
							// cm1.incrementCount(ot, s, 2);
						} else if (end == 0) {
							// System.out.println(ot);
							// String s = StrUtils.join(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, words, poss);
							// cm1.incrementCount(ot, s, 2);
						} else {
							int start = -1;
							for (int i = end - 1; i >= 0; i--) {
								String w2 = words[i];
								String p2 = poss[i];

								if (p2.startsWith("S")) {
									start = i + 1;
									break;
								}

								if (w2.equals(p2)) {
									start = i + 1;
									break;
								}
							}

							if (start > -1 && end - start != 0) {
								String str = StrUtils.join(Token.DELIM_TOKEN, MultiToken.DELIM_MULTI_TOKEN, words, poss, start, end + 1);
								int size = ts.size();

								if (size == 0) {
									// System.out.println(ot);
								} else if (size == 1) {
									if (ot.startsWith("#")) {
										ot = ot.replaceAll("[\\#]+", "~");

										int cnt1 = 0;
										int cnt2 = 0;

										for (int i = 0; i < ot.length(); i++) {
											char ch = ot.charAt(i);
											if (UnicodeUtils.isInRange(UnicodeUtils.HANGUL_SYLLABLES_RANGE, ch)) {
												cnt1++;
											} else {
												cnt2++;
											}
										}

										if (cnt1 > 0 && cnt2 < 2) {
											cm1.incrementCount(ot, str, 1);
										}
									}
								} else if (size > 1) {
									if (!ot.endsWith("#")) {
										int loc = 0;
										for (int i = ot.length() - 1; i >= 0; i--) {
											char ch = ot.charAt(i);
											if (ch != '#' && UnicodeUtils.isInRange(UnicodeUtils.HANGUL_SYLLABLES_RANGE, ch)) {
												loc = i;
												break;
											}
										}

										if (loc > 0) {
											ot.substring(0, ot.length() - loc);
										}

										ot = ot.replaceAll("[\\#]+", "~");
										ot = ot.replaceAll("[`'\"]+", "");

										loc = 0;

										for (int i = ot.length() - 1; i >= 0; i--) {
											if (ot.charAt(i) == '~') {
												loc = i;
												break;
											}
										}

										if (loc > 0) {
											ot = ot.substring(loc);
										}

										int cnt1 = 0;
										int cnt2 = 0;

										for (int i = 0; i < ot.length(); i++) {
											char ch = ot.charAt(i);
											if (UnicodeUtils.isInRange(UnicodeUtils.HANGUL_SYLLABLES_RANGE, ch)) {
												cnt1++;
											} else {
												cnt2++;
											}
										}

										if (cnt1 > 0 && cnt2 < 2) {
											// cm1.incrementCount(ot, str, 1);
										}
									}
								} else {
								}
							}
						}
					}
				}
			}
		}
		reader.close();

		// FileUtils.writeStrCounterMap(NLPPath.DICT_SUFFIX_FILE, cm1);

		writeDict(NLPPath.DICT_SUFFIX_FILE, cm1);
		// writeDict(NLPPath.DICT_WORD_FILE, cm2);
		// writeDict(NLPPath.DICT_ANALYZED_FILE, cm3);
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

		// FileUtils.writeStrCollection(NLPPath.DICT_SYSTEM_FILE, res);
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

	private void writeDict(String fileName, CounterMap<String, String> cm) throws Exception {
		List<String> keys = Generics.newArrayList(cm.keySet());
		Collections.sort(keys);

		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			List<String> values = Generics.newArrayList(cm.keySetOfCounter(key));
			Collections.sort(values);

			keys.set(i, key + "\t" + StrUtils.join("\t", values));
		}

		FileUtils.writeStrCollection(fileName, keys);
	}

}
