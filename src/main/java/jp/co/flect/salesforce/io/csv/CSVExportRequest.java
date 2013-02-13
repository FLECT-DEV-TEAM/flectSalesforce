package jp.co.flect.salesforce.io.csv;

import java.io.IOException;
import jp.co.flect.soap.SoapException;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.io.ExportRequest;

/**
 * CSVExportRequest
 */
public class CSVExportRequest extends ExportRequest {
	
	public CSVExportRequest(String query) {
		super(query);
	}
	
	public void invoke(SalesforceClient client) throws IOException, SoapException {
		throw new UnsupportedOperationException("Not implemented yet!");
	}
}
	