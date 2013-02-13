package jp.co.flect.salesforce;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.util.StringUtils;

public class SObjectDef extends AbstractDef {
	
	private static final long serialVersionUID = -3648056483769064898L;
	
	private static final Map<String, String> fieldShortNameMap = new HashMap<String, String>();
	
	static {
		fieldShortNameMap.put("ContentType".toLowerCase(),  "CT");
		fieldShortNameMap.put("CreatedById".toLowerCase(), "CI");
		fieldShortNameMap.put("CreatedDate".toLowerCase(), "CD");
		fieldShortNameMap.put("Description".toLowerCase(), "D");
		fieldShortNameMap.put("IsDeleted".toLowerCase(),  "I");
		fieldShortNameMap.put("LastModifiedById".toLowerCase(), "LI");
		fieldShortNameMap.put("LastModifiedDate".toLowerCase(),  "LD");
		fieldShortNameMap.put("Name".toLowerCase(), "N");
		fieldShortNameMap.put("OwnerId".toLowerCase(), "O");
		fieldShortNameMap.put("ParentId".toLowerCase(), "P");
		fieldShortNameMap.put("SystemModstamp".toLowerCase(), "S");
		fieldShortNameMap.put("Type".toLowerCase(), "T");
	}
	
	private List<RelationDef> relList;
	private Map<String, FieldDef> fieldMap;
	private List<RecordTypeDef> recordTypeList;
	
	private transient Map<String, FieldDef> singleRelMap;
	private transient Map<String, RelationDef> relMap;
	
	public SObjectDef(Metadata meta) {
		super(meta);
	}
	
	protected ComplexType getTypeFromMetadata() {
		return (ComplexType)getMetadata().getMessageType("DescribeSObjectResult");
	}
	
	public String getNamespace() { return getMetadata().getObjectURI();}
	
	public String getName() { return getString("name");}
	public String getLabel() { return getString("label");}
	
	public FieldDef getField(String name) { 
		if (name == null || this.fieldMap == null) {
			return null;
		}
		String key = name.toLowerCase();
		String shortKey = fieldShortNameMap.get(key);
		if (shortKey != null) {
			key = shortKey;
		}
		return this.fieldMap.get(key);
	}
	
	public RelationDef getMultipleRelation(String name) {
		if (name == null || this.relMap == null) {
			return null;
		}
		return this.relMap.get(name.toLowerCase());
	}
	
	public FieldDef getSingleRelation(String name) {
		if (name == null || this.singleRelMap == null) {
			return null;
		}
		return this.singleRelMap.get(name.toLowerCase());
	}
	
	public List<RelationDef> getRelationList() {
		if (this.relList == null) {
			return Collections.<RelationDef>emptyList();
		}
		return new ArrayList<RelationDef>(this.relList);
	}
	
	
	public List<FieldDef> getFieldList() {
		if (fieldMap == null) {
			return Collections.<FieldDef>emptyList();
		}
		List list = new ArrayList<FieldDef>(fieldMap.values());
		Collections.sort(list, new Comparator<FieldDef>() {
			public int compare(FieldDef f1, FieldDef f2) {
				return f1.getName().compareTo(f2.getName());
			}
		});
		return list;
	}
	
	public List<RecordTypeDef> getRecordTypeList() {
		return this.recordTypeList == null ? Collections.<RecordTypeDef>emptyList() : this.recordTypeList;
	}
	
	public FieldDef getExternalIdField() {
		if (fieldMap == null) {
			return null;
		}
		for (FieldDef f : fieldMap.values()) {
			if (f.isExternalId()) {
				return f;
			}
		}
		return null;
	}
	
	@Override
	protected boolean startElement(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		Metadata meta = getMetadata();
		String name = reader.getLocalName();
		if (name.equals("fields")) {
			if (fieldMap == null) {
				fieldMap = new HashMap<String, FieldDef>();
			}
			FieldDef field = new FieldDef(meta);
			field.build(reader);
			
			String key = field.getName().toLowerCase();
			String shortKey = fieldShortNameMap.get(key);
			if (shortKey != null) {
				key = shortKey;
			}
			fieldMap.put(key, field);
			if (field.getRelationshipName() != null) {
				if (singleRelMap == null) {
					singleRelMap = new HashMap<String, FieldDef>();
				}
				singleRelMap.put(field.getRelationshipName().toLowerCase(), field);
			}
		} else if (name.equals("childRelationships")) {
			if (relList == null) {
				relList = new ArrayList<RelationDef>();
				relMap = new HashMap<String, RelationDef>();
			}
			RelationDef rel = new RelationDef(meta);
			rel.build(reader);
			relList.add(rel);
			if (rel.getRelationshipName() != null) {
				relMap.put(rel.getRelationshipName().toLowerCase(), rel);
			}
		} else if (name.equals("recordTypeInfos")) {
			if (recordTypeList == null) {
				recordTypeList = new ArrayList<RecordTypeDef>();
			}
			RecordTypeDef obj = new RecordTypeDef(meta);
			obj.build(reader);
			recordTypeList.add(obj);
		} else {
			return super.startElement(reader);
		}
		return true;
	}
	
	@Override
	public void buildString(StringBuilder buf, int indent) {
		super.buildString(buf, indent);
		String strIndent = StringUtils.getSpace(indent);
		if (this.relList != null) {
			for (RelationDef rel : this.relList) {
				buf.append("\n");
				buf.append(strIndent).append("childRelationships:");
				rel.buildString(buf, indent + 2);
			}
		}
		if (this.fieldMap != null) {
			for (FieldDef field : getFieldList()) {
				buf.append("\n");
				buf.append(strIndent).append("fields:");
				field.buildString(buf, indent + 2);
			}
		}
		if (this.recordTypeList != null) {
			for (RecordTypeDef obj : this.recordTypeList) {
				buf.append("\n");
				buf.append(strIndent).append("recordTypeInfos:");
				obj.buildString(buf, indent + 2);
			}
		}
	}
	
	@Override
	public SObjectDef newInstance() {
		return (SObjectDef)super.newInstance();
	}
	
	public boolean isComplete() {
		return this.fieldMap != null;
	}
	
	public String getQueryString() {
		StringBuilder buf = new StringBuilder();
		buf.append("SELECT Id");
		Iterator<FieldDef> it = this.fieldMap.values().iterator();
		while (it.hasNext()) {
			FieldDef f = it.next();
			if (f.getName().equals("Id")) {
				continue;
			}
			buf.append(",").append(f.getName());
		}
		buf.append(" FROM ").append(getName());
		return buf.toString();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (this.fieldMap != null) {
			for (FieldDef field : this.fieldMap.values()) {
				if (field.getRelationshipName() != null) {
					if (this.singleRelMap == null) {
						this.singleRelMap = new HashMap<String, FieldDef>();
					}
					this.singleRelMap.put(field.getRelationshipName().toLowerCase(), field);
				}
			}
		}
		if (this.relList != null) {
			for (RelationDef rel : this.relList) {
				if (rel.getRelationshipName() != null) {
					if (this.relMap == null) {
						this.relMap = new HashMap<String, RelationDef>();
					}
					relMap.put(rel.getRelationshipName().toLowerCase(), rel);
				}
			}
		}
	}
	
}
