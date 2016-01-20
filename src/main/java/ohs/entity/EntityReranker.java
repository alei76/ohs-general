package ohs.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ohs.io.TextFileWriter;
import ohs.string.search.ppss.StringRecord;
import ohs.types.Counter;
import ohs.types.CounterMap;

/**
 * @author Heung-Seon Oh
 * 
 * 
 * 
 */
public class EntityReranker implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7199650129494305577L;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		String inputFileName = ENTPath.NAME_ORGANIZATION_FILE;
		String outputFileName = ENTPath.ENTITY_LINKER_FILE;

		if (inputFileName.contains("_orgs")) {
			outputFileName = outputFileName.replace(".ser", "_org.ser");
		} else if (inputFileName.contains("_locs")) {
			outputFileName = outputFileName.replace(".ser", "_loc.ser");
		} else if (inputFileName.contains("_pers")) {
			outputFileName = outputFileName.replace(".ser", "_per.ser");
		}

		EntityLinker el = new EntityLinker();
		el.train(inputFileName);
		el.write(outputFileName);
		el.read(outputFileName);
		// el.setTopK(20);

		List<Entity> ents = new ArrayList<Entity>(el.getEntityMap().values());

		TextFileWriter writer = new TextFileWriter(ENTPath.EX_FILE);

		{
			for (int i = 0; i < ents.size() && i < 100; i++) {
				Entity ent = ents.get(i);
				Counter<Entity> scores = el.link(ent.getText());

				CounterMap<Integer, Integer> candidates = el.getCandidates();

				StringBuffer sb = new StringBuffer();
				sb.append(ent.getId() + "\t" + ent.getText());

				for (int eid : candidates.keySet()) {
					Entity cent = el.getEntityMap().get(eid);
					sb.append("\n" + cent.getText());

					Counter<Integer> c = candidates.getCounter(eid);

					for (int rid : c.keySet()) {
						StringRecord sr = el.getStringSearcher().getStringRecords().get(rid);
						Double[] sscores = el.getStringSearcher().getSimScores().get(rid);
						sb.append("\t" + sr.getString());

						for (int j = 0; j < sscores.length; j++) {
							sb.append("\t" + sscores[j].doubleValue());
						}
					}
				}

				writer.write(sb.toString() + "\n\n");
			}
		}

		writer.close();

		System.out.println("process ends.");
	}

	public EntityReranker() {
	}

}
