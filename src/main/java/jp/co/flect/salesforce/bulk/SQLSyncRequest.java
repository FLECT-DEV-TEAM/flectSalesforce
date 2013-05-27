package jp.co.flect.salesforce.bulk;

import java.sql.Connection;
import javax.swing.event.EventListenerList;
import jp.co.flect.salesforce.event.SQLSynchronizerListener;

public class SQLSyncRequest {
	
	private Connection con;
	private String sql;
	private String objectName;
	private String externalIdFieldName;
	private Object[] params;
	private EventListenerList listeners = new EventListenerList();
	
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
	
	public void addSQLSynchronizerListener(SQLSynchronizerListener l) {
		this.listeners.add(SQLSynchronizerListener.class, l);
	}
	
	public void removeSQLSynchronizerListener(SQLSynchronizerListener l) {
		this.listeners.remove(SQLSynchronizerListener.class, l);
	}
	
	public SQLSynchronizerListener[] getSQLSynchronizerListeners() {
		return this.listeners.getListeners(SQLSynchronizerListener.class);
	}
}
