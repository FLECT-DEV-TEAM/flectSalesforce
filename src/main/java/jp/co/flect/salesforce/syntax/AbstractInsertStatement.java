package jp.co.flect.salesforce.syntax;

import java.io.IOException;
import java.util.ArrayList;
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
import jp.co.flect.salesforce.query.QueryRequest;
import jp.co.flect.salesforce.query.QueryResult;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.soap.SoapException;

public abstract class AbstractInsertStatement extends DmlStatement {
	
	private List<String> nameList = new ArrayList<String>();
	private List<Value[]> valueList = null;
	private SelectStatement select;
	
	protected void parse(Tokenizer t, StringBuilder buf) throws ParseException {
		String str = t.getString();
		
		checkToken(t, buf, getStatement());
		checkToken(t, buf, "into");
		this.objectName = t.nextLiteral(buf);
		
		//FieldName
		checkToken(t, buf, "(");
		while (true) {
			int n = t.next(buf);
			if (n != Tokenizer.T_LITERAL) {
				throw new ParseException(str, t.getPrevIndex());
			}
			
			this.nameList.add(buf.toString());
			int n2 = t.next(buf);
			if (n2 == Tokenizer.T_COMMA) {
				continue;
			} else if (n2 == Tokenizer.T_CLOSE_BRACKET) {
				break;
			} else {
				throw new ParseException(str, t.getPrevIndex());
			}
		}
		checkExternalId(t, buf);
		int tempIndex = t.skipWhitespace();
		String temp = t.nextLiteral(buf);
		if ("select".equalsIgnoreCase(temp)) {
			while (true) {
				int n = t.next(buf);
				if (n == Tokenizer.T_END) {
					break;
				}
			}
			t.setIndex(tempIndex);
			this.select = new SelectStatement(t);
			if (this.select.getFieldList().size() != this.nameList.size()) {
				throw new ParseException(str, tempIndex);
			}
			return;
		}
		if (!"values".equalsIgnoreCase(temp)) {
			throw new ParseException(str, t.getPrevIndex());
		}
		
		this.valueList = new ArrayList<Value[]>();
		while (true) {
			checkToken(t, buf, "(");
			int prevIndex = t.skipWhitespace();
			List<Value> list = getValueList(t, buf);
			if (list.size() != this.nameList.size()) {
				throw new ParseException(str, prevIndex);
			}
			Value[] values = new Value[list.size()];
			this.valueList.add((Value[])list.toArray(values));
			
			int n = t.next(buf);
			if (n == Tokenizer.T_END) {
				break;
			} else if (n == Tokenizer.T_COMMA) {
				continue;
			} else {
				throw new ParseException(str, t.getPrevIndex());
			}
		}
	}
	
	private List<Value> getValueList(Tokenizer t, StringBuilder buf) throws ParseException {
		List<Value> list = new ArrayList<Value>();
		while (true) {
			Value v = nextValue(t, buf);
			list.add(v);
			
			int n = t.next(buf);
			if (n == Tokenizer.T_COMMA) {
				continue;
			} else if (n == Tokenizer.T_CLOSE_BRACKET) {
				break;
			} else {
				throw new ParseException(t.getString(), t.getPrevIndex());
			}
		}
		return list;
	}
	
	public List<String> getNameList() { return this.nameList;}
	public List<Value[]> getValueList() { return this.valueList;}
	
	public SelectStatement getSelect() { return this.select;}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getStatement().toUpperCase())
			.append(" INTO ").append(this.objectName);
		buf.append(" (").append(this.nameList.get(0));
		for (int i=1; i<this.nameList.size(); i++) {
			buf.append(",").append(this.nameList.get(i));
		}
		buf.append(") ");
		appendExternalId(buf);
		if (this.select != null) {
			buf.append(this.select);
		} else {
			for (int i=0; i<this.valueList.size(); i++) {
				Value[] values = this.valueList.get(i);
				if (i != 0) {
					buf.append(",");
				}
				buf.append("VALUES(");
				for (int j=0; j<values.length; i++) {
					Value v = values[j];
					if (j != 0) {
						buf.append(",");
					}
					buf.append(v);
				}
				buf.append(")");
			}
		}
		return buf.toString();
	}
	
	protected List<FieldDef> normalize(Metadata meta) throws SalesforceException {
		SObjectDef objectDef = meta.getObjectDef(this.objectName);
		if (objectDef == null) {
			throw new SalesforceException("Object not found: " + this.objectName);
		}
		this.objectName = objectDef.getName();
		
		List<FieldDef> fieldList = new ArrayList<FieldDef>(this.nameList.size());
		List<String> newList = new ArrayList<String>(this.nameList.size());
		List<String> notFoundList = new ArrayList<String>();
		for (String name : this.nameList) {
			FieldDef fd = objectDef.getField(name);
			if (fd == null) {
				notFoundList.add(name);
			} else {
				fieldList.add(fd);
				newList.add(fd.getName());
			}
		}
		if (notFoundList.size() > 0) {
			Object param = notFoundList.size() == 1 ? notFoundList.get(0) : notFoundList;
			String msg = String.format("Invalid field: %s", param);
			throw new SalesforceException(msg, StatusCode.INVALID_FIELD, notFoundList);
		}
		this.nameList = newList;
		return fieldList;
	}
	
	private List<SObject> createObjectList(Metadata meta) throws SalesforceException {
		ArrayList<SObject> ret = new ArrayList<SObject>(this.valueList.size());
		List<FieldDef> fieldList = normalize(meta);
		
		SObjectDef objectDef = meta.getObjectDef(this.objectName);
		for (Value[] values : this.valueList) {
			SObject obj = meta.newObject(objectDef.getName());
			for (int i=0; i<fieldList.size(); i++) {
				FieldDef fd = fieldList.get(i);
				Value value = values[i];
				if (!checkType(fd, value)) {
					String msg = String.format("Invalid value: %s = %s", fd.getName(), value.getString());
					throw new SalesforceException(msg, StatusCode.INVALID_TYPE, Arrays.asList(fd.getName()));
				}
				obj.set(fd.getName(), value.getString());
			}
			ret.add(obj);
		}
		return ret;
	}
	
	public DmlResult execute(SalesforceClient client) throws IOException, SoapException {
		return execute(client, false);
	}
	
	public DmlResult execute(SalesforceClient client, boolean bAll) throws IOException, SoapException {
		DmlResult result = new DmlResult(getStatement());
		if (this.valueList != null && this.valueList.size() > 0) {
			List<SObject> list = createObjectList(client.getMetadata());
			int spos = 0;
			int epos = 0;
			while (epos < list.size()) {
				spos = epos;
				epos = Math.min(spos + SalesforceClient.MODIFY_MAX_SIZE, list.size());
				try {
					List<SaveResult> saveResult = doRequest(client, list.subList(spos, epos));
					result.add(saveResult);
				} catch (IOException e) {
					result.add(e);
				} catch (SoapException e) {
					result.add(e);
				}
			}
		} else {//select != null
			QueryRequest queryRequest = new QueryRequest(select.getSoql());
			queryRequest.setQueryAll(bAll);
			QueryResult<SObject> queryResult = client.query(queryRequest);
			if (queryResult.getAllSize() == 0) {
				return result;
			}
			select.normalize(client.getMetadata());
			try {
				List<String> selectList = select.getNameList();
				List<SObject> list = new ArrayList<SObject>();
				while (true) {
					for (SObject obj : queryResult.getRecords()) {
						SObject newObj = client.newObject(this.objectName);
						for (int i=0; i<this.nameList.size(); i++) {
							String name = this.nameList.get(i);
							String selName = selectList.get(i);
							newObj.set(name, obj.getDeep(selName));
						}
						list.add(newObj);
						if (list.size() == SalesforceClient.MODIFY_MAX_SIZE) {
							List<SaveResult> saveResult = doRequest(client, list);
							result.add(saveResult);
							list.clear();
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
				if (list.size() > 0) {
					List<SaveResult> saveResult = doRequest(client, list);
					result.add(saveResult);
					list.clear();
				}
			} catch (IOException e) {
				result.add(e);
			} catch (SoapException e) {
				result.add(e);
			}
		}
		return result;
	}
	
	protected abstract List<SaveResult> doRequest(SalesforceClient client, List<SObject> list) throws IOException, SoapException;
	
	//For upsert
	protected void checkExternalId(Tokenizer t, StringBuilder buf) throws ParseException {
	}
	
	protected void appendExternalId(StringBuilder buf) {
	}
	
}
