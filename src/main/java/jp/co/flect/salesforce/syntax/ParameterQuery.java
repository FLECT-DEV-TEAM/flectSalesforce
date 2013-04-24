package jp.co.flect.salesforce.syntax;

import java.math.BigDecimal;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.type.DateType;
import jp.co.flect.xmlschema.type.DatetimeType;
import jp.co.flect.xmlschema.type.TimeType;

public class ParameterQuery {
	
	public enum QueryType {
		SELECT,
		INSERT,
		UPDATE,
		UPSERT,
		DELETE,
		CREATE,
		DROP,
		ALTER,
		UNKNOWN
		;
		
		public boolean isDml() {
			return this == INSERT || this == UPDATE || this == UPSERT || this == DELETE;
		}
		
		public boolean isDdl() {
			return this == CREATE || this == DROP || this == ALTER;
		}
	};
	
	public enum ParameterType {
		TEXT,
		TEXTAREA,
		DATE,
		DATETIME,
		TIME,
		CHECKBOX,
		LITERAL,
		NUMBER,
		UNKNOWN
	};
	
	private String origin;
	private QueryType queryType;
	private String firstToken;
	private Map<String, Parameter> map = new LinkedHashMap<String, Parameter>();
	
	public ParameterQuery(String str) {
		this.origin = str;
		
		Tokenizer t = new Tokenizer(str);
		StringBuilder buf = new StringBuilder();
		
		int n = t.next(buf);
		this.firstToken = buf.toString();
		try {
			this.queryType = QueryType.valueOf(this.firstToken.toUpperCase());
		} catch (IllegalArgumentException e) {
			this.queryType = QueryType.UNKNOWN;
		}
		
		int paramIndex = 0;
		while (n != Tokenizer.T_END) {
			if (n == Tokenizer.T_LITERAL && buf.charAt(0) == ':') {
				Parameter p = parseParameter(buf.substring(1), paramIndex); 
				this.map.put(p.getName(), p);
			}
			n = t.next(buf);
		}
	}
	
	public String getOriginalQuery() { return this.origin;}
	public QueryType getQueryType() { return this.queryType;}
	
	public String getFirstToken() { return this.firstToken;}
	
	public String getParameterQuery() {
		if (!isParameterResolved()) {
			throw new IllegalArgumentException();
		}
		
		StringBuilder ret = new StringBuilder();
		Tokenizer t = new Tokenizer(this.origin);
		StringBuilder buf = new StringBuilder();
		boolean space = false;
		int paramIndex = 0;
		int n = t.next(buf);
		while (n != Tokenizer.T_END) {
			switch (n) {
				case Tokenizer.T_COMMA:
				case Tokenizer.T_OPEN_BRACKET:
				case Tokenizer.T_CLOSE_BRACKET:
					ret.append(buf);
					space = false;
					break;
				case Tokenizer.T_LITERAL:
				{
					String str = buf.toString();
					if (buf.charAt(0) == ':') {
						Parameter p = parseParameter(buf.substring(1), paramIndex);
						p = this.map.get(p.getName());
						str = p.getValue();
						if (p.getType() == ParameterType.TEXT || p.getType() == ParameterType.TEXTAREA) {
							str = "'" + Tokenizer.escapeQuotedString(str) + "'";
						}
					}
					if (space) {
						ret.append(" ");
					}
					ret.append(str);
					space = true;
					break;
				}
				case Tokenizer.T_STRING:
					if (space) {
						ret.append(" ");
					}
					ret.append("'").append(Tokenizer.escapeQuotedString(buf.toString())).append("'");
					space = true;
					break;
				case Tokenizer.T_NUMBER:
				case Tokenizer.T_BOOLEAN:
					if (space) {
						ret.append(" ");
					}
					ret.append(buf);
					space = true;
					break;
				case Tokenizer.T_ERROR:
					//Errorチェックはこのクラスの範囲外
					break;
				default:
					throw new IllegalStateException();
			}
			n = t.next(buf);
		}
		return ret.toString();
	}
	
	public List<Parameter> getParameterList() {
		return new ArrayList<Parameter>(this.map.values());
	}
	
	private Parameter parseParameter(String str, int paramIndex) {
		String name = str;
		ParameterType type = ParameterType.TEXT;
		int idx = str.indexOf(':');
		if (idx != -1) {
			name = str.substring(0, idx);
			try {
				type = ParameterType.valueOf(str.substring(idx+1).toUpperCase());
			} catch (IllegalArgumentException e) {
				type = ParameterType.UNKNOWN;
			}
		} 
		if (name.length() == 0) {
			name = "param" + Integer.toString(++paramIndex);
		}
		return new Parameter(name, type);
	}
	
	public boolean hasParameter() {
		return this.map.size() > 0;
	}
	
	public boolean setParameterValue(String name, String value) {
		Parameter p = this.map.get(name);
		if (p == null) {
			return false;
		}
		
		String schemaType = null;
		switch (p.type) {
			case DATE:
				schemaType = DateType.NAME;
				break;
			case DATETIME:
				schemaType = DatetimeType.NAME;
				break;
			case TIME:
				schemaType = TimeType.NAME;
				break;
			case CHECKBOX:
				if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
					throw new IllegalArgumentException("Invalid boolean: " + value);
				}
				break;
			case NUMBER:
				try {
					new BigDecimal(value);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid number: " + value);
				}
				break;
		}
		if (schemaType != null && !SimpleType.getBuiltinType(schemaType).isValid(value)) {
			throw new IllegalArgumentException("Invalid " + p.type.toString().toLowerCase() + ": " + value);
		}
		p.setValue(value);
		return true;
	}
	
	public boolean isParameterResolved() {
		for (Parameter p : this.map.values()) {
			if (p.getValue() == null) {
				return false;
			}
		}
		return true;
	}
	
	public static class Parameter {
		
		private String name;
		private ParameterType type;
		private String value;
		
		public Parameter(String name, ParameterType type) {
			this.name = name;
			this.type = type;
		}
		
		public String getName() { return this.name;}
		public ParameterType getType() { return this.type;}
		
		public String getValue() { return this.value;}
		public void setValue(String s) { this.value = s;}
	}
}
