package ohs.ir.task.medical.trec.cds_2014;

import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ohs.io.IOUtils;

public class CDSQuery {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		List<CDSQuery> queries = read(CDSPath.TEST_QUERY_FILE);

		for (int i = 0; i < queries.size(); i++) {
			System.out.println(queries.get(i).toString());

		}

		System.out.println("process ends.");
	}

	public static List<CDSQuery> read(String fileName) throws Exception {
		List<CDSQuery> ret = new ArrayList<CDSQuery>();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = dbf.newDocumentBuilder();

		Document xmlDoc = parser.parse(new InputSource(new StringReader(IOUtils.readText(fileName))));

		Element docElem = xmlDoc.getDocumentElement();
		NodeList nodeList = docElem.getElementsByTagName("topic");

		String[] nodeNames = { "description", "summary" };

		for (int i = 0; i < nodeList.getLength(); i++) {
			Element topicElem = (Element) nodeList.item(i);

			String id = topicElem.getAttribute("number");
			String type = topicElem.getAttribute("type");

			String[] values = new String[nodeNames.length];

			values[0] = topicElem.getAttribute(nodeNames[0]);
			for (int j = 0; j < nodeNames.length; j++) {
				NodeList nodes = topicElem.getElementsByTagName(nodeNames[j]);
				if (nodes.getLength() > 0) {
					values[j] = nodes.item(0).getTextContent();
				}
			}

			String description = values[0];
			String summary = values[1];

			id = new DecimalFormat("00").format(Integer.parseInt(id));

			CDSQuery query = new CDSQuery(id, description, summary, type);
			ret.add(query);

		}
		return ret;
	}

	private String id;

	private String description;

	private String summary;

	private String type;

	public CDSQuery(String id, String description, String summary, String type) {
		super();
		this.id = id;
		this.description = description;
		this.summary = summary;
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CDSQuery other = (CDSQuery) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (summary == null) {
			if (other.summary != null)
				return false;
		} else if (!summary.equals(other.summary))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((summary == null) ? 0 : summary.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("id:\t%s\n", id));
		sb.append(String.format("type:\t%s\n", type));
		sb.append(String.format("description:\t%s\n", description));
		sb.append(String.format("summary:\t%s\n", summary));
		return sb.toString();
	}
}
