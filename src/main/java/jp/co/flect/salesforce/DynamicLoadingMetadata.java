package jp.co.flect.salesforce;

import java.io.IOException;
import jp.co.flect.soap.SoapException;

/**
 * 要求されたSObjectDefが見つからない場合動的に情報取得するMetadata
 * !!! 注意 !!!
 * 要求されたSObjectDefがサーバー上に存在しない場合は何度もその問い合わせ
 * がサーバーに送信される可能性があるのでパフォーマンス劣化の可能性があります。
 */
public class DynamicLoadingMetadata extends Metadata {
	
	private SalesforceClient client;
	
	public DynamicLoadingMetadata(SalesforceClient client) {
		super(client.getMetadata());
		this.client = client;
	}
	
	@Override
	public SObjectDef getObjectDef(String name) {
		SObjectDef ret = super.getObjectDef(name);
		if (ret == null || !ret.isComplete()) {
			try {
				ret = client.describeSObject(name);
				addObjectDef(ret);
			} catch (IOException e) {
				//ignore
			} catch (SoapException e) {
				//ignore
			}
		}
		return ret;
	}
}
