package jp.co.flect.salesforce.syntax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.text.ParseException;
import jp.co.flect.util.SimpleJson;
import jp.co.flect.salesforce.metadata.AsyncResult;
import jp.co.flect.salesforce.metadata.BaseMetadata;
import jp.co.flect.salesforce.metadata.CustomField;
import jp.co.flect.salesforce.metadata.MetadataClient;
import jp.co.flect.salesforce.metadata.MetadataType;
import jp.co.flect.salesforce.metadata.UpdateMetadata;
import jp.co.flect.soap.SoapException;
import com.google.gson.JsonSyntaxException;

public class AlterStatement extends DdlStatement {
	
	private enum MethodType {
		ADD,
		MODIFY,
		DROP,
		RENAME,
		RENAME_COLUMN
	};
	
	private MethodType methodType;
	
	private LinkedList<UpdateMetadata> updates;
	private LinkedList<CustomField> fields;
	
	public AlterStatement(String str) throws ParseException {
		super("alter");
		StringBuilder buf = new StringBuilder();
		Tokenizer t = new Tokenizer(str);
		
		checkToken(t, buf, getStatementName());
		MetadataType type = checkType(t, buf);
		
		String objectName = t.nextLiteral(buf);
		MethodType methodType = checkMethodType(t, buf);
		
		switch (methodType) {
			case ADD:
			case MODIFY:
			case DROP:
				parseJson(objectName, t, buf);
				break;
			case RENAME:
			{
				String newName = t.nextLiteral(buf);
				this.updates = new LinkedList<UpdateMetadata>();
				BaseMetadata meta = new BaseMetadata(type);
				meta.setFullName(newName);
				this.updates.add(new UpdateMetadata(objectName, meta));
				break;
			}
			case RENAME_COLUMN:
			{
				String oldName = t.nextLiteral(buf);
				if (!t.nextLiteral(buf).equalsIgnoreCase("to")) {
					throw new ParseException(buf.toString(), t.getPrevIndex());
				}
				String newName = t.nextLiteral(buf);
				this.updates = new LinkedList<UpdateMetadata>();
				BaseMetadata meta = new CustomField();
				meta.setFullName(objectName + "." + newName);
				this.updates.add(new UpdateMetadata(objectName + "." + oldName, meta));
				break;
			}
		}
	}
	
	public MethodType getMethodType() { return this.methodType;}
	
	private MethodType checkMethodType(Tokenizer t, StringBuilder buf) throws ParseException {
		String str = t.nextLiteral(buf);
		for (MethodType mt : MethodType.values()) {
			if (mt.name().equalsIgnoreCase(str)) {
				if (mt == MethodType.RENAME) {
					String next = t.nextLiteral(buf);
					if ("column".equalsIgnoreCase(next)) {
						mt = MethodType.RENAME_COLUMN;
					} else if (!"to".equalsIgnoreCase(next)) {
						throw new ParseException(next, t.getPrevIndex());
					}
				}
				this.methodType = mt;
				return mt;
			}
		}
		throw new ParseException("Unknown alter method: " + str, t.getPrevIndex());
	}
	
	private void parseJson(String objectName, Tokenizer t, StringBuilder buf) throws ParseException {
		String str = t.getString().substring(t.getIndex());
		try {
			SimpleJson json = SimpleJson.fromJson(str);
			List list = (List)json.remove("fields");
			switch (this.methodType) {
				case ADD:
				case DROP:
					if (list == null) {
						throw new ParseException("fields not found", t.getIndex());
					}
					if (json.size() > 0) {
						throw new ParseException("invalid element: " + json, t.getIndex());
					}
					this.fields = new LinkedList<CustomField>();
					for (Object o : list) {
						CustomField field = new CustomField();
						field.getMap().putAll((Map)o);
						field.setFullName(objectName + "." + field.getFullName());
						this.fields.add(field);
					}
					break;
				case MODIFY:
					this.updates = new LinkedList<UpdateMetadata>();
					if (json.size() > 0) {
						BaseMetadata meta = new BaseMetadata(getMetadataType());
						meta.setFullName(objectName);
						meta.getMap().putAll(json);
						this.updates.add(new UpdateMetadata(objectName, meta));
					}
					if (list != null) {
						for (Object o : list) {
							CustomField field = new CustomField();
							field.getMap().putAll((Map)o);
							field.setFullName(objectName + "." + field.getFullName());
							this.updates.add(new UpdateMetadata(field.getFullName(), field));
						}
					}
					break;
			}
		} catch (JsonSyntaxException e) {
			ParseException e2 = new ParseException(e.getMessage(), t.getIndex());
			e2.initCause(e);
			throw e2;
		}
	}
	
	@Override
	public List<AsyncResult> execute(MetadataClient client) throws IOException, SoapException {
		switch (methodType) {
			case ADD:
				return doAdd(client);
			case MODIFY:
			case RENAME:
			case RENAME_COLUMN:
				return doModify(client);
			case DROP:
				return doDrop(client);
			default:
				throw new IllegalStateException();
		}
	}
	
	private List<AsyncResult> doAdd(MetadataClient client) throws IOException, SoapException {
		List<AsyncResult> ret = new ArrayList<AsyncResult>();
		List<CustomField> list = new ArrayList<CustomField>();
		while (this.fields.size() > 0) {
			while (this.fields.size() > 0 && list.size() < MetadataClient.MAX_REQUEST_COUNT) {
				list.add(this.fields.removeFirst());
			}
			ret.addAll(client.create(list));
			list.clear();
		}
		return ret;
	}
	
	private List<AsyncResult> doDrop(MetadataClient client) throws IOException, SoapException {
		List<AsyncResult> ret = new ArrayList<AsyncResult>();
		List<CustomField> list = new ArrayList<CustomField>();
		while (this.fields.size() > 0) {
			while (this.fields.size() > 0 && list.size() < MetadataClient.MAX_REQUEST_COUNT) {
				list.add(this.fields.removeFirst());
			}
			ret.addAll(client.delete(list));
			list.clear();
		}
		return ret;
	}
	
	private List<AsyncResult> doModify(MetadataClient client) throws IOException, SoapException {
		List<AsyncResult> ret = new ArrayList<AsyncResult>();
		List<UpdateMetadata> list = new ArrayList<UpdateMetadata>();
		while (this.updates.size() > 0) {
			while (this.updates.size() > 0 && list.size() < MetadataClient.MAX_REQUEST_COUNT) {
				list.add(this.updates.removeFirst());
			}
			ret.addAll(client.update(list));
			list.clear();
		}
		return ret;
	}
}
