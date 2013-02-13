package jp.co.flect.salesforce.bulk;

public class BulkApiException extends Exception {
	
	private String code;
	
	public BulkApiException(String msg, String code) {
		super(msg);
		this.code = code;
	}
	
	public String getCode() { return this.code;}
	
}