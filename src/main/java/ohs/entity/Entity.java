package ohs.entity;

import java.io.Serializable;

public class Entity implements Serializable {

	private String text;

	private int id;

	private String topic;

	public Entity(int id, String text, String topic) {
		this.id = id;
		this.text = text;
		this.topic = topic;
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
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		return true;
	}

	public int getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	public String getTopic() {
		return topic;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		return result;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	@Override
	public String toString() {
		return "Entity [text=" + text + ", id=" + id + ", topic=" + topic + "]";
	}
}
