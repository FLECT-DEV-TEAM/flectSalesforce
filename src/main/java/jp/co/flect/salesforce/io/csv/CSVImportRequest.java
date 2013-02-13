package jp.co.flect.salesforce.io.csv;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import jp.co.flect.soap.SoapException;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.update.ModifyOperation;
import jp.co.flect.salesforce.io.ImportRequest;
import jp.co.flect.salesforce.io.ImportResult;

/**
 * CSVImportRequest
 */
public class CSVImportRequest extends ImportRequest {
	
	public CSVImportRequest(ModifyOperation op, String objectName, File inputFile) {
		super(op, objectName, inputFile);
	}
	
	@Override
	public ImportResult invoke(SalesforceClient client, SObjectDef objectDef) throws IOException, SoapException {
		throw new UnsupportedOperationException("Not implemented yet!");
	}
	
}
	