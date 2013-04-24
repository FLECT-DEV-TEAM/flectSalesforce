package jp.co.flect.salesforce.syntax;

import java.util.List;
import java.io.IOException;
import java.text.ParseException;

import jp.co.flect.salesforce.Metadata;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.update.CreateRequest;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.soap.SoapException;

public class InsertStatement extends AbstractInsertStatement {
	
	public InsertStatement(String str) throws ParseException {
		StringBuilder buf = new StringBuilder();
		Tokenizer t = new Tokenizer(str);
		parse(t, buf);
		if (t.next(buf) != Tokenizer.T_END) {
			throw new ParseException(t.getString(), t.getPrevIndex());
		}
	}
	
	public InsertStatement(Tokenizer t) throws ParseException {
		parse(t, new StringBuilder());
	}
	
	@Override
	public String getStatement() { return "insert";}
	
	@Override
	protected List<SaveResult> doRequest(SalesforceClient client, List<SObject> list) throws IOException, SoapException {
		CreateRequest request = new CreateRequest(list);
		request.setAllOrNone(true);
		return client.create(request);
	}
	
}
