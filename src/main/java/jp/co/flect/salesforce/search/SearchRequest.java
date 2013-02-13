package jp.co.flect.salesforce.search;

import jp.co.flect.salesforce.query.AbstractQueryRequest;

/**
 * Searchリクエスト
 */
public class SearchRequest extends AbstractQueryRequest {
	
	/**
	 * Query文字列を指定するコンストラクタ
	 */
	public SearchRequest(String query) {
		super(query);
	}
	
	/** Query文字列を返します */
	public String getQuery() { return this.str;}
	/** Query文字列を設定します */
	public void setQuery(String s) { this.str = s;}
	
}
	