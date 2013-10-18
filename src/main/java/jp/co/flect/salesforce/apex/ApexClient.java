package jp.co.flect.salesforce.apex;

import jp.co.flect.salesforce.UserInfo;
import jp.co.flect.soap.SoapClient;
import jp.co.flect.soap.SoapException;
import jp.co.flect.soap.SoapResponse;
import jp.co.flect.soap.OperationDef;
import jp.co.flect.soap.MessageDef;
import jp.co.flect.soap.WSDL;
import jp.co.flect.soap.InvalidWSDLException;
import jp.co.flect.template.TemplateException;
import jp.co.flect.util.ExtendedMap;
import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.xml.XMLUtils;
import jp.co.flect.xmlschema.XMLSchemaException;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.TypeDef;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.StringReader;
import java.io.IOException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;

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
			"<soap:Header></soap:Header>" +
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
	private ApexConverter converter;
	
	/**
	 * Constructor
	 * @param wsdlFile WSDL file
	 */
	public ApexClient(File wsdlFile) throws IOException, SAXException, InvalidWSDLException, XMLSchemaException {
		super(wsdlFile);
		this.converter = new ApexConverter(getWSDL());
	}
	
	/**
	 * Constructor
	 * @param doc document of WSDL
	 */
	public ApexClient(Document doc) throws InvalidWSDLException, XMLSchemaException {
		super(doc);
		this.converter = new ApexConverter(getWSDL());
	}
	
	/**
	 * Constructor
	 * @param wsdl WSDL
	 */
	public ApexClient(WSDL wsdl) {
		super(wsdl);
		this.converter = new ApexConverter(getWSDL());
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
		this.converter = client.converter;
	}
	
	public <T extends ApexObject> void addApexClass(String objectName, Class<T> c) {
		this.converter.addApexClass(objectName, c);
	}
	
	public void addApexObject(ApexObject obj) {
		this.converter.addApexClass(obj.getObjectName(), obj.getClass());
	}
	
	public boolean isUseSandbox() { return this.useSandbox;}
	public void setUseSandbox(boolean v) { this.useSandbox = v;}
	
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
		String request = LOGIN_REQUEST
			.replace("${username}", username)
			.replace("${password}", password + secret);
		
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
	
	public Map<String, Object> invoke(String opName, Map<String, Object> simpleParams) throws IOException, SoapException {
		checkLogin();
		OperationDef op = getOperation(opName);
		if (op == null) {
			throw new IllegalArgumentException("Unknown operation: " + opName);
		}
		ExtendedMap params = new ExtendedMap(true);
		Iterator<QName> it = op.getRequestMessage().getBodies();
		while (it.hasNext()) {
			QName qname = it.next();
			Object value = simpleParams.get(qname.getLocalPart());
			if (value instanceof ApexObject) {
				Map<String, String> map = this.converter.toMap((ApexObject)value);
				for (Map.Entry<String, String> entry : map.entrySet()) {
					String key = entry.getKey();
					params.putDeep(opName + "." + qname.getLocalPart() + "." + entry.getKey(), entry.getValue());
				}
			} else if (value != null) {
				ElementDef el = getWSDL().getElement(qname.getNamespaceURI(), qname.getLocalPart());
				//assert el != null;
				SimpleType type = (SimpleType)el.getType();
				params.put(opName + "." + qname.getLocalPart(), type.format(value));
			}
		}
		params.putDeep("SessionHeader.sessionId", this.sessionId);
		params.putDeep("DebuggingHeader.debugLevel", this.debugLevel.toString());
		params.putDeep("AllowFieldTruncationHeader.allowFieldTruncation", Boolean.toString(this.allowFieldTruncation));
		try {
			SoapResponse res = invoke(opName, null, params);
			return parseResponse(op.getResponseMessage(), res.getAsString());
		} catch (TemplateException e) {
			//not occur
			throw new IllegalStateException(e);
		}
	}
	
	private Map<String, Object> parseResponse(MessageDef md, String body) throws SoapException {
		Map<String, Object> map = new HashMap<String, Object>();
		
		String soapUri = null;
		boolean startData = false;
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory. IS_COALESCING, Boolean.TRUE);
		try {
			XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(body));
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
					case XMLStreamReader.START_ELEMENT:
						String nsuri = reader.getNamespaceURI();
						String name = reader.getLocalName();
						if (soapUri == null) {
							if (!XMLUtils.XMLNS_SOAP12_ENVELOPE.equals(nsuri) && !XMLUtils.XMLNS_SOAP_ENVELOPE.equals(nsuri)) {
								throw new SoapException("Invalid soap namespace: " + nsuri);
							}
							soapUri = nsuri;
						}
						if (startData) {
							ElementDef el = null;
							Iterator<QName> it = md.getBodies();
							while (it.hasNext()) {
								QName qname = it.next();
								if (nsuri.equals(qname.getNamespaceURI()) && name.equals(qname.getLocalPart())) {
									el = getWSDL().getElement(nsuri, name);
									break;
								}
							}
							processElement(map, nsuri, name, el, reader);
						} else if (soapUri.equals(nsuri) && "Body".equals(name)) {
							startData = true;
						}
						break;
					case XMLStreamReader.CHARACTERS:
					case XMLStreamReader.CDATA:
						if (startData && !reader.isWhiteSpace()) {
							throw new IllegalStateException();
						}
						break;
					case XMLStreamReader.END_DOCUMENT:
						reader.close();
						break;
					case XMLStreamReader.END_ELEMENT:
					case XMLStreamReader.START_DOCUMENT:
					case XMLStreamReader.ATTRIBUTE:
					case XMLStreamReader.NAMESPACE:
					case XMLStreamReader.SPACE:
					case XMLStreamReader.COMMENT:
					case XMLStreamReader.PROCESSING_INSTRUCTION:
					case XMLStreamReader.ENTITY_REFERENCE:
					case XMLStreamReader.DTD:
						break;
				}
			}
			return map;
		} catch (XMLStreamException e) {
			throw new SoapException(e);
		}
	}
	
	private Object parseSimple(SimpleType type, XMLStreamReader reader) throws XMLStreamException {
		StringBuilder buf = new StringBuilder();
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.CHARACTERS:
				case XMLStreamReader.CDATA:
					buf.append(reader.getText());
					break;
				case XMLStreamReader.END_ELEMENT:
					return buf.length() > 0 ? type.parse(buf.toString()) : null;
				case XMLStreamReader.START_DOCUMENT:
				case XMLStreamReader.END_DOCUMENT:
				case XMLStreamReader.START_ELEMENT:
				case XMLStreamReader.ATTRIBUTE:
				case XMLStreamReader.NAMESPACE:
				case XMLStreamReader.SPACE:
				case XMLStreamReader.COMMENT:
				case XMLStreamReader.PROCESSING_INSTRUCTION:
				case XMLStreamReader.ENTITY_REFERENCE:
				case XMLStreamReader.DTD:
					throw new IllegalStateException();
			}
		}
		throw new IllegalStateException();
	}
	
	private Map<String, Object> parseComplex(ComplexType type, XMLStreamReader reader) throws XMLStreamException {
		Map<String, Object> map = new HashMap<String, Object>();
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
					String nsuri = reader.getNamespaceURI();
					String name = reader.getLocalName();
					ElementDef el = type.getModel(nsuri, name);
					processElement(map, nsuri, name, el, reader);
					break;
				case XMLStreamReader.CHARACTERS:
				case XMLStreamReader.CDATA:
					if (!reader.isWhiteSpace()) {
						throw new IllegalStateException();
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					return map.size() == 0 ? null : map;
				case XMLStreamReader.START_DOCUMENT:
				case XMLStreamReader.END_DOCUMENT:
				case XMLStreamReader.ATTRIBUTE:
				case XMLStreamReader.NAMESPACE:
				case XMLStreamReader.SPACE:
				case XMLStreamReader.COMMENT:
				case XMLStreamReader.PROCESSING_INSTRUCTION:
				case XMLStreamReader.ENTITY_REFERENCE:
				case XMLStreamReader.DTD:
					throw new IllegalStateException();
			}
		}
		throw new IllegalStateException();
	}
	
	private void processElement(Map<String, Object> map, String nsuri, String name, ElementDef el, XMLStreamReader reader) throws XMLStreamException {
		if (el == null) {
			throw new IllegalStateException("Unknown element: " + nsuri + ", " + name);
		}
		Object value = null;
		TypeDef type = el.getType();
		if (type.isSimpleType()) {
			value = parseSimple((SimpleType)type, reader);
		} else if (type.getName() != null && this.converter.hasApexClass(type.getName())) {
			value = this.converter.toObject(type.getName(), reader);
		} else {
			value = parseComplex((ComplexType)type, reader);
		}
		if (value != null) {
			map.put(name, value);
		}
	}
}

