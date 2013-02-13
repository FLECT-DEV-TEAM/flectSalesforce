package jp.co.flect.salesforce.bulk;

import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import jp.co.flect.csv.CSVReader;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.Metadata;

public class QueryResultIterator {
	
	private Metadata meta;
	private String objectName;
	private SObjectDef objectDef;
	
	private String[] names;
	private CSVReader reader;
	private String[] next;
	
	public QueryResultIterator(Metadata meta, String objectName, InputStream is) throws IOException {
		this.meta = meta;
		this.objectName = objectName;
		this.objectDef = meta.getObjectDef(objectName);
		
		this.reader = new CSVReader(is, "utf-8");
		this.names = this.reader.readNext();
		this.next = this.reader.readNext();
	}
	
	public boolean hasNext() {
		return this.next != null;
	}
	
	public SObject next() throws IOException {
		if (next == null) {
			throw new NoSuchElementException();
		}
		SObject obj = meta.newObject(objectName);
		for (int i=0; i<names.length; i++) {
			String name = names[i];
			String strValue = next[i];
			if (strValue == null || strValue.length() == 0) {
				continue;
			}
			Object value = strValue;
			if (objectDef != null) {
				FieldDef fd = objectDef.getField(name);
				if (fd != null) {
					value = fd.getSoapType().parse(strValue);
				}
			}
			obj.set(name, value);
		}
		next = reader.readNext();
		if (next == null) {
			close();
		}
		return obj;
	}
	
	public void close() throws IOException {
		this.reader.close();
	}
}
