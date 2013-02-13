package jp.co.flect.salesforce.io;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.SObjectDef;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.query.QueryResult;

/**
 * @deprecated 
 * SubqueryのQueryLocatorにうまく対応できないので作るには作ったが未使用(かつ未テスト)
 */
public class StrArrayIterator implements Iterator<String[]> {
	
	private static final int COL_SIMPLE   = 1;
	private static final int COL_PARENT   = 2;
	private static final int COL_SUBQUERY = 3;
	private static final int COL_UNKNOWN  = 4;
	
	private List<NameInfo> nameList;
	private boolean writeRecordNo;
	private Iterator<SObject> it;
	private int recNo;
	
	private SObject currentObject = null;
	private int currentSubqueryIndex = 0;
	private int currentSubqueryCount = 0;
	
	private SalesforceClient client;
	
	public StrArrayIterator(List<String> nameList, boolean writeRecordNo, List<SObject> list) {
		this(null, nameList, writeRecordNo, list);
	}
	
	public StrArrayIterator(SObjectDef objectDef, List<String> nameList, 
		boolean writeRecordNo, List<SObject> list) 
	{
		this.nameList = createNameList(objectDef, nameList);
		this.writeRecordNo = writeRecordNo;
		this.it = list.iterator();
	}
	
	public SalesforceClient getClient() { return this.client;}
	public void setClient(SalesforceClient client) { this.client = client;}
	
	
	@Override
	public boolean hasNext() {
		if (this.it.hasNext()) {
			return true;
		}
		return this.currentObject != null;
	}
	
	@Override
	public String[] next() {
		if (this.currentObject == null) {
			this.currentObject = it.next();
			this.currentSubqueryIndex = 0;
		}
		int subSize = 0;
		int len = this.nameList.size();
		if (this.writeRecordNo) {
			len++;
		}
		String[] ret = new String[len];
		int idx = 0;
		if (this.writeRecordNo) {
			ret[idx++] = Integer.toString(++this.recNo);
		}
		for (NameInfo info : this.nameList) {
			String str = getValue(currentObject, info, currentSubqueryIndex);
			if (info.colType == COL_SUBQUERY) {
				QueryResult result = (QueryResult)currentObject.get(info.subname);
				if (result != null && result.getAllSize() > subSize) {
					subSize = result.getAllSize();
				}
			}
			ret[idx++] = str == null ? "" : str;
		}
		if (currentSubqueryIndex + 1 < subSize) {
			currentSubqueryIndex++;
		} else {
			currentObject = null;
		}
		return ret;
	}
	
	private String getValue(SObject obj, NameInfo info, int subqueryIndex) {
		if (info.colType == COL_SUBQUERY) {
			QueryResult<?> result = (QueryResult<?>)obj.get(info.subname);
			if (result == null) {
				return null;
			} else if (subqueryIndex < result.getCurrentSize()) {
				SObject child = (SObject)result.getRecords().get(subqueryIndex);
				return child.getStringDeep(info.name);
			} else {
				return null;
			}
		} else if (subqueryIndex > 0) {
			return null;
		}
		switch (info.colType) {
			case COL_SIMPLE:
				return obj.getString(info.name);
			case COL_PARENT:
				return obj.getStringDeep(info.name);
			case COL_SUBQUERY:
				throw new IllegalStateException();
			case COL_UNKNOWN:
				Object o = obj.get(info.subname);
				if (o == null) {
					return null;
				} else if (o instanceof SObject) {
					info.colType = COL_PARENT;
					info.name = info.subname + "." + info.name;
					info.subname = null;
					return getValue(obj, info, subqueryIndex);
				} else if (o instanceof QueryResult) {
					info.colType = COL_SUBQUERY;
					return getValue(obj, info, subqueryIndex);
				} else {
					throw new IllegalStateException();
				}
		}
		throw new IllegalStateException();
	}
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	public void reset(List<SObject> list) {
		this.it = list.iterator();
	}
	
	private List<NameInfo> createNameList(SObjectDef objectDef, List<String> nameList) {
		List<NameInfo> ret = new ArrayList<NameInfo>();
		for (String str : nameList) {
			String name = str;
			String subname = null;
			int colType = getColType(objectDef, str);
			if (colType == COL_SUBQUERY || colType == COL_UNKNOWN) {
				int idx = str.indexOf('.');
				subname = str.substring(0, idx);
				name = str.substring(idx+1);
			}
			ret.add(new NameInfo(colType, name, subname));
		}
		return ret;
	}
	
	private static int getColType(SObjectDef objectDef, String str) {
		int idx = str.indexOf('.');
		if (idx == -1) {
			return COL_SIMPLE;
		}
		if (objectDef == null) {
			return COL_UNKNOWN;
		}
		String subname = str.substring(0, idx);
		if (objectDef.getSingleRelation(subname) != null) {
			return COL_PARENT;
		}
		if (objectDef.getMultipleRelation(subname) != null) {
			return COL_SUBQUERY;
		}
		return COL_UNKNOWN;
	}
	
	private static class NameInfo {
		
		public int colType;
		public String name;
		public String subname;
		
		public NameInfo(int colType, String name, String subName) {
			this.colType = colType;
			this.name = name;
			this.subname = subname;
		}
	}
}

