package jp.co.flect.salesforce.bulk;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

public class SObjectSyncRequest {
	
	private Connection con;
	private String tableName;
	private String objectName;
	private String where;
	private String[] keys;
	private Object[] params;
	private LinkedHashMap<String, String> fieldMapping = new LinkedHashMap<String, String>();
	
	public SObjectSyncRequest(Connection con, String objectName, String tableName) {
		this.con = con;
		this.objectName = objectName;
		this.tableName = tableName;
	}
	
	public Connection getConnection() { return this.con;}
	public String getObjectName() { return this.objectName;}
	public String getTableName() { return this.tableName;}
	
	public String getWhere() { return this.where;}
	public void setWhere(String s) { this.where = s;}
	
	public String[] getKeyColumns() { return this.keys;}
	public void setKeyColumns(String... keys) { this.keys = keys;}
	
	public Object[] getParams() { return this.params;}
	public void setParams(Object... params) { this.params = params;}
	
	public void addFieldMapping(String objectFieldName, String tableFieldName) {
		this.fieldMapping.put(objectFieldName, tableFieldName);
	}
	
	public List<String> getObjectFieldList() { return new ArrayList<String>(this.fieldMapping.keySet());}
	public List<String> getTableColumnList() { return new ArrayList<String>(this.fieldMapping.values());}
	
	public String getMappedTableColumn(String objectFieldName) { return this.fieldMapping.get(objectFieldName);}
}
