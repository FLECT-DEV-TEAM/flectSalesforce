package jp.co.flect.salesforce.query;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.xml.StAXConstructException;

public interface QueryFilter {
	
	public boolean filter(SObject obj) throws StAXConstructException;
	
}
