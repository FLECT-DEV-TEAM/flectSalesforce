package jp.co.flect.salesforce;

import jp.co.flect.xmlschema.ComplexType;

public class PicklistEntry extends AbstractDef {
	
	public PicklistEntry(Metadata meta) {
		super(meta);
	}
	
	protected ComplexType getTypeFromMetadata() {
		return (ComplexType)getMetadata().getMessageType("PicklistEntry");
	}
}
