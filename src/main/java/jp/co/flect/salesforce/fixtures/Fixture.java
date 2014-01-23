package jp.co.flect.salesforce.fixtures;

import java.util.Map;
import java.util.HashMap;

import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SalesforceException;
import jp.co.flect.salesforce.exceptions.ObjectNotFoundException;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.soap.SoapException;
import java.io.IOException;

public class Fixture {
	
	private String name;
	private String objectName;
	private String keyField;
	private String keyValue;
	private String desc;
	private String id;
	
	private boolean deletable = true;
	
	private Map<String, String> values = new HashMap<String, String>();
	private Map<String, Object> props = new HashMap<String, Object>();
	
	private boolean normalized = false;
	private Map<String, String> unknowns = null;
	
	public Fixture(String name, String objectName, String keyField, String keyValue) {
		this.name = name;
		this.objectName = objectName;
		this.keyField = keyField;
		this.keyValue = keyValue;
	}
	
	public String getName() { return this.name;}
	public String getObjectName() { return this.objectName;}
	public String getKeyField() { return this.keyField;}
	public String getKeyValue() { return this.keyValue;}
	
	public String getDescription() { return this.desc;}
	public void setDescription(String v) { this.desc = v;}
	
	public boolean canDelete() { return this.deletable;}
	public void setCanDelete(boolean b) { this.deletable = b;}
	
	public void addFieldValue(String name, String value) {
		if (value == null) {
			value = "";
		}
		this.values.put(name, value);
	}
	
	public Map<String, String> getFieldValues() { return this.values;}
	public String getFieldValue(String name) {
		return this.values.get(name);
	}
	
	public void addProperty(String name, Object value) {
		this.props.put(name, value);
	}
	
	public Object getProperty(String name) {
		return this.props.get(name);
	}
	
	public Map<String, String> getUnknowns() { return this.unknowns;}
	
	public boolean isNormalized() { return this.normalized;}
	
	String getId() { return this.id;}
	void setId(String v) { this.id = v;}
	
	public Fixture normalize(SalesforceClient client) throws IOException, SoapException, ObjectNotFoundException {
		Map<String, String> errors = new HashMap<String, String>();
		SObjectDef objectDef = client.getValidObjectDef(objectName);
		if (objectDef == null) {
			throw new ObjectNotFoundException(objectName);
		}
		String key = getValidFieldName(objectDef, keyField);
		if (key == null) {
			errors.put(keyField, keyValue);
			key = keyField;
		}
		Fixture newFx = new Fixture(this.name, objectDef.getName(), key, this.keyValue);
		newFx.desc = this.desc;
		newFx.id = this.id;
		newFx.deletable = this.deletable;
		newFx.props.putAll(this.props);
		newFx.normalized = true;
		
		for (Map.Entry<String, String> entry : this.values.entrySet()) {
			String name = entry.getKey();
			String newName = null;
			int idx = name.indexOf('.');
			if (idx != -1) {
				String cName = name.substring(0, idx);
				String cField = name.substring(idx + 1);
				FieldDef field = objectDef.getSingleRelation(cName);
				if (field != null) {
					SObjectDef childDef = client.getValidObjectDef(field.getReferenceToName());
					if (childDef != null) {
						cName = field.getRelationshipName();
						cField = getValidFieldName(childDef, cField);
						if (cField != null) {
							newName = cName + "." + cField;
						}
					}
				}
			} else {
				newName = getValidFieldName(objectDef, name);
			}
			if (newName == null) {
				errors.put(name, entry.getValue());
			} else {
				newFx.addFieldValue(newName, entry.getValue());
			}
		}
		if (errors.size() > 0) {
			newFx.unknowns = errors;
		}
		return newFx;
	}
	
	private String getValidFieldName(SObjectDef objectDef, String name) {
		FieldDef fd = objectDef.getField(name);
		return fd == null ? null : fd.getName();
	}
}
