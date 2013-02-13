package jp.co.flect.salesforce.update;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import jp.co.flect.util.StringUtils;
import static jp.co.flect.util.StringUtils.checkNull;
import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.salesforce.BasicResult;
import jp.co.flect.salesforce.SalesforceException;

public class SaveResult extends BasicResult {
	
	private String id;
	private Boolean created = null;
	
	public SaveResult() {}
	
	public SaveResult(String id, boolean success, boolean created, SalesforceException ex) {
		super(success, ex);
		this.id = id;
		this.created = Boolean.valueOf(created);
	}
	
	public String getId() { return this.id;}
	public boolean isCreated() { return this.created == null ? false : this.created.booleanValue();}
	
	@Override
	public void build(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		super.build(reader);
		for (SalesforceException ex : getErrors()) {
			ex.setId(id);
		}
	}
	
	@Override
	protected boolean startElement(String name, XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		if (super.startElement(name, reader)) {
			return true;
		}
		if ("id".equals(name)) {
			this.id = checkNull(reader.getElementText());
			return true;
		} else if ("created".equals(name)) {
			this.created = Boolean.valueOf(reader.getElementText());
			return true;
		}
		
		return false;
	}
	
	@Override
	public void buildString(StringBuilder buf, int indent) {
		super.buildString(buf, indent);
		String strIndent = StringUtils.getSpace(indent);
		buf.append(strIndent).append("id: ").append(this.id);
		if (this.created != null) {
			buf.append("created: ").append(this.created);
		}
	}
	
	@Override
	public SaveResult newInstance() {
		return new SaveResult();
	}
}
