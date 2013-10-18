package jp.co.flect.salesforce.apex;

/**
 * ApexのWebServiceを介してやり取りするオブジェクトのインターフェース
 */
public interface ApexObject {
	
	/**
	 * Apexで定義されたオブジェクト名
	 */
	public String getObjectName();
}
