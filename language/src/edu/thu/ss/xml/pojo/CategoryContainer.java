package edu.thu.ss.xml.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import edu.thu.ss.xml.parser.ParserConstant;
import edu.thu.ss.xml.parser.XMLUtil;

public abstract class CategoryContainer<T extends HierarchicalObject> extends DescribedObject {


	protected String base;

	protected Map<String, T> container = new HashMap<>();
	protected List<T> root = new ArrayList<>();

	public T get(String id) {
		return container.get(id);
	}

	public void set(String id, T category) {
		container.put(id, category);
	}

	public String getBase() {
		return base;
	}

	public Map<String, T> getContainer() {
		return container;
	}

	public List<T> getRoot() {
		return root;
	}

	public void setBase(String base) {
		this.base = base;
	}

	@Override
	public void parse(Node categoryNode) {
		super.parse(categoryNode);

		this.base = XMLUtil.getAttrValue(categoryNode, ParserConstant.Attr_Vocabulary_Base);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (T node : root) {
			toString(node, sb);
			sb.append("\n");
		}
		return sb.toString();
	}

	private void toString(HierarchicalObject node, StringBuilder sb) {
		sb.append("id: ");
		sb.append(node.id);
		if (node.parentId != null) {
			sb.append("\tparentId: ");
			sb.append(node.parentId);
		}
		if (node.children.size() > 0) {
			sb.append("\tchildIds: ");
			for (HierarchicalObject child : node.children) {
				sb.append(child.id);
				sb.append(' ');
			}
		}
		sb.append("\n");
		for (HierarchicalObject child : node.children) {
			toString(child, sb);
		}
	}
}