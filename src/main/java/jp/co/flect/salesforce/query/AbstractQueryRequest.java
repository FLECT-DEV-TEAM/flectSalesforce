package jp.co.flect.salesforce.query;

import jp.co.flect.salesforce.SObject;

/**
 * Queryリクエストの基底
 */
public abstract class AbstractQueryRequest {
	
	protected String str;
	private int batchSize;
	private Class resultClass = SObject.class;
	private QueryFilter filter;
	
	/**
	 * コンストラクタ
	 */
	public AbstractQueryRequest(String str) {
		this.str = str;
	}
	
	/** 戻り値のサイズを返します */
	public int getBatchSize() { return this.batchSize;}
	/** 戻り値のサイズを設定します。<br>設定可能な値は200-2000です。 */
	public void setBatchSize(int n) { this.batchSize = n;}
	
	/** 戻り値のクラスを返します。 */
	public <T extends SObject> Class<T> getResultClass() { return this.resultClass;}
	/** 戻り値のクラスを設定します。 */
	public <T extends SObject> void setResultClass(Class<T> c) { this.resultClass = c;}
	
	/** 結果をフィルタするクラスを返します。 */
	public QueryFilter getFilter() { return this.filter;}
	/** 結果をフィルタするクラスを設定します。 */
	public void setFilter(QueryFilter filter) { this.filter = filter;}
}
	