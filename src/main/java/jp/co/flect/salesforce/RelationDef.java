package jp.co.flect.salesforce;

import jp.co.flect.xmlschema.ComplexType;

public class RelationDef extends AbstractDef {
	
	private static final long serialVersionUID = -1016223494975950208L;

	public RelationDef(Metadata meta) {
		super(meta);
	}
	
	protected ComplexType getTypeFromMetadata() {
		return (ComplexType)getMetadata().getMessageType("ChildRelationship");
	}
	
	public boolean isCascadeDelete() { return getBoolean("cascadeDelete");}
	public String getObjectName() { return getString("childSObject");}
	public boolean isDeprecatedAndHidden() { return getBoolean("deprecatedAndHidden");}
	public String getFieldName() { return getString("field");}
	public String getRelationshipName() { return getString("relationshipName");}
	
	@Override
	public RelationDef newInstance() {
		return (RelationDef)super.newInstance();
	}
	
}
