package jp.co.flect.salesforce.apex;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jp.co.flect.salesforce.annotation.ApexField;
import jp.co.flect.soap.WSDL;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.XMLSchema;


class ApexConverter {
	
	private WSDL wsdl;
	private Map<String, Reflector> refMap = new Hashtable<String, Reflector>();
	
	public ApexConverter(WSDL wsdl) {
		this.wsdl = wsdl;
	}
	
	private Reflector getReflector(ApexObject obj) { 
		Reflector ret = refMap.get(obj.getObjectName());
		if (ret == null) {
			ret = new Reflector(obj.getClass());
			refMap.put(obj.getObjectName(), ret);
		}
		return ret;
	}
	
	public <T extends ApexObject> void addApexClass(String objectName, Class<T> c) {
		refMap.put(objectName, new Reflector(c));
	}
	
	public boolean hasApexClass(String objectName) {
		return refMap.get(objectName) != null;
	}
	
	private ComplexType getType(String name) {
		for (XMLSchema schema : this.wsdl.getSchemaList()) {
			TypeDef ret = schema.getType(name);
			if (ret != null) {
				return (ComplexType)ret;
			}
		}
		return null;
	}
	
	public Map<String, String> toMap(ApexObject obj) {
		Reflector ref = getReflector(obj);
		return ref.toMap(obj);
	}
	
	public ApexObject toObject(String objectName, XMLStreamReader reader) throws XMLStreamException {
		Reflector ref = refMap.get(objectName);
		ComplexType type = getType(objectName);
		if (ref == null || type == null) {
			throw new IllegalArgumentException(objectName);
		}
		return ref.toObject(type, reader);
	}
	
	private class Reflector {
		
		private Class clazz;
		private Map<String, Field> fieldMap = new HashMap<String, Field>();
		private Map<String, MethodInfo> methodMap = new HashMap<String, MethodInfo>();
		
		public Reflector(Class c) {
			this.clazz = c;
			Field[] fields = c.getDeclaredFields();
			for (Field f : fields) {
				String fieldName = f.getName();
				ApexField api = f.getAnnotation(ApexField.class);
				if (api != null) {
					String apiName = api.value();
					if (apiName == null || apiName.length() == 0) {
						apiName = fieldName;
					}
					f.setAccessible(true);
					this.fieldMap.put(apiName, f);
				}
			}
			Method[] methods = c.getDeclaredMethods();
			for (Method m : methods) {
				String fieldName = m.getName();
				ApexField api = m.getAnnotation(ApexField.class);
				if (api != null) {
					String apiName = api.value();
					if (apiName == null || apiName.length() == 0) {
						apiName = fieldName;
					}
					MethodInfo info = this.methodMap.get(apiName);
					if (info == null) {
						info = new MethodInfo();
						this.methodMap.put(apiName, info);
					}
					info.set(m);
					m.setAccessible(true);
				}
			}
		}
		
		public Map<String, String> toMap(ApexObject obj) {
			ComplexType type = getType(obj.getObjectName());
			if (type == null) {
				throw new IllegalArgumentException("Unknown type: " + obj.getObjectName());
			}
			Map<String, String> map = new HashMap<String, String>();
			for (Map.Entry<String, Field> entry : this.fieldMap.entrySet()) {
				String name = entry.getKey();
				ElementDef el = type.getModel(type.getNamespace(), name);
				if (el == null) {
					throw new IllegalArgumentException("Unknown fieldName: " + obj.getObjectName() + "." + name);
				}
				Field f = entry.getValue();
				try {
					Object value = f.get(obj);
					if (value != null) {
						putValue(map, name, value, el);
					}
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
			for (Map.Entry<String, MethodInfo> entry : this.methodMap.entrySet()) {
				String name = entry.getKey();
				MethodInfo info = entry.getValue();
				if (info.getter == null) {
					continue;
				}
				Method m = info.getter;
				ElementDef el = type.getModel(type.getNamespace(), name);
				if (el == null) {
					throw new IllegalArgumentException("Unknown fieldName: " + obj.getObjectName() + "." + name);
				}
				try {
					Object value = m.invoke(obj);
					if (value != null) {
						putValue(map, name, value, el);
					}
				} catch (InvocationTargetException e) {
					throw new IllegalStateException(e);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
			return map;
		}
		
		public ApexObject toObject(ComplexType type, XMLStreamReader reader) throws XMLStreamException {
			try {
				ApexObject ret = (ApexObject)this.clazz.newInstance();
				
				boolean hasValue = false;
				boolean endValue = false;
				while (reader.hasNext()) {
					int event = reader.next();
					switch (event) {
						case XMLStreamReader.START_ELEMENT:
							hasValue = true;
							String nsuri = reader.getNamespaceURI();
							String name = reader.getLocalName();
							ElementDef el = type.getModel(nsuri, name);
							if (el == null) {
								throw new IllegalStateException("Unknown element: " + nsuri + ", " + name);
							}
							TypeDef elType = el.getType();
							if (elType.isSimpleType()) {
								Object value = parseSimple((SimpleType)elType, reader);
								if (value != null) {
									setValue(ret, name, value);
								}
							} else {
								throw new IllegalStateException("Complex type is not implemented.: " + nsuri + ", " + name);
							}
							break;
						case XMLStreamReader.CHARACTERS:
						case XMLStreamReader.CDATA:
							if (!reader.isWhiteSpace()) {
								throw new IllegalStateException();
							}
							break;
						case XMLStreamReader.END_ELEMENT:
							endValue = true;
							break;
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
					if (endValue) {
						break;
					}
				}
				return hasValue ? ret : null;
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			} catch (InstantiationException e) {
				throw new IllegalStateException(e);
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
		private void putValue(Map<String, String> map, String name, Object value, ElementDef el) {
			//ToDo complexType
			SimpleType type = (SimpleType)el.getType();
			map.put(name, type.format(value));
		}
		
		private void setValue(ApexObject obj, String name, Object value) {
			try {
				Field f = fieldMap.get(name);
				if (f != null) {
					value = convertType(f.getType(), value);
					f.set(obj, value);
					return;
				}
				MethodInfo info = methodMap.get(name);
				if (info != null && info.setter != null) {
					Method m = info.setter;
					value = convertType(m.getParameterTypes()[0], value);
					m.invoke(obj, value);
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalStateException(e);
			}
		}
		
		private Object convertType(Class c, Object value) {
			if (c == String.class) return value.toString();
			if (value instanceof Number) {
				Number n = (Number)value;
				if (c == Integer.class) return n.intValue();
				if (c == Long.class) return n.longValue();
				if (c == Double.class) return n.doubleValue();
				if (c == Float.class) return n.floatValue();
				if (c == Byte.class) return n.byteValue();
				if (c == Short.class) return n.shortValue();
				if (c == BigDecimal.class) return new BigDecimal(n.toString());
				if (c == BigInteger.class) return new BigInteger(n.toString());
 			}
			return value;
		}
	}
	
	private static class MethodInfo {
		
		public Method getter;
		public Method setter;
		
		public void set(Method m) {
			if (isGetter(m)) {
				this.getter = m;
			} else if (isSetter(m)) {
				this.setter = m;
			} else {
				throw new IllegalStateException("Invalid method: " + m.getName());
			}
		}
		
		private boolean isGetter(Method m) {
			Class rt = m.getReturnType();
			if (rt == null || rt == Void.TYPE) {
				return false;
			}
			Class[] params = m.getParameterTypes();
			return params == null || params.length == 0;
		}
		
		private boolean isSetter(Method m) {
			Class[] params = m.getParameterTypes();
			return params != null || params.length == 1;
		}
	}
}
