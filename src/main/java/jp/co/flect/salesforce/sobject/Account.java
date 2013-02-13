package jp.co.flect.salesforce.sobject;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.Metadata;

public class Account extends SObject {
	
	public Account(Metadata meta) {
		super(meta);
	}
	public Account(SObjectDef objectDef) {
		super(objectDef);
	}
	
}
