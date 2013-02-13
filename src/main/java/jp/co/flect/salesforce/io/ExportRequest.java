package jp.co.flect.salesforce.io;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import jp.co.flect.soap.SoapException;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.query.QueryRequest;

/**
 * Queryリクエスト
 */
public abstract class ExportRequest extends QueryRequest {
	
	public enum LabelType {
		NAME,
		LABEL,
		BOTH,
		NONE
	}
	
	private LabelType labelType = LabelType.LABEL;
	private File outputFile;
	private boolean bulk = false;
	private boolean writeRecordNo = false;
	private TimeZone timezone;
	private int maxCount;
	
	public ExportRequest(String query) {
		super(query);
	}
	
	public LabelType getLabelType() { return this.labelType;}
	public void setLabelType(LabelType t) { this.labelType = t;}
	
	public boolean useBulk() { return this.bulk;}
	public void setUseBulk(boolean b) { this.bulk = b;}
	
	public File getOutputFile() { return this.outputFile;}
	public void setOutputFile(File f) { this.outputFile = f;}
	
	public boolean isWriteRecordNo() { return this.writeRecordNo;}
	public void setWriteRecordNo(boolean b) { this.writeRecordNo = b;}
	
	public TimeZone getTimeZone() { return this.timezone;}
	public void setTimeZone(TimeZone t) { this.timezone = t;}
	
	public int getMaxCount() { return this.maxCount;}
	public void setMaxCount(int n) { this.maxCount = n;}
	
	public abstract void invoke(SalesforceClient client) throws IOException, SoapException;
}
	