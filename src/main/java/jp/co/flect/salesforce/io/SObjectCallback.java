package jp.co.flect.salesforce.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.event.SObjectCallbackListener;


public interface SObjectCallback {
	
	public void addSObjectCallbackListener(SObjectCallbackListener l);
	public void removeSObjectCallbackListener(SObjectCallbackListener l);
	
	public void parse(File f) throws IOException;
	public void parse(InputStream is) throws IOException;
	
}
