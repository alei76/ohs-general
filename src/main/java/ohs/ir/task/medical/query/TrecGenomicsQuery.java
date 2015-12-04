package ohs.ir.task.medical.query;

import java.util.List;

import org.apache.lucene.search.Query;

import ohs.ir.task.medical.MIRPath;
import ohs.matrix.SparseVector;

public class TrecGenomicsQuery implements BaseQuery {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		List<BaseQuery> queries = QueryReader.readTrecCdsQueries(MIRPath.TREC_CDS_QUERY_2015_B_FILE);

		for (int i = 0; i < queries.size(); i++) {
			System.out.println(queries.get(i));
			System.out.println();
		}

		System.out.println("process ends.");
	}

	private String id;

	private String description;

	private Query luceneQuery;

	private SparseVector queryModel;

	private List<Integer> words;

	public TrecGenomicsQuery(String id, String description) {
		super();
		this.id = id;
		this.description = description;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrecGenomicsQuery other = (TrecGenomicsQuery) obj;
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
		return true;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	@Override
	public Query getLuceneQuery() {
		return luceneQuery;
	}

	@Override
	public List<Integer> getQueryWords() {
		return words;
	}

	public String getSearchText() {
		String ret = description;
		ret = ret.replaceAll("[\\p{Punct}]+", " ");
		return ret.trim();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void setLuceneQuery(Query luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("qid:\t%s\n", id));
		sb.append(String.format("description:\t%s\n", description));
		return sb.toString();
	}
}
