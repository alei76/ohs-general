package ohs.entity.data.struct;

import java.io.Serializable;

public class ControlNumber implements Serializable {
	private String cn1;
	private String cn2;

	public ControlNumber(String cn1, String cn2) {
		super();
		this.cn1 = cn1;
		this.cn2 = cn2;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ControlNumber other = (ControlNumber) obj;
		if (cn1 == null) {
			if (other.cn1 != null)
				return false;
		} else if (!cn1.equals(other.cn1))
			return false;
		if (cn2 == null) {
			if (other.cn2 != null)
				return false;
		} else if (!cn2.equals(other.cn2))
			return false;
		return true;
	}

	public String getCn1() {
		return cn1;
	}

	public String getCn2() {
		return cn2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cn1 == null) ? 0 : cn1.hashCode());
		result = prime * result + ((cn2 == null) ? 0 : cn2.hashCode());
		return result;
	}

	public void setCn1(String cn1) {
		this.cn1 = cn1;
	}

	public void setCn2(String cn2) {
		this.cn2 = cn2;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("CN1 = %s\n", cn1));
		sb.append(String.format("CN2 = %s\n", cn2));
		return sb.toString();
	}

}
