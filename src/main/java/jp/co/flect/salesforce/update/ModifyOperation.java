package jp.co.flect.salesforce.update;

public enum ModifyOperation {
	CREATE,
	UPDATE,
	UPSERT
	;
	
	public ModifyRequest createRequest() {
		switch (this) {
			case CREATE: return new CreateRequest();
			case UPDATE: return new UpdateRequest();
			case UPSERT: return new UpsertRequest();
		}
		throw new IllegalStateException();
	}
}

