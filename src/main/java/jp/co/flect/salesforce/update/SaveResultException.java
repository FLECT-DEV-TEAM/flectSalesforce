package jp.co.flect.salesforce.update;

import jp.co.flect.soap.SoapException;

public class SaveResultException extends SoapException {
	
	private static final long serialVersionUID = 3550183588401158220L;
	
	private SaveResult result;
	
	public SaveResultException(SaveResult result) {
		super(result.getError(0).getMessage());
		this.result = result;
	}
	
	public SaveResult getSaveResult() { return this.result;}
	
	
}
