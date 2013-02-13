package jp.co.flect.salesforce.io;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import jp.co.flect.soap.SoapException;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.update.ModifyOperation;
import jp.co.flect.salesforce.update.ModifyRequest;

/**
 * Queryリクエスト
 */
public abstract class ImportRequest {
	
	private File inputFile;
	private ModifyOperation op;
	private String objectName;
	private boolean stopAtError;
	private boolean stopAtInvalidColumn;
	private TimeZone timezone;
	private int maxCount;
	
	private String externalIdField;
	
	public ImportRequest(ModifyOperation op, String objectName, File inputFile) {
		this.op = op;;
		this.objectName = objectName;
		this.inputFile = inputFile;
	}
	
	public ModifyOperation getOperation() { return this.op;}
	public void setOperation(ModifyOperation op) { this.op = op;}
	
	public String getObjectName() { return this.objectName;}
	public void setObjectName(String s) { this.objectName = s;}
	
	public File getInputFile() { return this.inputFile;}
	public void setInputFile(File f) { this.inputFile = f;}
	
	public String getExternalIdField() { return this.externalIdField;}
	public void setExternalIdField(String s) { this.externalIdField = s;}
	
	public boolean isStopAtError() { return this.stopAtError;}
	public void setStopAtError(boolean b) { this.stopAtError = b;}
	
	public boolean isStopAtInvalidColumn() { return this.stopAtInvalidColumn;}
	public void setStopAtInvalidColumn(boolean b) { this.stopAtInvalidColumn = b;}
	
	public TimeZone getTimeZone() { return this.timezone;}
	public void setTimeZone(TimeZone t) { this.timezone = t;}
	
	public int getMaxCount() { return this.maxCount;}
	public void setMaxCount(int n) { this.maxCount = n;}
	
	public abstract ImportResult invoke(SalesforceClient client, SObjectDef objectDef) throws IOException, SoapException;
}
	