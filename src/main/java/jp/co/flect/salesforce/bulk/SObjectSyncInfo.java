package jp.co.flect.salesforce.bulk;

/**
 * syncSObjectの結果クラス
 */
public class SObjectSyncInfo {
	
	private int success;
	private int error;
	private Exception exception;
	
	public SObjectSyncInfo(int success, int error) {
		this(success, error, null);
	}
	
	public SObjectSyncInfo(Exception exception) {
		this(0, 0, exception);
	}
	
	public SObjectSyncInfo(int success, int error, Exception exception) {
		this.success = success;
		this.error = error;
		this.exception = exception;
	}
	
	/** 同期に成功したレコード数 */
	public int getSuccessCount() { return this.success;}
	/** 同期に失敗したレコード数(SObjectSyncRequest#isAllOrNo) */
	public int getErrorCount() { return this.error;}
	/** 同期に成功したレコード数 */
	public Exception getException() { return this.exception;}
}
