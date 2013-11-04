package jp.co.flect.salesforce;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.XMLSchemaException;
import jp.co.flect.xmlschema.template.TemplateHint;
import jp.co.flect.xmlschema.template.VelocityTemplateBuilder;
import jp.co.flect.template.TemplateException;
import jp.co.flect.soap.SoapClient;
import jp.co.flect.soap.SoapException;
import jp.co.flect.soap.SoapTemplate;
import jp.co.flect.soap.SoapResponse;
import jp.co.flect.soap.InvalidWSDLException;
import jp.co.flect.soap.OperationDef;
import jp.co.flect.soap.MessageDef;
import jp.co.flect.soap.SoapFaultException;
import jp.co.flect.soap.WSDL;
import jp.co.flect.util.ExtendedMap;
import jp.co.flect.net.OAuthResponse;
import jp.co.flect.net.HttpUtils;
import jp.co.flect.xml.StAXConstructor;
import jp.co.flect.xml.StAXConstructException;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;

import jp.co.flect.salesforce.bulk.JobInfo;
import jp.co.flect.salesforce.bulk.BatchInfo;
import jp.co.flect.salesforce.bulk.BulkApiException;
import jp.co.flect.salesforce.bulk.BulkClient;
import jp.co.flect.salesforce.bulk.SQLSynchronizer;
import jp.co.flect.salesforce.bulk.SQLSyncRequest;
import jp.co.flect.salesforce.bulk.SQLSyncResult;
import jp.co.flect.salesforce.bulk.SObjectSynchronizer;
import jp.co.flect.salesforce.bulk.SObjectSyncRequest;
import jp.co.flect.salesforce.bulk.SObjectSyncResult;
import jp.co.flect.salesforce.event.SQLSynchronizerEvent;
import jp.co.flect.salesforce.event.SQLSynchronizerListener;
import jp.co.flect.salesforce.query.QueryResult;
import jp.co.flect.salesforce.query.QueryRequest;
import jp.co.flect.salesforce.query.QueryMoreRequest;
import jp.co.flect.salesforce.update.SaveResult;
import jp.co.flect.salesforce.update.CreateRequest;
import jp.co.flect.salesforce.update.UpdateRequest;
import jp.co.flect.salesforce.update.UpsertRequest;
import jp.co.flect.salesforce.update.ModifyRequest;
import jp.co.flect.salesforce.update.ModifyRequestWriter;
import jp.co.flect.salesforce.update.ModifyOperation;
import jp.co.flect.salesforce.search.SearchRequest;
import jp.co.flect.salesforce.search.SearchResult;
import jp.co.flect.salesforce.io.Exporter;
import jp.co.flect.salesforce.io.ExportRequest;
import jp.co.flect.salesforce.io.ImportRequest;
import jp.co.flect.salesforce.io.ImportResult;
import jp.co.flect.salesforce.io.Importer;
import jp.co.flect.salesforce.metadata.MetadataClient;
import jp.co.flect.salesforce.syntax.ParameterQuery;
import jp.co.flect.salesforce.syntax.DmlResult;
import jp.co.flect.salesforce.syntax.InsertStatement;
import jp.co.flect.salesforce.syntax.UpsertStatement;
import jp.co.flect.salesforce.syntax.UpdateStatement;
import jp.co.flect.salesforce.syntax.DeleteStatement;
import jp.co.flect.salesforce.syntax.DmlStatement;
import jp.co.flect.xmlschema.SimpleType;


/**
 * SalesforceのSOAPクライアント
 */
public class SalesforceClient extends SoapClient {
	
	private static final long serialVersionUID = 5252641370219155552L;
	
	public static final int MODIFY_MAX_SIZE = 200;
	
	private String sessionId;
	private int sessionLifetime;
	
	private Metadata meta;
	private boolean defaultAllOrNone = false;
	
	private String metadataUrl;
	private LoginScope loginScope;
	
	/**
	 * コンストラクタ
	 * @param wsdlFile WSDLファイル
	 */
	public SalesforceClient(File wsdlFile) throws IOException, SAXException, InvalidWSDLException, XMLSchemaException {
		super(wsdlFile, new VelocityTemplateBuilder());
		initialize();
	}
	
	/**
	 * コンストラクタ
	 * @param doc WSDLのDOMドキュメント
	 */
	public SalesforceClient(Document doc) throws InvalidWSDLException, XMLSchemaException {
		super(doc, new VelocityTemplateBuilder());
		initialize();
	}
	
	/**
	 * コンストラクタ
	 * @param wsdl WSDL
	 */
	public SalesforceClient(WSDL wsdl) {
		super(wsdl, new VelocityTemplateBuilder());
		initialize();
	}
	
	/**
	 * コピーコンストラクタ<br>
	 * ログイン状態とMetadataを引き継ぎます。
	 */
	public SalesforceClient(SalesforceClient client) {
		super(client);
		this.sessionId = client.sessionId;
		this.sessionLifetime = client.sessionLifetime;
		this.meta = new Metadata(client.meta);
		this.defaultAllOrNone = client.defaultAllOrNone;
		this.metadataUrl = client.metadataUrl;
	}
	
	private void initialize() {
		this.meta = new Metadata(getWSDL());
		//SessionHeader以外は必須ではない
		for (OperationDef op : getWSDL().getOperationList()) {
			MessageDef msg = op.getRequestMessage();
			Iterator<QName> it = msg.getHeaders();
			while (it.hasNext()) {
				QName qname = it.next();
				ElementDef el = getWSDL().getElement(qname.getNamespaceURI(), qname.getLocalPart());
				if (el != null && !"SessionHeader".equals(el.getName())) {
					el.setMinOccurs(0);
				}
			}
		}
		setUserAgent("FLECT-SalesforceClient/1.0");
	}
	
	/** ログイン中の場合にセッションIDを返します。 */
	public String getSessionId() { return this.sessionId;}
	
	/** 
	 * セッションIDを設定します。<br>
	 * セッションIDが設定されるとログイン状態になります。
	 */
	public void setSessionId(String s) { this.sessionId = s;}
	
	/** ログイン中の場合にMetadat APIのURLを返します。 */
	public String getMetadataUrl() { return this.metadataUrl;}
	/** Metadata APIのURLを設定します。 */
	public void setMetadataUrl(String s) { this.metadataUrl = s;}
	
	/** ログイン中の場合にtrueを返します。 */
	public boolean isLogined() { return this.sessionId != null;}
	
	/** セッションの継続時間を秒単位で返します。 */
	public int getSessionLifetime() { return this.sessionLifetime;}
	/** セッションの継続時間を秒単位で設定します。 */
	public void setSessionLifetime(int n) { this.sessionLifetime = n;}
	
	/** ログイン時のスコープを返します。 */
	public LoginScope getLoginScope() { return this.loginScope;}
	/** ログイン時のスコープを設定します。 */
	public void setLoginScope(LoginScope v) { this.loginScope = v;}
	
	/** Metadataを返します */
	public Metadata getMetadata() { return this.meta;}
	/** Metadataを設定します */
	public void setMetadata(Metadata meta) { this.meta = meta;}
	
	/** 更新リクエストのAllOrNoneのデフォルト値を返します */
	public boolean isDefaultAllOrNone() { return this.defaultAllOrNone;}
	
	/** 更新リクエストのAllOrNoneのデフォルト値を設定します */
	public void setDefaultAllOrNone(boolean b) { this.defaultAllOrNone = b;}
	
	/**
	 * ログインします
	 * @param username ユーザー名
	 * @param password パスワード
	 * @param secret セキュリティトークン
	 * @return セッションID
	 */
	public UserInfo login(String username, String password, String secret) throws IOException, SoapException {
		ExtendedMap input = new ExtendedMap(true);
		input.putDeep("login.username", username);
		input.putDeep("login.password", password + secret);
		if (this.loginScope != null) {
			input.putDeep("LoginScopeHeader.organizationId", this.loginScope.getOrganizationId());
			input.putDeep("LoginScopeHeader.portalId", this.loginScope.getPortalId());
		}
		try {
			SoapResponse res = invoke("login", null, input);
			ExtendedMap output = res.getAsMap();
			this.sessionId = (String)output.getDeep("loginResponse.result.sessionId");
			this.sessionLifetime = Integer.parseInt((String)output.getDeep("loginResponse.result.userInfo.sessionSecondsValid"));
			this.metadataUrl = (String)output.getDeep("loginResponse.result.metadataServerUrl");
			setEndpoint((String)output.getDeep("loginResponse.result.serverUrl"));
			return UserInfo.fromSoap(res);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * OAuthの認可情報を使用してログインします
	 */
	public UserInfo login(OAuthResponse oauth) throws IOException {
		HttpClient client = new DefaultHttpClient();
		HttpGet method = new HttpGet(oauth.get("id"));
		method.addHeader("Authorization", "OAuth " + oauth.getAccessToken());
		HttpResponse res = client.execute(method);
		String content = HttpUtils.getContent(res);
		
		ExtendedMap map = HttpUtils.parseJson(content);
		String endpoint = getWSDL().getEndpoint();
		int idx = endpoint.lastIndexOf('/');
		String version = endpoint.substring(idx+1);
		int idx2 = endpoint.lastIndexOf('/', idx-1);
		String kind = endpoint.substring(idx2+1, idx);
		String targetPath = null;
		if (kind.equals("c")) {
			targetPath = "urls.enterprise";
		} else if (kind.equals("u")) {
			targetPath = "urls.partner";
		} else {
			throw new IllegalStateException(endpoint);
		}
		String newEndpoint = ((String)map.getDeep(targetPath)).replaceAll("\\{version\\}", version);
		this.metadataUrl = ((String)map.getDeep("urls.metadata")).replaceAll("\\{version\\}", version);
		
		this.sessionId = oauth.getAccessToken();
		setEndpoint(newEndpoint);
		return UserInfo.fromOAuth(map);
	}
	
	/**
	 * ログアウトします
	 */
	public void logout() throws IOException, SoapException {
		checkLogin();
		try {
			invoke("logout", null, null);
		} catch (TemplateException e) {
			//not occur
			e.printStackTrace();
		} finally {
			this.sessionId = null;
			setEndpoint(getWSDL().getEndpoint());
			this.meta = new Metadata(getWSDL());
			this.metadataUrl = null;
		}
	}
	
	/**
	 * ログイン状態を無効化します
	 * @param invoke trueの場合invalidateSessionsメソッドを発行します
	 */
	public void invalidateSession(boolean invoke) {
		String id = this.sessionId;
		if (id == null) {
			return;
		}
		this.sessionId = null;
		this.sessionLifetime = 0;
		
		if (invoke) {
			try {
				invalidateSessions(Arrays.asList(id));
			} catch (SoapException e) {
				//ignore
			} catch (IOException e) {
				//ignore
			}
		}
	}
	
	/**
	 * セッションを無効化します
	 * @param ids 無効化するセッションIDのリスト
	 */
	public List<BasicResult> invalidateSessions(List<String> ids) throws IOException, SoapException {
		checkLogin();
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("invalidateSessions.sessionIds", ids);
		try {
			SoapResponse res = invoke("invalidateSessions", null, params);
			BasicResult result = new BasicResult();
			StAXConstructor<BasicResult> builder = new StAXConstructor<BasicResult>(new QName(this.meta.getMessageURI(), "result"), result);
			return builder.build(res.getAsString());
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/** 
	 * 新規のSObjectインスタンスを返します。 <br>
	 * 指定されたnameに対して特化クラスが登録されている場合は戻り値はそのクラスになります。
	 */
	public SObject newObject(String name) {
		SObjectDef d = this.meta.getObjectDef(name);
		if (d == null || !d.isComplete() && isLogined()) {
			try {
				describeSObject(name);
			} catch (SoapFaultException e) {
				if (StatusCode.INVALID_TYPE.codeEquals(e.getFaultCode())) {
					throw new IllegalArgumentException(e);
				}
			} catch (SoapException e) {
				//ignore
			} catch (IOException e) {
				//ignore
			}
		}
		return this.meta.newObject(name);
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Metadata関連のAPI
	/////////////////////////////////////////////////////////////////////////////
	
	/**
	 * すべてのMetadataを収集します。<br>
	 * このメソッドの実行には時間がかかります。
	 */
	public Metadata collectMetadata() throws IOException, SoapException {
		if (this.meta.isComplete()) {
			return this.meta;
		}
		if (this.meta.getObjectDefCount() == 0) {
			describeGlobal();
		}
		List<SObjectDef> objects = this.meta.getObjectDefList();
		List<String> names = new ArrayList<String>(objects.size());
		for (SObjectDef obj : objects) {
			if (!obj.isComplete()) {
				names.add(obj.getName());
			}
		}
		if (names.size() <= 100) {
			describeSObjects(names);
		} else {
			int spos = 0;
			while (spos < names.size()) {
				int epos = spos + 100;
				if (epos > names.size()) {
					epos = names.size();
				}
				describeSObjects(names.subList(spos, epos));
				spos = epos;
			}
		}
		return this.meta;
	}
	
	/** 
	 * describeGlobalメソッドを発行します
	 */
	public Metadata describeGlobal() throws IOException, SoapException {
		checkLogin();
		try {
			SoapResponse res = invoke("describeGlobal", null, null);
			
			StAXConstructor<Metadata> builder = new StAXConstructor<Metadata>(new QName(this.meta.getMessageURI(), "result"), this.meta);
			List<Metadata> list = builder.build(res.getAsString());
			return list.get(0);
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/** 
	 * describeSObjectメソッドを発行し、指定のオブジェクトに関する情報を取得します。
	 */
	public SObjectDef describeSObject(String name) throws IOException, SoapException {
		checkLogin();
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("describeSObject.sObjectType", name);
		try {
			SoapResponse res = invoke("describeSObject", null, params);
			
			StAXConstructor<SObjectDef> builder = new StAXConstructor<SObjectDef>(new QName(this.meta.getMessageURI(), "result"), new SObjectDef(this.meta));
			List<SObjectDef> list = builder.build(res.getAsString());
			SObjectDef ret = list.get(0);
			this.meta.addObjectDef(ret);
			return ret;
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/** 
	 * describeSObjectsメソッドを発行し、指定のオブジェクトに関する情報を取得します。
	 */
	public List<SObjectDef> describeSObjects(List<String> names) throws IOException, SoapException {
		checkLogin();
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("describeSObjects.sObjectType", names);
		try {
			SoapResponse res = invoke("describeSObjects", null, params);
			
			StAXConstructor<SObjectDef> builder = new StAXConstructor<SObjectDef>(new QName(this.meta.getMessageURI(), "result"), new SObjectDef(this.meta));
			List<SObjectDef> list = builder.build(res.getAsString());
			for (SObjectDef obj : list) {
				this.meta.addObjectDef(obj);
			}
			return list;
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Query
	/////////////////////////////////////////////////////////////////////////////
	/** 
	 * queryを実行します。現在の実装では戻り値の型はSObjectになります。
	 */
	public <T extends SObject> QueryResult<T> query(String query) throws IOException, SoapException {
		return query(new QueryRequest(query));
	}
	
	/** 
	 * 戻り値の型を指定してqueryを実行します。
	 */
	public <T extends SObject> QueryResult<T> query(String query, Class<T> clazz) throws IOException, SoapException {
		QueryRequest request = new QueryRequest(query);
		request.setResultClass(clazz);
		return query(request);
	}
	
	/** 
	 * queryを実行します。
	 */
	public <T extends SObject> QueryResult<T> query(QueryRequest request) throws IOException, SoapException {
		long t = System.currentTimeMillis();
		checkLogin();
		
		String opName = request.isQueryAll() ? "queryAll" : "query";
		String query = request.getQuery();
		Class<T> clazz = request.getResultClass();
		log.debug("{0} start: query={1}", opName, query);
		
		
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep(opName + ".queryString", query);
		if (request.getBatchSize() > 0) {
			params.putDeep("QueryOptions.batchSize", Integer.toString(request.getBatchSize()));
		}
		try {
			SoapResponse res = invoke(opName, null, params);
			log.debug("{0} response: time={1}ms", opName, System.currentTimeMillis() - t);
			
			T obj = meta.newObject(clazz);
			QueryResult<T> result = new QueryResult<T>(this.meta, obj);
			if (request.getFilter() != null) {
				result.setFilter(request.getFilter());
			}
			StAXConstructor<QueryResult<T>> builder = new StAXConstructor<QueryResult<T>>(new QName(this.meta.getMessageURI(), "result"), result);
			List<QueryResult<T>> list = builder.build(res.getAsString());
			log.debug("{0} parse: time={1}ms", opName, System.currentTimeMillis() - t);
			result = list.get(0);
			if (request.isAutoMore() && result.getQueryLocator() != null) {
				while (result.getQueryLocator() != null) {
					QueryMoreRequest moreRequest = new QueryMoreRequest(result.getQueryLocator());
					moreRequest.setBatchSize(request.getBatchSize());
					moreRequest.setResultClass(request.getResultClass());
					moreRequest.setFilter(request.getFilter());
					result.addMoreResult((QueryResult<T>)queryMore(moreRequest));
				}
			}
			return result;
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/** 
	 * queryMoreを実行します。現在の実装では戻り値の型はSObjectになります。
	 */
	public <T extends SObject> QueryResult<T> queryMore(String locator) throws IOException, SoapException {
		return queryMore(new QueryMoreRequest(locator));
	}
	
	/** 
	 * 戻り値の型を指定してqueryMoreを実行します。
	 */
	public <T extends SObject> QueryResult<T> queryMore(String locator, Class<T> clazz) throws IOException, SoapException {
		QueryMoreRequest request = new QueryMoreRequest(locator);
		request.setResultClass(clazz);
		return queryMore(request);
	}
	
	/** 
	 * queryMoreを実行します。
	 */
	public <T extends SObject> QueryResult<T> queryMore(QueryMoreRequest request) throws IOException, SoapException {
		long t = System.currentTimeMillis();
		checkLogin();
		
		String locator = request.getQueryLocator();
		Class<T> clazz = request.getResultClass();
		log.debug("queryMore start: locator={0}", locator);
		
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("queryMore.queryLocator", locator);
		if (request.getBatchSize() > 0) {
			params.putDeep("QueryOptions.batchSize", Integer.toString(request.getBatchSize()));
		}
		try {
			SoapResponse res = invoke("queryMore", null, params);
			log.debug("queryMore response: time={0}ms", System.currentTimeMillis() - t);
			
			T obj = meta.newObject(clazz);
			QueryResult<T> result = new QueryResult<T>(this.meta, obj);
			if (request.getFilter() != null) {
				result.setFilter(request.getFilter());
			}
			StAXConstructor<QueryResult<T>> builder = new StAXConstructor<QueryResult<T>>(new QName(this.meta.getMessageURI(), "result"), result);
			List<QueryResult<T>> list = builder.build(res.getAsString());
			log.debug("queryMore parse: time={0}ms", System.currentTimeMillis() - t);
			return list.get(0);
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/** 
	 * searchを実行します。現在の実装では戻り値の型はSObjectになります。
	 */
	public <T extends SObject> SearchResult<T> search(String query) throws IOException, SoapException {
		return search(new SearchRequest(query));
	}
	
	/** 
	 * 戻り値の型を指定してsearchを実行します。
	 */
	public <T extends SObject> SearchResult<T> search(String query, Class<T> clazz) throws IOException, SoapException {
		SearchRequest request = new SearchRequest(query);
		request.setResultClass(clazz);
		return search(request);
	}
	
	/** 
	 * searchを実行します。
	 */
	public <T extends SObject> SearchResult<T> search(SearchRequest request) throws IOException, SoapException {
		long t = System.currentTimeMillis();
		checkLogin();
		
		String query = request.getQuery();
		Class<T> clazz = request.getResultClass();
		log.debug("search start: query={0}", query);
		
		
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("search.searchString", query);
		try {
			SoapResponse res = invoke("search", null, params);
			log.debug("search response: time={0}ms", System.currentTimeMillis() - t);
			
			T obj = meta.newObject(clazz);
			SearchResult<T> result = new SearchResult<T>(this.meta, obj);
			StAXConstructor<SearchResult<T>> builder = new StAXConstructor<SearchResult<T>>(new QName(this.meta.getMessageURI(), "result"), result);
			List<SearchResult<T>> list = builder.build(res.getAsString());
			log.debug("search parse: time={0}ms", System.currentTimeMillis() - t);
			return list.get(0);
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/** 
	 * Query結果をファイルにExportします。
	 */
	public void exportTo(ExportRequest request) throws IOException, SoapException {
		if (request.useBulk()) {
			throw new IllegalArgumentException("Not implemented yet.");
		}
		request.invoke(this);
	}
	
	/** 
	 * Fileの内容をSalesforceにImportします。
	 */
	public ImportResult importFrom(ImportRequest request) throws IOException, SoapException {
		SObjectDef objectDef = getMetadata().getObjectDef(request.getObjectName());
		if (objectDef == null || !objectDef.isComplete()) {
			objectDef = describeSObject(request.getObjectName());
		}
		return request.invoke(this, objectDef);
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// データ更新API
	/////////////////////////////////////////////////////////////////////////////
	
	/** 
	 * SObjectをSalesforce上に新規作成します。
	 */
	public SaveResult create(SObject obj) throws IOException, SoapException {
		CreateRequest request = new CreateRequest(obj);
		request.setAllOrNone(this.defaultAllOrNone);
		return create(request).get(0);
	}
	
	/** 
	 * 複数のSObjectをSalesforce上に新規作成します。
	 */
	public <T extends SObject> List<SaveResult> create(List<T> objs) throws IOException, SoapException {
		CreateRequest request = new CreateRequest(objs);
		request.setAllOrNone(this.defaultAllOrNone);
		return create(request);
	}
	
	/** 
	 * createリクエストを実行します。 <br>
	 */
	public List<SaveResult> create(CreateRequest request) throws IOException, SoapException {
		return doUpdateRequest(request);
	}
	
	/** 
	 * SObjectの内容でSalesforce上のオブジェクトを更新します。
	 */
	public SaveResult update(SObject obj) throws IOException, SoapException {
		UpdateRequest request = new UpdateRequest(obj);
		request.setAllOrNone(this.defaultAllOrNone);
		return update(request).get(0);
	}
	
	/** 
	 * 複数のSObjectの内容でSalesforce上のオブジェクトを更新します。
	 */
	public <T extends SObject> List<SaveResult> update(List<T> objs) throws IOException, SoapException {
		UpdateRequest request = new UpdateRequest(objs);
		request.setAllOrNone(this.defaultAllOrNone);
		return update(request);
	}
	
	/** 
	 * updateリクエストを実行します。 <br>
	 */
	public List<SaveResult> update(UpdateRequest request) throws IOException, SoapException {
		return doUpdateRequest(request);
	}
	
	/** 
	 * SObjectの内容でSalesforce上にオブジェクトを新規作成または更新します。
	 */
	public SaveResult upsert(SObject obj) throws IOException, SoapException {
		List<SObject> list = new ArrayList<SObject>();
		list.add(obj);
		return upsert(list).get(0);
	}
	
	/** 
	 * 複数のSObjectの内容でSalesforce上にオブジェクトを新規作成または更新します。
	 */
	public <T extends SObject> List<SaveResult> upsert(List<T> objs) throws IOException, SoapException {
		checkObjectDef(objs);
		FieldDef externalIdField = objs.get(0).getObjectDef().getExternalIdField();
		if (externalIdField == null) {
			throw new IllegalArgumentException("ExternalID field not found.");
		}
		UpsertRequest request = new UpsertRequest(objs);
		request.setAllOrNone(this.defaultAllOrNone);
		request.setExternalIdField(externalIdField.getName());
		return doUpdateRequest(request);
	}
	
	/** 
	 * upsertリクエストを実行します。 
	 */
	public List<SaveResult>  upsert(UpsertRequest request) throws IOException, SoapException {
		return doUpdateRequest(request);
	}
	
	private List<SaveResult> doUpdateRequest(ModifyRequest request) throws IOException, SoapException {
		checkLogin();
		checkObjectDef(request.getObjectList());
		
		ModifyRequestWriter writer = new ModifyRequestWriter(this.sessionId, request);
		writer.setIndent(getTemplateBuilder().getIndent());
		try {
			String msg = writer.toString();
			SoapResponse res = send("", request.getOperationName(), msg);
			
			SaveResult result = new SaveResult();
			StAXConstructor<SaveResult> builder = new StAXConstructor<SaveResult>(new QName(this.meta.getMessageURI(), "result"), result);
			return builder.build(res.getAsString());
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/** 
	 * 指定のオブジェクトを削除します。
	 */
	public SaveResult delete(String id) throws IOException, SoapException {
		IdRequest request = new IdRequest(id);
		request.setAllOrNone(this.defaultAllOrNone);
		return delete(request).get(0);
	}
	
	/** 
	 * 指定の複数のオブジェクトを削除します。
	 */
	public List<SaveResult> delete(List<String> ids) throws IOException, SoapException {
		IdRequest request = new IdRequest(ids);
		request.setAllOrNone(this.defaultAllOrNone);
		return delete(request);
	}
	
	/** 
	 * 指定の複数のオブジェクトを削除します。
	 */
	public List<SaveResult> delete(IdRequest request) throws IOException, SoapException {
		checkLogin();
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("delete.ids", request.getIdList());
		if (request.isAllOrNone()) {
			params.putDeep("AllOrNoneHeader.allOrNone", "true");
		}
		try {
			SoapResponse res = invoke("delete", null, params);
			
			SaveResult result = new SaveResult();
			StAXConstructor<SaveResult> builder = new StAXConstructor<SaveResult>(new QName(this.meta.getMessageURI(), "result"), result);
			return builder.build(res.getAsString());
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/** 
	 * 指定のオブジェクトをゴミ箱から削除します。
	 */
	public SaveResult emptyRecycleBin(String id) throws IOException, SoapException {
		IdRequest request = new IdRequest(id);
		request.setAllOrNone(this.defaultAllOrNone);
		return emptyRecycleBin(request).get(0);
	}
	
	/** 
	 * 指定の複数のオブジェクトをゴミ箱から削除します。
	 */
	public List<SaveResult> emptyRecycleBin(List<String> ids) throws IOException, SoapException {
		IdRequest request = new IdRequest(ids);
		request.setAllOrNone(this.defaultAllOrNone);
		return emptyRecycleBin(request);
	}
	
	/** 
	 * 指定の複数のオブジェクトをゴミ箱から削除します。
	 */
	public List<SaveResult> emptyRecycleBin(IdRequest request) throws IOException, SoapException {
		checkLogin();
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("emptyRecycleBin.ids", request.getIdList());
		if (request.isAllOrNone()) {
			params.putDeep("AllOrNoneHeader.allOrNone", "true");
		}
		try {
			SoapResponse res = invoke("emptyRecycleBin", null, params);
			
			SaveResult result = new SaveResult();
			StAXConstructor<SaveResult> builder = new StAXConstructor<SaveResult>(new QName(this.meta.getMessageURI(), "result"), result);
			return builder.build(res.getAsString());
		} catch (XMLStreamException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			//not occur
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// execute DML 
	/////////////////////////////////////////////////////////////////////////////
	/** 
	 * INSERT, UPDATE, UPSERT, DELETEのDML文を実行します。<br>
	 * パラメータはJDBCのSQLと同様に「?」で指定します。
	 */
	public DmlResult executeUpdate(String dml, Object... params) throws ParseException, IOException, SoapException {
		Map<String, Object> map = null;
		if (params != null && params.length > 0) {
			map = new HashMap<String, Object>();
			for (int i=0; i<params.length; i++) {
				map.put("param" + (i+1), params[i]);
			}
		}
		return executeUpdate(dml, map);
	}
	
	private DmlResult executeUpdate(String dml, Map<String, Object> params) throws ParseException, IOException, SoapException {
		ParameterQuery pq = new ParameterQuery(dml);
		if (pq.hasParameter() && params != null) {
			for (ParameterQuery.Parameter p : pq.getParameterList()) {
				Object value = params.get(p.getName());
				if (value == null) {
					if (params.containsKey(p.getName())) {
						p.setNull();
					} else {
						throw new SalesforceException("Parameter not specified: " + p.getName());
					}
				} else if (value instanceof String) {
					p.setValue(value.toString());
				} else if (value instanceof Number) {
					p.setType(ParameterQuery.ParameterType.NUMBER);
					p.setValue(value.toString());
				} else if (value instanceof Boolean) {
					p.setType(ParameterQuery.ParameterType.BOOLEAN);
					p.setValue(value.toString());
				} else if (value instanceof Date) {
					Date d = (Date)value;
					ParameterQuery.ParameterType t = p.getType();
					if (t != ParameterQuery.ParameterType.DATE && 
					    t != ParameterQuery.ParameterType.DATETIME &&
					    t != ParameterQuery.ParameterType.TIME)
					{
						t = ParameterQuery.ParameterType.DATETIME;
						p.setType(t);
					}
					switch (t) {
						case DATE:
							value = SimpleType.getBuiltinType("date").format(d);
							break;
						case DATETIME:
							value = SimpleType.getBuiltinType("dateTime").format(d);
							break;
						case TIME:
							value = SimpleType.getBuiltinType("time").format(d);
							break;
					}
					p.setValue(value.toString());
				}
			}
			dml = pq.getParameterQuery();
		}
		DmlStatement stmt = null;
		switch (pq.getQueryType()) {
			case UPDATE:
				stmt = new UpdateStatement(dml);
				break;
			case UPSERT:
				stmt = new UpsertStatement(dml);
				break;
			case INSERT:
				stmt = new InsertStatement(dml);
				break;
			case DELETE:
				stmt = new DeleteStatement(dml);
				break;
			default:
				throw new SalesforceException("Unknown statement: " + pq.getQueryType() + ", " + dml);
		}
		String objectName = stmt.getObjectName();
		SObjectDef objectDef = this.meta.getObjectDef(objectName);
		if (objectDef == null || !objectDef.isComplete()) {
			objectDef = describeSObject(objectName);
		}
		if (objectDef == null) {
			throw new SalesforceException("Unknown object: " + objectName);
		}
		return stmt.execute(this);
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Bulk API
	/////////////////////////////////////////////////////////////////////////////
	
	public BulkClient createBulkClient() {
		checkLogin();
		try {
			URL url = new URL(getEndpoint());
			String host = url.getHost();
			String apiVersion = getApiVersion();
			BulkClient ret = new BulkClient(this.meta, host, apiVersion, this.sessionId);
			ProxyInfo pi = getProxyInfo();
			if (pi != null) {
				ret.setProxyInfo(pi.getHost(), pi.getPort(), pi.getUserName(), pi.getPassword());
			}
			return ret;
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public SQLSyncResult syncSQL(SQLSyncRequest request) throws IOException, SQLException, SoapException {
		String objectName = request.getObjectName();
		SObjectDef objectDef = this.meta.getObjectDef(objectName);
		if (objectDef == null || !objectDef.isComplete()) {
			objectDef = describeSObject(objectName);
		}
		if (objectDef == null) {
			throw new SalesforceException("Unknown object: " + objectName);
		}
		BulkClient client = createBulkClient();
		ReentrantLock lock = new ReentrantLock();
		SQLSyncResult result = new SQLSyncResult(client, request, objectDef, lock);
		lock.lock();
		Exception e = result.getException();
		if (e != null) {
			if (e instanceof IOException) {
				throw (IOException)e;
			} else if (e instanceof SQLException) {
				throw (SQLException)e;
			} else if (e instanceof SoapException) {
				throw (SoapException)e;
			} else if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			} else {
				throw new IllegalStateException(e);
			}
		}
		return result;
	}
	
	public SObjectSyncResult syncSObject(SObjectSyncRequest request) throws IOException, SQLException, SoapException {
		SObjectSynchronizer sync = new SObjectSynchronizer(this, request);
		sync.prepare();
		return new SObjectSyncResult(sync);
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Metadata API
	/////////////////////////////////////////////////////////////////////////////
	
	public MetadataClient createMetadataClient(File wsdlFile) throws IOException {
		checkLogin();
		try {
			return new MetadataClient(wsdlFile, this.sessionId, this.metadataUrl);
		} catch (SAXException e) {
			throw new IllegalArgumentException(e);
		} catch (InvalidWSDLException e) {
			throw new IllegalArgumentException(e);
		} catch (XMLSchemaException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public MetadataClient createMetadataClient(WSDL wsdl) {
		checkLogin();
		return new MetadataClient(wsdl, this.sessionId, this.metadataUrl);
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// その他の内部メソッド
	/////////////////////////////////////////////////////////////////////////////
	@Override
	public SoapResponse invoke(String op, List<TemplateHint> hints, Map params, HttpResponseHandler h) throws IOException, TemplateException, SoapException {
		params = checkParams(params);
		return super.invoke(op, hints, params, h);
	}
	
	@Override
	public SoapResponse invoke(SoapTemplate template, Map params, HttpResponseHandler h) throws IOException, TemplateException, SoapException {
		params = checkParams(params);
		return super.invoke(template, params, h);
	}
	
	private void checkLogin() {
		if (!isLogined()) {
			throw new IllegalStateException("Not logined");
		}
	}
	
	private Map checkParams(Map params) {
		ExtendedMap input = null;
		if (params instanceof ExtendedMap) {
			input = (ExtendedMap)params;
		} else {
			input = new ExtendedMap(true);
			if (params != null) {
				input.putAll(params);
			}
		}
		if (this.sessionId != null) {
			input.putDeep("SessionHeader.sessionId", this.sessionId);
		}
		return input;
	}
	
	private <T extends SObject> void checkObjectDef(List<T> objs) throws IOException, SoapException {
		List<String> list = null;
		for (SObject obj : objs) {
			SObjectDef od = obj.getObjectDef();
			if (od == null || !od.isComplete()) {
				if (list == null) {
					list = new ArrayList<String>();
				}
				String name = obj.getObjectName();
				if (!list.contains(name)) {
					list.add(name);
				}
			} else {
				obj.validate();
			}
		}
		if (list == null) {
			return;
		}
		describeSObjects(list);
		for (SObject obj : objs) {
			SObjectDef od = obj.getObjectDef();
			if (od == null || !od.isComplete()) {
				String name = obj.getObjectName();
				obj.setObjectDef(this.meta.getObjectDef(name));
				obj.validate();
			}
		}
	}
	
	private String getApiVersion() {
		String endpoint = getWSDL().getEndpoint();
		int idx = endpoint.lastIndexOf('/');
		return endpoint.substring(idx+1);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		meta.setWSDL(getWSDL());
	}
}
