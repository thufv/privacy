package edu.thu.ss.spec.lang.pojo;

import org.w3c.dom.Node;

/**
 * class for desensitize operation
 * @author luochen
 *
 */
public class DesensitizeOperation implements Parsable {

	protected String name;

	@Override
	public void parse(Node opNode) {
		this.name = opNode.getTextContent();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DesensitizeOperation other = (DesensitizeOperation) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

}
