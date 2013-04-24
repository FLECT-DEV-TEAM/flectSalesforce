package jp.co.flect.salesforce.syntax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.text.ParseException;
import jp.co.flect.salesforce.metadata.BaseMetadata;
import jp.co.flect.salesforce.metadata.AsyncResult;
import jp.co.flect.salesforce.metadata.MetadataClient;
import jp.co.flect.soap.SoapException;

public class DropStatement extends DdlStatement {
	
	private String objectName;
	
	public DropStatement(String str) throws ParseException {
		super("drop");
		StringBuilder buf = new StringBuilder();
		Tokenizer t = new Tokenizer(str);
		checkToken(t, buf, getStatementName());
		checkType(t, buf);
		this.objectName = t.nextLiteral(buf);
	}
	
	@Override
	public List<AsyncResult> execute(MetadataClient client) throws IOException, SoapException {
		BaseMetadata obj = new BaseMetadata(getMetadataType());
		obj.setFullName(this.objectName);
		
		List<AsyncResult> ret = new ArrayList<AsyncResult>();
		AsyncResult result = client.delete(obj);
		ret.add(result);
		return ret;
	}
}
