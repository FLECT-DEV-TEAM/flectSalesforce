package jp.co.flect.salesforce.metadata;

import java.util.ArrayList;
import java.util.List;
import jp.co.flect.soap.SimpleObject;

/*
   <xsd:complexType name="Layout">
    <xsd:complexContent>
     <xsd:extension base="tns:Metadata">
      <xsd:sequence>
       <xsd:element name="customButtons" minOccurs="0" maxOccurs="unbounded" type="xsd:string"/>
       <xsd:element name="customConsoleComponents" minOccurs="0" type="tns:CustomConsoleComponents"/>
       <xsd:element name="emailDefault" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="excludeButtons" minOccurs="0" maxOccurs="unbounded" type="xsd:string"/>
       <xsd:element name="headers" minOccurs="0" maxOccurs="unbounded" type="tns:LayoutHeader"/>
       <xsd:element name="layoutSections" minOccurs="0" maxOccurs="unbounded" type="tns:LayoutSection"/>
       <xsd:element name="miniLayout" minOccurs="0" type="tns:MiniLayout"/>
       <xsd:element name="multilineLayoutFields" minOccurs="0" maxOccurs="unbounded" type="xsd:string"/>
       <xsd:element name="relatedLists" minOccurs="0" maxOccurs="unbounded" type="tns:RelatedListItem"/>
       <xsd:element name="relatedObjects" minOccurs="0" maxOccurs="unbounded" type="xsd:string"/>
       <xsd:element name="runAssignmentRulesDefault" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="showEmailCheckbox" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="showHighlightsPanel" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="showInteractionLogPanel" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="showKnowledgeComponent" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="showRunAssignmentRulesCheckbox" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="showSolutionSection" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="showSubmitAndAttachButton" minOccurs="0" type="xsd:boolean"/>
       <xsd:element name="summaryLayout" minOccurs="0" type="tns:SummaryLayout"/>
      </xsd:sequence>
     </xsd:extension>
    </xsd:complexContent>
*/
public class Layout extends BaseMetadata {
	
	public Layout() {
		super(MetadataType.Layout);
	}
	
	public List<String> getCustomButtons() { return (List<String>)get("customButtons");}
	public void addCustomButton(String v) {
		List<String> list = getCustomButtons();
		if (list == null) {
			list = new ArrayList<String>();
			set("customButtons", list);
		}
		list.add(v);
	}
	
	public void removeCustomButton(String v) {
		List<String> list = getCustomButtons();
		if (list != null) {
			list.remove(v);
		}
	}
	
	public List<LayoutSection> getLayoutSections() { return (List<LayoutSection>)get("layoutSections");}
	public void addLayoutSection(LayoutSection v) {
		List<LayoutSection> list = getLayoutSections();
		if (list == null) {
			list = new ArrayList<LayoutSection>();
			set("layoutSections", list);
		}
		list.add(v);
	}
	
	public void removeLayoutSection(LayoutSection v) {
		List<LayoutSection> list = getLayoutSections();
		if (list != null) {
			list.remove(v);
		}
	}
	
	//ToDo
	// <xsd:element name="customConsoleComponents" minOccurs="0" type="tns:CustomConsoleComponents"/>
	// <xsd:element name="emailDefault" minOccurs="0" type="xsd:boolean"/>
	// <xsd:element name="excludeButtons" minOccurs="0" maxOccurs="unbounded" type="xsd:string"/>
	// <xsd:element name="headers" minOccurs="0" maxOccurs="unbounded" type="tns:LayoutHeader"/>
	
	// <xsd:element name="miniLayout" minOccurs="0" type="tns:MiniLayout"/>
	// <xsd:element name="multilineLayoutFields" minOccurs="0" maxOccurs="unbounded" type="xsd:string"/>
	// <xsd:element name="relatedLists" minOccurs="0" maxOccurs="unbounded" type="tns:RelatedListItem"/>
	// <xsd:element name="relatedObjects" minOccurs="0" maxOccurs="unbounded" type="xsd:string"/>
	// <xsd:element name="runAssignmentRulesDefault" minOccurs="0" type="xsd:boolean"/>
	// <xsd:element name="showEmailCheckbox" minOccurs="0" type="xsd:boolean"/>
	// <xsd:element name="showHighlightsPanel" minOccurs="0" type="xsd:boolean"/>
	// <xsd:element name="showInteractionLogPanel" minOccurs="0" type="xsd:boolean"/>
	// <xsd:element name="showKnowledgeComponent" minOccurs="0" type="xsd:boolean"/>
	// <xsd:element name="showRunAssignmentRulesCheckbox" minOccurs="0" type="xsd:boolean"/>
	// <xsd:element name="showSolutionSection" minOccurs="0" type="xsd:boolean"/>
	// <xsd:element name="showSubmitAndAttachButton" minOccurs="0" type="xsd:boolean"/>
	// <xsd:element name="summaryLayout" minOccurs="0" type="tns:SummaryLayout"/>
}

