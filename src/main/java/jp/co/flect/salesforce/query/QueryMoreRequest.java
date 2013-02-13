package jp.co.flect.salesforce.query;

/**
 * QueryMoreリクエスト
 */
public class QueryMoreRequest extends AbstractQueryRequest {
	
	/**
	 * QueryLocatorを指定するコンストラクタ
	 */
	public QueryMoreRequest(String locator) {
		super(locator);
	}
	
	/** QueryLocatorを返します */
	public String getQueryLocator() { return this.str;}
	/** QueryLocatorを設定します */
	public void setQueryLocator(String s) { this.str = s;}
	
}
	