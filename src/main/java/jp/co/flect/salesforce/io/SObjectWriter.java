package jp.co.flect.salesforce.io;

import java.io.IOException;
import jp.co.flect.salesforce.SObject;

public interface SObjectWriter {
	
	public void write(SObject obj) throws IOException;
	
}
