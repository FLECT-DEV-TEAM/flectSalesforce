package jp.co.flect.salesforce.metadata;

import java.util.ArrayList;
import java.util.List;
import jp.co.flect.soap.SimpleObject;

/**
   <xsd:complexType name="FilterItem">
    <xsd:sequence>
     <xsd:element name="field" type="xsd:string"/>
     <xsd:element name="operation" type="tns:FilterOperation"/>
     <xsd:element name="value" minOccurs="0" type="xsd:string"/>
     <xsd:element name="valueField" minOccurs="0" type="xsd:string"/>
    </xsd:sequence>
   </xsd:complexType>
*/
public class FilterItem extends SimpleObject {
	
	public String getField() { return getString("field");}
	public void setField(String s) { set("field", s);}
	
	public FilterOperation getOperation() { return getEnumValue(FilterOperation.class, "operation");}
	public void setOperation(FilterOperation v) { set("operation", v.toString());}
	
	public String getValue() { return getString("value");}
	public void setValue(String s) { set("value", s);}
	
	public String getValueField() { return getString("valueField");}
	public void setValueField(String s) { set("valueField", s);}
	
}
