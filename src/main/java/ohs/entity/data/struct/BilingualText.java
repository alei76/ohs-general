package ohs.entity.data.struct;

import java.io.Serializable;

public class BilingualText implements Serializable {

	private String korean;

	private String english;

	public BilingualText(String korean, String english) {
		super();
		this.korean = korean;
		this.english = english;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BilingualText other = (BilingualText) obj;
		if (english == null) {
			if (other.english != null)
				return false;
		} else if (!english.equals(other.english))
			return false;
		if (korean == null) {
			if (other.korean != null)
				return false;
		} else if (!korean.equals(other.korean))
			return false;
		return true;
	}

	public String getEnglish() {
		return english;
	}

	public String getKorean() {
		return korean;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((english == null) ? 0 : english.hashCode());
		result = prime * result + ((korean == null) ? 0 : korean.hashCode());
		return result;
	}

	public void setEnglish(String english) {
		this.english = english;
	}

	public void setKorean(String korean) {
		this.korean = korean;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(boolean isSimpleFormat) {
		StringBuffer sb = new StringBuffer();
		if (isSimpleFormat) {
			sb.append(String.format("%s\t%s", korean, english));
		} else {
			sb.append(String.format("Korean = %s\n", korean));
			sb.append(String.format("English = %s", english));
		}
		return sb.toString();
	}
}
