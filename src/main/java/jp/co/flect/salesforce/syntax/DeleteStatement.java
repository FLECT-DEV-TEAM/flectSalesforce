package jp.co.flect.salesforce.syntax;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import jp.co.flect.salesforce.IdRequest;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.salesforce.query.QueryResult;
import jp.co.flect.soap.SoapException;

public class DeleteStatement extends DmlStatement {
	
	private String where;
	
	public DeleteStatement(String str) throws ParseException {
		StringBuilder buf = new StringBuilder();
		Tokenizer t = new Tokenizer(str);
		parse(t, buf);
		if (t.next(buf) != Tokenizer.T_END) {
			throw new ParseException(t.getString(), t.getPrevIndex());
		}
	}
	
	public DeleteStatement(Tokenizer t) throws ParseException {
		parse(t, new StringBuilder());
	}
	
	private void parse(Tokenizer t, StringBuilder buf) throws ParseException {
		String str = t.getString();
		
		checkToken(t, buf, "delete");
		checkToken(t, buf, "from");
		this.objectName = t.nextLiteral(buf);
		
		int whereFrom = t.skipWhitespace();
		while (true) {
			int n = t.next(buf);
			if (n == Tokenizer.T_END) {
				break;
			}
		}
		int whereTo = t.getPrevIndex();
		if (whereTo > whereFrom) {
			this.where = str.substring(whereFrom, whereTo);
		}
	}
	
	@Override
	public String getStatement() { return "delete";}
	
	public String getWhere() { return this.where;}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("DELEE FROM ").append(objectName);
		if (where != null) {
			buf.append(" ").append(where);
		}
		return buf.toString();
	}
	
	private String getSelectString() {
		StringBuilder buf = new StringBuilder();
		buf.append("SELECT Id FROM ").append(objectName);
		if (where != null) {
			buf.append(" ").append(where);
		}
		return buf.toString();
	}
	
	public DmlResult execute(SalesforceClient client) throws IOException, SoapException {
		QueryResult<SObject> queryResult = client.query(getSelectString());
		
		DmlResult result = new DmlResult("delete");
		if (queryResult.getAllSize() == 0) {
			return result;
		}
		try {
			IdRequest request = new IdRequest();
			request.setAllOrNone(true);
			while (true) {
				for (SObject obj : queryResult.getRecords()) {
					request.addId(obj.getId());
					if (request.getIdList().size() == SalesforceClient.MODIFY_MAX_SIZE) {
						List<SaveResult> delResult = client.delete(request);
						result.add(delResult);
						request.clear();
						if (result.getErrorCount() > 0) {
							break;
						}
					}
				}
				if (result.getErrorCount() > 0) {
					break;
				}
				if (queryResult.getQueryLocator() == null) {
					break;
				}
				queryResult = client.queryMore(queryResult.getQueryLocator());
			}
			if (request.getIdList().size() > 0) {
				List<SaveResult> delResult = client.delete(request);
				result.add(delResult);
				request.clear();
			}
		} catch (IOException e) {
			result.add(e);
		} catch (SoapException e) {
			result.add(e);
		}
		return result;
	}
	
}
