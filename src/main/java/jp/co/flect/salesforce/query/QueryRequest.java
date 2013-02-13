package jp.co.flect.salesforce.query;

/**
 * Queryリクエスト
 */
public class QueryRequest extends AbstractQueryRequest {
	
	private boolean all;
	private boolean autoMore = false;
	
	/**
	 * Query文字列を指定するコンストラクタ
	 */
	public QueryRequest(String query) {
		super(query);
	}
	
	/** Query文字列を返します */
	public String getQuery() { return this.str;}
	/** Query文字列を設定します */
	public void setQuery(String s) { this.str = s;}
	
	/** 削除されたレコードを含むかどうかを返します。 */
	public boolean isQueryAll() { return this.all;}
	/** 削除されたレコードを含むかどうかを設定します。 */
	public void setQueryAll(boolean b) { this.all = b;}
	
	/** QueryMoreを自動的に実行するかどうかを返します。 */
	public boolean isAutoMore() { return this.autoMore;}
	/** QueryMoreを自動的に実行するかどうかを設定します。 */
	public void setAutoMore(boolean b) { this.autoMore = b;}
}
	