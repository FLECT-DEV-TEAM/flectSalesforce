package jp.co.flect.salesforce;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import jp.co.flect.util.StringUtils;
import static jp.co.flect.util.StringUtils.checkNull;
import jp.co.flect.xml.StAXConstruct;
import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.soap.SoapException;

public class SalesforceException extends SoapException implements StAXConstruct<SalesforceException> {
	
	private boolean serverGenerated;
	private List<String> fields = new ArrayList<String>();
	private String message;
	private String statusCode;
	private String id;
	
	public SalesforceException() {
		super("");
	}
	
	public SalesforceException(String msg) {
		super(msg);
		this.message = msg;
	}
	
	public SalesforceException(String msg, Exception e) {
		super(msg, e);
		this.message = msg;
	}
	
	public SalesforceException(Exception e) {
		super(e);
		this.message = e.toString();
	}
	
	public SalesforceException(String msg, StatusCode code) {
		this(msg, code, null);
	}
	
	public SalesforceException(String msg, StatusCode code, List<String> fields) {
		super(msg);
		this.message = msg;
		if (code != null) {
			this.statusCode = code.toString();
		}
		if (fields != null) {
			this.fields = fields;
		}
	}
	
	public boolean isServerGenerated() { return serverGenerated;}
	
	public int getFieldCount() { return this.fields.size();}
	public String getField() { return this.fields.size() > 0 ? getField(0) : null;}
	public String getField(int idx) { return this.fields.get(idx);}
	public List<String> getFields() { return this.fields;}
	
	public void addField(String s) { this.fields.add(s);}
	
	public String getId() { return this.id;}
	public void setId(String s) { this.id = s;}
	
	@Override
	public String getMessage() { return this.message;}
	
	public String getStatusCode() { return this.statusCode;}
	
	public void build(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		this.serverGenerated = true;
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
						if ("fields".equals(name)) {
							String value = checkNull(reader.getElementText());
							if (value != null) {
								this.fields.add(value);
							}
						} else if ("message".equals(name)) {
							this.message = checkNull(reader.getElementText());
						} else if ("statusCode".equals(name)) {
							this.statusCode = checkNull(reader.getElementText());
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
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buildString(buf, 0);
		return buf.toString();
	}
	
	public void buildString(StringBuilder buf, int indent) {
		String strIndent = StringUtils.getSpace(indent);
		if (this.id != null) {
			buf.append(strIndent).append("id:").append(this.id).append("\n");
		}
		if (this.fields.size() > 0) {
			buf.append(strIndent).append("fields:");
			for (String s : this.fields) {
				buf.append("\n").append(strIndent).append("  ").append(s);
			}
			buf.append("\n");
		}
		buf.append(strIndent).append("message: ").append(this.message)
			.append("\n").append(strIndent).append("statusCode: ").append(this.statusCode);
	}
	
	public SalesforceException newInstance() {
		return new SalesforceException();
	}
}
