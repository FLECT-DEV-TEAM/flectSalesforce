package jp.co.flect.salesforce.syntax;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import jp.co.flect.salesforce.Metadata;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SalesforceException;
import jp.co.flect.salesforce.StatusCode;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.update.UpdateRequest;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.salesforce.query.QueryRequest;
import jp.co.flect.salesforce.query.QueryResult;
import jp.co.flect.soap.SoapException;

public class UpdateStatement extends DmlStatement {
	
	private Map<String, Value> valueMap = new HashMap<String, Value>();
	private String where;
	
	public UpdateStatement(String str) throws ParseException {
		StringBuilder buf = new StringBuilder();
		Tokenizer t = new Tokenizer(str);
		parse(t, buf);
		if (t.next(buf) != Tokenizer.T_END) {
			throw new ParseException(t.getString(), t.getPrevIndex());
		}
	}
	
	public UpdateStatement(Tokenizer t) throws ParseException {
		parse(t, new StringBuilder());
	}
	
	private void parse(Tokenizer t, StringBuilder buf) throws ParseException {
		String str = t.getString();
		
		checkToken(t, buf, "update");
		this.objectName = t.nextLiteral(buf);
		checkToken(t, buf, "set");
		
		while (true) {
			String name = t.nextLiteral(buf);
			checkToken(t, buf, "=");
			Value value = nextValue(t, buf);
			this.valueMap.put(name, value);
			
			int n = t.next(buf);
			if (n == Tokenizer.T_COMMA) {
				continue;
			} else if (n == Tokenizer.T_END) {
				return;
			} else if (n == Tokenizer.T_LITERAL && buf.toString().equalsIgnoreCase("where")) {
				break;
			} else {
				throw new ParseException(t.getString(), t.getPrevIndex());
			}
		}
		int whereFrom = t.getPrevIndex();
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
	public String getStatement() { return "update";}
	
	public String getWhere() { return this.where;}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("UPDATE ").append(this.objectName).append(" SET ");
		for (Map.Entry<String, Value> entry : this.valueMap.entrySet()) {
			buf.append(entry.getKey())
				.append(" = ")
				.append(entry.getValue());
		}
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
	
	private void normalize(Metadata meta) throws SalesforceException {
		SObjectDef objectDef = meta.getObjectDef(this.objectName);
		if (objectDef == null) {
			throw new SalesforceException("Object not found: " + this.objectName);
		}
		this.objectName = objectDef.getName();
		
		Map<String, Value> newMap = new HashMap<String, Value>();
		List<String> notFoundList = new ArrayList<String>();
		for (Map.Entry<String, Value> entry : this.valueMap.entrySet()) {
			String name = entry.getKey();
			Value value = entry.getValue();
			FieldDef fd = objectDef.getField(name);
			if (fd == null) {
				notFoundList.add(name);
				continue;
			}
			if (!checkType(fd, value)) {
				String msg = String.format("Invalid value: %s = %s", fd.getName(), value.getString());
				throw new SalesforceException(msg, StatusCode.INVALID_TYPE, Arrays.asList(fd.getName()));
			}
			newMap.put(name, value);
		}
		if (notFoundList.size() > 0) {
			Object param = notFoundList.size() == 1 ? notFoundList.get(0) : notFoundList;
			String msg = String.format("Invalid field: %s", param);
			throw new SalesforceException(msg, StatusCode.INVALID_FIELD, notFoundList);
		}
		this.valueMap = newMap;
	}
	
	public DmlResult execute(SalesforceClient client) throws IOException, SoapException {
		return execute(client, false);
	}
	
	public DmlResult execute(SalesforceClient client, boolean bAll) throws IOException, SoapException {
		normalize(client.getMetadata());
		QueryRequest queryRequest = new QueryRequest(getSelectString());
		queryRequest.setQueryAll(bAll);
		QueryResult<SObject> queryResult = client.query(queryRequest);
		
		DmlResult result = new DmlResult("update");
		if (queryResult.getAllSize() == 0) {
			return result;
		}
		try {
			UpdateRequest request = new UpdateRequest();
			request.setAllOrNone(true);
			while (true) {
				for (SObject obj : queryResult.getRecords()) {
					SObject newObj = client.newObject(this.objectName);
					newObj.setId(obj.getId());
					for (Map.Entry<String, Value> entry : this.valueMap.entrySet()) {
						newObj.set(entry.getKey(), entry.getValue().getString());
					}
					request.addObject(newObj);
					if (request.getObjectList().size() == SalesforceClient.MODIFY_MAX_SIZE) {
						List<SaveResult> updateResult = client.update(request);
						result.add(updateResult);
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
			if (request.getObjectList().size() > 0) {
				List<SaveResult> updateResult = client.update(request);
				result.add(updateResult);
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
