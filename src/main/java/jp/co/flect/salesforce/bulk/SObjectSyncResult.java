package jp.co.flect.salesforce.bulk;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

public class SObjectSyncResult implements Future<Integer> {
	
	private volatile Integer result = null;
	private boolean canceled = false;
	private Exception exception = null;
	private Thread execThread;
	
	public SObjectSyncResult(final SObjectSynchronizer sync) {
		this.execThread = new Thread() {
			public void run() {
				try {
					SObjectSyncResult.this.result = sync.execute();
				} catch (Exception e) {
					SObjectSyncResult.this.exception = e;
					SObjectSyncResult.this.result = 0;
				}
			}
		};
		this.execThread.start();
	}
	
	public Exception getException() { return this.exception;}
	
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (this.result != null) {
			return false;
		}
		if (mayInterruptIfRunning) {
			this.execThread.interrupt();
		}
		this.result = 0;
		this.canceled = true;
		return true;
	}
	
	public Integer get()  throws InterruptedException, ExecutionException {
		if (this.result != null) {
			return this.result;
		}
		execThread.join(0);
		return this.result;
	}
	
	public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (this.result != null) {
			return this.result;
		}
		execThread.join(unit.toMillis(timeout));
		return this.result;
	}
	
	public boolean isCancelled() {
		return this.canceled;
	}
	
	public boolean isDone() {
		return this.result != null;
	}
	
}
