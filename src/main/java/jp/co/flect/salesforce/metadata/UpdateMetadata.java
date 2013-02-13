package jp.co.flect.salesforce.metadata;

import jp.co.flect.soap.SimpleObject;

public class UpdateMetadata extends SimpleObject implements MetadataIntf {
	
	public UpdateMetadata(BaseMetadata meta) {
		this(meta.getFullName(), meta);
	}
	
	public UpdateMetadata(String currentName, BaseMetadata meta) {
		setCurrentName(currentName);
		setMetadata(meta);
	}
	
	public MetadataType getMetadataType() { return getMetadata().getMetadataType();}
	
	public String getCurrentName() { return getString("currentName");}
	public void setCurrentName(String s) { set("currentName", s);}
	
	public BaseMetadata getMetadata() { return (BaseMetadata)get("metadata");}
	public void setMetadata(BaseMetadata v) { set("metadata", v);}
	
}

