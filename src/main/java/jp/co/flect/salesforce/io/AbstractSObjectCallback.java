package jp.co.flect.salesforce.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.swing.event.EventListenerList;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.event.SObjectCallbackListener;
import jp.co.flect.salesforce.event.SObjectEvent;


public abstract class AbstractSObjectCallback implements SObjectCallback {
	
	protected EventListenerList listeners = new EventListenerList();
	
	public void addSObjectCallbackListener(SObjectCallbackListener l) {
		this.listeners.add(SObjectCallbackListener.class, l);
	}
	
	public void removeSObjectCallbackListener(SObjectCallbackListener l) {
		this.listeners.remove(SObjectCallbackListener.class, l);
	}
	
	protected boolean readObject(SObject obj) {
		SObjectCallbackListener[] ls = (SObjectCallbackListener[])this.listeners.getListeners(SObjectCallbackListener.class);
		if (ls == null || ls.length == 0) {
			return false;
		}
		SObjectEvent event = new SObjectEvent(this, obj);
		for (int i=0; i<ls.length; i++) {
			ls[i].readObject(event);
		}
		return event.isCanceled();
	}
	
}
