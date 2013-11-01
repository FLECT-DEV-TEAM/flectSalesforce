package jp.co.flect.salesforce.update;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.query.QueryResult;
import jp.co.flect.xml.XMLWriter;
import jp.co.flect.xml.XMLUtils;

/**
 * create/update/upsertリクエストを作成するWriter
 */
public class ModifyRequestWriter {
	
	private static final String MESSAGE_PREFIX = "tns:";
	private static final String OBJECT_PREFIX  = "ens:";
	
	private String messageUri;
	private String objectUri;
	private String sessionId;
	private boolean isPartner;
	private ModifyRequest request;
	
	private int indent = 0;
	
	public ModifyRequestWriter(String sessionId, ModifyRequest request) {
		this.sessionId = sessionId;
		this.request = request;
		
		SObject obj = request.getObjectList().get(0);
		this.messageUri = obj.getMetadata().getMessageURI();
		this.objectUri = obj.getMetadata().getObjectURI();
		
		this.isPartner = this.messageUri.indexOf("partner") != -1;
	}
	
	public int getIndent() { return this.indent;}
	public void setIndent(int n) { this.indent = n;}
	
	public void writeTo(OutputStream os) throws IOException {
		XMLWriter writer = new XMLWriter(os, "utf-8", this.indent);
		try {
			doWrite(writer);
		} finally {
			writer.close();
		}
	}
	
	public String toString() {
		StringWriter sw = new StringWriter();
		XMLWriter writer = new XMLWriter(sw, "utf-8", this.indent);
		try {
			doWrite(writer);
			writer.close();
			return sw.toString();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private void doWrite(XMLWriter writer) throws IOException {
		writer.xmlDecl();
		writer.indent(false);
		
		writer.openElement("soap:Envelope");
		writer.attr("xmlns:soap", XMLUtils.XMLNS_SOAP_ENVELOPE);
		writer.attr("xmlns:xsd", XMLUtils.XMLNS_XSD);
		writer.attr("xmlns:xsi", XMLUtils.XMLNS_XSI);
		writer.attr("xmlns:" + MESSAGE_PREFIX.substring(0, MESSAGE_PREFIX.length()-1), this.messageUri);
		writer.attr("xmlns:" + OBJECT_PREFIX.substring(0, OBJECT_PREFIX.length()-1), this.objectUri);
		writer.endTag();
		writer.indent(true);
		
		writer.openElement("soap:Header");
		writer.endTag();
		writer.indent(true);
		
		writeHeader(writer, "SessionHeader", "sessionId", this.sessionId);
		if (request.isAllOrNone()) {
			writer.indent(false);
			writeHeader(writer, "AllOrNoneHeader", "allOrNone", "true");
		}
		writer.unindent();
		writer.endElement("soap:Header");
		writer.indent(false);
		writer.openElement("soap:Body");
		writer.endTag();
		writer.indent(true);
		
		String opName = MESSAGE_PREFIX + this.request.getOperationName();
		writer.openElement(opName);
		writer.endTag();
		
		boolean indent = true;
		if ("upsert".equals(this.request.getOperationName())) {
			writer.indent(true);
			String name = MESSAGE_PREFIX + "externalIDFieldName";
			writer.openElement(name);
			writer.endTag();
			writer.content(((UpsertRequest)this.request).getExternalIdField());
			writer.endElement(name);
			indent = false;
		}
		for (SObject obj : this.request.getObjectList()) {
			writer.indent(indent);
			writeObject(writer, obj);
			indent = false;
		}
		writer.unindent();
		writer.endElement(opName);
		writer.unindent();
		writer.endElement("soap:Body");
		writer.unindent();
		writer.endElement("soap:Envelope");
	}
	
	private void writeHeader(XMLWriter writer, String headerName, String childName, String value) throws IOException {
		if (value == null) {
			return;
		}
		headerName = MESSAGE_PREFIX + headerName;
		childName = MESSAGE_PREFIX + childName;
		
		writer.openElement(headerName);
		writer.endTag();
		writer.indent(true);
		
		writer.openElement(childName);
		writer.endTag();
		writer.content(value);
		writer.endElement(childName);
		writer.unindent();
		
		writer.endElement(headerName);
	}
	
	private void writeObject(XMLWriter writer, SObject obj) throws IOException {
		obj.validate();
		
		String sObjects = MESSAGE_PREFIX + "sObjects";
		writer.openElement(sObjects);
		writer.attr("xsi:type", getTypeName(obj));
		writer.endTag();
		
		boolean indent = true;
		if (this.isPartner) {
			writer.indent(indent);
			writeValue(writer, "type", obj.getObjectName());
			indent = false;
		}
		SObjectDef objectDef = obj.getObjectDef();
		Map<String, Object> map = new TreeMap<String, Object>(obj.getMap());
		String id = null;
		//fieldsToNull
		Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			if (name.equals("Id")) {
				id = (String)value;
				it.remove();
				continue;
			}
			if (value instanceof String && value.toString().length() == 0) {
				FieldDef field = objectDef.getField(name);
				if (field != null && isTargetField(field)) {
					writer.indent(indent);
					writeValue(writer, "fieldsToNull", name);
					indent = false;
				}
				it.remove();
			}
		}
		if (id != null) {
			writer.indent(indent);
			writeValue(writer, "Id", id);
			indent = false;
		}
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof QueryResult) {
				continue;
			} else if (value instanceof SObject) {
				SObject child = (SObject)value;
				FieldDef field = objectDef.getSingleRelation(name);
				if (field == null) {
					continue;
				}
				if (map.get(field.getName()) != null) {
					continue;
				}
				if (child.getId() != null) {
					writer.indent(indent);
					writeValue(writer, field.getName(), child.getId());
					indent = false;
				} else {
					FieldDef exField = null;
					for (FieldDef f : child.getObjectDef().getFieldList()) {
						if (f.isExternalId() && child.get(f.getName()) != null) {
							exField = f;
							break;
						}
					}
					if (exField != null) {
						writer.indent(indent);
						writer.openElement(OBJECT_PREFIX + name);
						writer.attr("xsi:type", getTypeName(child));
						writer.endTag();
						writer.indent(true);
						
						if (this.isPartner) {
							writeValue(writer, "type", child.getObjectName());
							writer.indent(false);
						}
						
						writeValue(writer, exField.getName(), exField.getSoapType().format(child.get(exField.getName())));
						writer.unindent();
						writer.endElement(OBJECT_PREFIX + name);
						indent = false;
					}
				}
			} else {
				FieldDef field = objectDef.getField(name);
				if (field != null && isTargetField(field)) {
					writer.indent(indent);
					writeValue(writer, name, field.getSoapType().format(value));
					indent = false;
				}
			}
		}
		if (!indent) {
			writer.unindent();
		}
		writer.endElement(sObjects);
	}
	
	private String getTypeName(SObject obj) {
		return OBJECT_PREFIX + (this.isPartner ? "sObject" : obj.getObjectName());
	}
	
	private void writeValue(XMLWriter writer, String name, String value) throws IOException {
		if (value == null) {
			return;
		}
		name = OBJECT_PREFIX + name;
		writer.openElement(name);
		if (value.length() == 0) {
			writer.attr("xsi:nil", "true");
		}
		writer.endTag();
		writer.content(value);
		writer.endElement(name);
	}
	
	private boolean isTargetField(FieldDef f) {
		String op = this.request.getOperationName();
		if (op.equals("create")) {
			return f.isCreateable();
		} else if (op.equals("update")) {
			return f.isUpdateable();
		} else {//upsert
			return f.isCreateable() && f.isUpdateable();
		}
	}
	
}