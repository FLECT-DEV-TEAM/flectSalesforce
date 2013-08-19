package jp.co.flect.salesforce.metadata;

import jp.co.flect.soap.SimpleObject;
import java.util.Date;

public class AsyncResult extends SimpleObject {
	
	private BaseMetadata related;
	
	public enum AsyncRequestState {
		Queued,
		InProgress,
		Completed,
		Error
	}
	
	public boolean isDone() { return getBoolean("done");}
	public String getId() { return getString("id");}
	
	public AsyncRequestState getState() { return AsyncRequestState.valueOf(getString("state"));}
	
	public String getMessage() { return getString("message");}
	public String getStatusCode() { return getString("statusCode");}
	
	public void setRelatedMetadata(BaseMetadata v) { this.related = v;}
	public BaseMetadata getRelatedMetadata() { return this.related;}
}

