package jp.co.flect.salesforce.bulk;

import jp.co.flect.salesforce.SObject;

public class DefaultValue implements SObjectSyncRequest.Function {
	
	private Object value;
	
	public DefaultValue(Object value) {
		this.value = value;
	}
	
	@Override
	public Object evaluate(SObject obj) {
		return this.value;
	}
}