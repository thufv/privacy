package edu.thu.ss.xml.pojo;

import org.w3c.dom.Node;

import edu.thu.ss.xml.parser.ParserConstant;
import edu.thu.ss.xml.parser.XMLUtil;

public class ReferringObject {
	protected String refid;

	public void parse(Node node) {

		this.refid = XMLUtil.getAttrValue(node, ParserConstant.Ele_Policy_Refid);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((refid == null) ? 0 : refid.hashCode());
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
		ReferringObject other = (ReferringObject) obj;
		if (refid == null) {
			if (other.refid != null)
				return false;
		} else if (!refid.equals(other.refid))
			return false;
		return true;
	}

}
