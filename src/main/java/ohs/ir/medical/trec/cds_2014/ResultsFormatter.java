package ohs.ir.medical.trec.cds_2014;

import java.io.File;
import java.util.List;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;

public class ResultsFormatter {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		ResultsFormatter rf = new ResultsFormatter();
		rf.convertToTrecFormat();
		System.out.println("process ends.");
	}

	public void convertToTrecFormat() throws Exception {
		List<File> files = FileUtils.getFilesUnder(new File(CDSPath.OUTPUT_RERANKING_DIR));

		for (int i = 0; i < files.size(); i++) {
			File inputFile = files.get(i);
			if (!inputFile.getName().startsWith("search_results")) {
				continue;
			}

			String fileName = FileUtils.removeExtension(inputFile.getName());
			String info = fileName.substring("search_results-".length());

			String runId = "";

			if (info.equals("init")) {
				runId = "KISTI01";
			} else if (info.equals("init+prf")) {
				runId = "KISTI02";
			} else if (info.equals("init+prf+esa")) {
				runId = "KISTI03";
			} else if (info.equals("init+prf+MetaMap-alpha1")) {
				runId = "KISTI04";
			} else if (info.equals("init+prf+MetaMap-alpha2")) {
				runId = "KISTI05";
			} else if (info.equals("init+prf+MetaMap-alpha3")) {
				runId = "KISTI06";
			} else if (info.equals("init+prf+MetaMap-times")) {
				runId = "KISTI07";
			}

			TextFileReader reader = new TextFileReader(inputFile);

			StringBuffer outputBuff = new StringBuffer();
			int rank = 0;

			while (reader.hasNext()) {
				String line = reader.next();

				if (reader.getNumLines() == 1) {

				} else {
					String[] parts = line.split("\t");
					int queryId = Integer.parseInt(parts[0]);
					String docId = parts[1];
					String score = parts[2];
					outputBuff.append(String.format("%d\t%s\t%s\t%d\t%s\t%s\n", queryId, "Q0", docId, ++rank, score, runId));
				}
			}
			reader.close();

			// String outputFileName = inputFile.getName().replace("search_", "trec_");
			String outputFileName = String.format("%s.txt", runId);
			File outputFile = new File(inputFile.getParent(), outputFileName);

			FileUtils.write(outputFile.getPath(), outputBuff.toString().trim());

		}
	}

}
