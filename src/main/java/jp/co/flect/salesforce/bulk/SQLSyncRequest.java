package jp.co.flect.salesforce.bulk;

import java.sql.Connection;

public class SQLSyncRequest {
	
	private Connection con;
	private String sql;
	private String objectName;
	private String externalIdFieldName;
	private Object[] params;
	
	public SQLSyncRequest(Connection con, String sql, String objectName) {
		this.con = con;
		this.sql = sql;
		this.objectName = objectName;
	}
	
	public Connection getConnection() { return this.con;}
	public String getSQL() { return this.sql;}
	public String getObjectName() { return this.objectName;}
	
	public String getExternalIdFieldName() { return this.externalIdFieldName;}
	public void setExternalIdFieldName(String s) { this.externalIdFieldName = s;}
	
	public Object[] getParams() { return this.params;}
	public void setParams(Object... params) { this.params = params;}
}
