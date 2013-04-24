package jp.co.flect.salesforce.syntax;

import jp.co.flect.salesforce.Metadata;
import jp.co.flect.salesforce.SObjectDef;

public interface SelectField {
	
	public static final int SIMPLE   = 1;
	public static final int FUNCTION = 2;
	public static final int SUBQUERY = 3;
	
	public int getType();
	
	public void normalize(Metadata meta, SObjectDef parent);
}
