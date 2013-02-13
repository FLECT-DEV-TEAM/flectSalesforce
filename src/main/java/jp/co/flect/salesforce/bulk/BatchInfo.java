package jp.co.flect.salesforce.bulk;

/** API Version 24.0
    <complexType name="BatchInfo">
        <sequence>
            <element minOccurs="1" name="id" type="string"/>
            <element maxOccurs="1" minOccurs="1" name="jobId" type="string"/>
            <element maxOccurs="1" minOccurs="1" name="state" type="tns:BatchStateEnum"/>
            <element maxOccurs="1" minOccurs="0" name="stateMessage" type="string"/>
            <element maxOccurs="1" minOccurs="1" name="createdDate" type="dateTime"/>
            <element maxOccurs="1" minOccurs="0" name="systemModstamp" type="dateTime"/>
            <element maxOccurs="1" minOccurs="1"
                name="numberRecordsProcessed" type="int"/>
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
public class BatchInfo extends AbstractBulkInfo {
	
	private static final String[] ELEMENT_SEQ = {
		"id",
		"jobId",
		"state",
		"stateMessage",
		"createdDate",
		"systemModstamp",
		"numberRecordsProcessed",
		"numberRecordsFailed",
		"totalProcessingTime",
		"apiActiveProcessingTime",
		"apexProcessingTime",
	};
	
	public enum BatchState {
		Queued,
		InProgress,
		Completed,
		Failed,
		NotProcessed
	};
	
	private String objectName;
	
	public BatchInfo(String objectName) {
		super("batchInfo", ELEMENT_SEQ);
		this.objectName = objectName;
	}
	
	public String getObjectName() { return this.objectName;}
	
	public String getJobId() { return (String)get("jobId");}
	public BatchState getState() { return (BatchState)get("state");}
	
	@Override
	protected Object parseValue(String name, String value) {
		if ("state".equals(name)) {
			return BatchState.valueOf(value);
		}
		return super.parseValue(name, value);
	}
	
}
