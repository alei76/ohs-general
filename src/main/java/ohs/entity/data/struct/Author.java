package ohs.entity.data.struct;

import java.io.Serializable;

public class Author implements Serializable {

	private String sid;

	private BilingualText name;

	private Organization org;

	private String email;

	private int id;

	public Author(int id, String sid, BilingualText name, Organization org, String email) {
		super();
		this.id = id;
		this.sid = sid;
		this.name = name;
		this.org = org;
		this.email = email;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Author other = (Author) obj;
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

	public String getEmail() {
		return email;
	}

	public int getId() {
		return id;
	}

	public BilingualText getName() {
		return name;
	}

	public Organization getOrganization() {
		return org;
	}

	public String getStringId() {
		return sid;
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

	public void setEmail(String email) {
		this.email = email;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(BilingualText name) {
		this.name = name;
	}

	public void setOrganization(Organization organization) {
		this.org = organization;
	}

	public void setStringId(String sid) {
		this.sid = sid;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("ID = %s\n", id));
		sb.append(String.format("SID = %s\n", sid));
		sb.append(String.format("Korean Name = %s\n", name.getKorean()));
		sb.append(String.format("English Name = %s\n", name.getEnglish()));
		sb.append(String.format("Korean ORG = %s\n", org.getName().getKorean()));
		sb.append(String.format("English ORG = %s\n", org.getName().getEnglish()));
		sb.append(String.format("E-mail = %s\n", email));
		return sb.toString();
	}

}
