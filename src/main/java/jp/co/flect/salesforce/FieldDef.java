package jp.co.flect.salesforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.TypeDef;

public class FieldDef extends AbstractDef {
	
	private static final long serialVersionUID = 4822415064903856131L;
	
	private transient SimpleType soapType;
	
	public FieldDef(Metadata meta) {
		super(meta);
	}
	
	protected ComplexType getTypeFromMetadata() {
		return (ComplexType)getMetadata().getMessageType("Field");
	}
	
	public String getName() { return getString("name");}
	public String getLabel() { return getString("label");}
	
	public String getRelationshipName() { return getString("relationshipName");}
	public List<String> getReferenceTo() { 
		Object o = get("referenceTo");
		if (o == null) {
			return Collections.<String>emptyList();
		}
		List<String> list = new ArrayList<String>();
		if (o instanceof String) {
			list.add(o.toString());
		} else if (o instanceof List) {
			for (Object value : (List)o) {
				list.add(value.toString());
			}
		} else {
			throw new IllegalStateException();
		}
		return list;
	}
	
	public String getReferenceToName() {
		Object o = get("referenceTo");
		if (o == null) {
			return null;
		}
		if (o instanceof String) {
			return o.toString();
		} else if (o instanceof List) {
			return "Name";
		} else {
			throw new IllegalStateException();
		}
	}
	
	public boolean isExternalId() { return getBoolean("externalId");}
	
	public boolean isCreateable() { return getBoolean("createable");}
	public boolean isUpdateable() { return getBoolean("updateable");}
	
	public boolean isNillable() { return getBoolean("nillable");}
	public boolean isRequired() { return !isNillable();}
	
	public String getFieldType() { return getString("type");}
	
	public SimpleType getSoapType() {
		if (this.soapType != null) {
			return this.soapType;
		}
		String strType = getString("soapType");
		if (strType == null) {
			return null;
		}
		strType = strType.substring(strType.indexOf(':') + 1);
		TypeDef type = getMetadata().getMessageType(strType);
		if (type != null) {
			this.soapType = (SimpleType)type;
		} else {
			this.soapType = SimpleType.getBuiltinType(strType);
		}
		return this.soapType;
	}
	
	@Override
	public FieldDef newInstance() {
		return (FieldDef)super.newInstance();
	}
	
	public SalesforceException validate(Object value) {
		//ToDo データ型チェック
		return null;
	}
	
	@Override
	protected boolean startElement(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		String name = reader.getLocalName();
		if (name.equals("picklistValues")) {
			List<PicklistEntry> list = (List<PicklistEntry>)get("picklistValues");
			if (list == null) {
				list = new ArrayList<PicklistEntry>();
				set("picklistValues", list);
			}
			PicklistEntry pl = new PicklistEntry(getMetadata());
			pl.build(reader);
			list.add(pl);
			return true;
		} else {
			return super.startElement(reader);
		}
	}
}
