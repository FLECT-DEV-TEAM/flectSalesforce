package jp.co.flect.salesforce.apex;

import jp.co.flect.salesforce.UserInfo;
import jp.co.flect.salesforce.LoginScope;
import jp.co.flect.soap.SoapClient;
import jp.co.flect.soap.SoapException;
import jp.co.flect.util.ExtendedMap;
import jp.co.flect.soap.SoapResponse;
import jp.co.flect.soap.InvalidWSDLException;
import java.io.IOException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import jp.co.flect.xmlschema.XMLSchemaException;
import java.io.File;
import jp.co.flect.soap.WSDL;
import jp.co.flect.soap.OperationDef;
import jp.co.flect.soap.TypedObject;
import java.util.Map;
import java.util.List;
import jp.co.flect.template.TemplateException;

/**
 * ApexのWebServiceを実行するクライアント
 */
public class ApexClient extends SoapClient {
	
	private static final String LOGIN_ENDPOINT_BASE = "https://login.salesforce.com/services/Soap/u/";
	private static String LOGIN_API_VERSION = "28.0";
	
	public static String getLoginApiVersion() { return LOGIN_API_VERSION;}
	public static void setLoginApiVersion(String v) { LOGIN_API_VERSION = v;}
	
	private static final String LOGIN_REQUEST = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
		"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
			"<soap:Header>${header}</soap:Header>" +
			"<soap:Body>" +
				"<tns:login xmlns:tns=\"urn:partner.soap.sforce.com\">" +
					"<tns:username>${username}</tns:username>" + 
					"<tns:password>${password}</tns:password>" + 
				"</tns:login>" +
			"</soap:Body>" +
		"</soap:Envelope>";
	
	public enum LogType {
		None,
		Debugonly,
		Db,
		Profiling,
		Callout,
		Detail
	}
	
	private boolean useSandbox = false;
	private LogType debugLevel = LogType.None;
	private boolean allowFieldTruncation = false;
	private String sessionId;
	private LoginScope loginScope;
	
	/**
	 * Constructor
	 * @param wsdlFile WSDL file
	 */
	public ApexClient(File wsdlFile) throws IOException, SAXException, InvalidWSDLException, XMLSchemaException {
		super(wsdlFile);
	}
	
	/**
	 * Constructor
	 * @param doc document of WSDL
	 */
	public ApexClient(Document doc) throws InvalidWSDLException, XMLSchemaException {
		super(doc);
	}
	
	/**
	 * Constructor
	 * @param wsdl WSDL
	 */
	public ApexClient(WSDL wsdl) {
		super(wsdl);
	}
	
	/**
	 * コピーコンストラクタ<br>
	 * ログイン状態を引き継ぎます。
	 */
	public ApexClient(ApexClient client) {
		super(client);
		this.useSandbox = client.useSandbox;
		this.debugLevel = client.debugLevel;
		this.allowFieldTruncation = client.allowFieldTruncation;
		this.sessionId = client.sessionId;
	}
	
	public boolean isUseSandbox() { return this.useSandbox;}
	public void setUseSandbox(boolean v) { this.useSandbox = v;}
	
	/** ログイン時のスコープを返します。 */
	public LoginScope getLoginScope() { return this.loginScope;}
	/** ログイン時のスコープを設定します。 */
	public void setLoginScope(LoginScope v) { this.loginScope = v;}
	
	public LogType getDebugLevel() { return this.debugLevel;}
	public void setDebugLeve(LogType v) { this.debugLevel = v;}
	
	public boolean isAllowFieldTruncation() { return this.allowFieldTruncation;}
	public void setAllowFieldTruncation(boolean v) { this.allowFieldTruncation = v;}
	
	public String getSessionId() { return this.sessionId;}
	public void setSessionId(String v) { this.sessionId = v;}
	
	/** ログイン中の場合にtrueを返します。 */
	public boolean isLogined() { return this.sessionId != null;}
	
	private void checkLogin() {
		if (!isLogined()) {
			throw new IllegalStateException("Not logined");
		}
	}
	
	/**
	 * ログインします
	 * @param username ユーザー名
	 * @param password パスワード
	 * @param secret セキュリティトークン
	 * @return UserInfo
	 */
	public UserInfo login(String username, String password, String secret) throws IOException, SoapException {
		String header = "";
		if (this.loginScope != null) {
			header += "<tns:LoginScopeHeader xmlns:tns=\"urn:partner.soap.sforce.com\">" +
						"<tns:organizationId>" + this.loginScope.getOrganizationId() + "</tns:organizationId>" +
						"<tns:portalId>" + this.loginScope.getPortalId() + "</tns:portalId>" +
					"</tns:LoginScopeHeader>";
		}
		String request = LOGIN_REQUEST
			.replace("${username}", username)
			.replace("${password}", password + secret)
			.replace("${header}", header);
		
		String orgEndpoint = getEndpoint();
		try {
			String loginEndpoint = LOGIN_ENDPOINT_BASE + LOGIN_API_VERSION;
			if (isUseSandbox()) {
				loginEndpoint = loginEndpoint.replace("login", "test");
			}
			setEndpoint(loginEndpoint);
			
			SoapResponse res = send("", "login", request, null);
			ExtendedMap output = res.getAsMap();
			this.sessionId = (String)output.getDeep("loginResponse.result.sessionId");
			return UserInfo.fromSoap(res);
		} finally {
			setEndpoint(orgEndpoint);
		}
	}
	
	public ExtendedMap simpleInvoke(String opName, Map<String, Object> simpleParams) throws IOException, SoapException {
		SoapResponse res = doSimpleInvoke(opName, simpleParams);
		ExtendedMap resultMap = res.getAsObjectMap();
		return (ExtendedMap)resultMap.get(opName + "Response");
	}
	
	public <T extends TypedObject> T simpleInvoke(String opName, Map<String, Object> simpleParams, Class<T> clazz) throws IOException, SoapException {
		SoapResponse res = doSimpleInvoke(opName, simpleParams);
		return res.getAsObject(clazz);
	}
	
	public <T extends TypedObject> List<T> simpleInvokeAsList(String opName, Map<String, Object> simpleParams, Class<T> clazz) throws IOException, SoapException {
		SoapResponse res = doSimpleInvoke(opName, simpleParams);
		return res.getAsList(clazz);
	}
	
	private SoapResponse doSimpleInvoke(String opName, Map<String, Object> simpleParams) throws IOException, SoapException {
		checkLogin();
		OperationDef op = getOperation(opName);
		if (op == null) {
			throw new IllegalArgumentException("Unknown operation: " + opName);
		}
		ExtendedMap params = new ExtendedMap(true);
		params.put(opName, simpleParams);
		params.putDeep("SessionHeader.sessionId", this.sessionId);
		params.putDeep("DebuggingHeader.debugLevel", this.debugLevel.toString());
		params.putDeep("AllowFieldTruncationHeader.allowFieldTruncation", Boolean.toString(this.allowFieldTruncation));
		try {
			return invoke(opName, null, params);
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
}

