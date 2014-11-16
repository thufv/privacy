package edu.thu.ss.spec.lang.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import edu.thu.ss.spec.lang.analyzer.CategoryVisitor;
import edu.thu.ss.spec.lang.parser.ParserConstant;
import edu.thu.ss.spec.lang.pojo.HierarchicalObject;
import edu.thu.ss.spec.util.XMLUtil;

public abstract class XMLCategoryContainer<T extends HierarchicalObject<T>> extends XMLDescribedObject {

	protected String base;

	protected List<T> categories = new LinkedList<>();
	protected Map<String, T> index = new HashMap<>();
	protected List<T> root = new ArrayList<>();
	protected Object[] labelIndex;

	public void accept(CategoryVisitor<T> visitor) {
		for (T t : root) {
			visitor.visit(t);
		}
	}

	public void buildLabels() {
		labelIndex = new Object[categories.size()];
		for (T t : categories) {
			labelIndex[t.getLabel()] = t;
		}
	}

	public T getCategory(int label) {
		return (T) labelIndex[label];
	}

	public T get(String id) {
		return index.get(id);
	}

	public void set(String id, T category) {
		index.put(id, category);
		categories.add(category);
	}

	public String getBase() {
		return base;
	}

	public List<T> getCategories() {
		return categories;
	}

	public Map<String, T> getIndex() {
		return index;
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

	private void toString(T node, StringBuilder sb) {
		sb.append(node.toFullString());
		sb.append("\n");
		if (node.getChildren() != null) {
			for (T child : node.getChildren()) {
				toString(child, sb);
			}
		}

	}
}