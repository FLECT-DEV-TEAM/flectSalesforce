package jp.co.flect.salesforce;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.text.Format;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

import jp.co.flect.soap.SimpleObject;
import jp.co.flect.util.Base64;
import jp.co.flect.salesforce.query.QueryResult;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.type.DatetimeType;

public class SObjectToJson {
	
	public static Gson createGson() {
		DatetimeType type = (DatetimeType)SimpleType.getBuiltinType(DatetimeType.NAME);
		return createGson(type.getFormatObject());
	}
	
	public static Gson createGson(Format datetimeFormat) {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(SimpleObject.class, new SimpleObjectSerializer(datetimeFormat));
		builder.registerTypeAdapter(QueryResult.class, new QueryResultSerializer());
		return builder.create();
	}
	
	private static class SimpleObjectSerializer implements JsonSerializer<SimpleObject> {
		
		private Format datetimeFormat;
		
		public SimpleObjectSerializer(Format datetimeFormat) {
			this.datetimeFormat = datetimeFormat;
		}
		
		public JsonElement serialize(SimpleObject src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject ret = new JsonObject();
			for (String name : src.getNameList()) {
				Object value = src.get(name);
				if (value == null) {
					continue;
				} else if (value instanceof String) {
					ret.addProperty(name, (String)value);
				} else if (value instanceof Number) {
					ret.addProperty(name, (Number)value);
				} else if (value instanceof Boolean) {
					ret.addProperty(name, (Boolean)value);
				} else if (value instanceof byte[]) {
					ret.addProperty(name, Base64.encode((byte[])value));
				} else if (value instanceof Date) {
					Date d = (Date)value;
					SimpleType type = null;
					if (src instanceof SObject) {
						SObjectDef od = ((SObject)src).getObjectDef();
						if (od != null && od.getField(name) != null) {
							type = od.getField(name).getSoapType();
						}
					}
					if (type == null && src.getType() != null) {
						Iterator<ElementDef> it = src.getType().modelIterator();
						while (it.hasNext()) {
							ElementDef el = it.next();
							if (el.getName().equals(name) && el.getType().isSimpleType()) {
								type = (SimpleType)el.getType();
								break;
							}
						}
					}
					
					if (type == null || type.getName().equals(DatetimeType.NAME)) {
						value = this.datetimeFormat.format(d);
					} else {
						value = type.format(d);
					}
					ret.addProperty(name, (String)value);
				} else if (value instanceof List) {
					ret.add(name, context.serialize(value));
				} else if (value instanceof SObject) {
					ret.add(name, context.serialize(value));
				} else if (value instanceof QueryResult) {
					QueryResult qr = (QueryResult)value;
					if (qr.getAllSize() > 0) {
						ret.add(name, context.serialize(qr));
					}
				} else {
					throw new IllegalStateException(value.getClass().getName() + ": " + value.toString());
				}
			}
			if (src.getOptimizeLevel() == SimpleObject.OptimizeLevel.ROBUST && src.getType() != null) {
				ComplexType ct = (ComplexType)src.getType();
				Iterator<ElementDef> it = ct.modelIterator();
				while (it.hasNext()) {
					ElementDef el = it.next();
					if (!el.getType().isSimpleType()) {
						continue;
					}
					SimpleType st = (SimpleType)el.getType();
					if (st.isBooleanType()) {
						if (src.get(el.getName()) == null) {
							ret.addProperty(el.getName(), Boolean.FALSE);
						}
					} else if (st.isNumberType()) {
						if (src.get(el.getName()) == null) {
							ret.addProperty(el.getName(), Integer.valueOf(0));
						}
					}
				}
			}
			return ret;
		}
	}
	
	private static class QueryResultSerializer implements JsonSerializer<QueryResult<?>> {
		public JsonElement serialize(QueryResult<?> src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject ret = new JsonObject();
			ret.addProperty("done", src.isDone());
			if (src.getQueryLocator() != null) {
				ret.addProperty("queryLocator", src.getQueryLocator());
			}
			ret.addProperty("size", src.getAllSize());
			ret.add("records", context.serialize(src.getRecords()));
			return ret;
		}
	}
}
