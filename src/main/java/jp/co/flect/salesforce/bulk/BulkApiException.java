package jp.co.flect.salesforce.bulk;

import jp.co.flect.salesforce.SalesforceException;

public class BulkApiException extends SalesforceException {
	
	private String code;
	
	public BulkApiException(String msg, String code) {
		super(msg);
		this.code = code;
	}
	
	public String getCode() { return this.code;}
	
}