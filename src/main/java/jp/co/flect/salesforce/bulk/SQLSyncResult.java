package jp.co.flect.salesforce.bulk;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.event.SQLSynchronizerEvent;
import jp.co.flect.salesforce.event.SQLSynchronizerEvent.EventType;
import jp.co.flect.salesforce.event.SQLSynchronizerListener;

public class SQLSyncResult implements Future<JobInfo> {
	
	private BulkClient client;
	private SQLSyncRequest request;
	private SObjectDef objectDef;
	private ReentrantLock lock;
	
	private volatile boolean started = false;
	
	private EventType status;
	private JobInfo currentJobInfo = null;
	private boolean canceled = false;
	private Exception exception = null;
	
	public SQLSyncResult(BulkClient client, SQLSyncRequest request, SObjectDef objectDef, ReentrantLock lock) {
		this.client = client;
		this.request = request;
		this.objectDef = objectDef;
		this.lock = lock;
		new ExecuteThread().start();
		while (!this.started) {
			;//Wait thread start
		}
	}
	
	public EventType getStatus() { return this.status;}
	
	public Exception getException() { return this.exception;}
	
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (!this.canceled) {
			if (isDone()) {
				return false;
			}
		}
		return this.canceled;
	}
	
	public JobInfo get()  throws InterruptedException, ExecutionException {
		try {
			return get(0, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public JobInfo get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		long start = System.currentTimeMillis();
		timeout = unit.toMillis(timeout);
		while (!isDone()) {
			if (currentJobInfo != null) {
				try {
					currentJobInfo = client.getJobStatus(currentJobInfo);
					if (currentJobInfo.isCompleted()) {
						this.status = EventType.COMPLETE_JOB;
					} else if (currentJobInfo.isAborted()) {
						this.status = EventType.ABORT_JOB;
					}
				} catch (IOException e) {
					throw new ExecutionException(e);
				} catch (BulkApiException e) {
					throw new ExecutionException(e);
				}
			}
			if (!isDone()) {
				if (timeout != 0 && (System.currentTimeMillis() - start) > timeout) {
					throw new TimeoutException();
				}
				Thread.sleep(1000);
			}
		}
		if (this.exception != null) {
			throw new ExecutionException(this.exception);
		}
		return currentJobInfo;
	}
	
	public boolean isCancelled() {
		return this.canceled;
	}
	
	public boolean isDone() {
		EventType s = getStatus();
		if (s == null) {
			return false;
		}
		switch (s) {
			case SELECT:
			case MAKE_CSV:
			case ADD_BATCH:
			case OPEN_JOB:
			case CLOSE_JOB:
				return false;
			case COMPLETE_JOB:
			case NOT_PROCESSED:
			case ERROR:
			case ABORT_JOB:
				return true;
			default:
				throw new IllegalStateException();
		}
	}
	
	private class ExecuteThread extends Thread {
		
		public void run() {
			lock.lock();
			try {
				SQLSyncResult.this.started = true;
				
				SQLSynchronizer sync = new SQLSynchronizer(request.getConnection(), client);
				sync.addSQLSynchronizerListener(new MyListener());
				sync.sqlToSalesforce(objectDef, request.getExternalIdFieldName(), request.getSQL(), request.getParams());
			} catch (RuntimeException e) {
				e.printStackTrace();
				SQLSyncResult.this.exception = e;
				SQLSyncResult.this.status = EventType.ERROR;
			} catch (Exception e) {
				//Do nothing
			} finally {
				if (lock.isHeldByCurrentThread()) {
					lock.unlock();
				}
			}
		};
	}
	
	private class MyListener implements SQLSynchronizerListener {
		public void handleEvent(SQLSynchronizerEvent e) {
			SQLSyncResult.this.status = e.getType();
			switch (e.getType()) {
				case SELECT:
					lock.unlock();
					break;
				case NOT_PROCESSED:
					break;
				case MAKE_CSV:
				case ADD_BATCH:
				case COMPLETE_JOB:
					break;
				case ERROR:
					SQLSyncResult.this.exception = e.getException();
					break;
				case OPEN_JOB:
				case CLOSE_JOB:
				case ABORT_JOB:
					SQLSyncResult.this.currentJobInfo = e.getJobInfo();
					break;
				default:
					throw new IllegalStateException();
			}
		}
	}
}
