package jp.co.flect.salesforce;

import java.util.Date;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.util.StringUtils;
import static jp.co.flect.util.StringUtils.checkNull;
import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.xml.XMLUtils;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.salesforce.annotation.ApiName;
import jp.co.flect.salesforce.query.QueryResult;
import jp.co.flect.salesforce.sobject.User;
import jp.co.flect.soap.SimpleObject;


/**
 * Salesforceのオブジェクトを表すクラスです。
 */
public class SObject extends SimpleObject {
	
	private static final long serialVersionUID = 8598475059446861366L;
	
	private static Map<Class, Reflector> classMap = new HashMap<Class, Reflector>();
	
	public static Reflector getReflector(Class c) { return classMap.get(c);}
	
	private static boolean isTargetField(Field f) {
		if (Modifier.isTransient(f.getModifiers())) {
			return false;
		}
		Class type = f.getType();
		if (type.isPrimitive()) {
			return true;
		}
		return type.equals(String.class) || 
		       Date.class.isAssignableFrom(type) ||
		       SObject.class.isAssignableFrom(type);
	}
	
	private Metadata meta;
	private SObjectDef objectDef;
	private String objectName;
	private Class reflectBase;
	
	/**
	 * Metadataを引数にとるコンストラクタ
	 */
	public SObject(Metadata meta) {
		this.meta = meta;
		this.objectName = getClass().getSimpleName();
		setObjectDef(meta.getObjectDef(this.objectName));
		setType((ComplexType)meta.getObjectType(this.objectName));
	}
	
	/**
	 * Metadataとオブジェクト名を引数にとるコンストラクタ
	 */
	public SObject(Metadata meta, String objectName) {
		this.meta = meta;
		this.objectName = objectName;
		setObjectDef(meta.getObjectDef(this.objectName));
		setType((ComplexType)meta.getObjectType(this.objectName));
	}
	
	/**
	 * オブジェクト定義を引数にとるコンストラクタ
	 */
	public SObject(SObjectDef objectDef) {
		this.meta = objectDef.getMetadata();
		setObjectDef(objectDef);
		setType((ComplexType)meta.getObjectType(objectDef.getName()));
	}
	
	/**
	 * 引数のオブジェクトの内容をこのオブジェクトにコピーします。
	 */
	public void assign(SObject obj) {
		Map<String, Object> map = getMap();
		map.clear();
		map.putAll(obj.getMap());
		setType(obj.getType());
		setOptimizeLevel(obj.getOptimizeLevel());
		this.meta = obj.meta;
		this.objectDef = obj.objectDef;
		this.objectName = obj.objectName;
		if (obj.reflectBase != null && obj.reflectBase.isAssignableFrom(this.getClass())) {
			this.reflectBase = obj.reflectBase;
		}
		mapToField();
	}
	
	/**
	 * Metadataを返します。
	 */
	public Metadata getMetadata() { return this.meta;}
	
	/**
	 * オブジェクト定義を返します。
	 */
	public SObjectDef getObjectDef() { return this.objectDef;}
	
	/**
	 * オブジェクト定義を設定します。
	 */
	public void setObjectDef(SObjectDef o) { this.objectDef = o;}
	
	/**
	 * Reflectionを行うベースクラスを返します。
	 */
	protected Class getReflectBase() { return this.reflectBase;}
	
	/**
	 * Reflectionを行うベースクラスを返します。
	 */
	protected void setReflectBase(Class c) {
		this.reflectBase = c;
		if (c != null) {
			Class myClass = this.getClass();
			Reflector ref = classMap.get(myClass);
			if (ref == null) {
				String name = myClass.getSimpleName();
				ApiName api = (ApiName)myClass.getAnnotation(ApiName.class);
				if (api != null) {
					name = api.value();
				}
				ref = new Reflector(name);
				classMap.put(myClass, ref);
				while (c.isAssignableFrom(myClass)) {
					Field[] fields = myClass.getDeclaredFields();
					for (int i=0; i<fields.length; i++) {
						Field f = fields[i];
						if (isTargetField(f)) {
							name = f.getName();
							api = f.getAnnotation(ApiName.class);
							if (api != null) {
								name = api.value();
							}
							ref.add(name, f);
						}
					}
					myClass = myClass.getSuperclass();
				}
			}
			setObjectName(ref.getName());
			setObjectDef(meta.getObjectDef(ref.getName()));
			setType((ComplexType)meta.getObjectType(ref.getName()));
		}
	}
	
	/**
	 * このオブジェクトのNamespaceURIを返します。
	 */
	public String getNamespace() { return this.meta.getObjectURI();}
	/**
	 * オブジェクト名を返します。
	 */
	public String getObjectName() { 
		if (this.objectDef != null) {
			return this.objectDef.getName();
		} else if (getType() != null) {
			return getType().getName();
		} else {
			return this.objectName;
		}
	}
	/**
	 * オブジェクト名を設定します。
	 */
	protected void setObjectName(String s) { this.objectName = s;}
	
	@Override
	public void set(String name, Object value) {
		super.set(name, value);
		mapToField(name);
	}
	
	/**
	 * Idを返します。
	 */
	public String getId() { return getString("Id");}
	/**
	 * Idを設定します。
	 */
	public void setId(String s) { set("Id", s);}
	
	@Override
	protected TypeDef getSoapType(String name) {
		if (this.objectDef != null) {
			FieldDef fd = this.objectDef.getField(name);
			if (fd != null) {
				return fd.getSoapType();
			}
		}
		return super.getSoapType(name);
	}
	
	public Object getDeep(String name) {
		int idx = name.indexOf('.');
		if (idx == -1) {
			return get(name);
		} else {
			String cur = name.substring(0, idx);
			String next = name.substring(idx+1);
			Object o = get(cur);
			if (o instanceof SObject) {
				return ((SObject)o).getDeep(next);
			}
		}
		return null;
	}
	
	public String getStringDeep(String name) {
		int idx = name.indexOf('.');
		if (idx == -1) {
			return getString(name);
		} else {
			String cur = name.substring(0, idx);
			String next = name.substring(idx+1);
			Object o = get(cur);
			if (o instanceof SObject) {
				return ((SObject)o).getStringDeep(next);
			}
		}
		return null;
	}
	
	/**
	 * 名前に対応する値をSObjectで返します。
	 */
	public SObject getObject(String name) {
		Object o = get(name);
		if (o == null) {
			return null;
		} else if (o instanceof SObject) {
			return (SObject)o;
		} else {
			throw new IllegalArgumentException(name);
		}
	}
	
	/**
	 * 名前に対応する値をQueryResultで返します。
	 */
	public <T extends SObject> QueryResult<T> getQueryResult(String name) {
		Object o = get(name);
		if (o == null) {
			return null;
		} else if (o instanceof QueryResult) {
			return (QueryResult<T>)o;
		} else {
			throw new IllegalArgumentException(name);
		}
	}
	
	@Override
	public void build(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		if (this.objectDef != null && this.objectDef.isComplete()) {
			buildByDef(reader, 1);
		} else if (getType() != null) {
			buildByType(reader, 1);
		} else {
			String type = reader.getAttributeValue(XMLUtils.XMLNS_XSI, "type");
			if (type == null) {
				//SObject要素ではありえない
				throw new IllegalStateException();
			}
			type = type.substring(type.indexOf(':')+1);
			if (!type.equals("sObject")) {
				SObjectDef d = meta.getObjectDef(type);
				ComplexType t = (ComplexType)meta.getObjectType(type);
				
				this.objectName = type;
				setObjectDef(d);
				setType(t);
				
				if (d != null && d.isComplete()) {
					buildByDef(reader, 1);
				} else if (t != null) {
					buildByType(reader, 1);
				} else {
					buildByNone(reader, 1);
				}
			} else {
				//partner wsdl
				int n = reader.next();
				if (n == XMLStreamReader.START_ELEMENT && 
				    "type".equals(reader.getLocalName()) && 
				    meta.getObjectURI().equals(reader.getNamespaceURI())) 
				{
					type = reader.getElementText();
					reader.next();
					
					this.objectName = type;
					SObjectDef d = meta.getObjectDef(type);
					ComplexType t = (ComplexType)meta.getObjectType(type);
					
					setObjectDef(d);
					setType(t);
					
					if (d != null && d.isComplete()) {
						buildByDef(reader, 2);
					} else if (t != null) {
						buildByType(reader, 2);
					} else {
						buildByNone(reader, 2);
					}
				} else {
					throw new IllegalStateException();
				}
			}
		}
		mapToField();
	}
	
	private void buildByDef(XMLStreamReader reader, int depth) throws XMLStreamException, StAXConstructException {
		Map<String, Object> map = getMap();
		boolean finished = false;
		while (!finished && reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
				{
					String name = reader.getLocalName();
					FieldDef field = objectDef.getField(name);
					if (field != null) {
						String value = checkNull(reader.getElementText());
						if (value != null) {
							SimpleType soapType = field.getSoapType();
							map.put(name, soapType.parse(value));
						}
						continue;
					}
					field = objectDef.getSingleRelation(name);
					if (field != null) {
						String relName = field.getReferenceToName();
						SObject obj = meta.newObject(relName);
						obj.build(reader);
						map.put(name, obj);
						continue;
					}
					RelationDef rel = objectDef.getMultipleRelation(name);
					if (rel != null) {
						SObject obj = meta.newObject(rel.getObjectName());
						QueryResult<?> result = QueryResult.newInstance(this.meta, obj);
						result.build(reader);
						map.put(name, result);
						continue;
					}
					//定義のわからないフィールド
					String value = checkNull(reader.getElementText());
					if (value != null) {
						map.put(name, value);
					}
					break;
				}
				case XMLStreamReader.END_ELEMENT:
					depth--;
					if (depth == 0) {
						finished = true;
					}
					break;
			}
		}
	}
	
	private void buildByType(XMLStreamReader reader, int depth) throws XMLStreamException, StAXConstructException {
		Map<String, Object> map = getMap();
		boolean finished = false;
		while (!finished && reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
				{
					String name = reader.getLocalName();
					ElementDef el = ((ComplexType)getType()).getModel(meta.getObjectURI(), name);
					if (el != null) {
						TypeDef type = el.getType();
						if (type.isSimpleType()) {
							String value = checkNull(reader.getElementText());
							if (value != null) {
								SimpleType soapType = (SimpleType)type;
								map.put(name, soapType.parse(value));
							}
						} else if (meta.getObjectURI().equals(type.getNamespace())) {
							String objectName = type.getName();
							SObject obj = meta.newObject(objectName);
							obj.build(reader);
							map.put(name, obj);
						} else if (type.getName().equals("QueryResult")) {
							QueryResult<?> ret = QueryResult.create(reader, meta);
							map.put(name, ret);
						} else {
							throw new IllegalStateException();
						}
					} else {
						String type = reader.getAttributeValue(XMLUtils.XMLNS_XSI, "type");
						if (type != null) {
							SObject obj = new SObject(meta);
							obj.build(reader);
							if (Metadata.isClassRegistered(obj.getObjectName())) {
								SObject obj2 = meta.newObject(obj.getObjectName());
								obj2.assign(obj);
								obj = obj2;
							}
							map.put(name, obj);
						} else {
							event = reader.next();
							while (event == XMLStreamReader.CHARACTERS && reader.isWhiteSpace()) {
								event = reader.next();
							}
							if (event == XMLStreamReader.CHARACTERS) {
								map.put(name, reader.getText());
								if (reader.next() != XMLStreamReader.END_ELEMENT) {
									throw new IllegalStateException();
								}
							} else if (event == XMLStreamReader.START_ELEMENT) {
								if (meta.getMessageURI().equals(reader.getNamespaceURI()) && "done".equals(reader.getLocalName())) {
									QueryResult<?> ret = QueryResult.create(reader, meta);
									map.put(name, ret);
								} else {
									throw new IllegalStateException(reader.getName().toString());
								}
							}
						}
					}
					break;
				}
				case XMLStreamReader.END_ELEMENT:
					depth--;
					if (depth == 0) {
						finished = true;
					}
					break;
			}
		}
	}
	
	private void buildByNone(XMLStreamReader reader, int depth) throws XMLStreamException, StAXConstructException {
		Map<String, Object> map = getMap();
		boolean finished = false;
		while (!finished && reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
				{
					String name = reader.getLocalName();
					String type = reader.getAttributeValue(XMLUtils.XMLNS_XSI, "type");
					if (type != null) {
						if (type.endsWith("QueryResult")) {
							QueryResult<?> ret = QueryResult.create(reader, meta);
							map.put(name, ret);
						} else {
							SObject obj = new SObject(meta);
							obj.build(reader);
							if (Metadata.isClassRegistered(obj.getObjectName())) {
								SObject obj2 = meta.newObject(obj.getObjectName());
								obj2.assign(obj);
								obj = obj2;
							}
							map.put(name, obj);
						}
					} else {
						event = reader.next();
						while (event == XMLStreamReader.CHARACTERS && reader.isWhiteSpace()) {
							event = reader.next();
						}
						if (event == XMLStreamReader.CHARACTERS) {
							map.put(name, reader.getText());
							if (reader.next() != XMLStreamReader.END_ELEMENT) {
								throw new IllegalStateException();
							}
						} else if (event == XMLStreamReader.START_ELEMENT) {
							if (meta.getMessageURI().equals(reader.getNamespaceURI()) && "done".equals(reader.getLocalName())) {
								QueryResult<?> ret = QueryResult.create(reader, meta);
								map.put(name, ret);
							} else {
								throw new IllegalStateException(reader.getName().toString());
							}
						}
					}
					break;
				}
				case XMLStreamReader.END_ELEMENT:
					depth--;
					if (depth == 0) {
						finished = true;
					}
					break;
			}
		}
	}
	
	@Override
	public void buildString(StringBuilder buf, int indent) {
		String strIndent = StringUtils.getSpace(indent);
		List<String> list = new ArrayList<String>(getMap().keySet());
		Collections.sort(list);
		for (String key : list) {
			if (buf.length() > 0) {
				buf.append("\n");
			}
			buf.append(strIndent)
				.append(key).append(":");
			Object o = get(key);
			if (o instanceof SObject) {
				((SObject)o).buildString(buf, indent + 2);
			} else if (o instanceof QueryResult) {
				buf.append("\n");
				((QueryResult)o).buildString(buf, indent + 2);
			} else {
				String value = o.toString();
				buf.append(" ").append(value);
			}
		}
	}
	
	@Override
	public SObject newInstance() {
		return (SObject)super.newInstance();
	}
	
	/**
	 * 更新前にこのオブジェクトの内容を検証します。
	 */
	public List<SalesforceException> validate() {
		fieldToMap();
		
		List<SalesforceException> list = new ArrayList<SalesforceException>();
		Iterator<Map.Entry<String, Object>> it = getMap().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof String && ((String)value).length() == 0) {
				it.remove();
				continue;
			} else if (value instanceof QueryResult) {
				continue;
			} else if (value instanceof SObject) {
				SObject child = (SObject)value;
				list.addAll(child.validate());
			} else {
				FieldDef field = objectDef.getField(name);
				if (field != null) {
					SalesforceException ex = field.validate(value);
					if (ex != null) {
						list.add(ex);
					}
				}
			}
		}
		return list;
	}
	
	void fieldToMap() {
		if (this.reflectBase != null) {
			classMap.get(this.getClass()).toMap(this);
		}
	}
	
	private void fieldToMap(String name) {
		if (this.reflectBase != null) {
			Reflector ref = classMap.get(this.getClass());
			Field f = ref.get(name);
			if (f != null) {
				ref.toMap(this, name, f);
			}
		}
	}
	
	private void mapToField() {
		if (this.reflectBase != null) {
			classMap.get(this.getClass()).fromMap(this);
		}
	}
	
	private void mapToField(String name) {
		if (this.reflectBase != null) {
			Reflector ref = classMap.get(this.getClass());
			Field f = ref.get(name);
			if (f != null) {
				ref.fromMap(this, name, f);
			}
		}
	}
	
	public static class Reflector {
		
		private String name;
		private Map<String, Field> map = new HashMap<String, Field>();
		
		public Reflector(String name) {
			this.name = name;
		}
		
		public String getName() { return this.name;}
		
		public void add(String name, Field f) { 
			f.setAccessible(true);
			this.map.put(name, f);
		}
		
		public Field get(String name) {
			return this.map.get(name);
		}
		
		public void fromMap(SObject obj) {
			for (Map.Entry<String, Field> entry : this.map.entrySet()) {
				String name = entry.getKey();
				Field f = entry.getValue();
				fromMap(obj, name, f);
			}
		}
		
		private void fromMap(SObject obj, String name, Field f) {
			Object value = obj.get(name);
			if (value != null) {
				try {
					if (!value.getClass().equals(f.getType()) && Number.class.isAssignableFrom(value.getClass())) {
						if (f.getType().equals(Integer.class) || f.getType().equals(int.class)) {
							f.setInt(obj, ((Number)value).intValue());
						} else if (f.getType().equals(Long.class) || f.getType().equals(long.class)) {
							f.setLong(obj, ((Number)value).longValue());
						} else if (f.getType().equals(Byte.class) || f.getType().equals(byte.class)) {
							f.setByte(obj, ((Number)value).byteValue());
						} else if (f.getType().equals(Double.class) || f.getType().equals(double.class)) {
							f.setDouble(obj, ((Number)value).doubleValue());
						} else if (f.getType().equals(Float.class) || f.getType().equals(float.class)) {
							f.setFloat(obj, ((Number)value).floatValue());
						} else if (f.getType().equals(Short.class) || f.getType().equals(short.class)) {
							f.setShort(obj, ((Number)value).shortValue());
						} else {
							throw new IllegalStateException(value.getClass() + ", " + f.getType());
						}
					} else {
						f.set(obj, value);
					}
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		
		public void toMap(SObject obj) {
			for (Map.Entry<String, Field> entry : this.map.entrySet()) {
				String name = entry.getKey();
				Field f = entry.getValue();
				toMap(obj, name, f);
			}
		}
		
		private void toMap(SObject obj, String name, Field f) {
			try {
				Object value = f.get(obj);
				if (value != null) {
					obj.set(name, value);
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
	}
	
	//Common fields
	public String getName() { return getString("Name");}
	public void setName(String s) { set("Name", s);}
	
	public User getCreatedBy() { return (User)getObject("CreatedBy");}
	
	public String getCreatedById() {
		String id = getString("CreatedById");
		if (id != null) {
			return id;
		}
		User user = getCreatedBy();
		return user != null ? user.getId() : null;
	}
	
	public Date getCreatedDate() { return getDate("CreatedDate");}
	
	public boolean isDeleted() { return getBoolean("IsDeleted");}
	
	public User getLastModifiedBy() { return (User)getObject("LastModifiedBy");}
	
	public String getLastModifiedById() {
		String id = getString("LastModifiedById");
		if (id != null) {
			return id;
		}
		User user = getLastModifiedBy();
		return user != null ? user.getId() : null;
	}
	
	public Date getLastModifiedDate() { return getDate("LastModifiedDate");}
	
	public User getOwner() { return (User)getObject("Owner");}
	
	public String getOwnerId() {
		String id = getString("OwnerId");
		if (id != null) {
			return id;
		}
		User user = getOwner();
		return user != null ? user.getId() : null;
	}
	
	public Date getSystemModstamp() { return getDate("SystemModstamp");}
}
