package jp.co.flect.salesforce.update;

import java.util.ArrayList;
import java.util.List;
import jp.co.flect.salesforce.SObject;

/**
 * create, update, upserリクエストの基底クラス
 */
public abstract class ModifyRequest {
	
	private List<SObject> list = new ArrayList<SObject>();
	private boolean allOrNone = false;
	
	/**
	 * コンストラクタ
	 */
	public ModifyRequest() {
	}
	
	/**
	 * コンストラクタ
	 */
	public <T extends SObject> ModifyRequest(List<T> list) {
		this.list.addAll(list);
	}
	
	/**
	 * 実行するオペレーションを返します
	 */
	public abstract ModifyOperation getOperation();
	
	/**
	 * 実行するオペレーション名を返します
	 */
	public String getOperationName() {
		return getOperation().toString().toLowerCase();
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
	 *更新対象のオブジェクトリストを返します
	 */
	public List<SObject> getObjectList() { return this.list;}
	
	/**
	 *更新対象のオブジェクトを追加します
	 */
	public void addObject(SObject obj) { this.list.add(obj);}
	
	/**
	 *更新対象のオブジェクトリストをクリアします
	 */
	public void clear() { this.list.clear();}
}
