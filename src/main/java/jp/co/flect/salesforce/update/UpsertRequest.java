package jp.co.flect.salesforce.update;

import java.util.List;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.FieldDef;

/**
 * Upsertリクエスト
 */
public class UpsertRequest extends ModifyRequest {
	
	private String externalIdField;
	/**
	 * コンストラクタ
	 */
	public UpsertRequest() {
	}
	
	/**
	 * コンストラクタ
	 */
	public <T extends SObject> UpsertRequest(List<T> list) {
		super(list);
	}
	
	/**
	 * コンストラクタ
	 */
	public UpsertRequest(SObject obj) {
		addObject(obj);
	}
	
	@Override
	public ModifyOperation getOperation() { return ModifyOperation.UPSERT;}
	
	/**
	 * 外部IDフィールド名を返します
	 */
	public String getExternalIdField() { 
		if (this.externalIdField != null) {
			return this.externalIdField;
		}
		if (getObjectList().size() == 0) {
			return null;
		}
		SObject obj = getObjectList().get(0);
		FieldDef f = obj.getObjectDef().getExternalIdField();
		return f == null ? null : f.getName();
	}
	
	/**
	 * 外部IDフィールド名を設定します
	 */
	public void setExternalIdField(String s) { this.externalIdField = s;}
	
}
