package jp.co.flect.salesforce.bulk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.BatchUpdateException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import jp.co.flect.soap.SoapException;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.query.QueryRequest;
import jp.co.flect.salesforce.query.QueryMoreRequest;
import jp.co.flect.salesforce.query.QueryResult;
import jp.co.flect.salesforce.event.SObjectSynchronizerListener;
import jp.co.flect.salesforce.event.SObjectSynchronizerEvent;
import jp.co.flect.salesforce.event.SObjectSynchronizerEvent.EventType;
import jp.co.flect.xmlschema.TypedValue;
import jp.co.flect.xmlschema.SimpleType;

public class SObjectSynchronizer {
	
	private SalesforceClient client;
	private SObjectSyncRequest request;
	private ColumnMap colMap = null;
	
	private int successCount;
	private int errorCount;
	
	public SObjectSynchronizer(SalesforceClient client, SObjectSyncRequest request) {
		this.client = client;
		this.request = request;
	}
	
	public SObjectSyncRequest getRequest() { return this.request;}
	public SalesforceClient getClient() { return this.client;}
	
	public int getSuccessCount() { return this.successCount;}
	public int getErrorCount() { return this.errorCount;}
	
	public SObjectSyncInfo execute() throws IOException, SoapException, SQLException {
		if (this.colMap == null) {
			prepare();
		}
		this.successCount = 0;
		this.errorCount = 0;
		
		MergeTable table = createMergeTable(colMap, request.getConnection());
		String query = buildQuery();
		try {
			QueryRequest qRequest = new QueryRequest(query);
			qRequest.setBatchSize(request.getBatchSize());
			QueryResult<SObject> result = client.query(qRequest);
			while (true) {
				fireEvent(new SObjectSynchronizerEvent(this, result));
				for (SObject obj : result.getRecords()) {
					table.addObject(obj);
				}
				try {
					table.execute();
					this.successCount += result.getCurrentSize();
				} catch (BatchUpdateException e) {
					table.rollback();
					if (request.getPolicy() != SObjectSyncRequest.SObjectSyncPolicy.IgnoreRecordError) {
						fireEvent(new SObjectSynchronizerEvent(this, EventType.BATCH_ERROR, e));
						throw e;
					} else {
						retry(table, result, e);
					}
				}
				if (request.getPolicy() != SObjectSyncRequest.SObjectSyncPolicy.CommitOnce) {
					table.commit();
				}
				if (result.getQueryLocator() != null) {
					QueryMoreRequest qmRequest = new QueryMoreRequest(result.getQueryLocator());
					qmRequest.setBatchSize(request.getBatchSize());
					result = client.queryMore(qmRequest);
				} else {
					break;
				}
			}
			if (request.getPolicy() == SObjectSyncRequest.SObjectSyncPolicy.CommitOnce) {
				table.commit();
			}
			return new SObjectSyncInfo(this.successCount, this.errorCount, null);
		} catch (IOException e) {
			table.rollback();
			fireEvent(new SObjectSynchronizerEvent(this, EventType.OTHER_ERROR, e));
			throw e;
		} catch (SoapException e) {
			table.rollback();
			fireEvent(new SObjectSynchronizerEvent(this, EventType.OTHER_ERROR, e));
			throw e;
		} catch (BatchUpdateException e) {
			throw e;
		} catch (SQLException e) {
			table.rollback();
			fireEvent(new SObjectSynchronizerEvent(this, EventType.OTHER_ERROR, e));
			throw e;
		} finally {
			fireEvent(EventType.FINISHED);
		}
	}
	
	private void retry(MergeTable table, QueryResult<SObject> result, BatchUpdateException e) throws SQLException {
		int[] updates = e.getUpdateCounts();
		//assert updates.length == result.getCurrentSize();
		//Retry error records
		for (int i=0; i<updates.length; i++) {
			int n = updates[i];
			SObject obj = result.getRecords().get(i);
			if (n < 0) {
				try {
					table.addObject(obj);
					table.execute();
					this.successCount++;
				} catch (SQLException e2) {
					this.errorCount++;
					table.rollback();
					fireEvent(new SObjectSynchronizerEvent(this, obj, e2));
				}
			}
		}
		//Retry normal record
		for (int i=0; i<updates.length; i++) {
			int n = updates[i];
			SObject obj = result.getRecords().get(i);
			if (n > 0) {
				table.addObject(obj);
			}
		}
		try {
			int[] ret = table.execute();
			this.successCount += ret.length;
		} catch (BatchUpdateException e2) {
			throw new SQLException(e2);
		}
	}
	
	public void prepare() throws IOException, SoapException, SQLException {
		fireEvent(EventType.STARTED);
		//Check rdb schema
		ColumnMap colMap = new ColumnMap();
		
		DatabaseMetaData meta = request.getConnection().getMetaData();
		ResultSet rs = meta.getColumns(null, null, request.getTableName(), "%");
		try {
			while (rs.next()) {
				String columnName = rs.getString(4);
				int type = rs.getInt(5);
				colMap.add(columnName, type);
			}
		} finally {
			rs.close();
		}
		//Check Salesforce object schema
		SObjectDef objectDef = getSObjectDef(request.getObjectName());
		for (String name: request.getObjectFieldList()) {
			checkSchema(objectDef, name);
			String colName = request.getMappedTableColumn(name);
			if (colName != null && !colMap.hasColumn(colName)) {
				throw new IllegalArgumentException("Unknown table column: " + request.getTableName() + "." + colName);
			}
		}
		for (String colName : request.getDefaultMapping().keySet()) {
			if (!colMap.hasColumn(colName)) {
				throw new IllegalArgumentException("Unknown table column: " + request.getTableName() + "." + colName);
			}
		}
		if (request.getKeyColumns() != null) {
			for (String key : request.getKeyColumns()) {
				if (!colMap.hasColumn(key)) {
					throw new IllegalArgumentException("Unknown table column: " + request.getTableName() + "." + key);
				}
			}
		}
		this.colMap = colMap;
		fireEvent(EventType.PREPARED);
	}
	
	private SObjectDef getSObjectDef(String objectName) throws IOException, SoapException {
		SObjectDef objectDef = this.client.getMetadata().getObjectDef(objectName);
		if (objectDef == null || !objectDef.isComplete()) {
			objectDef = client.describeSObject(objectName);
		}
		return objectDef;
	}
	
	private void checkSchema(SObjectDef objectDef, String name) throws IOException, SoapException {
		int idx = name.indexOf(".");
		if (idx == -1) {
			FieldDef field = objectDef.getField(name);
			if (field == null) {
				throw new IllegalArgumentException("Unknown field: " + objectDef.getName() + "." + name);
			}
		} else {
			String childName = name.substring(0, idx);
			String childField = name.substring(idx + 1);
			FieldDef field = objectDef.getSingleRelation(childName);
			if (field == null) {
				throw new IllegalArgumentException("Unknown field: " + objectDef.getName() + "." + childName);
			}
			SObjectDef childDef = getSObjectDef(field.getReferenceToName());
			checkSchema(childDef, childField);
		}
	}
	
	private String buildQuery() {
		StringBuilder buf = new StringBuilder();
		buf.append("SELECT ");
		boolean bFirst = true;
		for (String key : this.request.getObjectFieldList()) {
			if (bFirst) {
				bFirst = false;
			} else {
				buf.append(", ");
			}
			buf.append(key);
		}
		buf.append(" FROM ").append(request.getObjectName());
		String where = request.getWhere();
		if (where != null) {
			buf.append(" WHERE ");
			Object[] params = request.getParams();
			if (params == null) {
				params = new Object[0];
			}
			int paramIndex = 0;
			for (int i=0; i<where.length(); i++) {
				char c = where.charAt(i);
				if (c == '?') {
					if (paramIndex < params.length) {
						buf.append(convertParam(params[paramIndex++]));
					} else {
						throw new IllegalArgumentException("Parameter isn't specified");
					}
				} else {
					buf.append(c);
				}
			}
		}
		return buf.toString();
	}
	
	private String convertParam(Object obj) {
		if (obj instanceof String) return "'" + obj.toString() + "'";
		if (obj instanceof Number) return obj.toString();
		if (obj instanceof Date) {
			return SimpleType.getBuiltinType("dateTime").format(obj);
		}
		if (obj instanceof TypedValue) {
			TypedValue tv = (TypedValue)obj;
			if (tv.getType().isSimpleType()) {
				return convertParam(tv.toString());
			} else {
				return tv.toString();
			}
		}
		throw new IllegalArgumentException("Unsupported value: " + obj.getClass().getName() + ": " + obj);
	}
	
	private static boolean isMySQL(Connection con) throws SQLException {
		return con.getMetaData().getDatabaseProductName().equals("MySQL");
	}
	
	private MergeTable createMergeTable(ColumnMap colMap, Connection con) throws SQLException {
		if (isMySQL(con)) return new MergeTableForMySQL(colMap, con);
		
		throw new UnsupportedOperationException("Unsupported database: " + con.getMetaData().getDatabaseProductName());
	}
	
	private boolean isKeyColumn(String name) {
		if (request.getKeyColumns() == null) {
			return false;
		}
		for (String key : request.getKeyColumns()) {
			if (key.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
	
	private abstract class MergeTable {
		
		private ColumnMap colMap;
		private Connection con;
		
		public MergeTable(ColumnMap colMap, Connection con) throws SQLException {
			this.colMap = colMap;
			this.con = con;
		}
		
		protected int getColumnType(String name) {
			return this.colMap.getType(name);
		}
		
		public abstract void addObject(SObject obj) throws SQLException;
		public abstract int[] execute() throws SQLException;
		
		public void commit() throws SQLException { 
			this.con.commit();
			fireEvent(EventType.COMMITED);
		}
		public void rollback() throws SQLException { 
			this.con.rollback();
			fireEvent(EventType.ROLLBACKED);
		}
		
	}
	
	private class MergeTableForMySQL extends MergeTable {
		
		private PreparedStatement stmt;
		
		public MergeTableForMySQL(ColumnMap colMap, Connection con) throws SQLException {
			super(colMap, con);
			this.stmt = con.prepareStatement(createStatement());
		}
		
		private String createStatement() {
			StringBuilder buf = new StringBuilder();
			StringBuilder valueBuf = new StringBuilder();
			buf.append("INSERT INTO ").append(request.getTableName()).append(" (");
			
			List<String> colList = request.getTableColumnList();
			for (String name: colList) {
				if (name == null) {
					continue;
				}
				if (valueBuf.length() > 0) {
					buf.append(", ");
					valueBuf.append(", ");
				}
				buf.append(name);
				valueBuf.append("?");
			}
			for (String name : request.getDefaultMapping().keySet()) {
				if (valueBuf.length() > 0) {
					buf.append(", ");
					valueBuf.append(", ");
				}
				buf.append(name);
				valueBuf.append("?");
			}
			buf.append(") VALUES(").append(valueBuf)
				.append(") ON DUPLICATE KEY UPDATE ");
			
			valueBuf.setLength(0);
			for (String name: colList) {
				if (name == null || isKeyColumn(name)) {
					continue;
				}
				if (valueBuf.length() > 0) {
					valueBuf.append(", ");
				}
				valueBuf.append(name).append(" = ?");
			}
			buf.append(valueBuf);
			return buf.toString();
		}
		
		public void addObject(SObject obj) throws SQLException {
			int idx = 1;
			List<String> fieldList = request.getObjectFieldList();
			//INSERT clause
			for (String name : fieldList) {
				String colName = request.getMappedTableColumn(name);
				if (colName == null) {
					continue;
				}
				Object value = obj.getDeep(name);
				int type = getColumnType(colName);
				if (value == null) {
					this.stmt.setNull(idx++, type);
				} else {
					this.stmt.setObject(idx++, value, type);
				}
			}
			for (Map.Entry<String, Object> entry : request.getDefaultMapping().entrySet()) {
				String colName = entry.getKey();
				Object value = entry.getValue();
				int type = getColumnType(colName);
				this.stmt.setObject(idx++, value, type);
			}
			//UPDATE clause
			for (String name : fieldList) {
				String colName = request.getMappedTableColumn(name);
				if (colName == null || isKeyColumn(colName)) {
					continue;
				}
				Object value = obj.getDeep(name);
				int type = getColumnType(colName);
				if (value == null) {
					this.stmt.setNull(idx++, type);
				} else {
					this.stmt.setObject(idx++, value, type);
				}
			}
			this.stmt.addBatch();
		}
		
		public int[] execute() throws SQLException {
			int[] ret = this.stmt.executeBatch();
			fireEvent(EventType.UPDATED);
			return ret;
		}
	}
	
	private static class ColumnMap {
		private Map<String, Integer> map = new HashMap<String, Integer>();
		
		public void add(String name, int type) {
			this.map.put(name.toLowerCase(), type);
		}
		
		public boolean hasColumn(String name) {
			return this.map.get(name.toLowerCase()) != null;
		}
		
		public int getType(String name) {
			return this.map.get(name.toLowerCase());
		}
	}
	
	private SObjectSynchronizerListener[] getSObjectSynchronizerListeners() {
		return this.request.getSObjectSynchronizerListeners();
	}
	
	private void fireEvent(EventType type) {
		SObjectSynchronizerListener[] ls = getSObjectSynchronizerListeners();
		if (ls == null || ls.length == 0) {
			return;
		}
		
		SObjectSynchronizerEvent event = new SObjectSynchronizerEvent(this, type);
		for (int i=0; i<ls.length; i++) {
			ls[i].handleEvent(event);
		}
	}
	
	private void fireEvent(SObjectSynchronizerEvent event) {
		SObjectSynchronizerListener[] ls = getSObjectSynchronizerListeners();
		if (ls == null || ls.length == 0) {
			return;
		}
		
		for (int i=0; i<ls.length; i++) {
			ls[i].handleEvent(event);
		}
	}
}
