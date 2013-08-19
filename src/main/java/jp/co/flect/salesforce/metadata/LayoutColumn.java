package jp.co.flect.salesforce.metadata;

import java.util.ArrayList;
import java.util.List;
import jp.co.flect.soap.SimpleObject;

/**
   <xsd:complexType name="LayoutColumn">
    <xsd:sequence>
     <xsd:element name="layoutItems" minOccurs="0" maxOccurs="unbounded" type="tns:LayoutItem"/>
     <xsd:element name="reserved" minOccurs="0" type="xsd:string"/>
    </xsd:sequence>
   </xsd:complexType>
*/
public class LayoutColumn extends SimpleObject {
	
	public String getReserved() { return getString("reserved");}
	public void setReserved(String s) { set("reserved", s);}
	
	public List<LayoutItem> getLayoutItems() { return (List<LayoutItem>)get("layoutItems");}
	public void addLayoutItem(LayoutItem v) {
		List<LayoutItem> list = getLayoutItems();
		if (list == null) {
			list = new ArrayList<LayoutItem>();
			set("layoutItems", list);
		}
		list.add(v);
	}
	
	public void removeLayoutItem(LayoutItem v) {
		List<LayoutItem> list = getLayoutItems();
		if (list != null) {
			list.remove(v);
		}
	}
	
}
