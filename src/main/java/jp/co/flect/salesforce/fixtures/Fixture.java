package jp.co.flect.salesforce.fixtures;

import java.util.Map;
import java.util.HashMap;

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
	
	String getId() { return this.id;}
	void setId(String v) { this.id = v;}
	
}
