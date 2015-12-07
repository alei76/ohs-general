package ohs.ir.medical.query;

import java.util.List;

import org.apache.lucene.search.Query;

import ohs.ir.medical.general.MIRPath;
import ohs.matrix.SparseVector;

public class ClefEHealthQuery implements BaseQuery {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		List<BaseQuery> queries = QueryReader.readClefEHealthQueries(MIRPath.CLEF_EHEALTH_QUERY_2014_FILE);

		for (int i = 0; i < queries.size(); i++) {
			System.out.printf("%dth query\n%s\n", i + 1, queries.get(i));

		}

		System.out.println("process ends.");
	}

	private String id;

	private String discharge;

	private String title;

	private String description;

	private String profile;

	private String narrative;

	private Query luceneQuery;

	private SparseVector queryModel;

	private List<Integer> words;

	public ClefEHealthQuery(String id, String discharge, String title, String description, String profile, String narrative) {
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
		ClefEHealthQuery other = (ClefEHealthQuery) obj;
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

	@Override
	public Query getLuceneQuery() {
		return luceneQuery;
	}

	public String getNarrative() {
		return narrative;
	}

	public String getProfile() {
		return profile;
	}

	@Override
	public List<Integer> getQueryWords() {
		return words;
	}

	@Override
	public String getSearchText() {
		String ret = title + "\n" + description;
		ret = ret.replaceAll("[\\p{Punct}]+", " ");
		return ret;
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

	@Override
	public void setLuceneQuery(Query luceneQuery) {
		this.luceneQuery = luceneQuery;
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
		sb.append(String.format("QID:\t%s\n", id));
		sb.append(String.format("Title:\t%s\n", title));
		sb.append(String.format("Description:\t%s\n", description));
		sb.append(String.format("Profile:\t%s\n", profile));
		sb.append(String.format("Narrative:\t%s\n", narrative));
		sb.append(String.format("Discharge:\n%s", discharge));
		return sb.toString();
	}
}
