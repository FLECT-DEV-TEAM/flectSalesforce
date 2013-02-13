package jp.co.flect.salesforce;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import jp.co.flect.util.StringUtils;
import jp.co.flect.xml.StAXConstruct;
import jp.co.flect.xml.StAXConstructException;

public class BasicResult implements StAXConstruct<BasicResult> {
	
	private List<SalesforceException> errors;
	private boolean success;
	
	public BasicResult() {}
	
	public BasicResult(boolean success, SalesforceException ex) {
		this.success = success;
		if (ex != null) {
			this.errors = new ArrayList<SalesforceException>();
			this.errors.add(ex);
		}
	}
	
	public List<SalesforceException> getErrors() { return this.errors == null ? Collections.<SalesforceException>emptyList() : this.errors;}
	public int getErrorCount() { return this.errors == null ? 0 : this.errors.size();}
	public SalesforceException getError(int idx) { return this.errors == null ? null : this.errors.get(idx);}
	
	public boolean isSuccess() { return this.success;}
	
	public void build(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		int depth = 1;
		String targetNamespace = reader.getNamespaceURI();
		
		boolean finished = false;
		while (!finished && reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
				{
					String nsuri = reader.getNamespaceURI();
					String name = reader.getLocalName();
					if (targetNamespace.equals(nsuri)) {
						if (!startElement(name, reader)) {
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
	
	protected boolean startElement(String name, XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		if ("errors".equals(name)) {
			if (this.errors == null) {
				this.errors = new ArrayList<SalesforceException>();
			}
			SalesforceException err = new SalesforceException();
			err.build(reader);
			this.errors.add(err);
			return true;
		} else if ("success".equals(name)) {
			this.success = Boolean.valueOf(reader.getElementText()).booleanValue();
			return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buildString(buf, 0);
		return buf.toString();
	}
	
	public void buildString(StringBuilder buf, int indent) {
		String strIndent = StringUtils.getSpace(indent);
		if (this.errors != null && this.errors.size() > 0) {
			buf.append(strIndent).append("errors:");
			for (SalesforceException err : this.errors) {
				buf.append("\n");
				err.buildString(buf, indent + 2);
			}
			buf.append("\n");
		}
		buf.append(strIndent).append("success: ").append(this.success);
	}
	
	public BasicResult newInstance() {
		return new BasicResult();
	}
}
