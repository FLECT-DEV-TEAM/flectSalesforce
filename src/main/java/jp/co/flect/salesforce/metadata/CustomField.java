package jp.co.flect.salesforce.metadata;

import java.util.ArrayList;
import java.util.List;
import jp.co.flect.soap.SimpleObject;

/*
   <xsd:complexType name="CustomField">
    <xsd:complexContent>
     <xsd:extension base="tns:Metadata">
      <xsd:sequence>
       <xsd:element name="caseSensitive" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="customDataType" minOccurs="0" type="xsd:string"/>
       <xsd:element name="defaultValue" minOccurs="0" type="xsd:string"/>
       <xsd:element name="deleteConstraint" minOccurs="0" type="tns:DeleteConstraint"/>
       <xsd:element name="deprecated" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="description" minOccurs="0" type="xsd:string"/>
       <xsd:element name="displayFormat" minOccurs="0" type="xsd:string"/>
       <xsd:element name="escapeMarkup" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="externalDeveloperName" minOccurs="0" type="xsd:string"/>
       <xsd:element name="externalId" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="formula" minOccurs="0" type="xsd:string"/>
       <xsd:element name="formulaTreatBlanksAs" minOccurs="0" type="tns:TreatBlanksAs"/>
       <xsd:element name="inlineHelpText" minOccurs="0" type="xsd:string"/>
       <xsd:element name="label" minOccurs="0" type="xsd:string"/>
       <xsd:element name="length" minOccurs="0" type="xsd:int"/>
       <xsd:element name="maskChar" minOccurs="0" type="tns:EncryptedFieldMaskChar"/>
       <xsd:element name="maskType" minOccurs="0" type="tns:EncryptedFieldMaskType"/>
       <xsd:element name="picklist" minOccurs="0" type="tns:Picklist"/>
       <xsd:element name="populateExistingRows" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="precision" minOccurs="0" type="xsd:int"/>
       <xsd:element name="referenceTo" minOccurs="0" type="xsd:string"/>
       <xsd:element name="relationshipLabel" minOccurs="0" type="xsd:string"/>
       <xsd:element name="relationshipName" minOccurs="0" type="xsd:string"/>
       <xsd:element name="relationshipOrder" minOccurs="0" type="xsd:int"/>
       <xsd:element name="reparentableMasterDetail" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="required" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="restrictedAdminField" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="scale" minOccurs="0" type="xsd:int"/>
       <xsd:element name="startingNumber" minOccurs="0" type="xsd:int"/>
       <xsd:element name="stripMarkup" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="summarizedField" minOccurs="0" type="xsd:string"/>
       <xsd:element name="summaryFilterItems" minOccurs="0" maxOccurs="unbounded" type="tns:FilterItem"/>
       <xsd:element name="summaryForeignKey" minOccurs="0" type="xsd:string"/>
       <xsd:element name="summaryOperation" minOccurs="0" type="tns:SummaryOperations"/>
       <xsd:element name="trackFeedHistory" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="trackHistory" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="type" type="tns:FieldType"/>
       <xsd:element name="unique" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="visibleLines" minOccurs="0" type="xsd:int"/>
       <xsd:element name="writeRequiresMasterRead" minOccurs="0" type="xsd:boolean"/>
      </xsd:sequence>
     </xsd:extension>
    </xsd:complexContent>
   </xsd:complexType>
*/
public class CustomField extends BaseMetadata {
	
	public enum FieldType {
		AutoNumber,
		Lookup,
		MasterDetail,
		Checkbox,
		Currency,
		Date,
		DateTime,
		Email,
		Number,
		Percent,
		Phone,
		Picklist,
		MultiselectPicklist,
		Text,
		TextArea,
		LongTextArea,
		Html,
		Url,
		EncryptedText,
		Summary,
		Hierarchy,
		File,
		CustomDataType,
	};
	
	public enum DeleteConstraint {
		Cascade,
		Restrict,
		SetNull,
	}
	
	public enum TreatBlanksAs {
		BlankAsBlank,
		BlankAsZero,
	};
	
	public enum EncryptedFieldMaskChar {
		asterisk,
		X,
	};
	
	public enum EncryptedFieldMaskType {
		all,
		creditCard,
		ssn,
		lastFour,
		sin,
		nino,
	};
	
	public enum SummaryOperations {
		count,
		sum,
		min,
		max,
	}
	
	public CustomField() {
		super(MetadataType.CustomField);
	}
	
	public CustomField(FieldType type, String fullname, String label) {
		this();
		setFieldType(type);
		setFullName(fullname);
		setLabel(label);
	}
	
	public boolean isCaseSensitive() { return getBoolean("caseSensitive");}
	public void setCaseSensitive(boolean b) { set("caseSensitive", b);}

	public boolean isDeprecated() { return getBoolean("deprecated");}
	public void setDeprecated(boolean b) { set("deprecated", b);}

	public boolean isEscapeMarkup() { return getBoolean("escapeMarkup");}
	public void setEscapeMarkup(boolean b) { set("escapeMarkup", b);}

	public boolean isExternalId() { return getBoolean("externalId");}
	public void setExternalId(boolean b) { set("externalId", b);}

	public boolean isPopulateExistingRows() { return getBoolean("populateExistingRows");}
	public void setPopulateExistingRows(boolean b) { set("populateExistingRows", b);}

	public boolean isReparentableMasterDetail() { return getBoolean("reparentableMasterDetail");}
	public void setReparentableMasterDetail(boolean b) { set("reparentableMasterDetail", b);}

	public boolean isRequired() { return getBoolean("required");}
	public void setRequired(boolean b) { set("required", b);}

	public boolean isRestrictedAdminField() { return getBoolean("restrictedAdminField");}
	public void setRestrictedAdminField(boolean b) { set("restrictedAdminField", b);}

	public boolean isStripMarkup() { return getBoolean("stripMarkup");}
	public void setStripMarkup(boolean b) { set("stripMarkup", b);}

	public boolean isTrackFeedHistory() { return getBoolean("trackFeedHistory");}
	public void setTrackFeedHistory(boolean b) { set("trackFeedHistory", b);}

	public boolean isTrackHistory() { return getBoolean("trackHistory");}
	public void setTrackHistory(boolean b) { set("trackHistory", b);}

	public boolean isUnique() { return getBoolean("unique");}
	public void setUnique(boolean b) { set("unique", b);}

	public boolean isWriteRequiresMasterRead() { return getBoolean("writeRequiresMasterRead");}
	public void setWriteRequiresMasterRead(boolean b) { set("writeRequiresMasterRead", b);}

	public String getCustomDataType() { return getString("customDataType");}
	public void setCustomDataType(String s) { set("customDataType", s);}

	public String getDefaultValue() { return getString("defaultValue");}
	public void setDefaultValue(String s) { set("defaultValue", s);}

	public String getDescription() { return getString("description");}
	public void setDescription(String s) { set("description", s);}

	public String getDisplayFormat() { return getString("displayFormat");}
	public void setDisplayFormat(String s) { set("displayFormat", s);}

	public String getExternalDeveloperName() { return getString("externalDeveloperName");}
	public void setExternalDeveloperName(String s) { set("externalDeveloperName", s);}

	public String getFormula() { return getString("formula");}
	public void setFormula(String s) { set("formula", s);}

	public String getInlineHelpText() { return getString("inlineHelpText");}
	public void setInlineHelpText(String s) { set("inlineHelpText", s);}

	public String getLabel() { return getString("label");}
	public void setLabel(String s) { set("label", s);}

	public String getReferenceTo() { return getString("referenceTo");}
	public void setReferenceTo(String s) { set("referenceTo", s);}

	public String getRelationshipLabel() { return getString("relationshipLabel");}
	public void setRelationshipLabel(String s) { set("relationshipLabel", s);}

	public String getRelationshipName() { return getString("relationshipName");}
	public void setRelationshipName(String s) { set("relationshipName", s);}

	public String getSummarizedField() { return getString("summarizedField");}
	public void setSummarizedField(String s) { set("summarizedField", s);}

	public String getSummaryForeignKey() { return getString("summaryForeignKey");}
	public void setSummaryForeignKey(String s) { set("summaryForeignKey", s);}

	public DeleteConstraint getDeleteConstraint() { return getEnumValue(DeleteConstraint.class, "deleteConstraint");}
	public void setDeleteConstraint(DeleteConstraint v) { set("deleteConstraint", v.toString());}
	
	public TreatBlanksAs getFormulaTreatBlanksAs() { return getEnumValue(TreatBlanksAs.class, "formulaTreatBlanksAs");}
	public void setFormulaTreatBlanksAs(TreatBlanksAs v) { set("formulaTreatBlanksAs", v.toString());}
	
	public EncryptedFieldMaskChar getMaskChar() { return getEnumValue(EncryptedFieldMaskChar.class, "maskChar");}
	public void setMaskChar(EncryptedFieldMaskChar v) { set("maskChar", v.toString());}
	
	public EncryptedFieldMaskType getMaskType() { return getEnumValue(EncryptedFieldMaskType.class, "maskType");}
	public void setMaskType(EncryptedFieldMaskType v) { set("maskType", v.toString());}
	
	public SummaryOperations getSummaryOperation() { return getEnumValue(SummaryOperations.class, "summaryOperation");}
	public void setSummaryOperation(SummaryOperations v) { set("summaryOperation", v.toString());}
	
	public FieldType getFieldType() { return getEnumValue(FieldType.class, "type");}
	public void setFieldType(FieldType v) { set("type", v.toString());}

	public int getLength() { return getInt("length");}
	public void setLength(int n) { set("length", n);}
	
	public int getPrecision() { return getInt("precision");}
	public void setPrecision(int n) { set("precision", n);}
	
	public int getRelationshipOrder() { return getInt("relationshipOrder");}
	public void setRelationshipOrder(int n) { set("relationshipOrder", n);}
	
	public int getScale() { return getInt("scale");}
	public void setScale(int n) { set("scale", n);}
	
	public int getStartingNumber() { return getInt("startingNumber");}
	public void setStartingNumber(int n) { set("startingNumber", n);}
	
	public int getVisibleLines() { return getInt("visibleLines");}
	public void setVisibleLines(int n) { set("visibleLines", n);}
	
	public Picklist getPicklist() { return (Picklist)get("picklist");}
	public void setPicklist(Picklist v) { set("picklist", v);}
	
	List<FilterItem> getSummaryFilterItems() { return (List)get("summaryFilterItems");}
	public void addSummaryFilterItems(FilterItem v) { 
		List<FilterItem> list = getSummaryFilterItems();
		if (list == null) {
			list = new ArrayList<FilterItem>();
			set("summaryFilterItems", list);
		}
		list.add(v);
	}
	
	public void removeSummaryFilterItems(FilterItem v) {
		List<FilterItem> list = getSummaryFilterItems();
		if (list != null) {
			list.remove(v);
		}
	}
}

