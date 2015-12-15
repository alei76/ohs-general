package ohs.entity;

import java.io.Serializable;
import java.util.Set;

import ohs.entity.data.struct.BilingualText;

public class Entity implements Serializable {

	private static final long serialVersionUID = 6089009132374704108L;

	private BilingualText name;

	private Set<String> korVariants;

	private Set<String> engVariants;

	private int id;

	public Entity(int id, BilingualText name, Set<String> korVariants, Set<String> engVariants) {
		super();
		this.id = id;
		this.name = name;
		this.korVariants = korVariants;
		this.engVariants = engVariants;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public Set<String> getEnglishVariants() {
		return engVariants;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public void setEnglishVariants(Set<String> engVariants) {
		this.engVariants = engVariants;
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

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		// sb.append(String.format("ID = %d\n", id));
		// sb.append(String.format("SID = %s\n", sid));
		sb.append(String.format("Korean Name = %s\n", name.getKorean()));
		// sb.append(String.format("English Name = %s\n", name.getEnglish()));
		// sb.append(String.format("Korean Variants = %s\n", korVariants));
		// sb.append(String.format("English Variants = %s\n", engVariants));
		// sb.append(String.format("Homepage = %s\n", homepage));
		return sb.toString().trim();
	}

}
