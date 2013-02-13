package jp.co.flect.salesforce.io;

import java.io.IOException;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.query.QueryFilter;
import jp.co.flect.xml.StAXConstructException;

public class Exporter implements QueryFilter {
	
	private SObjectWriter writer;
	private int maxCount;
	private int writeCount;
	
	public Exporter(SObjectWriter writer) {
		this(writer, 0);
	}
	
	public Exporter(SObjectWriter writer, int maxCount) {
		this.writer = writer;
		this.maxCount = maxCount;
	}
	
	public int getMaxCount() { return this.maxCount;}
	public void setMaxCount(int n) { this.maxCount = n;}
	
	public int getWriteCount() { return this.writeCount;}
	
	
	public boolean filter(SObject obj) throws StAXConstructException {
		if (this.maxCount > 0 && this.writeCount >= this.maxCount) {
			return false;
		}
		try {
			this.writer.write(obj);
			this.writeCount++;
		} catch (IOException ex) {
			throw new StAXConstructException(ex);
		}
		return false;
	}
}
