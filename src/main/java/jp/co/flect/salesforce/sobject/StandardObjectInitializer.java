package jp.co.flect.salesforce.sobject;

import jp.co.flect.salesforce.Metadata;

public class StandardObjectInitializer {
	
	public static void init() {
		Metadata.registerClass("Account", Account.class);
		Metadata.registerClass("Contact", Contact.class);
		Metadata.registerClass("Opportunity", Opportunity.class);
		Metadata.registerClass("OpportunityHistory", OpportunityHistory.class);
	}
	
}
