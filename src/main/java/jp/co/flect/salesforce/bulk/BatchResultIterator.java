package jp.co.flect.salesforce.bulk;

import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import jp.co.flect.csv.CSVReader;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.salesforce.SalesforceException;
import jp.co.flect.salesforce.StatusCode;

public class BatchResultIterator {
	
	private CSVReader reader;
	private String[] next;
	
	public BatchResultIterator(InputStream is) throws IOException {
		this.reader = new CSVReader(is, "utf-8");
		this.reader.readNext();
		this.next = this.reader.readNext();
	}
	
	public boolean hasNext() {
		return this.next != null;
	}
	
	public SaveResult next() throws IOException {
		if (this.next == null) {
			throw new NoSuchElementException();
		}
		if (this.next.length < 4) {
			throw new IllegalStateException();
		}
		String id = this.next[0];
		boolean success = Boolean.valueOf(this.next[1]);
		boolean created = Boolean.valueOf(this.next[2]);
		SalesforceException ex = null;
		if (this.next[3] != null && this.next[3].length() > 0) {
			String msg = this.next[3];
			StatusCode code = null;
			int idx = msg.indexOf(':');
			if (idx != -1) {
				code = StatusCode.valueOf(msg.substring(0, idx));
				if (code != null) {
					msg = msg.substring(idx + 1);
				}
			}
			ex = new SalesforceException(msg, code);
		}
		this.next = this.reader.readNext();
		if (this.next == null) {
			close();
		}
		return new SaveResult(id, success, created, ex);
	}
	
	public void close() throws IOException {
		this.reader.close();
	}
}
