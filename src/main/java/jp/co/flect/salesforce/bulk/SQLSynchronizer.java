package jp.co.flect.salesforce.bulk;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.List;;
import java.util.ArrayList;
import javax.swing.event.EventListenerList;

import jp.co.flect.csv.CSVWriter;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.type.DateType;
import jp.co.flect.xmlschema.type.DatetimeType;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.event.SQLSynchronizerListener;
import jp.co.flect.salesforce.event.SQLSynchronizerEvent;

public class SQLSynchronizer {
	
	private BulkClient client;
	private Connection con;
	
	private boolean useNAifNull = false;
	private int batchRecords = 10000;
	private boolean autoClose = true;
	
	private EventListenerList listeners = new EventListenerList();
	
	public SQLSynchronizer(Connection con, BulkClient client) {
		this.con = con;
		this.client = client;
	}
	
	public boolean isUseNAifNull() { return this.useNAifNull;}
	public void setUseNAifNull(boolean b) { this.useNAifNull = b;}
	
	public int getBatchRecords() { return batchRecords;}
	public void setBatchRecords(int n) { this.batchRecords = n;}
	
	public boolean isAutoClose() { return this.autoClose;}
	public void setAutoClose(boolean b) { this.autoClose = b;}
	
	public JobInfo closeJob(BatchInfo batch) throws IOException, BulkApiException {
		if (batch.getJobId() == null) {
			return null;
		}
		JobInfo job = new JobInfo(batch.getJobId());
		return doCloseJob(job);
	}
	
	private JobInfo doCloseJob(JobInfo job) throws IOException, BulkApiException {
		job = client.closeJob(job);
		fireEvent(new SQLSynchronizerEvent(this, SQLSynchronizerEvent.EventType.CLOSE_JOB, job));
		return job;
	}
	
	public JobInfo abortJob(BatchInfo batch) throws IOException, BulkApiException {
		if (batch.getJobId() == null) {
			return null;
		}
		JobInfo job = new JobInfo(batch.getJobId());
		return doAbortJob(job);
	}
	
	private JobInfo doAbortJob(JobInfo job) throws IOException, BulkApiException {
		job = client.abortJob(job);
		fireEvent(new SQLSynchronizerEvent(this, SQLSynchronizerEvent.EventType.ABORT_JOB, job));
		return job;
	}
	
	public List<BatchInfo> sqlToSalesforce(SObjectDef objectDef, String externalIdFieldName, String sql, Object... params) throws SQLException, IOException, BulkApiException {
		List<BatchInfo> ret = new ArrayList<BatchInfo>();
		List<File> list = selectToCsv(objectDef, externalIdFieldName, sql, params);
		if (list == null) {
			ret.add(BatchInfo.createNotProcessed(objectDef.getName()));
			return ret;
		}
		JobInfo job = null;
		try {
			job = new JobInfo(JobInfo.Operation.Upsert, objectDef.getName());
			job.setExternalIdFieldName(externalIdFieldName);
			
			job = client.openJob(job);
			fireEvent(new SQLSynchronizerEvent(this, SQLSynchronizerEvent.EventType.OPEN_JOB, job));
			for (File f : list) {
				BatchInfo batch = client.addBatch(job, f); 
				ret.add(batch);
				fireEvent(new SQLSynchronizerEvent(this, SQLSynchronizerEvent.EventType.ADD_BATCH, batch));
			}
			if (this.autoClose) {
				doCloseJob(job);
			}
			return ret;
		} catch (IOException e) {
			if (job != null && job.getId() != null) {
				doAbortJob(job);
			}
			throw e;
		} catch (BulkApiException e) {
			if (job != null && job.getId() != null) {
				doAbortJob(job);
			}
			throw e;
		} finally {
			for (File f : list) {
				f.delete();
			}
		}
	}
	
	private List<File> selectToCsv(SObjectDef objectDef, String externalIdFieldName, String sql, Object... params) throws SQLException, IOException, BulkApiException {
		List<File> list = null;
		PreparedStatement stmt = con.prepareStatement(sql);
		try {
			for (int i=0; i<params.length; i++) {
				setParameter(stmt, i + 1, params[i]);
			}
			ResultSet rs = stmt.executeQuery();
			fireEvent(new SQLSynchronizerEvent(this, SQLSynchronizerEvent.EventType.SELECT));
			try {
				if (!rs.next()) {
					return null;
				}
				ResultSetMetaData meta = rs.getMetaData();
				int colCount = meta.getColumnCount();
				boolean existExternalCol = false;
				String[] names = new String[colCount];
				FieldDef[] fields = new FieldDef[colCount];
				for (int i=0; i<colCount; i++) {
					String name = meta.getColumnLabel(i+1);
					FieldDef f = objectDef.getField(name);
					if (f == null) {
						throw new BulkApiException("UnknownField : " + name, "SQLSync");
					}
					if (name.equalsIgnoreCase(externalIdFieldName)) {
						existExternalCol = true;
					}
					names[i] = name;
					fields[i] = f;
				}
				if (!existExternalCol) {
					throw new BulkApiException("ExternalID column not found : " + externalIdFieldName, "SQLSync");
				}
				String[] values = new String[colCount];
				list = new ArrayList<File>();
				boolean bNext = true;
				while (bNext) {
					File temp = File.createTempFile("tmp", ".csv");
					CSVWriter writer = new CSVWriter(temp);
					try {
						writer.setQuoteType(CSVWriter.QuoteType.ALL_EXPECT_NULL);
						writer.write(names);
						int cnt = 0;
						do {
							for (int i=0; i<colCount; i++) {
								String v = getString(rs, i+1, fields[i]);
								if (rs.wasNull()) {
									v = null;
								}
								if (v == null && useNAifNull) {
									v = "#N/A";
								} else if (v != null && v.length() == 0) {
									v = null;
								}
								values[i] = v;
							}
							writer.write(values);
							cnt++;
							bNext = rs.next();
						} while (bNext && cnt < batchRecords);
					} finally {
						writer.close();
					}
					list.add(temp);
					fireEvent(new SQLSynchronizerEvent(this, SQLSynchronizerEvent.EventType.MAKE_CSV, temp));
				}
				return list;
			} finally {
				rs.close();
			}
		} catch (IOException e) {
			if (list != null) {
				for (File f : list) {
					f.delete();
				}
			}
			throw e;
		} catch (SQLException e) {
			if (list != null) {
				for (File f : list) {
					f.delete();
				}
			}
			throw e;
		} finally {
			stmt.close();
		}
	}
	
	private String getString(ResultSet rs, int idx, FieldDef f) throws SQLException {
		SimpleType soapType = f.getSoapType();
		if (soapType.isDateType()) {
			if (soapType.getName().equals(DateType.NAME)) {
				java.sql.Date d = rs.getDate(idx);
				return soapType.format(d);
			} else if (soapType.getName().equals(DatetimeType.NAME)) {
				java.sql.Timestamp t = rs.getTimestamp(idx);
				return soapType.format(t);
			} else {
				throw new IllegalStateException("Unknown type: " + soapType);
			}
		} else {
			return rs.getString(idx);
		}
	}
	
	private void setParameter(PreparedStatement stmt, int idx, Object o) throws SQLException {
		if (o instanceof String) {
			stmt.setString(idx, o.toString());
		} else if (o instanceof Integer) {
			stmt.setInt(idx, ((Integer)o).intValue());
		} else if (o instanceof Boolean) {
			stmt.setBoolean(idx, ((Boolean)o).booleanValue());
		} else if (o instanceof Long) {
			stmt.setLong(idx, ((Long)o).longValue());
		} else if (o instanceof Double) {
			stmt.setDouble(idx, ((Double)o).doubleValue());
		} else if (o instanceof BigDecimal) {
			stmt.setBigDecimal(idx, (BigDecimal)o);
		} else if (o instanceof java.sql.Date) {
			stmt.setDate(idx, (java.sql.Date)o);
		} else if (o instanceof Timestamp) {
			stmt.setTimestamp(idx, (Timestamp)o);
		} else if (o instanceof byte[]) {
			stmt.setBytes(idx, (byte[])o);
		} else {
			throw new IllegalStateException(o.getClass().toString());
		}
	}
	
	public void addSQLSynchronizerListener(SQLSynchronizerListener l) {
		this.listeners.add(SQLSynchronizerListener.class, l);
	}
	
	public void removeSQLSynchronizerListener(SQLSynchronizerListener l) {
		this.listeners.remove(SQLSynchronizerListener.class, l);
	}
	
	private void fireEvent(SQLSynchronizerEvent event) {
		SQLSynchronizerListener[] ls = (SQLSynchronizerListener[])this.listeners.getListeners(SQLSynchronizerListener.class);
		if (ls == null || ls.length == 0) {
			return;
		}
		for (int i=0; i<ls.length; i++) {
			ls[i].handleEvent(event);
		}
	}
	
	/*
	public static class BatchFuture implements Future<BatchInfo> {
		
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}
		
		public BatchInfo get() {
			return get(7, TimeUnit.DAYS);
		}
		
		public BatchInfo get(long timeout, TimeUnit unit) {
			return null;
		}
		
		public boolean isCancelled() {
			return false;
		}
		
		public boolean isDone() {
			return false;
		}
	}
	*/
}
