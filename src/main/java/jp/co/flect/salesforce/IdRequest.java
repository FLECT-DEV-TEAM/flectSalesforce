package jp.co.flect.salesforce;

import java.util.ArrayList;
import java.util.List;

/**
 * deleteやundeleteなどのIDを指定して実行するリクエスト
 */
public class IdRequest {
	
	private List<String> list = new ArrayList<String>();
	private boolean allOrNone = false;
	
	/**
	 * コンストラクタ
	 */
	public IdRequest() {
	}
	
	/**
	 * コンストラクタ
	 */
	public IdRequest(String id) {
		this.list.add(id);
	}
	
	/**
	 * コンストラクタ
	 */
	public IdRequest(List<String> list) {
		this.list.addAll(list);
	}
	
	/**
	 * 部分的な更新を許可するかどうかを返します。
	 * @return 部分的な更新を許可しない場合はtrue
	 */
	public boolean isAllOrNone() { return this.allOrNone;}
	
	/**
	 * 部分的な更新を許可するかどうかを設定します。
	 */
	public void setAllOrNone(boolean b) { this.allOrNone = b;}
	
	/**
	 *更新対象のIDリストを返します
	 */
	public List<String> getIdList() { return this.list;}
	
	/**
	 *更新対象のIDを追加します
	 */
	public void addId(String id) { this.list.add(id);}
	
	/**
	 *更新対象のIDをクリアします
	 */
	public void clear() { this.list.clear();}
}
