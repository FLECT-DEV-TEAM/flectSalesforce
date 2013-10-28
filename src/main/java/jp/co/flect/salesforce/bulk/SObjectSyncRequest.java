package jp.co.flect.salesforce.bulk;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import javax.swing.event.EventListenerList;
import jp.co.flect.salesforce.event.SObjectSynchronizerListener;

/**
 * SalesforceのオブジェクトをRDBに同期するためのリクエストクラス
 */
public class SObjectSyncRequest {
	
	/** 同期処理のポリシー */
	public enum SObjectSyncPolicy {
		/** 
		 * 全レコードを１度にコミット
		 * 同期はすべて成功するかすべて失敗するかのいずれかになります。
		 */
		CommitOnce,
		/** 
		 * Salesforceへのクエリ実行ごとにコミットします。
		 * 500行だけ同期に成功してその後エラーが発生するということがあり得ます。
		 */
		CommitPerQuery,
		/** 
		 * レコード単位のエラーを無視します。
		 * 成功数とエラー数はSObjectSyncInfo#getSuccessCountとgetErrorCountで取得できます。
		 */
		IgnoreRecordError
	};
	
	private Connection con;
	private String tableName;
	private String objectName;
	private String where;
	private String[] keys;
	private Object[] params;
	private int batchSize;
	private SObjectSyncPolicy policy = SObjectSyncPolicy.CommitPerQuery;
	private LinkedHashMap<String, String> fieldMapping = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, Object> defaultMapping = new LinkedHashMap<String, Object>();
	private EventListenerList listeners = new EventListenerList();
	
	/**
	 * コンストラクタ
	 * @param con JDBCコネクション
	 * @objectName 同期元のSalesforceのオブジェクト名
	 * @tableName 同期先のRDBテーブル名
	 */
	public SObjectSyncRequest(Connection con, String objectName, String tableName) {
		this.con = con;
		this.objectName = objectName;
		this.tableName = tableName;
	}
	
	/** JDBCコネクションを返します。 */
	public Connection getConnection() { return this.con;}
	/** 同期元のSalesforceのオブジェクト名を返します。 */
	public String getObjectName() { return this.objectName;}
	/** 同期先のRDBテーブル名を返します。 */
	public String getTableName() { return this.tableName;}
	
	/** 
	 * 同期元のSalesforceのオブジェクトに対してクエリを発行する際のWHERE句以降を返します。
	 */
	public String getWhere() { return this.where;}
	/** 
	 * 同期元のSalesforceのオブジェクトに対してクエリを発行する際のWHERE句以降を指定します。<br>
	 * 指定する文字列の先頭に「WHERE」をつける必要はありません
	 * WHERE句内ではパラメータとして「?」を使用できます。
	 */
	public void setWhere(String s) { 
		if (s != null) {
			s = s.trim();
			if (s.toUpperCase().startsWith("WHERE")) {
				s = s.substring(5).trim();
			}
		}
		this.where = s;
	}
	
	/** 
	 * WHERE句内で使用するパラメータを返します。
	 */
	public Object[] getParams() { return this.params;}
	/** 
	 * WHERE句内で使用するパラメータを設定します。<br>
	 * WHERE句内の「?」の数と設定するパラメータ数は同じでなければなりません。
	 */
	public void setParams(Object... params) { this.params = params;}
	
	/**
	 * UPDATE実行時に使用するRDBのキー項目を返します。
	 */
	public String[] getKeyColumns() { return this.keys;}
	/**
	 * UPDATE実行時に使用するRDBのキー項目を設定します。
	 */
	public void setKeyColumns(String... keys) { this.keys = keys;}
	
	/**
	 * SalesforceのオブジェクトフィールドとRDBのテーブルカラムのマッピングを登録します。<br>
	 * 参照項目を指定する場合は「MyObject__r.Field1__c」のようにドット区切りで指定します。
	 */
	public void addFieldMapping(String objectFieldName, String tableFieldName) {
		this.fieldMapping.put(objectFieldName, tableFieldName);
	}
	
	/**
	 * マッピングされたSalesforceのオブジェクトフィールドのリストを返します。
	 */
	public List<String> getObjectFieldList() { return new ArrayList<String>(this.fieldMapping.keySet());}
	
	/**
	 * マッピングされたRDBのテーブルカラムのリストを返します。
	 */
	public List<String> getTableColumnList() { return new ArrayList<String>(this.fieldMapping.values());}
	
	/**
	 * マッピングされたSalesforceのオブジェクトフィールドに対応するRDBのテーブルカラムを返します。
	 */
	public String getMappedTableColumn(String objectFieldName) { return this.fieldMapping.get(objectFieldName);}
	
	/**
	 * Salesforceに対応する列が無い場合のデフォルト値を登録済ます。<br>
	 * @param tableFieldName テーブルのカラム名
	 * @param value デフォルト値
	 */
	public void addDefaultMapping(String tableFieldName, Object value) {
		this.defaultMapping.put(tableFieldName, value);
	}
	
	public LinkedHashMap<String, Object> getDefaultMapping() { return this.defaultMapping;}
	
	/** 同期ポリシーを返します */
	public SObjectSyncPolicy getPolicy() { return this.policy;}
	/** 同期ポリシーを設定します */
	public void setPolicy(SObjectSyncPolicy v) { this.policy = v;}
	
	/** Salesforceクエリのバッチサイズを返します */
	public int getBatchSize() { return this.batchSize;}
	/** Salesforceクエリのバッチサイズを設定します */
	public void setBatchSize(int n) { this.batchSize = n;}
	
	public void addSObjectSynchronizerListener(SObjectSynchronizerListener l) {
		this.listeners.add(SObjectSynchronizerListener.class, l);
	}
	
	public void removeSObjectSynchronizerListener(SObjectSynchronizerListener l) {
		this.listeners.remove(SObjectSynchronizerListener.class, l);
	}
	
	public SObjectSynchronizerListener[] getSObjectSynchronizerListeners() {
		return this.listeners.getListeners(SObjectSynchronizerListener.class);
	}
}
