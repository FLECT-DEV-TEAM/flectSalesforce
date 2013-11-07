package jp.co.flect.salesforce.fixtures;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.query.QueryResult;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.soap.SoapException;

public class FixtureRunner {
	
	private SalesforceClient client;
	private boolean cacheId;
	private Map<String, List<Fixture>> idMap = null;
	
	public FixtureRunner(SalesforceClient client) {
		this.client = client;
	}
	
	public boolean isCacheId() { return this.cacheId;}
	public void setCacheId(boolean b) { this.cacheId = b;}
	
	public boolean update(Fixture fx) throws IOException, SoapException {
		String id = this.cacheId ? fx.getId() : null;
		if (id == null) {
			id = queryId(fx);
		}
		SaveResult result = null;
		SObject obj = client.newObject(fx.getObjectName());
		for (Map.Entry<String, String> entry : fx.getFieldValues().entrySet()) {
			obj.set(entry.getKey(), entry.getValue());
		}
		if (id == null) {
			obj.set(fx.getKeyField(), fx.getKeyValue());
			result = client.create(obj);
		} else {
			obj.setId(id);
			result = client.update(obj);
		}
		if (result.isSuccess() && this.cacheId) {
			cacheId(fx, result.getId());
		}
		return result.isSuccess();
	}
	
	public boolean delete(Fixture fx) throws IOException, SoapException {
		if (!fx.canDelete()) {
			return false;
		}
		String id = this.cacheId ? fx.getId() : null;
		if (id == null) {
			id = queryId(fx);
		}
		if (id == null) {
			return false;
		}
		SaveResult result = client.delete(id);
		if (this.cacheId) {
			removeId(fx, id);
		}
		return result.isSuccess();
	}
	
	private String queryId(Fixture fx) throws IOException, SoapException {
		String value = fx.getKeyValue();
		boolean bStr = isString(fx.getObjectName(), fx.getKeyField(), value);
		if (bStr) {
			value = "'" + value + "'";
		}
		
		StringBuilder buf = new StringBuilder();
		buf.append("SELECT Id FROM ").append(fx.getObjectName())
			.append(" WHERE ").append(fx.getKeyField()).append(" = ").append(value);
		
		QueryResult<SObject> result = client.query(buf.toString());
		if (result.getCurrentSize() != 1) {
			return null;
		}
		String id = result.getRecords().get(0).getId();
		if (this.cacheId) {
			fx.setId(id);
		}
		return id;
	}
	
	private boolean isString(String objectName, String fieldName, String value) throws IOException, SoapException {
		//Not number
		for (int i=0; i<value.length(); i++) {
			char c = value.charAt(i);
			if ((c >= '0' && c <= '9') || c == '-' || c == '.') {
				continue;
			}
			return true;
		}
		SObjectDef objectDef = client.getMetadata().getObjectDef(objectName);
		if (objectDef == null || !objectDef.isComplete()) {
			objectDef = client.describeSObject(objectName);
		}
		FieldDef field = objectDef.getField(fieldName);
		if (field == null) {
			return true;
		}
		return field.getSoapType().isStringType();
	}
	
	private void cacheId(Fixture fx, String id) {
		fx.setId(id);
		if (this.idMap == null) {
			this.idMap = new HashMap<String, List<Fixture>>();
		}
		List<Fixture> list = this.idMap.get(id);
		if (list == null) {
			list = new ArrayList<Fixture>();
			this.idMap.put(id, list);
		}
		if (!list.contains(fx)) {
			list.add(fx);
		}
	}
	
	private void removeId(Fixture fx, String id) {
		fx.setId(null);
		if (this.idMap == null) {
			return;
		}
		List<Fixture> list = this.idMap.remove(id);
		if (list != null) {
			for (Fixture f : list) {
				f.setId(null);
			}
		}
	}
}
