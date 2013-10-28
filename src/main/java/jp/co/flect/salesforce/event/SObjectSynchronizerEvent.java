package jp.co.flect.salesforce.event;

import java.util.EventObject;
import jp.co.flect.salesforce.bulk.SObjectSynchronizer;
import jp.co.flect.salesforce.bulk.SObjectSyncRequest;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.query.QueryResult;

public class SObjectSynchronizerEvent extends EventObject {
	
	public enum EventType {
		STARTED,//
		PREPARED,//
		SELECTED,//
		UPDATED,
		BATCH_ERROR,
		RECORD_ERROR,
		OTHER_ERROR,//
		COMMITED,//
		ROLLBACKED,//
		FINISHED //
	}
	
	private SObjectSynchronizer sync;
	private EventType eventType;
	private SObject errorObject;
	private Exception exception;
	private QueryResult<SObject> queryResult;
	
	public SObjectSynchronizerEvent(SObjectSynchronizer source, EventType eventType) {
		super(source);
		this.eventType = eventType;
	}
	
	public SObjectSynchronizerEvent(SObjectSynchronizer source, EventType eventType, Exception ex) {
		super(source);
		this.eventType = eventType;
		this.exception = ex;
	}
	
	public SObjectSynchronizerEvent(SObjectSynchronizer source, SObject errorObject, Exception ex) {
		super(source);
		this.eventType = EventType.RECORD_ERROR;
		this.errorObject = errorObject;
		this.exception = ex;
	}
	
	public SObjectSynchronizerEvent(SObjectSynchronizer source, QueryResult<SObject> queryResult) {
		super(source);
		this.eventType = EventType.SELECTED;
		this.queryResult = queryResult;
	}
	
	public EventType getType() { return this.eventType;}
	
	public SObjectSynchronizer getSObjectSynchronizer() { return (SObjectSynchronizer)getSource();}
	public SObjectSyncRequest getRequest() { return getSObjectSynchronizer().getRequest();}
	
	public SObject getErrorObject() { return this.errorObject;}
	public Exception getException() { return this.exception;}
	
	public QueryResult<SObject> getQueryResult() { return this.queryResult;}
	
}
