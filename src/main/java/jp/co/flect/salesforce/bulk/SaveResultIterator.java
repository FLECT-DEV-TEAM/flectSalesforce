package jp.co.flect.salesforce.bulk;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import jp.co.flect.csv.CSVReader;
import jp.co.flect.salesforce.SalesforceException;
import jp.co.flect.salesforce.StatusCode;
import jp.co.flect.salesforce.update.SaveResult;

public class SaveResultIterator {
	
	private CSVReader reader;
	private String next[];
	
	public SaveResultIterator(InputStream is) throws IOException {
		reader = new CSVReader(is, "utf-8");
		reader.readNext();
		next = reader.readNext();
	}
	
	public boolean hasNext() {
		return next != null;
	}
	
	public SaveResult next() throws IOException {
		if(next == null) {
			throw new NoSuchElementException();
		}
		if(next.length < 4) {
			throw new IllegalStateException();
		}
		String id = next[0];
		boolean success = Boolean.valueOf(next[1]).booleanValue();
		boolean created = Boolean.valueOf(next[2]).booleanValue();
		SalesforceException ex = null;
		if(next[3] != null && next[3].length() > 0) {
			String msg = next[3];
			StatusCode code = null;
			int idx = msg.indexOf(':');
			if(idx != -1) {
				code = StatusCode.valueOf(msg.substring(0, idx));
				if(code != null) {
					msg = msg.substring(idx + 1);
				}
			}
			ex = new SalesforceException(msg, code);
		}
		next = reader.readNext();
		if(next == null) {
			reader.close();
		}
		return new SaveResult(id, success, created, ex);
	}
	
}
