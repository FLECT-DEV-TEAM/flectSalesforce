package jp.co.flect.salesforce.event;

import java.util.List;
import java.util.EventObject;
import jp.co.flect.salesforce.SObject;

public class NameListEvent extends EventObject {
	
	private List<String> list;
	private boolean canceled;
	
	public NameListEvent(Object source, List<String> list) {
		super(source);
		this.list = list;
	}
	
	public List<String> getNameList() { return this.list;}
	
	public boolean isCanceled() { return this.canceled;}
	public void setCanceled(boolean b) { this.canceled = b;}
}
