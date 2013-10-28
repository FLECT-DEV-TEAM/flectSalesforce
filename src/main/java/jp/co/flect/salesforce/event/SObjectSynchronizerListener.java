package jp.co.flect.salesforce.event;

import java.util.EventListener;

public interface SObjectSynchronizerListener extends EventListener {
	
	public void handleEvent(SObjectSynchronizerEvent e);
}
