package jp.co.flect.salesforce;

import jp.co.flect.xmlschema.ComplexType;

public class RecordTypeDef extends AbstractDef {
	
	public RecordTypeDef(Metadata meta) {
		super(meta);
	}
	
	protected ComplexType getTypeFromMetadata() {
		return (ComplexType)getMetadata().getMessageType("RecordTypeInfo");
	}
}
