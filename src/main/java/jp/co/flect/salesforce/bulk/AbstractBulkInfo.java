package jp.co.flect.salesforce.bulk;

import java.io.StringWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.xml.XMLWriter;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.type.DatetimeType;

public abstract class AbstractBulkInfo {
	
	private String elementName;
	private String[] elementSeq;
	private Map<String, Object> map = new HashMap<String, Object>();
	
	public AbstractBulkInfo(String elementName, String[] elementSeq) {
		this.elementName = elementName;
		this.elementSeq = elementSeq;
	}
	
	protected Object get(String name) { return this.map.get(name);}
	protected void put(String name, Object value) { this.map.put(name, value);}
	
	protected int getInt(String name) {
		Number n = (Number)get(name);
		return n == null ? 0 : n.intValue();
	}
	
	protected long getLong(String name) {
		Number n = (Number)get(name);
		return n == null ? 0 : n.longValue();
	}
	
	public void parse(String str) {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory. IS_COALESCING, Boolean.TRUE);
		try {
			XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(str));
			
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
					case XMLStreamReader.START_ELEMENT:
						if (reader.getLocalName().equals(this.elementName)) {
							parse(reader);
							return;
						}
						break;
				}
			}
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}
	
	//package local
	void parse(XMLStreamReader reader) throws XMLStreamException {
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
					String strValue = reader.getElementText();
					if (strValue != null) {
						String name = reader.getLocalName();
						Object value = parseValue(name, strValue);
						if (value != null) {
							put(name, value);
						}
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					if (reader.getLocalName().equals(this.elementName)) {
						return;
					}
					break;
			}
		}
	}
	
	protected Object parseValue(String name, String value) {
		if (name.startsWith("number")) {
			return Integer.valueOf(value);
		} else if (name.endsWith("Time")) {
			return Long.valueOf(value);
		} else if (name.equals("createdDate") || name.equals("systemModstamp")) {
			return SimpleType.getBuiltinType(DatetimeType.NAME).parse(value);
		}
		return value;
	}
	
	public String toXML(boolean indent) {
		Map<String, Object> temp = new HashMap<String, Object>(this.map);
		
		StringWriter sw = new StringWriter();
		try {
			XMLWriter writer = new XMLWriter(sw);
			writer.setIndent(indent ? 4 : 0);
			
			writer.openElement(this.elementName);
			writer.attr("xmlns", BulkClient.API_SCHEMA);
			writer.endTag();
			
			boolean first = true;
			for (int i=0; i<elementSeq.length; i++) {
				String name = elementSeq[i];
				Object value = temp.remove(name);
				first = writeValue(writer, name, value, first);
			}
			for (Map.Entry<String, Object> entry : temp.entrySet()) {
				String name = entry.getKey();
				Object value = entry.getValue();
				first = writeValue(writer, name, value, first);
			}
			if (!first) {
				writer.unindent();
			}
			writer.endElement(this.elementName);
		} catch (IOException e) {
			//not occur
			throw new IllegalStateException(e);
		}
		return sw.toString();
	}
	
	private boolean writeValue(XMLWriter writer, String name, Object value, boolean first) throws IOException {
		if (value == null) {
			return first;
		}
		if (value instanceof Date) {
			value = SimpleType.getBuiltinType(DatetimeType.NAME).format(value);
		}
		writer.indent(first);
		writer.openElement(name);
		writer.endTag();
		writer.content(value.toString());
		writer.endElement(name);
		return false;
	}
	
	public String toString() {
		return toXML(true);
	}
	
	//共通
	public String getId() { return (String)get("id");}
	public Date getCreatedDate() { return (Date)get("createdDate");}
	public Date getSystemModstamp() { return (Date)get("systemModstamp");}
	
	public int getRecordsProcessed() { return getInt("numberRecordsProcessed");}
	public int getRecordsFailed() { return getInt("numberRecordsFailed");}
	
	public long getTotalProcessingTime() { return getLong("totalProcessingTime");}
	public long getApiActiveProcessingTime() { return getLong("apiActiveProcessingTime");}
	public long getApexProcessingTime() { return getLong("apexProcessingTime");}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		
		if (o instanceof AbstractBulkInfo) {
			AbstractBulkInfo a = (AbstractBulkInfo)o;
			return this.elementName.equals(a.elementName) && this.map.equals(a.map);
		}
		return false;
	}
	
}
