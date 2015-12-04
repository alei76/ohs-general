package ohs.entity.data.struct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Paper implements Serializable {

	private String sid;

	private int id;

	private Author[] authors;

	private BilingualText title;

	private BilingualText abs;

	private Set<String> subjects;

	private ControlNumber cn;

	private PaperSource source;

	public Paper(int id, String sid, ControlNumber cn, Author[] authors, BilingualText title, BilingualText abs, Set<String> subjects,
			PaperSource source) {
		super();
		this.id = id;
		this.sid = sid;
		this.cn = cn;
		this.authors = authors;
		this.title = title;
		this.abs = abs;
		this.subjects = subjects;
		this.source = source;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Paper other = (Paper) obj;
		if (cn == null) {
			if (other.cn != null)
				return false;
		} else if (!cn.equals(other.cn))
			return false;
		return true;
	}

	public BilingualText getAbstract() {
		return abs;
	}

	public Author[] getAuthors() {
		return authors;
	}

	public ControlNumber getControlNumber() {
		return cn;
	}

	public int getId() {
		return id;
	}

	public PaperSource getSource() {
		return source;
	}

	public String getStringId() {
		return sid;
	}

	public Set<String> getSubjects() {
		return subjects;
	}

	public BilingualText getTitle() {
		return title;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cn == null) ? 0 : cn.hashCode());
		return result;
	}

	public void setAbstract(BilingualText abs) {
		this.abs = abs;
	}

	public void setAuthors(Author[] authors) {
		this.authors = authors;
	}

	public void setControlNumber(ControlNumber controlNumber) {
		this.cn = controlNumber;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setSource(PaperSource source) {
		this.source = source;
	}

	public void setStringid(String sid) {
		this.sid = sid;
	}

	public void setSubjects(Set<String> subjects) {
		this.subjects = subjects;
	}

	public void setTitle(BilingualText title) {
		this.title = title;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("SID = %s\n", sid));
		sb.append(String.format("ID = %s\n", id));
		sb.append(String.format("CN1 = %s\n", cn.getCn1()));
		sb.append(String.format("CN2 = %s\n", cn.getCn2()));

		sb.append(String.format("Korean Source = %s\n", source.getName().getKorean()));
		sb.append(String.format("English Source = %s\n", source.getName().getEnglish()));
		sb.append(String.format("Year = %s\n", source.getYear()));

		sb.append(String.format("Korean Title = %s\n", title.getKorean()));
		sb.append(String.format("English Title = %s\n", title.getEnglish()));
		sb.append(String.format("Korean Abstract = %s\n", abs.getKorean() == null ? null : "available"));
		sb.append(String.format("English Abstarct = %s\n", abs.getEnglish() == null ? null : "available"));
		List<String> subs = new ArrayList<String>();

		if (subjects != null) {
			subs.addAll(subjects);
		}

		sb.append("Subjects = ");

		for (int i = 0; i < subs.size(); i++) {
			sb.append(subs.get(i));
			if (i != subs.size() - 1) {
				sb.append(" ");
			}
		}
		sb.append("\n");

		for (int i = 0; i < authors.length; i++) {
			Author author = authors[i];
			Organization org = author.getOrganization();
			sb.append(String.format("Author = %d\n", i + 1));
			sb.append(String.format("Korean Name = %s\n", author.getName().getKorean()));
			sb.append(String.format("English Name = %s\n", author.getName().getEnglish()));
			sb.append(String.format("Korean ORG = %s\n", org.getName().getKorean()));
			sb.append(String.format("English ORG = %s\n", org.getName().getEnglish()));
			sb.append(String.format("E-mail = %s\n", author.getEmail()));
			if (i != authors.length - 1) {
				sb.append("\n");
			}
		}

		return sb.toString();
	}
}
