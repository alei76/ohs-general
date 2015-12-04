package ohs.entity.data.struct;

public class PaperSource {

	private BilingualText name;

	private String year;

	public PaperSource(BilingualText name, String year) {
		super();
		this.name = name;
		this.year = year;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PaperSource other = (PaperSource) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (year == null) {
			if (other.year != null)
				return false;
		} else if (!year.equals(other.year))
			return false;
		return true;
	}

	public BilingualText getName() {
		return name;
	}

	public String getYear() {
		return year;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((year == null) ? 0 : year.hashCode());
		return result;
	}

	public void setName(BilingualText name) {
		this.name = name;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("Korean Name= %s\n", name.getKorean()));
		sb.append(String.format("English Name= %s\n", name.getKorean()));
		sb.append(String.format("Year = %s", year));
		return sb.toString();
	}

}
