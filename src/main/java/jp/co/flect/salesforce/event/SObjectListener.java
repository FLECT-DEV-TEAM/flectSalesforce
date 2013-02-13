package jp.co.flect.salesforce.event;

import java.util.EventListener;

public interface SObjectListener extends EventListener {
	
	public void handleEvent(SObjectEvent e);
}
