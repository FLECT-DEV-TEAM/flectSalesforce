package jp.co.flect.salesforce.syntax;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.text.ParseException;

import jp.co.flect.salesforce.Metadata;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SalesforceException;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.StatusCode;
import jp.co.flect.salesforce.update.UpsertRequest;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.soap.SoapException;

public class UpsertStatement extends AbstractInsertStatement {
	
	private String externalIdField = null;
	
	public UpsertStatement(String str) throws ParseException {
		StringBuilder buf = new StringBuilder();
		Tokenizer t = new Tokenizer(str);
		parse(t, buf);
		if (t.next(buf) != Tokenizer.T_END) {
			throw new ParseException(t.getString(), t.getPrevIndex());
		}
	}
	
	public UpsertStatement(Tokenizer t) throws ParseException {
		parse(t, new StringBuilder());
	}
	
	@Override
	public String getStatement() { return "upsert";}
	
	public String getExternalIdField() { return this.externalIdField;}
	
	@Override
	protected void checkExternalId(Tokenizer t, StringBuilder buf) throws ParseException {
		int prevIndex = t.getIndex();
		String temp = t.nextLiteral(buf);
		if ("externalId".equalsIgnoreCase(temp)) {
			this.externalIdField = t.nextLiteral(buf);
		} else {
			t.setIndex(prevIndex);
		}
	}
	
	@Override
	protected void appendExternalId(StringBuilder buf) {
		if (this.externalIdField != null) {
			buf.append("externalId ").append(this.externalIdField).append(" ");
		}
	}
	
	@Override
	protected List<FieldDef> normalize(Metadata meta) throws SalesforceException {
		List<FieldDef> list = super.normalize(meta);
		if (this.externalIdField != null) {
			SObjectDef objectDef = meta.getObjectDef(this.objectName);
			FieldDef fd = objectDef.getField(this.externalIdField);
			if (fd == null) {
				throw new SalesforceException("Invalid field: " + this.externalIdField, StatusCode.INVALID_FIELD, Arrays.asList(this.externalIdField));
			}
			this.externalIdField = fd.getName();
		}
		return list;
	}
	
	@Override
	protected List<SaveResult> doRequest(SalesforceClient client, List<SObject> list) throws IOException, SoapException {
		UpsertRequest request = new UpsertRequest(list);
		request.setAllOrNone(true);
		request.setExternalIdField(this.externalIdField);
		return client.upsert(request);
	}
	
}
