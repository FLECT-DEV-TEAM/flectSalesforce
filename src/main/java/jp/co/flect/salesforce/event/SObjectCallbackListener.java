package jp.co.flect.salesforce.event;

import java.util.EventListener;

public interface SObjectCallbackListener extends EventListener {
	
	public void readLabel(NameListEvent e);
	public void readObject(SObjectEvent e);
}
