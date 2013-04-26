package jp.co.flect.salesforce.bulk;

/** API Version 24.0
    <complexType name="JobInfo">
        <sequence>
            <element maxOccurs="1" minOccurs="0" name="id" type="string"/>
            <element maxOccurs="1" minOccurs="0" name="operation" type="tns:OperationEnum"/>
            <element maxOccurs="1" minOccurs="0" name="object" type="string"/>
            <element maxOccurs="1" minOccurs="0" name="createdById" type="string"/>
            <element maxOccurs="1" minOccurs="0" name="createdDate" type="dateTime"/>
            <element maxOccurs="1" minOccurs="0" name="systemModstamp" type="dateTime"/>
            <element maxOccurs="1" minOccurs="0" name="state" type="tns:JobStateEnum"/>
            <element maxOccurs="1" minOccurs="0"
                name="externalIdFieldName" type="string"/>
            <element maxOccurs="1" minOccurs="0" name="concurrencyMode" type="tns:ConcurrencyModeEnum"/>
            <element maxOccurs="1" minOccurs="0" name="contentType" type="tns:ContentType"/>
            <element maxOccurs="1" minOccurs="0"
                name="numberBatchesQueued" type="int"/>
            <element maxOccurs="1" minOccurs="0"
                name="numberBatchesInProgress" type="int"/>
            <element maxOccurs="1" minOccurs="0"
                name="numberBatchesCompleted" type="int"/>
            <element maxOccurs="1" minOccurs="0"
                name="numberBatchesFailed" type="int"/>
            <element maxOccurs="1" minOccurs="0"
                name="numberBatchesTotal" type="int"/>
            <element maxOccurs="1" minOccurs="0"
                name="numberRecordsProcessed" type="int"/>
            <element maxOccurs="1" minOccurs="0" name="numberRetries" type="int"/>
            <element maxOccurs="1" minOccurs="0" name="apiVersion" type="string"/>
            <element maxOccurs="1" minOccurs="0" name="assignmentRuleId" type="string"/>
            <element maxOccurs="1" minOccurs="0"
                name="numberRecordsFailed" type="int"/>
            <element maxOccurs="1" minOccurs="0"
                name="totalProcessingTime" type="long"/>
            <element maxOccurs="1" minOccurs="0"
                name="apiActiveProcessingTime" type="long"/>
            <element maxOccurs="1" minOccurs="0"
                name="apexProcessingTime" type="long"/>
        </sequence>
    </complexType>
*/
public class JobInfo extends AbstractBulkInfo {
	
	private static final String[] ELEMENT_SEQ = {
		"id",
		"operation",
		"object",
		"createdById",
		"createdDate",
		"systemModstamp",
		"state",
		"externalIdFieldName",
		"concurrencyMode",
		"contentType",
		"numberBatchesQueued",
		"numberBatchesInProgress",
		"numberBatchesCompleted",
		"numberBatchesFailed",
		"numberBatchesTotal",
		"numberRecordsProcessed",
		"numberRetries",
		"apiVersion",
		"assignmentRuleId",
		"numberRecordsFailed",
		"totalProcessingTime",
		"apiActiveProcessingTime",
		"apexProcessingTime"
	};
	
	public enum Operation {
		Insert,
		Update,
		Upsert,
		Delete,
		HardDelete,
		Query
		;
		
		public String toString() {
			String s= super.toString();
			return s.substring(0, 1).toLowerCase() + s.substring(1);
		}
		
		public static Operation fromValue(String value) {
			value = value.substring(0, 1).toUpperCase() + value.substring(1);
			return Operation.valueOf(value);
		}
	};
	
	public enum ContentType {
		XML("application/xml"),
		CSV("text/csv"),
		ZIP_XML("zip/xml"),
		ZIP_CSV("zip/csv")
		;
		
		private String value;
		
		private ContentType(String value) {
			this.value = value;
		}
		
		public String getHeaderValue() { return value;}
	};
	
	public enum ConcurrencyMode {
		Parallel,
		Serial
	};
	
	public enum JobState {
		Open,
		Closed,
		Aborted,
		Failed
	};
	
	public static JobInfo createStateJob(JobState state) {
		JobInfo job = new JobInfo();
		job.put("state", state);
		return job;
	}
	
	public JobInfo() {
		super("jobInfo", ELEMENT_SEQ);
	}
	
	public JobInfo(Operation op, String objectName) {
		this(op, objectName, ContentType.CSV);
	}
	
	public JobInfo(Operation op, String objectName, ContentType contentType) {
		super("jobInfo", ELEMENT_SEQ);
		put("operation", op);
		put("object", objectName);
		put("contentType", contentType);
	}
	
	//For BatchInfo
	public JobInfo(String jobId) {
		super("jobInfo", ELEMENT_SEQ);
		put("id", jobId);
	}
	
	@Override
	protected Object parseValue(String name, String value) {
		if ("operation".equals(name)) {
			return Operation.fromValue(value);
		} else if ("state".equals(name)) {
			return JobState.valueOf(value);
		} else if ("concurrencyMode".equals(name)) {
			return ConcurrencyMode.valueOf(value);
		} else if ("contentType".equals(name)) {
			return ContentType.valueOf(value);
		}
		return super.parseValue(name, value);
	}
	
	public String getObjectName() { return (String)get("object");}
	public Operation getOperation() { return (Operation)get("operation");}
	public ContentType getContentType() { return (ContentType)get("contentType");}
	
	public String getExternalIdFieldName() { return (String)get("externalIdFieldName");}
	public void setExternalIdFieldName(String s) { put("externalIdFieldName", s);}
	
	public ConcurrencyMode getConcurrencyMode() { return (ConcurrencyMode)get("concurrencyMode");}
	public void setConccurencyMode(ConcurrencyMode mode) { put("concurrencyMode", mode);}
	
	public int getBatchesCompleted() { return getInt("numberBatchesCompleted");}
	public int getBatchesFailed() { return getInt("numberBatchesFailed");}
	public int getBatchesTotal() { return getInt("numberBatchesTotal");}
	
	//Response
	public JobState getState() { return (JobState)get("state");}
	
	public boolean isCompleted() {
		JobState s = getState();
		return s == JobState.Closed && getBatchesTotal() == getBatchesCompleted() + getBatchesFailed();
	}
	
	public boolean isAborted() {
		return getState() == JobState.Aborted;
	}
	
}
