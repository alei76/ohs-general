package ohs.ir.medical.clef.ehealth_2014;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ohs.io.FileUtils;

public class EHealthQuery {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		List<EHealthQuery> eHealthQueries = read(EHPath.QUERY_2014_TRAIN_FILE, EHPath.DISCHARGE_DIR);

		tokenize(eHealthQueries);

		for (int i = 0; i < eHealthQueries.size(); i++) {
			System.out.printf("%dth query\n%s\n", i + 1, eHealthQueries.get(i));

		}

		System.out.println("process ends.");
	}

	public static List<EHealthQuery> read(String queryFileName, String dischargeDir) throws Exception {
		boolean is_2013_queries = false;

		if (queryFileName.contains("2013ehealth")) {
			is_2013_queries = true;
		}

		List<EHealthQuery> ret = new ArrayList<EHealthQuery>();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = dbf.newDocumentBuilder();

		Document xmlDoc = parser.parse(new InputSource(new StringReader(FileUtils.readText(queryFileName))));

		Element docElem = xmlDoc.getDocumentElement();
		NodeList nodeList = null;

		if (is_2013_queries) {
			nodeList = docElem.getElementsByTagName("query");
		} else {
			nodeList = docElem.getElementsByTagName("topic");
		}

		Map<String, File> dischargeFileMap = new TreeMap<String, File>();

		for (File file : new File(dischargeDir).listFiles()) {
			dischargeFileMap.put(file.getName(), file);
		}

		String[] nodeNames = { "id", "discharge_summary", "title", "desc", "profile", "narr" };

		for (int i = 0; i < nodeList.getLength(); i++) {
			Element queryElem = (Element) nodeList.item(i);

			String[] values = new String[nodeNames.length];

			values[0] = queryElem.getAttribute(nodeNames[0]);
			for (int j = 0; j < nodeNames.length; j++) {
				NodeList nodes = queryElem.getElementsByTagName(nodeNames[j]);
				if (nodes.getLength() > 0) {
					values[j] = nodes.item(0).getTextContent();
				}
			}

			String id = values[0];
			String dischargeFileName = values[1].trim();

			File dischargeFile = dischargeFileMap.get(dischargeFileName);
			String discharge = "";

			if (dischargeFile != null) {
				discharge = FileUtils.readText(dischargeFile.getPath());
			} else {
				new FileNotFoundException(dischargeFileName);
			}

			String title = values[2];
			String description = values[3];
			String profile = values[4];
			String narrative = values[5];

			EHealthQuery cq = new EHealthQuery(id, discharge, title, description, profile, narrative);
			ret.add(cq);
		}
		return ret;
	}

	public static void tokenize(List<EHealthQuery> eHealthQueries) {
		for (int i = 0; i < eHealthQueries.size(); i++) {
			EHealthQuery q = eHealthQueries.get(i);

			String title = q.getTitle();
			String desc = q.getDescription();
			String discharge = q.getDischarge();

			title = DataHandler.tokenize(title).replace("\n", " ");
			desc = DataHandler.tokenize(desc).replace("\n", " ");
			discharge = DataHandler.tokenize(discharge);

			q.setTitle(title);
			q.setDescription(desc);
			q.setDischarge(discharge);
		}
	}

	private String id;

	private String discharge;

	private String title;

	private String description;

	private String profile;

	private String narrative;

	public EHealthQuery(String id, String discharge, String title, String description, String profile, String narrative) {
		super();
		this.id = id;
		this.discharge = discharge;
		this.title = title;
		this.description = description;
		this.profile = profile;
		this.narrative = narrative;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EHealthQuery other = (EHealthQuery) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public String getDescription() {
		return description;
	}

	public String getDischarge() {
		return discharge;
	}

	public String getId() {
		return id;
	}

	public String getNarrative() {
		return narrative;
	}

	public String getProfile() {
		return profile;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDischarge(String discharge) {
		this.discharge = discharge;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setNarrative(String narrative) {
		this.narrative = narrative;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("id:\t%s\n", id));
		sb.append(String.format("title:\t%s\n", title));
		sb.append(String.format("desc:\t%s\n", description));
		sb.append(String.format("profile:\t%s\n", profile));
		sb.append(String.format("narr:\t%s\n", narrative));
		sb.append(String.format("discharge:\n%s\n", discharge));
		return sb.toString();
	}
}
