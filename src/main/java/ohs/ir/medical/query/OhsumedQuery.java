package ohs.ir.medical.query;

import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;

import ohs.ir.medical.general.MIRPath;
import ohs.matrix.SparseVector;

public class OhsumedQuery implements BaseQuery {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");
		List<BaseQuery> queries = QueryReader.readOhsumedQueries(MIRPath.OHSUMED_QUERY_FILE);

		for (int i = 0; i < queries.size(); i++) {
			System.out.println(queries.get(i) + "\n");
		}

		System.out.println("process ends.");
	}

	private String id;

	private String patientInfo;

	private String infoRequest;

	private Query luceneQuery;

	private SparseVector queryModel;

	private List<Integer> words;

	public OhsumedQuery(String id, String patientInfo, String infoRequest) {
		super();
		this.id = id;
		this.patientInfo = patientInfo;
		this.infoRequest = infoRequest;
	}

	@Override
	public String getId() {
		return id;
	}

	public String getInfoRequest() {
		return infoRequest;
	}

	@Override
	public Query getLuceneQuery() {
		return luceneQuery;
	}

	public String getPatientInfo() {
		return patientInfo;
	}

	@Override
	public List<Integer> getQueryWords() {
		return words;
	}

	@Override
	public String getSearchText() {
		String ret = patientInfo + "\n" + infoRequest;
		ret = ret.replaceAll("[\\p{Punct}]+", " ");
		return ret;
	}

	private String makeOutput(Map<String, String> map) {
		String seqId = map.get(".I");
		String medlineId = map.get(".U");
		String meshTerms = map.get(".M");
		String title = map.get(".T");
		String publicationType = map.get(".P");
		String abs = map.get(".W");
		String authors = map.get(".A");
		String source = map.get(".S");

		String output = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", seqId, medlineId, meshTerms, title, publicationType, abs, authors,
				source);

		return output;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setInfoRequest(String infoRequest) {
		this.infoRequest = infoRequest;
	}

	@Override
	public void setLuceneQuery(Query luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

	public void setPatientInfo(String patientInfo) {
		this.patientInfo = patientInfo;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("qid:\t%s\n", id));
		sb.append(String.format("patient info:\t%s\n", patientInfo));
		sb.append(String.format("info request:\t%s", infoRequest));
		return sb.toString();
	}
}
