package jp.co.flect.salesforce.sobject;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.Metadata;

public class Contact extends SObject {
	
	public Contact(Metadata meta) {
		super(meta);
	}
	public Contact(SObjectDef objectDef) {
		super(objectDef);
	}
	
}
