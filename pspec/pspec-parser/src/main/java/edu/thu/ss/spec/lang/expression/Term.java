package edu.thu.ss.spec.lang.expression;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.thu.ss.spec.lang.parser.ParserConstant;
import edu.thu.ss.spec.lang.pojo.DataCategory;

public class Term extends Expression<DataCategory> {
	public enum TermTypes {
		dataCategory, value
	}
	
	private TermTypes termType;
	private DataCategory dataCategory;
	private String data;
	
	public Term() {
		super.ExpressionType = ExpressionTypes.term;
	}
	
	public TermTypes getTermType() {
		return termType;
	}
	
	public DataCategory getDataCategory() {
		return dataCategory;
	}
	
	public String getData() {
		return data;
	}
	
	@Override
	public Set<DataCategory> getDataSet() {
		if (dataSet != null) {
			return dataSet;
		}
		Set<DataCategory> set = new HashSet<>();
		if (termType.equals(TermTypes.dataCategory)) {
			set.add(dataCategory);
		}
		dataSet = set;
		return dataSet;
	}

	@Override
	public void parse(Node tNode) {
		// TODO Auto-generated method stub
		NodeList list = tNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String name = node.getLocalName();
			if (ParserConstant.Ele_Policy_Rule_Value.equals(name)) {
				data = node.getTextContent();
				termType = TermTypes.value;
				return;
			}
			else if (ParserConstant.Ele_Vocabulary_Data_Category.equals(name)) {
				dataCategory = new DataCategory();
				dataCategory.parse(node);
				termType = TermTypes.dataCategory;
				return;
			}
		}
		
	}

	@Override
	public Element outputType(Document document, String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element outputElement(Document document) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (termType.equals(TermTypes.value)) {
			sb.append(data);
		}
		else if (termType.equals(TermTypes.dataCategory)) {
			sb.append(dataCategory.getId());
		}
		return sb.toString();
	}
}
