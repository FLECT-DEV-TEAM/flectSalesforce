package jp.co.flect.salesforce.update;

import java.util.List;
import jp.co.flect.salesforce.SObject;

/**
 * Createリクエスト
 */
public class CreateRequest extends ModifyRequest {
	
	/**
	 * コンストラクタ
	 */
	public CreateRequest() {
	}
	
	/**
	 * コンストラクタ
	 */
	public <T extends SObject> CreateRequest(List<T> list) {
		super(list);
	}
	
	/**
	 * コンストラクタ
	 */
	public CreateRequest(SObject obj) {
		addObject(obj);
	}
	
	@Override
	public ModifyOperation getOperation() { return ModifyOperation.CREATE;}
	
}
