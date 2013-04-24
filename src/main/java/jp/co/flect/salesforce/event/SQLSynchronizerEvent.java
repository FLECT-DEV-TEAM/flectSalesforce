package jp.co.flect.salesforce.event;

import java.util.EventObject;
import java.io.File;
import jp.co.flect.salesforce.bulk.SQLSynchronizer;
import jp.co.flect.salesforce.bulk.BatchInfo;
import jp.co.flect.salesforce.bulk.JobInfo;

public class SQLSynchronizerEvent extends EventObject {
	
	public enum EventType {
		SELECT,
		MAKE_CSV,
		OPEN_JOB,
		ADD_BATCH,
		CLOSE_JOB,
		ABORT_JOB
	}
	
	private SQLSynchronizer sync;
	private EventType eventType;
	private File file;//File when id == MAKE_CSV;
	private JobInfo job;//JobInfo when id == JOB_xxxx;
	private BatchInfo batch;//BatchInfo when id == ADD_BATCH;
	
	public SQLSynchronizerEvent(SQLSynchronizer source, EventType eventType) {
		super(source);
		this.eventType = eventType;
	}
	
	public SQLSynchronizerEvent(SQLSynchronizer source, EventType eventType, File file) {
		this(source, eventType);
		this.file = file;
	}
	
	public SQLSynchronizerEvent(SQLSynchronizer source, EventType eventType, JobInfo job) {
		this(source, eventType);
		this.job = job;
	}
	
	public SQLSynchronizerEvent(SQLSynchronizer source, EventType eventType, BatchInfo batch) {
		this(source, eventType);
		this.batch = batch;
	}
	
	public SQLSynchronizer getSQLSynchronizer() { return (SQLSynchronizer)getSource();}
	public EventType getType() { return this.eventType;}
	public File getFile() { return this.file;}
	public JobInfo getJobInfo() { return this.job;}
	public BatchInfo getBatchInfo() { return this.batch;}
}
