package jp.co.flect.salesforce.sobject;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.Metadata;

public class Opportunity extends SObject {
	
	public Opportunity(Metadata meta) {
		super(meta);
	}
	public Opportunity(SObjectDef objectDef) {
		super(objectDef);
	}
	
}
