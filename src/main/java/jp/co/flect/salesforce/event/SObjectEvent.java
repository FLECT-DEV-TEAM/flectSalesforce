package jp.co.flect.salesforce.event;

import java.util.EventObject;
import jp.co.flect.salesforce.SObject;

public class SObjectEvent extends EventObject {
	
	private SObject obj;
	private boolean canceled;
	
	public SObjectEvent(Object source, SObject obj) {
		super(source);
		this.obj = obj;
	}
	
	public SObject getObject() { return this.obj;}
	
	public boolean isCanceled() { return this.canceled;}
	public void setCanceled(boolean b) { this.canceled = b;}
}
