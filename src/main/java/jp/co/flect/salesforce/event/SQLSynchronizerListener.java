package jp.co.flect.salesforce.event;

import java.util.EventListener;

public interface SQLSynchronizerListener extends EventListener {
	
	public void handleEvent(SQLSynchronizerEvent e);
}
