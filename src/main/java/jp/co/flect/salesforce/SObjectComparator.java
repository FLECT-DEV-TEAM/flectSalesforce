package jp.co.flect.salesforce;

import java.util.Comparator;
import jp.co.flect.salesforce.query.QueryResult;

public class SObjectComparator implements Comparator<SObject> {
	
	private String field;
	private boolean asc;
	
	public SObjectComparator(String field, boolean asc) {
		this.field = field;
		this.asc = asc;
	}
	
	public int compare(SObject o1, SObject o2) {
		Object v1 = o1.getDeep(this.field);
		Object v2 = o2.getDeep(this.field);
		int ret = 0;
		if (v1 == null) {
			return v2 == null ? 0 : 1;
		} else if (v2 == null) {
			return -1;
		} else {
			if (v1 instanceof QueryResult && v2 instanceof QueryResult) {
				QueryResult q1 = (QueryResult)v1;
				QueryResult q2 = (QueryResult)v2;
				ret = q2.getAllSize() - q1.getAllSize();
			} else if (v1 instanceof Comparable && v2 instanceof Comparable) {
				ret = ((Comparable)v1).compareTo(v2);
			}
		}
		if (!this.asc) {
			ret = 0 - ret;
		}
		return ret;
	}
}
