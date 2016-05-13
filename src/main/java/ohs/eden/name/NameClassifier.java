package ohs.eden.name;

import java.util.List;

import ohs.io.FileUtils;
import ohs.ir.weight.TermWeighting;
import ohs.math.VectorUtils;
import ohs.matrix.SparseVector;
import ohs.ml.svm.wrapper.LibLinearTrainer;
import ohs.ml.svm.wrapper.LibLinearWrapper;
import ohs.types.Counter;
import ohs.types.Indexer;
import ohs.types.ListMap;
import ohs.utils.Generics;
import ohs.utils.StrUtils;

public class NameClassifier {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		NameClassifier nc = new NameClassifier();

		if (FileUtils.exists(NamePath.SVM_MODEL_FILE)) {
			nc.read(NamePath.SVM_MODEL_FILE);
		} else {
			nc = NameClassifier.train();
			nc.write(NamePath.SVM_MODEL_FILE);
		}

		System.out.println(nc.scorePerOrg("오흥선", ""));
		System.out.println(nc.scorePerOrg("김진영", ""));
		System.out.println(nc.scorePerOrg("LG전자", ""));
		System.out.println(nc.scorePerOrg("한국과학기술", ""));
		System.out.println(nc.scorePerOrg("", "samsun"));
		System.out.println(nc.scorePerOrg("마이크로소프트", ""));

		System.out.println("process ends.");
	}

	public static NameClassifier train() throws Exception {
		String[] fileNames = { NamePath.NAME_PER_KOR_FILE, NamePath.NAME_PER_ENG_FILE,

				NamePath.NAME_ORG_KOR_FILE, NamePath.NAME_ORG_ENG_FILE };

		Indexer<String> featIndexer = Generics.newIndexer();
		Indexer<String> labelIndexer = Generics.newIndexer();

		labelIndexer.add("PER_KOR");
		labelIndexer.add("PER_ENG");
		labelIndexer.add("ORG_KOR");
		labelIndexer.add("ORG_ENG");

		// List<SparseVector>[] datasets = new List[fileNames.length];

		ListMap<Integer, SparseVector> lm = Generics.newListMap();

		NameFeatureExtractor ext = new NameFeatureExtractor();

		for (int i = 0; i < fileNames.length; i++) {
			// List<SparseVector> data = Generics.newArrayList();

			// datasets[i] = data;

			Counter<String> c = FileUtils.readStrCounter(fileNames[i]);

			List<String> keys = c.getSortedKeys();

			for (int j = 0; j < keys.size(); j++) {
				String key = keys.get(j);
				double cnt = c.getCount(key);

				String[] two = key.split("\t");
				two = StrUtils.unwrap(two);

				String kor = two[0];
				String eng = two[1];

				if (i == 0) {
					if (kor.length() > 0 && kor.length() <= 5) {

						if (kor.endsWith("회") || kor.endsWith("터") || kor.endsWith("회의") || kor.endsWith("합")) {
							continue;
						}
					}
				}

				Counter<String> featCnts = ext.extract(kor, eng);

				if (featCnts.size() > 0) {
					SparseVector sv = VectorUtils.toSparseVector(featCnts, featIndexer, true);
					sv.scale(cnt);

					// data.add(sv);

					lm.put(i, sv);
				}
			}
		}

		List<SparseVector> data = Generics.newArrayList();

		for (int label : lm.keySet()) {
			data.addAll(lm.get(label));
		}
		TermWeighting.computeTFIDFs(data);

		// List<SparseVector>[] two = DataSplitter.splitInOrder(data, ArrayUtils.array(0.8, 0.2));
		//
		// List<SparseVector> trainData = two[0];
		// List<SparseVector> testData = two[1];

		LibLinearTrainer t = new LibLinearTrainer();
		LibLinearWrapper wrapper = t.train(labelIndexer, featIndexer, data);
		return new NameClassifier(wrapper);
	}

	private LibLinearWrapper wrapper = new LibLinearWrapper();

	private NameFeatureExtractor extractor = new NameFeatureExtractor();

	public NameClassifier() {

	}

	// public Counter<String> classifyKorean(String s) {
	// return classify(s, "");
	// }
	//
	// public Counter<String> classifyEnglish(String s) {
	// return classify("", s);
	// }

	public NameClassifier(LibLinearWrapper wrapper) {
		this.wrapper = wrapper;
	}

	public Counter<String> classifyPerson(String kor, String eng) {
		return wrapper.score(extractor.extract(kor, eng));
	}

	public void read(String fileName) throws Exception {
		wrapper.read(fileName);
	}

	public Counter<String> score(String kor, String eng) {
		return wrapper.score(extractor.extract(kor, eng));
	}

	public Counter<String> scorePerOrg(Counter<String> scores) {
		Counter<String> ret = Generics.newCounter();
		for (String label : scores.keySet()) {
			ret.incrementCount(label.split("_")[0], scores.getCount(label));
		}
		return ret;
	}

	public Counter<String> scorePerOrg(String kor, String eng) {
		return scorePerOrg(score(kor, eng));
	}

	public void write(String fileName) throws Exception {
		wrapper.write(fileName);
	}

}
