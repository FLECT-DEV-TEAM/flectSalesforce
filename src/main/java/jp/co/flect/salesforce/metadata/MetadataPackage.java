package jp.co.flect.salesforce.metadata;

import java.util.ArrayList;
import java.util.List;
import jp.co.flect.soap.SimpleObject;
/*
       <xsd:element name="apiAccessLevel" minOccurs="0" type="tns:APIAccessLevel"/>
       <xsd:element name="description" minOccurs="0" type="xsd:string"/>
       <xsd:element name="namespacePrefix" minOccurs="0" type="xsd:string"/>
       <xsd:element name="objectPermissions" minOccurs="0" maxOccurs="unbounded" type="tns:ProfileObjectPermissions"/>
       <xsd:element name="postInstallClass" minOccurs="0" type="xsd:string"/>
       <xsd:element name="setupWeblink" minOccurs="0" type="xsd:string"/>
       <xsd:element name="types" minOccurs="0" maxOccurs="unbounded" type="tns:PackageTypeMembers"/>
       <xsd:element name="uninstallClass" minOccurs="0" type="xsd:string"/>
       <xsd:element name="version" type="xsd:string"/>
*/
public class MetadataPackage extends BaseMetadata {
	
	public enum APIAccessLevel {
		Unrestricted,
		Restricted
	}
	
	public static class PackageTypeMembers extends SimpleObject {
		
		public List<String> getMembers() { return (List<String>)get("members");}
		public void setMembers(List<String> list) { set("members", list);}
		
		public void addMembers(String s) {
			List<String> list = getMembers();
			if (list == null) {
				list = new ArrayList<String>();
				setMembers(list);
			}
			list.add(s);
		}
		
		public String getName() { return getString("name");}
		public void setName(String s) { set("name", s);}
	}
	
	public MetadataPackage() {
		super(MetadataType.Package);
	}
	
	public APIAccessLevel getAPIAccessLevel() { return getEnumValue(APIAccessLevel.class, "apiAccessLevel");}
	public void setAPIAccessLevel(APIAccessLevel v) { set("apiAccessLevel", v.toString());}
	
	public String getDescription() { return getString("description");}
	public void setDescription(String s) { set("description", s);}
	
	public String getNamespacePrefix() { return getString("namespacePrefix");}
	public void setNamespacePrefix(String s) { set("namespacePrefix", s);}
	
	public String getPostInstallClass() { return getString("postInstallClass");}
	public void setPostInstallClass(String s) { set("postInstallClass", s);}
	
	public String getSetupWeblink() { return getString("setupWeblink");}
	public void setSetupWeblink(String s) { set("setupWeblink", s);}
	
	public String getUninstallClass() { return getString("uninstallClass");}
	public void setUninstallClass(String s) { set("uninstallClass", s);}
	
	public String getVersion() { return getString("version");}
	public void setVersion(String s) { set("version", s);}
	
	public List<PackageTypeMembers> getTypes() { return (List<PackageTypeMembers>)get("types");}
	public void setTypes(List<PackageTypeMembers> list) { set("types", list);}
	
	public void addTypes(PackageTypeMembers v) {
		List<PackageTypeMembers> list = getTypes();
		if (list == null) {
			list = new ArrayList<PackageTypeMembers>();
			setTypes(list);
		}
		list.add(v);
	}
	
	public void addTypes(String name, String... members) {
		PackageTypeMembers types = new PackageTypeMembers();
		types.setName(name);
		for (String s : members) {
			types.addMembers(s);
		}
		addTypes(types);
	}
}

