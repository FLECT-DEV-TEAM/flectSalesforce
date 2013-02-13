package jp.co.flect.salesforce.search;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.util.StringUtils;
import jp.co.flect.xml.StAXConstruct;
import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.Metadata;

public class SearchResult<T extends SObject> implements StAXConstruct<SearchResult<T>>, Cloneable {
	
	private Metadata meta;
	private T baseObject;
	
	private List<T> records = new ArrayList<T>();
	
	public SearchResult(Metadata meta, T obj) {
		this.meta = meta;
		this.baseObject = obj;
	}
	
	private void addBaseObject() {
		this.records.add(this.baseObject);
	}
	
	public List<T> getRecords() { return this.records;}
	public T getRecord(int idx) { return this.records.get(idx);}
	
	public int size() { return this.records.size();}
	
	public void build(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		int depth = 1;
		String targetNamespace = this.meta.getMessageURI();
		
		boolean finished = false;
		while (!finished && reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
				{
					String nsuri = reader.getNamespaceURI();
					String name = reader.getLocalName();
					if (targetNamespace.equals(nsuri)) {
						if ("record".equals(name)) {
							T obj = (T)this.baseObject.newInstance();
							obj.build(reader);
							this.records.add(obj);
						} else {
							depth++;
						}
					} else {
						depth++;
					}
					break;
				}
				case XMLStreamReader.END_ELEMENT:
				{
					depth--;
					if (depth == 0) {
						finished = true;
					}
					break;
				}
			}
		}
	}
	
	public SearchResult<T> newInstance() {
		try {
			return (SearchResult<T>)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buildString(buf, 0);
		return buf.toString();
	}
	
	public void buildString(StringBuilder buf, int indent) {
		String strIndent = StringUtils.getSpace(indent);
		buf.append(strIndent).append("size: ").append(this.records.size());
		for (T obj : this.records) {
			buf.append("\n").append(strIndent).append("records:");
			obj.buildString(buf, indent + 2);
		}
	}
	
}
