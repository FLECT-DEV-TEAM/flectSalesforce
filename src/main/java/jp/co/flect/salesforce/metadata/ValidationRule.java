package jp.co.flect.salesforce.metadata;

/**
	xsd:complexType name="ValidationRule">
    <xsd:complexContent>
     <xsd:extension base="tns:Metadata">
      <xsd:sequence>
       <xsd:element name="active" type="xsd:boolean"/>
       <xsd:element name="description" minOccurs="0" type="xsd:string"/>
       <xsd:element name="errorConditionFormula" type="xsd:string"/>
       <xsd:element name="errorDisplayField" minOccurs="0" type="xsd:string"/>
       <xsd:element name="errorMessage" type="xsd:string"/>
      </xsd:sequence>
     </xsd:extension>
    </xsd:complexContent>
   </xsd:complexType>
*/
public class ValidationRule extends BaseMetadata  {
	
	public ValidationRule() {
		super(MetadataType.ValidationRule);
		setActive(true);
	}
	
	public boolean isActive() { return getBoolean("active");}
	public void setActive(boolean b) { set("active", b);}
	
	public String getDescription() { return getString("description");}
	public void setDescription(String s) { set("description", s);}
	
	public String getErrorConditionFormula() { return getString("errorConditionFormula");}
	public void setErrorConditionFormula(String s) { set("errorConditionFormula", s);}
	
	public String getErrorDisplayField() { return getString("errorDisplayField");}
	public void setErrorDisplayField(String s) { set("errorDisplayField", s);}
	
	public String getErrorMessage() { return getString("errorMessage");}
	public void setErrorMessage(String s) { set("errorMessage", s);}

}

