package jp.co.flect.salesforce.bulk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.BatchUpdateException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
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
		try {
			String query = buildQuery();
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
						retry(table, result.getRecords(), e);
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
			table.close();
			fireEvent(EventType.FINISHED);
		}
	}
	
	private void retry(MergeTable table, List<SObject> list, BatchUpdateException e) throws SQLException {
		int[] updates = e.getUpdateCounts();
		if (updates.length == list.size()) {
			//MySQL 
			//BatchUpdateException#getUpdatesCounts and the number of addBatch are always same.
			//Retry error records
			for (int i=0; i<updates.length; i++) {
				int n = updates[i];
				SObject obj = list.get(i);
				if (n < 0) {
					try {
						table.execute(obj);
						table.commit();
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
				SObject obj = list.get(i);
				if (n > 0) {
					table.addObject(obj);
				}
			}
			try {
				int[] ret = table.execute();
				this.successCount += ret.length;
			} catch (BatchUpdateException e2) {
				throw notBatchUpdateException(e2);
			}
		} else {
			//PostgreSQL
			//BatchUpdateException#getUpdatesCounts stop at error record.
			for (SObject obj : list) {
				try {
					table.execute(obj);
					table.commit();
					this.successCount++;
				} catch (SQLException e2) {
					this.errorCount++;
					table.rollback();
					fireEvent(new SObjectSynchronizerEvent(this, obj, e2));
				}
			}
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
		if (colMap.size() == 0) {
			normalizeTableName(meta, colMap);
		}
		//Check Salesforce object schema
		SObjectDef objectDef = getSObjectDef(request.getObjectName());
		List<String> normalizeList = new ArrayList<String>();
		for (String name: request.getObjectFieldList()) {
			normalizeList.add(checkSchema(objectDef, name));
		}
		request.normalizeObjectFieldList(normalizeList);
		for (String colName : request.getTableColumnList()) {
			if (!colMap.hasColumn(colName)) {
				throw new IllegalArgumentException("Unknown table column: " + request.getTableName() + "." + colName);
			}
		}
		if (request.getKeyColumns() != null) {
			for (String colName : request.getKeyColumns()) {
				if (!colMap.hasColumn(colName)) {
					throw new IllegalArgumentException("Unknown table column: " + request.getTableName() + "." + colName);
				}
			}
		}
		this.colMap = colMap;
		fireEvent(EventType.PREPARED);
	}
	
	//Adjust case sensitive
	private void normalizeTableName(DatabaseMetaData meta, ColumnMap colMap) throws SQLException{
		String tableName = request.getTableName();
		String newName = null;
		ResultSet rs = meta.getTables(null, null, "%", null);
		try {
			while (rs.next()) {
				String s = rs.getString(3);
				if (tableName.equalsIgnoreCase(s)) {
					newName = s;
					break;
				}
			}
		} finally {
			rs.close();
		}
		if (newName == null || newName.equals(tableName)) {
			throw new IllegalArgumentException("Unknown table: " + tableName);
		}
		request.setTableName(newName);
		rs = meta.getColumns(null, null, newName, "%");
		try {
			while (rs.next()) {
				String columnName = rs.getString(4);
				int type = rs.getInt(5);
				colMap.add(columnName, type);
			}
		} finally {
			rs.close();
		}
		if (colMap.size() == 0) {
			throw new IllegalArgumentException("Unknown table: " + tableName);
		}
	}
	
	private SObjectDef getSObjectDef(String objectName) throws IOException, SoapException {
		SObjectDef objectDef = this.client.getMetadata().getObjectDef(objectName);
		if (objectDef == null || !objectDef.isComplete()) {
			objectDef = client.describeSObject(objectName);
		}
		return objectDef;
	}
	
	private String checkSchema(SObjectDef objectDef, String name) throws IOException, SoapException {
		int idx = name.indexOf(".");
		if (idx == -1) {
			FieldDef field = objectDef.getField(name);
			if (field == null) {
				throw new IllegalArgumentException("Unknown field: " + objectDef.getName() + "." + name);
			}
			return field.getName();
		} else {
			String childName = name.substring(0, idx);
			String childField = name.substring(idx + 1);
			FieldDef field = objectDef.getSingleRelation(childName);
			if (field == null) {
				throw new IllegalArgumentException("Unknown field: " + objectDef.getName() + "." + childName);
			}
			SObjectDef childDef = getSObjectDef(field.getReferenceToName());
			return field.getRelationshipName() + "." + checkSchema(childDef, childField);
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
		
		return new DefaultMergeTable(colMap, con);
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
	
	private boolean hasKeyColumn() {
		String[] keys = request.getKeyColumns();
		return keys != null && keys.length > 0;
	}
	
	private static SQLException notBatchUpdateException(BatchUpdateException e) {
		SQLException target = e;
		while (target instanceof BatchUpdateException) {
			target = target.getNextException();
		}
		return target == null ? new SQLException(e) : target;
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
		
		protected void addParameter(PreparedStatement stmt, int idx, SObject obj, String colName) throws SQLException {
			Object value = request.getFunction(colName).evaluate(obj);
			int type = getColumnType(colName);
			if (value == null) {
				stmt.setNull(idx, type);
			} else {
				if (value instanceof Date) {
					Date d = (Date)value;
					switch (type) {
						case Types.DATE:
							value = new java.sql.Date(d.getTime());
							break;
						case Types.TIMESTAMP:
							value = new java.sql.Timestamp(d.getTime());
							break;
					}
				}
				stmt.setObject(idx, value, type);
			}
		}
		
		public abstract void addObject(SObject obj) throws SQLException;
		public abstract int[] execute() throws SQLException;
		public abstract void close();
		
		public int execute(SObject obj) throws SQLException {
			try {
				addObject(obj);
				int[] ret = execute();
				return ret[0];
			} catch (BatchUpdateException e) {
				throw notBatchUpdateException(e);
			}
		}
		
		public void commit() throws SQLException { 
			this.con.commit();
			fireEvent(EventType.COMMITED);
		}
		
		public void rollback() throws SQLException { 
			this.con.rollback();
			fireEvent(EventType.ROLLBACKED);
		}
		
	}
	
	private class DefaultMergeTable extends MergeTable {
		
		private PreparedStatement insertStmt;
		private PreparedStatement updateStmt;
		private List<SObject> objList = new ArrayList<SObject>();
		
		public DefaultMergeTable(ColumnMap colMap, Connection con) throws SQLException {
			super(colMap, con);
			this.insertStmt = con.prepareStatement(createInsertStatement());
			if (hasKeyColumn()) {
				this.updateStmt = con.prepareStatement(createUpdateStatement());
			}
		}
		
		private String createInsertStatement() {
			StringBuilder buf = new StringBuilder();
			StringBuilder valueBuf = new StringBuilder();
			buf.append("INSERT INTO ").append(request.getTableName()).append(" (");
			
			List<String> colList = request.getTableColumnList();
			for (String name: colList) {
				if (valueBuf.length() > 0) {
					buf.append(", ");
					valueBuf.append(", ");
				}
				buf.append(name);
				valueBuf.append("?");
			}
			buf.append(") VALUES(").append(valueBuf).append(")");
			return buf.toString();
		}
		
		private String createUpdateStatement() {
			StringBuilder buf = new StringBuilder();
			buf.append("UPDATE ").append(request.getTableName()).append(" SET ");
			
			List<String> colList = request.getTableColumnList();
			boolean bFirst = true;
			for (String name: colList) {
				if (isKeyColumn(name)) {
					continue;
				}
				if (bFirst) {
					bFirst = false;
				} else {
					buf.append(", ");
				}
				buf.append(name).append(" = ?");
			}
			buf.append(" WHERE ");
			bFirst = true;
			for (String name : request.getKeyColumns()) {
				if (bFirst) {
					bFirst = false;
				} else {
					buf.append(", ");
				}
				buf.append(name).append(" = ?");
			}
			return buf.toString();
		}
		
		public void addObject(SObject obj) throws SQLException {
			if (this.updateStmt == null) {
				addToInsert(obj);
			} else {
				addToUpdate(obj);
			}
			this.objList.add(obj);
		}
		
		private void addToInsert(SObject obj) throws SQLException {
			int idx = 1;
			List<String> colList = request.getTableColumnList();
			//INSERT clause
			for (String colName : colList) {
				addParameter(this.insertStmt, idx++, obj, colName);
			}
			this.insertStmt.addBatch();
		}
		
		private void addToUpdate(SObject obj) throws SQLException {
			int idx = 1;
			List<String> colList = request.getTableColumnList();
			//UPDATE clause
			for (String colName : colList) {
				if (isKeyColumn(colName)) {
					continue;
				}
				addParameter(this.updateStmt, idx++, obj, colName);
			}
			for (String colName : request.getKeyColumns()) {
				addParameter(this.updateStmt, idx++, obj, colName);
			}
			this.updateStmt.addBatch();
		}
		
		public int[] execute() throws SQLException {
			int[] ret = null;
			try {
				if (this.updateStmt == null) {
					ret = this.insertStmt.executeBatch();
				} else {
					ret = this.updateStmt.executeBatch();
					
					int insertCnt = 0;
					for (int i=0; i<ret.length; i++) {
						int n = ret[i];
						SObject obj = this.objList.get(i);
						if (n == 0) {
							addToInsert(obj);
							insertCnt++;
						}
					}
					if (insertCnt > 0) {
						int[] inserts = null;
						BatchUpdateException ex = null;
						try {
							inserts = this.insertStmt.executeBatch();
						} catch (BatchUpdateException e) {
							ex = e;
							inserts = e.getUpdateCounts();
						}
						int insertIndex = 0;
						for (int i=0; i<ret.length; i++) {
							if (ret[i] == 0) {
								if (insertIndex < inserts.length) {
									ret[i] = inserts[insertIndex++];
								} else {
									ret[i] = -1;
								}
							}
						}
						if (ex != null) {
							BatchUpdateException newEx = new BatchUpdateException(ret, ex);
							newEx.setNextException(ex);
							throw newEx;
						}
					}
				}
				fireEvent(EventType.UPDATED);
			} finally {
				this.objList.clear();
			}
			return ret;
		}
		
		public void close() {
			try {
				this.insertStmt.close();
			} catch (SQLException e) {
				//ignore
			}
			if (this.updateStmt != null) {
				try {
					this.updateStmt.close();
				} catch (SQLException e) {
					//ignore
				}
			}
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
				if (valueBuf.length() > 0) {
					buf.append(", ");
					valueBuf.append(", ");
				}
				buf.append(name);
				valueBuf.append("?");
			}
			buf.append(") VALUES(").append(valueBuf).append(")");
			if (hasKeyColumn()) {
				buf.append(" ON DUPLICATE KEY UPDATE ");
				valueBuf.setLength(0);
				for (String name: colList) {
					if (isKeyColumn(name)) {
						continue;
					}
					if (valueBuf.length() > 0) {
						valueBuf.append(", ");
					}
					valueBuf.append(name).append(" = ?");
				}
				buf.append(valueBuf);
			}
			return buf.toString();
		}
		
		public void addObject(SObject obj) throws SQLException {
			int idx = 1;
			List<String> colList = request.getTableColumnList();
			//INSERT clause
			for (String colName : colList) {
				addParameter(this.stmt, idx++, obj, colName);
			}
			if (hasKeyColumn()) {
				//UPDATE clause
				for (String colName : colList) {
					if (isKeyColumn(colName)) {
						continue;
					}
					addParameter(this.stmt, idx++, obj, colName);
				}
			}
			this.stmt.addBatch();
		}
		
		public int[] execute() throws SQLException {
			int[] ret = this.stmt.executeBatch();
			fireEvent(EventType.UPDATED);
			return ret;
		}
		
		public void close() {
			try {
				this.stmt.close();
			} catch (SQLException e) {
				//ignore
			}
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
		
		public int size() { return this.map.size();}
		
		@Override
		public String toString() { return this.map.toString();}
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
