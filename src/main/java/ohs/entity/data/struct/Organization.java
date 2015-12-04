package ohs.entity.data.struct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ohs.io.TextFileReader;

public class Organization implements Serializable {

	public static List<Organization> read(String fileName) {
		List<Organization> ret = new ArrayList<Organization>();

		TextFileReader reader = new TextFileReader(fileName);
		while (reader.hasNext()) {
			if (reader.getNumLines() == 1) {
				continue;
			}
			String line = reader.next();
		}
		reader.close();

		return ret;
	}

	private String sid;

	private BilingualText name;

	private Set<String> korVariants;

	private Set<String> engVariants;

	private int year;

	private List<Organization> history;

	private int id;

	private OrganizationType type;

	private String homepage;

	public Organization(int id, String sid, BilingualText name) {
		this(id, sid, name, 0, OrganizationType.NONE, new HashSet<String>(), new HashSet<String>(), new ArrayList<Organization>(), null);
	}

	public Organization(int id, String sid, BilingualText name, int year, OrganizationType type,

	Set<String> korVariants, Set<String> engVariants, List<Organization> history, String homepage) {
		super();
		this.id = id;
		this.sid = sid;
		this.name = name;
		this.year = year;
		this.type = type;
		this.korVariants = korVariants;
		this.engVariants = engVariants;
		this.history = history;
		this.homepage = homepage;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Organization other = (Organization) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (sid == null) {
			if (other.sid != null)
				return false;
		} else if (!sid.equals(other.sid))
			return false;
		return true;
	}

	public Set<String> getEnglishVariants() {
		return engVariants;
	}

	public List<Organization> getHistory() {
		return history;
	}

	public String getHomepage() {
		return homepage;
	}

	public int getId() {
		return id;
	}

	public Set<String> getKoreanVariants() {
		return korVariants;
	}

	public BilingualText getName() {
		return name;
	}

	public String getStringId() {
		return sid;
	}

	public OrganizationType getType() {
		return type;
	}

	public int getYear() {
		return year;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sid == null) ? 0 : sid.hashCode());
		return result;
	}

	public void setEnglishVariants(Set<String> engVariants) {
		this.engVariants = engVariants;
	}

	public void setHistory(List<Organization> history) {
		this.history = history;
	}

	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setKoreanVariants(Set<String> korVariants) {
		this.korVariants = korVariants;
	}

	public void setName(BilingualText name) {
		this.name = name;
	}

	public void setStringId(String sid) {
		this.sid = sid;
	}

	public void setType(OrganizationType type) {
		this.type = type;
	}

	public void setYear(int year) {
		this.year = year;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		// sb.append(String.format("ID = %d\n", id));
		// sb.append(String.format("SID = %s\n", sid));
		sb.append(String.format("Korean Name = %s\n", name.getKorean()));
		// sb.append(String.format("English Name = %s\n", name.getEnglish()));
		sb.append(String.format("Year = %d\n", year));
		// sb.append(String.format("Korean Variants = %s\n", korVariants));
		// sb.append(String.format("English Variants = %s\n", engVariants));
		// sb.append(String.format("Homepage = %s\n", homepage));
		sb.append(String.format("History\n"));
		for (int i = 0; i < history.size(); i++) {
			Organization org = history.get(i);
			sb.append(String.format("%d\t%s\t%d\n", i + 1, org.getName().getKorean(), org.getYear()));
		}
		return sb.toString().trim();
	}

}
