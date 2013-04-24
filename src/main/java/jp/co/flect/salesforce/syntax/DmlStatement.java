package jp.co.flect.salesforce.syntax;

import java.io.IOException;
import java.text.ParseException;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.soap.SoapException;
import jp.co.flect.xmlschema.SimpleType;

public abstract class DmlStatement {
	
	public enum ValueType {
		STRING,
		NUMBER,
		BOOLEAN,
		UNKNOWN
	};
	
	protected String objectName;
	
	public abstract String getStatement();
	public String getObjectName() { return this.objectName;}
	
	protected void checkToken(Tokenizer t, StringBuilder buf, String str) throws ParseException {
		int n = t.next(buf);
		if (n == Tokenizer.T_END) {
			throw new ParseException(t.getString(), t.getPrevIndex());
		}
		if (!str.equalsIgnoreCase(buf.toString())) {
			throw new ParseException(t.getString(), t.getPrevIndex());
		}
	}
	
	protected Value nextValue(Tokenizer t, StringBuilder buf) throws ParseException {
		int n = t.next(buf);
		String str = buf.toString();
		ValueType type = null;
		if (n == Tokenizer.T_STRING) {
			type = ValueType.STRING;
		} else if (n == Tokenizer.T_BOOLEAN) {
			type = ValueType.BOOLEAN;
		} else if (n == Tokenizer.T_NUMBER) {
			type = ValueType.NUMBER;
		} else if (n == Tokenizer.T_LITERAL) {
			type = ValueType.UNKNOWN;
		} else {
			throw new ParseException(t.getString(), t.getPrevIndex());
		} 
		return new Value(type, str);
	}
	
	protected boolean checkType(FieldDef fd, Value value) {
		SimpleType type = fd.getSoapType();
		if (type.isStringType()) {
			return value.getType() == ValueType.STRING;
		}
		if (type.isNumberType()) {
			return value.getType() == ValueType.NUMBER;
		}
		if (type.isBooleanType()) {
			return value.getType() == ValueType.BOOLEAN;
		}
		//ToDo datetime
		return true;
	}
	
	public abstract DmlResult execute(SalesforceClient client) throws IOException, SoapException;
	
	public static class Value {
		
		private ValueType type;
		private String str;
		
		public Value(ValueType type, String str) {
			this.type = type;
			this.str = str;
		}
		
		public ValueType getType() { return type;}
		public String getString() { return str;}
		
		public String toString() {
			if (type == ValueType.STRING) {
				return "'" + this.str + "'";
			} else {
				return this.str;
			}
		}
	}
}
