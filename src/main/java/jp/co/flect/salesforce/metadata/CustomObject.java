package jp.co.flect.salesforce.metadata;

import jp.co.flect.soap.SimpleObject;
import java.util.ArrayList;
import java.util.List;

public class CustomObject extends BaseMetadata {
	
	public enum DeploymentStatus {
		InDevelopment,
		Deployed
	};
	
	public enum SharingModel {
		Private,
		Read,
		ReadWrite,
		ReadWriteTransfer,
		FullAccess,
		ControlledByParent,
	}
	
	public CustomObject() {
		super(MetadataType.CustomObject);
	}
	
	public CustomField getNameField() { return (CustomField)get("nameField");}
	public void setNameField(CustomField v) { set("nameField", v);}
	
	public String getLabel() { return getString("label");}
	public void setLabel(String s) { set("label", s);}
	
	public DeploymentStatus getDeploymentStatus() { return getEnumValue(DeploymentStatus.class, "deploymentStatus");}
	public void setDeploymentStatus(DeploymentStatus v) { set("deploymentStatus", v.toString());}
	
	public SharingModel getSharingModel() { return getEnumValue(SharingModel.class, "sharingModel");}
	public void setSharingModel(SharingModel v) { set("sharingModel", v.toString());}
	
	public List<CustomField> getFields() { return (List<CustomField>)get("fields");}
	public void addField(CustomField f) {
		List<CustomField> list = getFields();
		if (list == null) {
			list = new ArrayList<CustomField>();
			set("fields", list);
		}
		list.add(f);
	}
	
	public void removeField(CustomField f) {
		List<CustomField> list = getFields();
		if (list != null) {
			list.remove(f);
		}
	}
	
	public String getCustomHelp() { return getString("customHelp");}
	public void setCustomHelp(String s) { set("customHelp", s);}
	
	public String getCustomHelpPage() { return getString("customHelpPage");}
	public void setCustomHelpPage(String s) { set("customHelpPage", s);}
	
	public String getDescription() { return getString("description");}
	public void setDescription(String s) { set("description", s);}
	
	public String getPluralLabel() { return getString("pluralLabel");}
	public void setPluralLabel(String s) { set("pluralLabel", s);}
	
	public boolean isDeprecated() { return getBoolean("deprecated");}
	public void setDeprecated(boolean b) { set("deprecated", b);}
	
	public boolean isEnableActivities() { return getBoolean("enableActivities");}
	public void setEnableActivities(boolean b) { set("enableActivities", b);}
	
	public boolean isEnableDivisions() { return getBoolean("enableDivisions");}
	public void setEnableDivisions(boolean b) { set("enableDivisions", b);}
	
	public boolean isEnableEnhancedLookup() { return getBoolean("enableEnhancedLookup");}
	public void setEnableEnhancedLookup(boolean b) { set("enableEnhancedLookup", b);}
	
	public boolean isEnableFeeds() { return getBoolean("enableFeeds");}
	public void setEnableFeeds(boolean b) { set("enableFeeds", b);}
	
	public boolean isEnableHistory() { return getBoolean("enableHistory");}
	public void setEnableHistory(boolean b) { set("enableHistory", b);}
	
	public boolean isEnableReports() { return getBoolean("enableReports");}
	public void setEnableReports(boolean b) { set("enableReports", b);}
	
	public boolean isHousehold() { return getBoolean("household");}
	public void setHousehold(boolean b) { set("household", b);}
	
	public boolean isRecordTypeTrackFeedHistory() { return getBoolean("recordTypeTrackFeedHistory");}
	public void setRecordTypeTrackFeedHistory(boolean b) { set("recordTypeTrackFeedHistory", b);}
	
	public boolean isRecordTypeTrackHistory() { return getBoolean("recordTypeTrackHistory");}
	public void setRecordTypeTrackHistory(boolean b) { set("recordTypeTrackHistory", b);}
	
	/*
       
       
       <xsd:element name="actionOverrides" minOccurs="0" maxOccurs="unbounded" type="tns:ActionOverride"/>
       <xsd:element name="articleTypeChannelDisplay" minOccurs="0" type="tns:ArticleTypeChannelDisplay"/>
       <xsd:element name="businessProcesses" minOccurs="0" maxOccurs="unbounded" type="tns:BusinessProcess"/>
       <xsd:element name="customSettingsType" minOccurs="0" type="tns:CustomSettingsType"/>
       <xsd:element name="customSettingsVisibility" minOccurs="0" type="tns:CustomSettingsVisibility"/>
       
       <xsd:element name="fieldSets" minOccurs="0" maxOccurs="unbounded" type="tns:FieldSet"/>
       <xsd:element name="gender" minOccurs="0" type="tns:Gender"/>
       <xsd:element name="listViews" minOccurs="0" maxOccurs="unbounded" type="tns:ListView"/>
       <xsd:element name="namedFilters" minOccurs="0" maxOccurs="unbounded" type="tns:NamedFilter"/>
       <xsd:element name="recordTypes" minOccurs="0" maxOccurs="unbounded" type="tns:RecordType"/>
       <xsd:element name="searchLayouts" minOccurs="0" type="tns:SearchLayouts"/>
       <xsd:element name="sharingReasons" minOccurs="0" maxOccurs="unbounded" type="tns:SharingReason"/>
       <xsd:element name="sharingRecalculations" minOccurs="0" maxOccurs="unbounded" type="tns:SharingRecalculation"/>
       <xsd:element name="startsWith" minOccurs="0" type="tns:StartsWith"/>
       <xsd:element name="validationRules" minOccurs="0" maxOccurs="unbounded" type="tns:ValidationRule"/>
       <xsd:element name="webLinks" minOccurs="0" maxOccurs="unbounded" type="tns:WebLink"/>
	*/
}

