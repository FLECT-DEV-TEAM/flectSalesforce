package jp.co.flect.salesforce.metadata;

import jp.co.flect.soap.SimpleObject;

public class BaseMetadata extends SimpleObject implements MetadataIntf {
	
	private MetadataType type;
	
	public BaseMetadata(MetadataType type) {
		this.type = type;
	}
	
	public MetadataType getMetadataType() { return this.type;}
	public BaseMetadata getMetadata() { return this;}
	
	public String getFullName() { return getString("fullName");}
	public void setFullName(String s) { set("fullName", s);}
	
}

