package jp.co.flect.salesforce.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.update.ModifyOperation;
import jp.co.flect.salesforce.update.ModifyRequest;
import jp.co.flect.salesforce.update.CreateRequest;
import jp.co.flect.salesforce.update.UpdateRequest;
import jp.co.flect.salesforce.update.UpsertRequest;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.salesforce.event.SObjectEvent;
import jp.co.flect.salesforce.event.SObjectCallbackListener;
import jp.co.flect.salesforce.event.NameListEvent;
import jp.co.flect.soap.SoapException;

public class Importer implements SObjectCallbackListener {
	
	private SalesforceClient client;
	private SObjectDef objectDef;
	private ModifyRequest request;
	private List<Exception> exList = new ArrayList<Exception>();
	private List<ImportResult.Warning> warnList = new ArrayList<ImportResult.Warning>();
	private boolean stopAtInvalidColumn;
	private int successCnt;
	private int maxCount;
	
	public Importer(SalesforceClient client, SObjectDef objectDef, ModifyRequest request) {
		this.client = client;
		this.objectDef = objectDef;
		this.request = request;
	}
	
	public boolean isStopAtInvalidColumn() { return this.stopAtInvalidColumn;}
	public void setStopAtInvalidColumn(boolean b) { this.stopAtInvalidColumn = b;}
	
	public int getMaxCount() { return this.maxCount;}
	public void setMaxCount(int n) { this.maxCount = n;}
	
	public void readLabel(NameListEvent e) {
		ModifyOperation op = request.getOperation();
		for (String name : e.getNameList()) {
			FieldDef fd = this.objectDef.getField(name);
			if (fd == null) {
				warnList.add(new ImportResult.Warning(ImportResult.FIELD_NOT_FOUND, name));
				continue;
			}
			if (fd.getName().equals("Id")) {
				continue;
			}
			if (op == ModifyOperation.CREATE && !fd.isCreateable()) {
				warnList.add(new ImportResult.Warning(ImportResult.NOT_CREATEABLE_FIELD, name));
				continue;
			}
			if (op == ModifyOperation.UPDATE && !fd.isUpdateable()) {
				warnList.add(new ImportResult.Warning(ImportResult.NOT_UPDATEABLE_FIELD, name));
				continue;
			}
			if (op == ModifyOperation.UPSERT && !(fd.isCreateable() && fd.isUpdateable())) {
				warnList.add(new ImportResult.Warning(ImportResult.NOT_UPSERTABLE_FIELD, name));
				continue;
			}
		}
		if (this.stopAtInvalidColumn && warnList.size() > 0) {
			e.setCanceled(true);
		}
	}
	
	public void readObject(SObjectEvent e) {
		request.addObject(e.getObject());
		int size = request.getObjectList().size();
		if (size == 200 || (this.maxCount > 0 && this.successCnt + size >= this.maxCount)) {
			fireRequest();
			if (request.isAllOrNone() && exList.size() > 0) {
				e.setCanceled(true);
			}
			if (this.maxCount > 0 && this.successCnt >= this.maxCount) {
				warnList.add(new ImportResult.Warning(ImportResult.MAX_COUNT_EXCEED, Integer.toString(this.maxCount)));
				e.setCanceled(true);
			}
		}
	}
	
	public void fireRequest() {
		if (request.getObjectList().size() == 0) {
			return;
		}
		if (request.isAllOrNone() && exList.size() > 0) {
			return;
		}
		try {
			List<SaveResult> ret = null;
			switch (request.getOperation()) {
				case CREATE:
					ret = client.create((CreateRequest)request);
					break;
				case UPDATE:
					ret = client.update((UpdateRequest)request);
					break;
				case UPSERT:
					ret = client.upsert((UpsertRequest)request);
					break;
				default:
					throw new IllegalStateException();
			}
			for (SaveResult sr : ret) {
				if (sr.isSuccess()) {
					successCnt++;
				} else if (sr.getErrorCount() > 0) {
					exList.addAll(sr.getErrors());
				}
			}
			request.clear();
		} catch (IOException e) {
			exList.add(e);
		} catch (SoapException e) {
			exList.add(e);
		}
	}
	
	public ImportResult getResult() {
		fireRequest();
		return new ImportResult(successCnt, exList, warnList);
	}
}

