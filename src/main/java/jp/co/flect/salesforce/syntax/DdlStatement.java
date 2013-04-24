package jp.co.flect.salesforce.syntax;

import java.io.IOException;
import java.util.List;
import java.text.ParseException;
import jp.co.flect.salesforce.metadata.MetadataType;
import jp.co.flect.salesforce.metadata.MetadataClient;
import jp.co.flect.salesforce.metadata.AsyncResult;
import jp.co.flect.soap.SoapException;

public abstract class DdlStatement {
	
	private String name;
	private MetadataType type;
	
	protected DdlStatement(String name) {
		this.name = name;
	}
	
	public String getStatementName() { return this.name;}
	public MetadataType getMetadataType() { return this.type;}
	
	protected MetadataType checkType(Tokenizer t, StringBuilder buf) throws ParseException {
		String str = t.nextLiteral(buf);
		MetadataType ret = MetadataType.getTargetType(str);
		if (ret == null) {
			throw new ParseException("Unknown type: " + str, t.getPrevIndex());
		}
		this.type = ret;
		return ret;
	}
	
	protected void checkToken(Tokenizer t, StringBuilder buf, String str) throws ParseException {
		int n = t.next(buf);
		if (n == Tokenizer.T_END) {
			throw new ParseException(t.getString(), t.getPrevIndex());
		}
		if (!str.equalsIgnoreCase(buf.toString())) {
			throw new ParseException(t.getString(), t.getPrevIndex());
		}
	}
	
	public abstract List<AsyncResult> execute(MetadataClient client) throws IOException, SoapException;
}
