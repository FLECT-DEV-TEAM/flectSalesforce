package jp.co.flect.salesforce.update;

import java.util.List;
import jp.co.flect.salesforce.SObject;

/**
 * Updateリクエスト
 */
public class UpdateRequest extends ModifyRequest {
	
	/**
	 * コンストラクタ
	 */
	public UpdateRequest() {
	}
	
	/**
	 * コンストラクタ
	 */
	public <T extends SObject> UpdateRequest(List<T> list) {
		super(list);
	}
	
	/**
	 * コンストラクタ
	 */
	public UpdateRequest(SObject obj) {
		addObject(obj);
	}
	
	@Override
	public ModifyOperation getOperation() { return ModifyOperation.UPDATE;}
	
}
