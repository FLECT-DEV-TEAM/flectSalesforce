package jp.co.flect.salesforce.sobject;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.Metadata;

public class OpportunityHistory extends SObject {
	
	public OpportunityHistory(Metadata meta) {
		super(meta);
	}
	public OpportunityHistory(SObjectDef objectDef) {
		super(objectDef);
	}
	
}
