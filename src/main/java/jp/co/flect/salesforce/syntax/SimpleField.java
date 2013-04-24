package jp.co.flect.salesforce.syntax;

import jp.co.flect.salesforce.Metadata;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.RelationDef;
import jp.co.flect.salesforce.SObjectDef;

public class SimpleField implements SelectField {
	
	private String name;
	private String alias;
	
	public SimpleField(String name) {
		this.name = name;
	}
	
	public int getType() { return SelectField.SIMPLE;}
	
	public String getName() { return this.name;}
	
	public String getAlias() { return this.alias;}
	public void setAlias(String s) { this.alias = s;}
	
	public String getAggregateName() {
		if (this.alias != null) {
			return this.alias;
		}
		int idx = this.name.lastIndexOf('.');
		return idx == -1 ? this.name : this.name.substring(idx+1);
	}
	
	public String toString() { 
		if (this.alias == null) {
			return getName();
		} else {
			return getName() + " " + alias;
		}
	}
	
	public void normalize(Metadata meta, SObjectDef parent) {
		String[] names = this.name.split("\\.");
		if (names.length > 1 && names[0].equalsIgnoreCase(parent.getName())) {
			String[] temp = new String[names.length-1];
			System.arraycopy(names, 1, temp, 0, names.length-1);
			names = temp;
		}
		for (int i=0; i<names.length-1; i++) {
			FieldDef f = parent.getSingleRelation(names[i]);
			if (f != null) {
				names[i] = f.getRelationshipName();
				parent = meta.getObjectDef(f.getReferenceToName());
			} else {
				parent = null;
			}
			if (parent == null) {
				break;
			}
		}
		if (parent != null) {
			String name = names[names.length-1];
			FieldDef f = parent.getField(name);
			if (f != null) {
				names[names.length-1] = f.getName();
			} else {
				RelationDef rel = parent.getMultipleRelation(name);
				if (rel != null) {
					names[names.length-1] = rel.getRelationshipName();
				}
			}
		}
		if (names.length == 1) {
			this.name = names[0];
		} else {
			StringBuilder buf = new StringBuilder();
			buf.append(names[0]);
			for (int i=1; i<names.length; i++) {
				buf.append('.').append(names[i]);
			}
			this.name = buf.toString();
		}
	}
	
	public FieldDef getField(Metadata meta, SObjectDef parent) {
		//assert normalize
		String[] names = this.name.split("\\.");
		for (int i=0; i<names.length-1; i++) {
			FieldDef f = parent.getSingleRelation(names[i]);
			if (f == null) {
				return null;
			}
			parent = meta.getObjectDef(f.getReferenceToName());
			if (parent == null) {
				return null;
			}
		}
		return parent.getField(names[names.length-1]);
	}
	
	public RelationDef getRelation(Metadata meta, SObjectDef parent) {
		//assert normalize
		String[] names = this.name.split("\\.");
		for (int i=0; i<names.length-1; i++) {
			FieldDef f = parent.getSingleRelation(names[i]);
			if (f == null) {
				return null;
			}
			parent = meta.getObjectDef(f.getReferenceToName());
			if (parent == null) {
				return null;
			}
		}
		return parent.getMultipleRelation(names[names.length-1]);
	}
	
	public SObjectDef getParent(Metadata meta, SObjectDef parent) {
		//assert normalize
		String[] names = this.name.split("\\.");
		for (int i=0; i<names.length-1; i++) {
			FieldDef f = parent.getSingleRelation(names[i]);
			if (f == null) {
				return null;
			}
			parent = meta.getObjectDef(f.getReferenceToName());
			if (parent == null) {
				return null;
			}
		}
		return parent;
	}
}
