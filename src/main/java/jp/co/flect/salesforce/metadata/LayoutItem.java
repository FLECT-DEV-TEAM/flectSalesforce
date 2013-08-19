package jp.co.flect.salesforce.metadata;

import java.util.ArrayList;
import java.util.List;
import jp.co.flect.soap.SimpleObject;

/**
   <xsd:complexType name="LayoutItem">
    <xsd:sequence>
     <xsd:element name="behavior" minOccurs="0" type="tns:UiBehavior"/>
     <xsd:element name="customLink" minOccurs="0" type="xsd:string"/>
     <xsd:element name="emptySpace" minOccurs="0" type="xsd:boolean"/>
     <xsd:element name="field" minOccurs="0" type="xsd:string"/>
     <xsd:element name="height" minOccurs="0" type="xsd:int"/>
     <xsd:element name="page" minOccurs="0" type="xsd:string"/>
     <xsd:element name="scontrol" minOccurs="0" type="xsd:string"/>
     <xsd:element name="showLabel" minOccurs="0" type="xsd:boolean"/>
     <xsd:element name="showScrollbars" minOccurs="0" type="xsd:boolean"/>
     <xsd:element name="width" minOccurs="0" type="xsd:string"/>
    </xsd:sequence>
   </xsd:complexType>
*/
public class LayoutItem extends SimpleObject {
	
	public enum UiBehavior {
		Edit,
		Required,
		Readonly
	}
	
	public UiBehavior getBehavior() { return getEnumValue(UiBehavior.class, "behavior");}
	public void setBehavior(UiBehavior v) { set("behavior", v.toString());}
	
	public String getCustomLink() { return getString("customLink");}
	public void setCustomLink(String s) { set("customLink", s);}
	
	public boolean isEmptySpace() { return getBoolean("emptySpace");}
	public void setEmptySpace(boolean b) { set("emptySpace", b);}
	
	public String getField() { return getString("field");}
	public void setField(String s) { set("field", s);}
	
	public int getHeight() { return getInt("height");}
	public void setHeight(int n) { set("height", n);}
	
	public String getPage() { return getString("page");}
	public void setPage(String s) { set("page", s);}
	
	public String getScontrol() { return getString("scontrol");}
	public void setScontrol(String s) { set("scontrol", s);}
	
	public boolean isShowLabel() { return getBoolean("showLabel");}
	public void setShowLabel(boolean b) { set("showLabel", b);}
	
	public boolean isShowScrollbars() { return getBoolean("showScrollbars");}
	public void setShowScrollbars(boolean b) { set("showScrollbars", b);}
	
	public int getWidth() { return getInt("width");}
	public void setWidth(int n) { set("width", n);}
	
}
