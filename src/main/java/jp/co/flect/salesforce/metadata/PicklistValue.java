package jp.co.flect.salesforce.metadata;

import java.util.ArrayList;
import java.util.List;

/**
   <xsd:complexType name="PicklistValue">
    <xsd:complexContent>
     <xsd:extension base="tns:Metadata">
      <xsd:sequence>
       <xsd:element name="allowEmail" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="closed" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="color" minOccurs="0" type="xsd:string"/>
       <xsd:element name="controllingFieldValues" minOccurs="0" maxOccurs="unbounded" type="xsd:string"/>
       <xsd:element name="converted" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="cssExposed" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="default" type="xsd:boolean"/>
       <xsd:element name="description" minOccurs="0" type="xsd:string"/>
       <xsd:element name="forecastCategory" minOccurs="0" type="tns:ForecastCategories"/>
       <xsd:element name="highPriority" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="probability" minOccurs="0" type="xsd:int"/>
       <xsd:element name="reverseRole" minOccurs="0" type="xsd:string"/>
       <xsd:element name="reviewed" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="won" minOccurs="0" type="xsd:boolean"/>
      </xsd:sequence>
     </xsd:extension>
    </xsd:complexContent>
   </xsd:complexType>
*/
public class PicklistValue extends BaseMetadata {
	
	public enum ForecastCategories {
		Omitted,
		Pipeline,
		BestCase,
		Forecast,
		Closed,
	};
	
	public PicklistValue() {
		super(MetadataType.PicklistValue);
	}
	
	public boolean isAllowEmail() { return getBoolean("allowEmail");}
	public void setAllowEmail(boolean b) { set("allowEmail", b);}
	
	public boolean isClosed() { return getBoolean("closed");}
	public void setClosed(boolean b) { set("closed", b);}
	
	public boolean isConverted() { return getBoolean("converted");}
	public void setConverted(boolean b) { set("converted", b);}
	
	public boolean isCssExposed() { return getBoolean("cssExposed");}
	public void setCssExposed(boolean b) { set("cssExposed", b);}
	
	public boolean isDefault() { return getBoolean("default");}
	public void setDefault(boolean b) { set("default", b);}
	
	public boolean isHighPriority() { return getBoolean("highPriority");}
	public void setHighPriority(boolean b) { set("highPriority", b);}
	
	public boolean isReviewed() { return getBoolean("reviewed");}
	public void setReviewed(boolean b) { set("reviewed", b);}
	
	public boolean isWon() { return getBoolean("won");}
	public void setWon(boolean b) { set("won", b);}
	
	public String getColor() { return getString("color");}
	public void setColor(String s) { set("color", s);}
	
	public List<String> getControllingFieldValues() { return (List)get("controllingFieldValues");}
	public void addControllingFieldValues(String s) { 
		List<String> list = getControllingFieldValues();
		if (list == null) {
			list = new ArrayList<String>();
			set("controllingFieldValues", list);
		}
		list.add(s);
	}
	
	public void removeControllingFieldValues(String s) {
		List<String> list = getControllingFieldValues();
		if (list != null) {
			list.remove(s);
		}
	}
	
	public String getDescription() { return getString("description");}
	public void setDescription(String s) { set("description", s);}
	
	public String getReverseRole() { return getString("reverseRole");}
	public void setReverseRole(String s) { set("reverseRole", s);}
	
	public ForecastCategories getForecastCategory() { return getEnumValue(ForecastCategories.class, "forecastCategory");}
	public void setforecastCategory(ForecastCategories v) { set("forecastCategory", v.toString());}
	
	public int getProbability() { return getInt("probability");}
	public void setProbability(int n) { set("probability", n);}
	
}

