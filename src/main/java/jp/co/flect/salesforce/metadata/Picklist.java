package jp.co.flect.salesforce.metadata;

import java.util.ArrayList;
import java.util.List;
import jp.co.flect.soap.SimpleObject;

/**
   <xsd:complexType name="Picklist">
    <xsd:sequence>
     <xsd:element name="controllingField" minOccurs="0" type="xsd:string"/>
     <xsd:element name="picklistValues" minOccurs="0" maxOccurs="unbounded" type="tns:PicklistValue"/>
     <xsd:element name="sorted" type="xsd:boolean"/>
    </xsd:sequence>
   </xsd:complexType>
*/
public class Picklist extends SimpleObject {
	
	public boolean isSorted() { return getBoolean("sorted");}
	public void setSorted(boolean b) { set("sorted", b);}
	
	public String getControllingField() { return getString("controllingField");}
	public void setControllingField(String s) { set("controllingField", s);}
	
	public List<PicklistValue> getPicklistValues() { return (List<PicklistValue>)get("picklistValues");}
	public void addPicklistValue(PicklistValue v) {
		List<PicklistValue> list = getPicklistValues();
		if (list == null) {
			list = new ArrayList<PicklistValue>();
			set("picklistValues", list);
		}
		list.add(v);
	}
	
	public void removePicklistValue(PicklistValue v) {
		List<PicklistValue> list = getPicklistValues();
		if (list != null) {
			list.remove(v);
		}
	}
	
}
