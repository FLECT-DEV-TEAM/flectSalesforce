package jp.co.flect.salesforce.metadata;

import jp.co.flect.soap.SimpleObject;
import java.util.Date;

public class FileProperties extends SimpleObject {
	
	public enum ManageableState {
		released,
		deleted,
		deprecated,
		installed,
		beta,
		unmanaged
	};
	
	public String getCreatedById() { return getString("createdById");}
	public String getCreatedByName() { return getString("createdByName");}
	public Date getCreatedDate() { return getDate("createdDate");}
	public String getFileName() { return getString("fileName");}
	public String getFullName() { return getString("fullName");}
	public String getId() { return getString("id");}
	public String getLastModifiedById() { return getString("lastModifiedById");}
	public String getLastModifiedByName() { return getString("lastModifiedByName");}
	public Date getLastModifiedDate() { return getDate("lastModifiedDate");}
	public ManageableState getManageableState() { return ManageableState.valueOf(getString("manageableState"));}
	public String getNamespacePrefix() { return getString("namespacePrefix");}
	public MetadataType getMetadataType() { return MetadataType.valueOf(getString("type"));}
}

