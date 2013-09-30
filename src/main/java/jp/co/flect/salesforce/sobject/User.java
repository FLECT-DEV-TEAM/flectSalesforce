package jp.co.flect.salesforce.sobject;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.Metadata;

public class User extends SObject {
	
	public User(Metadata meta) {
		super(meta);
	}
	public User(SObjectDef objectDef) {
		super(objectDef);
	}
	
}
