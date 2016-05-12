package ohs.ir.medical.wiki;

import java.util.List;
import java.util.Set;

import ohs.io.TextFileReader;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.Vocab;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class SentenceGenerator {

	public static void main(String[] args) {

	}

	private Set<Integer> textLocs;

	private Vocab vocab;

	private int cutoff_cnt;

	private int[][] sents;

	private int max_train_sents = Integer.MAX_VALUE;

	public SentenceGenerator() {
		textLocs = Generics.newHashSet();
	}

	public void addTextLoc(int loc) {
		textLocs.add(loc);
	}

	public int[][] getSentences() {
		return sents;
	}

	public Vocab getVocab() {
		return vocab;
	}

	public void process(String fileName) {
		int num_train_sents = 0;

		Set<Integer> locs = Generics.newHashSet();

		if (textLocs.size() > 0) {
			locs.addAll(textLocs);
		}

		{
			System.out.printf("read [%s]\n", fileName);
			Counter<String> wordCnts = Generics.newCounter();
			TextFileReader reader = new TextFileReader(fileName);
			reader.setPrintNexts(false);

			while (reader.hasNext()) {
				reader.printProgress();

				String[] parts = reader.next().split("\t");
				parts = StrUtils.unwrap(parts);

				if (locs.size() == 0) {
					for (int i = 0; i < parts.length; i++) {
						locs.add(i);
					}
				}

				for (int loc : locs) {
					String s = parts[loc].replace("<nl>", "\n");

					for (String sent : s.split("\n")) {
						sent = sent.toLowerCase();
						for (String word : sent.split(" ")) {
							wordCnts.incrementCount(word, 1);
						}
						num_train_sents++;

						if (max_train_sents == num_train_sents) {
							break;
						}
					}

					if (max_train_sents == num_train_sents) {
						break;
					}
				}

				if (max_train_sents == num_train_sents) {
					break;
				}
			}
			reader.printProgress();
			reader.close();

			wordCnts.pruneKeysBelowThreshold(cutoff_cnt);

			Indexer<String> wordIndexer = Generics.newIndexer();
			List<String> words = wordCnts.getSortedKeys();
			int[] cnts = new int[words.size()];

			for (int i = 0; i < words.size(); i++) {
				String word = words.get(i);
				wordIndexer.add(word);
				cnts[i] = (int) wordCnts.getCount(word);
			}

			vocab = new Vocab(wordIndexer, cnts, new int[0], 0);
		}

		{

			sents = new int[num_train_sents][];

			TextFileReader reader = new TextFileReader(fileName);
			reader.setPrintNexts(false);

			int num_sents = 0;

			while (reader.hasNext()) {
				reader.printProgress();

				String[] parts = reader.next().split("\t");
				parts = StrUtils.unwrap(parts);

				for (int loc : locs) {
					String s = parts[loc].replace("<nl>", "\n");

					for (String sent : s.split("\n")) {
						sent = sent.toLowerCase();
						int[] ws = vocab.getWordIndexer().indexesOfKnown(sent.split(" "));
						sents[num_sents++] = ws;

						if (num_sents == num_train_sents) {
							break;
						}
					}
					if (num_sents == num_train_sents) {
						break;
					}
				}
				
				if (num_sents == num_train_sents) {
					break;
				}
			}
			reader.printProgress();
			reader.close();
		}
	}

	public void setMaxTrainSents(int max_train_sents) {
		this.max_train_sents = max_train_sents;
	}

}
