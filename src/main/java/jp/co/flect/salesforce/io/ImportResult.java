package jp.co.flect.salesforce.io;

import java.util.List;

public class ImportResult {
	
	public static final int FIELD_NOT_FOUND      = 1;
	public static final int NOT_CREATEABLE_FIELD = 2;
	public static final int NOT_UPDATEABLE_FIELD = 3;
	public static final int NOT_UPSERTABLE_FIELD = 4;
	public static final int MAX_COUNT_EXCEED     = 5;
	
	private List<Exception> exList;
	private List<Warning> warnList;
	private int successCnt;
	
	public ImportResult(int successCnt, List<Exception> exList, List<Warning> warnList) {
		this.successCnt = successCnt;
		this.exList = exList;
		this.warnList = warnList;
	}
	
	public int getSuccessCount() { return this.successCnt;}
	public List<Exception> getExceptionList() { return this.exList;}
	public List<Warning> getWarningList() { return this.warnList;}
	
	public static class Warning extends Exception {
		
		private int type;
		
		public Warning(int type, String name) {
			super(name);
			this.type = type;
		}
		
		public int getType() { return this.type;}
		public String getFieldName() { return getMessage();}
	}
}
