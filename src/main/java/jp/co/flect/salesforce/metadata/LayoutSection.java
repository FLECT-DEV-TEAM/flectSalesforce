package jp.co.flect.salesforce.metadata;

import java.util.ArrayList;
import java.util.List;
import jp.co.flect.soap.SimpleObject;

/**
   <xsd:complexType name="LayoutSection">
    <xsd:sequence>
     <xsd:element name="customLabel" minOccurs="0" type="xsd:boolean"/>
     <xsd:element name="detailHeading" minOccurs="0" type="xsd:boolean"/>
     <xsd:element name="editHeading" minOccurs="0" type="xsd:boolean"/>
     <xsd:element name="label" minOccurs="0" type="xsd:string"/>
     <xsd:element name="layoutColumns" minOccurs="0" maxOccurs="unbounded" type="tns:LayoutColumn"/>
     <xsd:element name="style" type="tns:LayoutSectionStyle"/>
    </xsd:sequence>
   </xsd:complexType>
*/
public class LayoutSection extends SimpleObject {
	
	public enum LayoutSectionStyle {
		TwoColumnsTopToBottom,
		TwoColumnsLeftToRight,
		OneColumn,
		CustomLinks
	}
	
	public boolean isCustomLabel() { return getBoolean("customLabel");}
	public void setCustomLabel(boolean b) { set("customLabel", b);}
	
	public boolean isDetailHeading() { return getBoolean("detailHeading");}
	public void setDetailHeading(boolean b) { set("detailHeading", b);}
	
	public boolean isEditHeading() { return getBoolean("editHeading");}
	public void setEditHeading(boolean b) { set("editHeading", b);}
	
	public String getLabel() { return getString("label");}
	public void setLabel(String s) { set("label", s);}
	
	public List<LayoutColumn> getLayoutColumns() { return (List<LayoutColumn>)get("layoutColumns");}
	public void addLayoutColumn(LayoutColumn v) {
		List<LayoutColumn> list = getLayoutColumns();
		if (list == null) {
			list = new ArrayList<LayoutColumn>();
			set("layoutColumns", list);
		}
		list.add(v);
	}
	
	public void removeLayoutColumn(LayoutColumn v) {
		List<LayoutColumn> list = getLayoutColumns();
		if (list != null) {
			list.remove(v);
		}
	}
	
	public LayoutSectionStyle getStyle() { return getEnumValue(LayoutSectionStyle.class, "style");}
	public void setStyle(LayoutSectionStyle v) { set("style", v.toString());}
	
}
