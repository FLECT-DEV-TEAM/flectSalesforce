package jp.co.flect.salesforce.syntax;

import java.util.ArrayList;
import java.util.List;

import jp.co.flect.salesforce.SalesforceException;
import jp.co.flect.salesforce.StatusCode;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.salesforce.update.ModifyOperation;
import jp.co.flect.util.SimpleJson;

public class DmlResult {
	
	private String type;
	private int successCount;
	private int createdCount;
	private List<Exception> errorList = new ArrayList<Exception>();
	
	public DmlResult(String type) {
		this.type = type;
	}
	
	public String getType() { return this.type;}
	
	public int getSuccessCount() { return this.successCount;}
	public int getCreatedCount() { return this.createdCount;}
	
	public int getErrorCount() { return this.errorList.size();
	}
	
	public List<Exception> getErrorList() { return this.errorList;}
	
	public void add(List<SaveResult> list) {
		boolean upsert = this.type.equals("upsert");
		for (SaveResult ret : list) {
			if (ret.isSuccess()) {
				this.successCount++;
			}
			if (upsert && ret.isSuccess() && ret.isCreated()) {
				this.createdCount++;
			}
			if (ret.getErrorCount() > 0) {
				for (SalesforceException ex : ret.getErrors()) {
					if (!StatusCode.ALL_OR_NONE_OPERATION_ROLLED_BACK.codeEquals(ex.getStatusCode())) {
						add(ex);
					}
				}
			}
		}
	}
	
	public void add(Exception ex) {
		this.errorList.add(ex);
	}
	
	public String toJson() {
		SimpleJson json = new SimpleJson("type", type);
		json.set("success", Integer.valueOf(successCount));
		if (type.equals("upsert")) {
			json.set("created", Integer.valueOf(createdCount));
		}
		if (errorList.size() > 0) {
			List<String> list = new ArrayList<String>();
			for (Exception ex : errorList) {
				list.add(ex.getMessage());
			}
			json.set("errors", list);
		}
		return json.toString();
	}
	
	public String toString() {
		return toJson();
	}
}