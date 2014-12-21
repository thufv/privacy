package edu.thu.ss.spec.lang.pojo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.thu.ss.spec.lang.parser.ParserConstant;
import edu.thu.ss.spec.util.XMLUtil;

/**
 * class for restriction, contains multiple {@link Desensitization}
 * @author luochen
 *
 */
public class Restriction implements Parsable {

	private List<Desensitization> list = new ArrayList<>();

	private Desensitization[] desensitizations;

	private boolean forbid = false;

	public void setForbid(boolean forbid) {
		this.forbid = forbid;
	}

	public boolean isForbid() {
		return forbid;
	}

	public List<Desensitization> getList() {
		return list;
	}

	public void setDesensitizations(Desensitization[] desensitizations) {
		this.desensitizations = desensitizations;
	}

	public Desensitization[] getDesensitizations() {
		return desensitizations;
	}

	public Desensitization getDesensitization() {
		return desensitizations[0];
	}

	@Override
	public Restriction clone() {
		Restriction res = new Restriction();
		res.forbid = this.forbid;
		if (this.desensitizations != null) {
			res.desensitizations = new Desensitization[desensitizations.length];
			for (int i = 0; i < res.desensitizations.length; i++) {
				res.desensitizations[i] = desensitizations[i].clone();
			}
		}
		return res;
	}

	@Override
	public void parse(Node resNode) {
		NodeList list = resNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String name = node.getLocalName();
			if (ParserConstant.Ele_Policy_Rule_Desensitize.equals(name)) {
				parseDesensitization(node);
			} else if (ParserConstant.Ele_Policy_Rule_Forbid.equals(name)) {
				forbid = true;
			}
		}
	}

	public void parseDesensitization(Node deNode) {
		Set<String> dataRefIds = new HashSet<String>();
		Set<DesensitizeOperation> operations = new LinkedHashSet<>();
		NodeList list = deNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String name = node.getLocalName();
			if (ParserConstant.Ele_Policy_Rule_DataRef.equals(name)) {
				String refid = XMLUtil.getAttrValue(node, ParserConstant.Attr_Refid);
				dataRefIds.add(refid);
			} else if (ParserConstant.Ele_Policy_Rule_Desensitize_Operation.equals(name)) {
				DesensitizeOperation op = DesensitizeOperation.parse(node);
				operations.add(op);
			}
		}
		if (dataRefIds.size() == 0) {
			Desensitization de = new Desensitization();
			de.setOperations(operations);
			this.list.add(de);
		} else {
			for (String refId : dataRefIds) {
				Desensitization de = new Desensitization();
				de.setOperations(operations);
				de.setDataRefId(refId);
				this.list.add(de);
			}
		}

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Restriction: ");
		if (forbid) {
			sb.append("forbid");
		} else {
			for (Desensitization de : desensitizations) {
				sb.append("{");
				sb.append(de);
				sb.append("} ");
			}
		}
		return sb.toString();
	}
}
