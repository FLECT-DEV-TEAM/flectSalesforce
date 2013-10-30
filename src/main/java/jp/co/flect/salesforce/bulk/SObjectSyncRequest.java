package jp.co.flect.salesforce.bulk;

import java.sql.Connection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import javax.swing.event.EventListenerList;
import jp.co.flect.salesforce.event.SObjectSynchronizerListener;
import jp.co.flect.salesforce.SObject;

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
	private EventListenerList listeners = new EventListenerList();
	
	private List<String> fieldList = new ArrayList<String>();
	private LinkedHashMap<String, Function> tableMapping = new LinkedHashMap<String, Function>();
	
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
	 * 参照項目を指定する場合は「MyObject__r.Field1__c」のようにドット区切りで指定します。<br>
	 * FunctionMappingのみで使用する項目はtableColumnにnullを指定します。
	 * @param objectField Salesforceオブジェクトのフィールド名
	 * @param tableColumn RDBテーブルの列名。nullでも良い
	 */
	public void addFieldMapping(String objectField, String tableColumn) {
		if (objectField == null) {
			throw new IllegalArgumentException();
		}
		this.fieldList.add(objectField);
		if (tableColumn != null) {
			this.tableMapping.put(tableColumn, new SimpleMapping(objectField));
		}
	}
	
	public void addFunctionMapping(String tableColumn, Function func) {
		this.tableMapping.put(tableColumn, func);
	}
	
	/**
	 * マッピングされたSalesforceのオブジェクトフィールドのリストを返します。
	 */
	public List<String> getObjectFieldList() { 
		return Collections.unmodifiableList(this.fieldList);
	}
	
	/**
	 * マッピングされたRDBのテーブルカラムのリストを返します。
	 */
	public List<String> getTableColumnList() {
		return new ArrayList<String>(this.tableMapping.keySet());
	}
	
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
	
	/**
	 * FunctionMappingで使用するインターフェース
	 */
	public interface Function {
		
		/**
		 * 引数のSObjectから値を取得し計算した結果を返します。
		 */
		public Object evaluate(SObject obj);
		
	}
	
	private static class SimpleMapping implements Function {
		
		private String fieldName;
		
		public SimpleMapping(String fieldName) {
			this.fieldName = fieldName;
		}
		
		public String getFieldName() { return this.fieldName;}
		public void setFieldName(String v) { this.fieldName = v;}
		
		@Override
		public Object evaluate(SObject obj) {
			return obj.getDeep(fieldName);
		}
		
	}
	
	//Package local
	Function getFunction(String tableColumn) { return this.tableMapping.get(tableColumn);}
	
	//For adujsting case sensitive
	void normalizeObjectFieldList(List<String> list) {
		//assert this.fieldList.size() == list.size();
		for (int i=0; i<list.size(); i++) {
			String orgValue = this.fieldList.get(i);
			String newValue = list.get(i);
			if (!orgValue.equals(newValue)) {
				for (Function f : this.tableMapping.values()) {
					if (f instanceof SimpleMapping) {
						SimpleMapping sm = (SimpleMapping)f;
						if (sm.getFieldName().equals(orgValue)) {
							sm.setFieldName(newValue);
							break;
						}
					}
				}
				this.fieldList.set(i, newValue);
			}
		}
	}
}
