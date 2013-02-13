package jp.co.flect.salesforce.metadata;

/**
   <xsd:simpleType name="FilterOperation">
    <xsd:restriction base="xsd:string">
     <xsd:enumeration value="equals"/>
     <xsd:enumeration value="notEqual"/>
     <xsd:enumeration value="lessThan"/>
     <xsd:enumeration value="greaterThan"/>
     <xsd:enumeration value="lessOrEqual"/>
     <xsd:enumeration value="greaterOrEqual"/>
     <xsd:enumeration value="contains"/>
     <xsd:enumeration value="notContain"/>
     <xsd:enumeration value="startsWith"/>
     <xsd:enumeration value="includes"/>
     <xsd:enumeration value="excludes"/>
    </xsd:restriction>
   </xsd:simpleType>
*/
public enum FilterOperation {
	equals,
	notEqual,
	lessThan,
	greaterThan,
	lessOrEqual,
	greaterOrEqual,
	contains,
	notContain,
	startsWith,
	includes,
	excludes,
}
