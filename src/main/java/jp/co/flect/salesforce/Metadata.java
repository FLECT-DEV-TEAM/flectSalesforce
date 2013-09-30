package jp.co.flect.salesforce;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.soap.WSDL;
import jp.co.flect.xml.StAXConstruct;
import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.salesforce.sobject.User;

/**
 * Metadata
 * !!!注意!!!
 * Serialize時にはWSDLはserializeされないので復帰時に再設定する必要があります。
 */
public class Metadata implements StAXConstruct<Metadata>, Serializable {
	
	private static final long serialVersionUID = -1786233218892678903L;
	
	private static Map<String, Class<? extends SObject>> classMap = new HashMap<String, Class<? extends SObject>>();
	
	public static <T extends SObject> void registerClass(String name, Class<T> clazz) {
		classMap.put(name, clazz);
	}
	
	static {
		Metadata.registerClass("User", User.class);
	}
	
	public static boolean isClassRegistered(String name) {
		return classMap.containsKey(name);
	}
	
	private Map<String, SObjectDef> map = new HashMap<String, SObjectDef>();
	
	private transient WSDL wsdl;
	private transient String messageUri;
	private transient String sobjectUri;
	private transient String faultUri;
	
	private String encoding;
	private int maxBatchSize;
	
	public Metadata(WSDL wsdl) {
		setWSDL(wsdl);
	}
	
	public Metadata(Metadata meta) {
		this.wsdl = meta.wsdl;
		this.map.putAll(meta.map);
		
		this.messageUri = meta.messageUri;
		this.sobjectUri = meta.sobjectUri;
		this.faultUri = meta.faultUri;
		
		this.encoding = meta.encoding;
		this.maxBatchSize = meta.maxBatchSize;
	}
	
	public WSDL getWSDL() { return this.wsdl;}
	public void setWSDL(WSDL wsdl) {
		this.wsdl = wsdl;
		//enterprise.wsdlとpartner.wsdlで名前空間が異なるのでwsdlから取得
		for (XMLSchema schema : wsdl.getSchemaList()) {
			String nsuri = schema.getTargetNamespace();
			if (nsuri.indexOf("sobject") != -1) {
				sobjectUri = nsuri;
			} else if (nsuri.indexOf("fault") != -1) {
				faultUri = nsuri;
			} else if (messageUri == null) {
				messageUri = nsuri;
			} else {
				throw new IllegalStateException();
			}
		}
	}
	
	public String getMessageURI() { return this.messageUri;}
	public String getObjectURI() { return this.sobjectUri;}
	public String getFaultURI() { return this.faultUri;}
	
	public TypeDef getMessageType(String name) { return getType(messageUri, name);}
	public TypeDef getObjectType(String name) { return getType(sobjectUri, name);}
	public TypeDef getFaultType(String name) { return getType(faultUri, name);}
	
	private TypeDef getType(String nsuri, String name) {
		if (this.wsdl == null) {
			return null;
		}
		return this.wsdl.getType(nsuri, name);
	}
	
	public SObjectDef getObjectDef(String name) { 
		if (name == null) {
			return null;
		}
		return this.map.get(name.toLowerCase());
	}
	
	public void addObjectDef(SObjectDef obj) {
		this.map.put(obj.getName().toLowerCase(), obj);
	}
	
	public List<SObjectDef> getObjectDefList() {
		List<SObjectDef> list = new ArrayList<SObjectDef>(this.map.values());
		Collections.sort(list, new Comparator<SObjectDef>() {
			public int compare(SObjectDef f1, SObjectDef f2) {
				return f1.getName().compareTo(f2.getName());
			}
		});
		return list;
	}
	
	public int getObjectDefCount() { return this.map.size();}
	
	public void build(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		String targetNamespace = getMessageURI();
		
		int depth = 1;
		boolean finished = false;
		while (!finished && reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
				{
					String nsuri = reader.getNamespaceURI();
					String name = reader.getLocalName();
					if (targetNamespace.equals(nsuri)) {
						if ("sobjects".equals(name)) {
							SObjectDef obj = new SObjectDef(this);
							obj.build(reader);
							this.map.put(obj.getName().toLowerCase(), obj);
						} else if ("encoding".equals(name)) {
							this.encoding = reader.getElementText();
						} else if ("maxBatchSize".equals(name)) {
							this.maxBatchSize = Integer.parseInt(reader.getElementText());
						} else {
							depth++;
						}
					} else {
						depth++;
					}
					break;
				}
				case XMLStreamReader.END_ELEMENT:
				{
					depth--;
					if (depth == 0) {
						finished = true;
					}
					break;
				}
			}
		}
	}
	
	public Metadata newInstance() {
		//新規の構築時にも新しいインスタンスを使用しない
		return this;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("MessageURI: ").append(this.messageUri)
			.append("\nObjectURI: ").append(this.sobjectUri)
			.append("\nFaultURI: ").append(this.faultUri);
		if (this.encoding != null) {
			buf.append("\nencoding: ").append(this.encoding);
		}
		if (this.maxBatchSize > 0) {
			buf.append("\nmaxBatchSize: ").append(this.maxBatchSize);
		}
		if (this.map.size() > 0) {
			for (SObjectDef obj : getObjectDefList()) {
				buf.append("\nsobjects:");
				obj.buildString(buf, 2);
			}
		}
		return buf.toString();
	}
	
	public boolean isComplete() {
		if (this.map.size() == 0) {
			return false;
		}
		for (SObjectDef obj : this.map.values()) {
			if (!obj.isComplete()) {
				return false;
			}
		}
		return true;
	}
	
	public void clear() {
		this.map.clear();
	}
	
	public SObject newObject(String name) {
		SObjectDef d = getObjectDef(name);
		ComplexType type = (ComplexType)getObjectType(name);
		Class<? extends SObject> clazz = classMap.get(name);
		SObject ret = clazz != null ? newObject(clazz) : d != null ? new SObject(d) : new SObject(this, name);
		ret.setObjectDef(d);
		ret.setType(type);
		ret.setObjectName(name);
		return ret;
	}
	
	public <T extends SObject> T newObject(Class<T> clazz) {
		try {
			Constructor<T> c = clazz.getConstructor(Metadata.class);
			return c.newInstance(this);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
