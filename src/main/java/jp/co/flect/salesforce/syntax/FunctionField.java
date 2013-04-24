package jp.co.flect.salesforce.syntax;

import jp.co.flect.salesforce.Metadata;
import jp.co.flect.salesforce.SObjectDef;

public class FunctionField implements SelectField {
	
	private String name;
	private SimpleField arg;
	private String alias;
	
	public FunctionField(String name, SimpleField arg) {
		this.name = name;
		this.arg = arg;
	}
	
	public int getType() { return SelectField.FUNCTION;}
	
	public String getName() { return this.name;}
	public SimpleField getArg() { return this.arg;}
	
	public String getAlias() { return this.alias;}
	public void setAlias(String s) { this.alias = s;}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(this.name).append("(");
		if (this.arg != null) {
			buf.append(this.arg);
		}
		buf.append(")");
		if (this.alias != null) {
			buf.append(" ").append(this.alias);
		}
		return buf.toString();
	}
	
	public void normalize(Metadata meta, SObjectDef parent) {
		if (this.arg != null) {
			this.arg.normalize(meta, parent);
		}
	}
}
